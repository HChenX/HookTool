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
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.core.CoreTool;
import com.hchen.hooktool.hook.AbsHook;
import com.hchen.hooktool.hook.HookRegistry;
import com.hchen.hooktool.log.AndroidLog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * Xposed 模块的启动入口基类，继承自 {@link XposedModule}。
 * <p>
 * 该类作为框架回调与业务逻辑之间的桥梁，负责管理从模块加载到 Application 创建
 * 的完整生命周期流程。子类必须实现 {@link #initModuleConfig()} 完成配置初始化，
 * 可选择性覆写 {@link #ignorePackages()} 排除不需要处理的目标包。
 * <p>
 * 当目标应用的 {@link Application#attach(Context)} 被调用时，框架会自动
 * 触发 {@link #handleApplicationCreated(Context)} 回调。
 * <p>
 * 从 API 102 开始支持热更新（Hot Reload）：
 * <ul>
 *   <li>{@link #handleHotReloading(Bundle)} — 热更新前在旧代码中执行，返回需保存的状态数据</li>
 *   <li>{@link #handleHotReloadingFailed(Throwable)} — 热更新准备阶段发生异常时的回调</li>
 *   <li>{@link #handleHotReloaded(HotReloadedParam, ClassLoader)} — 热更新完成后在新代码中执行，
 *       携带恢复的 ClassLoader，自动解除旧 Hook 并重新分发状态</li>
 * </ul>
 *
 * @author 焕晨HChen
 * @see AbsModule
 * @see ModuleConfig
 * @see XposedModuleInterface
 */
public abstract class ModuleEntrance extends XposedModule {
    // 标记当前包是否应被跳过处理
    private volatile boolean shouldSkip = false;
    private volatile String processName = "";

    /**
     * 初始化模块配置的抽象方法。
     * <p>
     * 子类必须实现此方法，在其中通过 {@link ModuleConfig} 的静态方法完成
     * 日志标签、日志等级、SharedPreferences 名称等基本参数的设置。
     * 此方法在 {@link #onModuleLoaded(ModuleLoadedParam)} 回调中最先被调用。
     */
    public abstract void initModuleConfig();

    /**
     * 返回需要跳过处理的目标包名列表。
     * <p>
     * 当目标应用的包名与列表中任一项匹配时，后续的
     * {@link #handlePackageLoaded(PackageLoadedParam)}、
     * {@link #handlePackageReady(PackageReadyParam)} 以及
     * {@link #handleApplicationCreated(Context)} 回调将被自动跳过。
     *
     * @return 需要忽略的包名数组，默认返回空数组表示不跳过任何包
     */
    @NonNull
    public String[] ignorePackages() {
        return new String[]{};
    }

    /**
     * 模块加载完成时的回调。
     * <p>
     * Xposed 框架完成模块加载后触发。子类可覆写此方法执行模块级别的初始化操作，
     * 例如注册全局 Hook 或初始化共享资源。
     *
     * @param param 模块加载参数，包含框架相关信息
     */
    public void handleModuleLoaded(@NonNull ModuleLoadedParam param) {
    }

    /**
     * 目标应用包加载时的回调。
     * <p>
     * 当目标应用的包被框架加载时触发。子类可覆写此方法在包加载阶段
     * 提前进行 Hook 准备。
     *
     * @param param 包加载参数，包含目标包的相关信息
     */
    public void handlePackageLoaded(@NonNull PackageLoadedParam param) {
    }

    /**
     * 目标应用包资源就绪时的回调。
     * <p>
     * 当目标应用包的资源完成加载并就绪后触发。子类可覆写此方法在
     * 包就绪阶段执行需要完整包资源的 Hook 操作。
     *
     * @param param 包就绪参数，包含目标包的相关信息
     */
    public void handlePackageReady(@NonNull PackageReadyParam param) {
    }

    /**
     * 目标应用 Application 创建时的回调。
     * <p>
     * 当目标应用的 {@link Application#attach(Context)} 被调用时触发，
     * 此时应用 {@link Context} 已可用。子类可覆写此方法执行依赖应用上下文的 Hook 逻辑。
     *
     * @param context 目标应用的上下文对象
     */
    public void handleApplicationCreated(@NonNull Context context) {
    }

    /**
     * 系统服务器启动时的回调。
     * <p>
     * 当 Android 系统服务器进程启动时触发。子类可覆写此方法执行针对系统服务的 Hook 操作。
     *
     * @param param 系统服务器启动参数
     */
    public void handleSystemServerStarting(@NonNull SystemServerStartingParam param) {
    }

    /**
     * 模块即将被热更新时触发的回调（在旧代码中执行）。
     * <p>
     * 此回调在热更新触发时于旧模块代码中运行，子类应覆写此方法返回需要
     * 在热更新后恢复的状态键值对。返回的 {@link Map} 会被
     * {@link ModuleEntrance#onHotReloading(HotReloadingParam)} 合并到全局快照中，
     * 并通过 {@code param.setSavedInstanceState(merged)} 持久化。
     * <p>
     * 注意：该方法不再直接控制是否允许热更新——允许/拒绝逻辑统一由
     * {@code onHotReloading} 的顶层实现管理。子类仅需关心状态数据的保存。
     *
     * @param extras 热更新触发的附加数据 {@link Bundle}，可能为 {@code null}
     *               （当框架未传递额外数据时）
     * @return 模块级状态键值对的 {@link Map}；默认返回空 {@link HashMap}，
     *         表示无需保存任何状态
     * @see #handleHotReloaded(HotReloadedParam, ClassLoader)
     * @see HookRegistry#reloading(Bundle)
     */
    @NonNull
    public Map<String, Object> handleHotReloading(@Nullable Bundle extras) {
        return new HashMap<>();
    }

    /**
     * 热更新准备阶段发生异常时的回调。
     * <p>
     * 当 {@link #handleHotReloading(Bundle)} 或其内部钩子的状态收集过程
     * 抛出异常时触发。子类可覆写此方法执行资源清理或异常记录。
     * <p>
     * 注意：此回调执行完毕后，异常会通过 {@link CoreTool#throwIt(Throwable)}
     * 继续向上传播，最终 {@code onHotReloading} 返回 {@code false} 拒绝热更新。
     *
     * @param throwable 在热更新准备阶段被捕获的异常实例，不为 {@code null}
     * @see #handleHotReloading(Bundle)
     */
    public void handleHotReloadingFailed(Throwable throwable) {
    }

    /**
     * 模块已完成热更新时触发的回调（在新代码中执行）。
     * <p>
     * 此回调在热更新完成后于新模块代码中运行，此时已从旧代码保存的状态中
     * 恢复了宿主应用的 {@link ClassLoader}（通过 {@link ModuleData#setClassLoader(ClassLoader)}）。
     * 子类可覆写此方法执行重新挂钩或特定的初始化操作。
     * <p>
     * 旧 Hook 句柄的解除由 {@code onHotReloaded} 的 {@code finally} 块统一处理，
     * 子类无需手动解除。
     *
     * @param param       热更新完成参数，包含旧 Hook 句柄、保存的状态等信息，不为 {@code null}
     * @param classLoader 从旧代码保存的状态中恢复的宿主应用 ClassLoader，不为 {@code null}
     * @see #handleHotReloading(Bundle)
     * @see ModuleData#MODULE_HOST_CLASSLOADER
     */
    public void handleHotReloaded(@NonNull HotReloadedParam param, @NonNull ClassLoader classLoader) {
    }

    // ------------------------- Inner -----------------------------
    @Override
    public final void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        processName = param.getProcessName();
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

    @Override
    public final boolean onHotReloading(@NonNull HotReloadingParam param) {
        try {
            Map<String, Object> merged = new HashMap<>();

            merged.putAll(handleHotReloading(param.getExtras()));
            merged.putAll(HookRegistry.reloading(param.getExtras()));
            merged.put(ModuleData.MODULE_HOST_CLASSLOADER, ModuleData.getClassLoader());

            param.setSavedInstanceState(merged);
            return true;
        } catch (Throwable throwable) {
            handleHotReloadingFailed(throwable);
            CoreTool.throwIt(throwable);
            return false;
        }
    }

    @Override
    public final void onHotReloaded(@NonNull HotReloadedParam param) {
        try {
            processName = param.getProcessName();
            ModuleData.setXposedEnvironment(true);
            ModuleData.setWrapper(this);

            initModuleConfig();

            ClassLoader classLoader = null;
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) param.getSavedInstanceState();
            if (map != null) {
                classLoader = (ClassLoader) map.get(ModuleData.MODULE_HOST_CLASSLOADER);
            }
            Objects.requireNonNull(classLoader);
            handleHotReloaded(param, classLoader);
            HookRegistry.reloaded(param);
        } finally {
            for (HookHandle handle : param.getOldHookHandles()) {
                handle.unhook();
            }
        }
    }

    private volatile HookHandle handle;

    private void hookApplication(@NonNull PackageLoadedParam param) {
        if (param.isFirstPackage()) {
            if (TextUtils.equals(param.getPackageName(), processName)) {
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
}
