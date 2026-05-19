/*
 * This file is part of HookTool.
 *
 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * HookTool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with HookTool. If not, see <https://www.gnu.org/licenses/lgpl-2.1>.
 *
 * Copyright (C) 2024–2026 HChenX
 */
package com.hchen.hooktool;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.hchen.hooktool.core.CoreTool;
import com.hchen.hooktool.hook.AbsHook;
import com.hchen.hooktool.log.AndroidLog;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

import io.github.libxposed.api.XposedModule;

/**
 * Xposed 模块入口基类。
 * <p>
 * 推荐模块实现此类作为入口点。继承自 {@link XposedModule}，提供模块的完整生命周期管理，
 * 包括模块加载、包加载、包就绪、系统服务器启动以及应用创建等阶段。
 * <p>
 * 子类必须实现 {@link #initModuleConfig()} 方法来初始化模块配置。
 * 可通过 {@link #ignorePackages()} 方法指定需要跳过的包名列表。
 * <p>
 * 当目标应用的 {@link Application#attach(Context)} 被调用时，会自动触发
 * {@link #handleApplicationCreated(Context)} 回调。
 *
 * @author 焕晨HChen
 * @see AbsModule
 * @see ModuleConfig
 */
public abstract class ModuleEntrance extends XposedModule {
    // 跳过被 ignorePackages 匹配上的包
    private volatile boolean shouldSkip = false;

    /**
     * 初始化模块配置。
     * <p>
     * 子类必须实现此方法，在其中调用 {@link ModuleConfig} 的相关方法来设置日志标签、
     * 日志等级、共享首选项名称等基本配置。此方法在 {@link #onModuleLoaded(ModuleLoadedParam)} 中被调用。
     */
    public abstract void initModuleConfig();

    /**
     * 返回需要跳过的包名列表。
     * <p>
     * 当目标包名匹配列表中的任意一项时，后续的 {@link #handlePackageLoaded(PackageLoadedParam)}、
     * {@link #handlePackageReady(PackageReadyParam)} 以及 {@link #handleApplicationCreated(Context)}
     * 回调将被跳过。
     *
     * @return 需要忽略的包名数组，默认为空数组
     */
    @NonNull
    public String[] ignorePackages() {
        return new String[]{};
    }

    /**
     * 模块加载回调。
     * <p>
     * 当模块被 Xposed 框架加载时调用。子类可重写此方法以执行模块级别的初始化逻辑。
     *
     * @param param 模块加载参数
     */
    public void handleModuleLoaded(@NonNull ModuleLoadedParam param) {
    }

    /**
     * 包加载回调。
     * <p>
     * 当目标应用的包被加载时调用。子类可重写此方法以在包加载阶段执行 Hook 逻辑。
     *
     * @param param 包加载参数
     */
    public void handlePackageLoaded(@NonNull PackageLoadedParam param) {
    }

    /**
     * 包就绪回调。
     * <p>
     * 当目标应用的包就绪时调用。子类可重写此方法以在包就绪阶段执行 Hook 逻辑。
     *
     * @param param 包就绪参数
     */
    public void handlePackageReady(@NonNull PackageReadyParam param) {
    }

    /**
     * 应用创建回调。
     * <p>
     * 当目标应用的 {@link Application#attach(Context)} 被调用时触发。
     * 子类可重写此方法以在应用创建阶段执行 Hook 逻辑。
     *
     * @param context 应用上下文
     */
    public void handleApplicationCreated(@NonNull Context context) {
    }

    /**
     * 系统服务器启动回调。
     * <p>
     * 当系统服务器启动时调用。子类可重写此方法以在系统服务器启动阶段执行 Hook 逻辑。
     *
     * @param param 系统服务器启动参数
     */
    public void handleSystemServerStarting(@NonNull SystemServerStartingParam param) {
    }

    // ------------------------- Inner -----------------------------
    @Override
    public final void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        ModuleData.setXposedEnvironment(true);
        ModuleData.setWrapper(this);

        initModuleConfig();
        handleModuleLoaded(param);
    }

    @Override
    public final void onPackageLoaded(@NonNull PackageLoadedParam param) {
        String[] ignored = ignorePackages();
        if (ignored.length > 0) {
            shouldSkip = Arrays.stream(ignored).anyMatch(
                new Predicate<String>() {
                    @Override
                    public boolean test(String packageName) {
                        return TextUtils.equals(packageName, param.getPackageName());
                    }
                }
            );
        } else {
            shouldSkip = false;
        }

        if (shouldSkip) {
            return;
        }

        handlePackageLoaded(param);
    }

    @Override
    public final void onPackageReady(@NonNull PackageReadyParam param) {
        if (shouldSkip) {
            return;
        }

        hookApplication(param);
        handlePackageReady(param);
    }

    @Override
    public final void onSystemServerStarting(@NonNull SystemServerStartingParam param) {
        handleSystemServerStarting(param);
    }

    private volatile HookHandle handle;

    private void hookApplication(@NonNull PackageLoadedParam param) {
        if (param.isFirstPackage()) {
            if (handle != null) {
                handle.unhook();
                handle = null;
            }

            try {
                handle = CoreTool.hookMethod(
                    Application.class,
                    "attach",
                    Context.class,
                    new AbsHook() {
                        @Override
                        public void before() {
                            Context context = (Context) getArg(0);
                            Objects.requireNonNull(context);
                            handleApplicationCreated(context);
                        }
                    }
                );
            } catch (Throwable e) {
                AndroidLog.logW("ModuleEntrance", "Failed to hook Application.attach()", e);
            }
        }
    }
}
