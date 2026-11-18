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
package com.hchen.hooktool.entry;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.hook.AbsHook;
import com.hchen.hooktool.hook.Hook;
import com.hchen.hooktool.hook.HookRegistry;
import com.hchen.hooktool.host.Host;
import com.hchen.hooktool.log.AndroidLog;
import com.hchen.hooktool.reflect.ReflectEnv;
import com.hchen.hooktool.reflect.Throwables;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.github.libxposed.api.XposedModule;

/**
 * Xposed 模块的启动入口基类，继承自 {@link XposedModule}。
 * <p>
 * 该类作为框架回调与业务逻辑之间的桥梁，负责管理模块完整生命周期流程。
 *
 * @author 焕晨HChen
 */
public abstract class ModuleEntrance extends XposedModule {
    private static final String TAG = "ModuleEntrance";
    private volatile boolean mShouldSkipPackage = false;
    private volatile String mProcessName = "";
    private volatile String mPackageName = "";
    private volatile HookHandle mHandle;

    /**
     * 初始化模块配置的抽象方法。
     * <p>
     * 子类必须实现此方法，在其中通过 {@link com.hchen.hooktool.config.HookConfig} 的静态方法完成日志标签、日志等级、SharedPreferences 名称等基本参数的设置。
     */
    public abstract void loadConfig();

    /**
     * 返回需要跳过处理的目标包名集合。
     *
     * @return 需要跳过的包名集合，默认返回空集合表示不跳过任何包
     */
    @NonNull
    public Set<String> skipPackages() {
        return Collections.emptySet();
    }

    /**
     * 模块加载完成时的回调。
     * <p>
     * Xposed 框架完成模块加载后触发。子类可覆写此方法执行模块级别的初始化操作。
     *
     * @param param 模块加载参数，包含框架相关信息
     */
    public void handleModuleLoaded(@NonNull ModuleLoadedParam param) {
    }

    /**
     * 目标应用包加载时的回调。
     * <p>
     * 当目标应用的包被框架加载时触发。
     *
     * @param param 包加载参数，包含目标包的相关信息
     */
    public void handlePackageLoaded(@NonNull PackageLoadedParam param) {
    }

    /**
     * 目标应用包资源就绪时的回调。
     * <p>
     * 当目标应用包的资源完成加载并就绪后触发。
     *
     * @param param 包就绪参数，包含目标包的相关信息
     */
    public void handlePackageReady(@NonNull PackageReadyParam param) {
    }

    /**
     * 目标应用 Application 创建时的回调。
     * <p>
     * 当目标应用的 {@link Application#attach(Context)} 被调用时触发，此时应用 {@link Context} 已可用。
     *
     * @param context 目标应用的上下文对象
     */
    public void handleApplicationCreated(@NonNull Context context) {
    }

    /**
     * 系统服务器启动时的回调。
     * <p>
     * 当 Android 系统服务器进程启动时触发。
     *
     * @param param 系统服务器启动参数
     */
    public void handleSystemServerStarting(@NonNull SystemServerStartingParam param) {
    }

    /**
     * 模块即将被热更新时触发的回调（在旧代码中执行）。
     * <p>
     * 此回调在热更新触发时于旧模块代码中运行，子类应覆写此方法返回需要在热更新后恢复的状态键值对。
     *
     * @param extras 热更新触发的附加数据 {@link Bundle}，可能为 {@code null}（当框架未传递额外数据时）
     * @return 模块级状态键值对的 {@link Map}；默认返回空 {@link HashMap}，表示无需保存任何状态
     */
    @NonNull
    public Map<String, Object> handleHotReloading(@Nullable Bundle extras) {
        return new HashMap<>();
    }

    /**
     * 热更新准备阶段发生异常时的回调。
     * <p>
     * 当 {@link #handleHotReloading(Bundle)} 或其内部钩子的状态收集过程抛出异常时触发。
     *
     * @param throwable 在热更新准备阶段被捕获的异常实例
     */
    public void handleHotReloadingFailed(@NonNull Throwable throwable) {
    }

    /**
     * 判断当前模块是否允许目标应用进行热重载。
     * <p>
     * 默认返回 {@code true}（允许全部）。子类可覆写此方法，对需要阻止热重载的目标包名返回 {@code false}。
     *
     * @param packageName 目标被 Hook 应用的包名
     * @return {@code true} 允许热重载；{@code false} 拒绝热重载
     */
    public boolean isHotReloadingAllowed(@NonNull String packageName) {
        return true;
    }

    /**
     * 模块已完成热更新时触发的回调（在新代码中执行）。
     * <p>
     * 此回调在热更新完成后于新模块代码中运行，子类可覆写此方法执行重新挂钩或特定的初始化操作。
     *
     * @param param       热更新完成参数，包含旧 Hook 句柄、保存的状态等信息
     * @param classLoader 从旧代码保存的状态中恢复的宿主应用 ClassLoader
     */
    public void handleHotReloaded(@NonNull HotReloadedParam param, @NonNull ClassLoader classLoader) {
    }

    // ------------------------- Inner -----------------------------

    /**
     * 框架回调入口：模块加载完成。初始化 Xposed 环境与包装器、模块配置后分发业务回调。
     */
    @Override
    public final void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        mProcessName = param.getProcessName();
        ModuleData.setWrapper(this);
        Host.set(new XposedHost(this));

        loadConfig();
        handleModuleLoaded(param);
    }

    /**
     * 框架回调入口：目标应用包加载完成。按忽略列表过滤后记录目标包名并分发业务回调。
     */
    @Override
    public final void onPackageLoaded(@NonNull PackageLoadedParam param) {
        Set<String> skips = skipPackages();
        if (Objects.nonNull(skips)) {
            mShouldSkipPackage = skips.contains(param.getPackageName());
            if (mShouldSkipPackage) {
                return;
            }
        }

        mShouldSkipPackage = false;
        mPackageName = param.getPackageName();
        ClassLoader defaultClassLoader = param.getDefaultClassLoader();
        if (defaultClassLoader != null) {
            ModuleData.setClassLoader(defaultClassLoader);
            ReflectEnv.setDefaultClassLoader(defaultClassLoader);
        }
        handlePackageLoaded(param);
    }

    /**
     * 框架回调入口：目标应用包资源就绪。挂接 Application.attach 钩子后分发业务回调。
     */
    @Override
    public final void onPackageReady(@NonNull PackageReadyParam param) {
        if (mShouldSkipPackage) {
            return;
        }

        ClassLoader classLoader = param.getClassLoader();
        if (classLoader != null) {
            ModuleData.setClassLoader(classLoader);
            ReflectEnv.setDefaultClassLoader(classLoader);
        }

        hookApplication(param);
        handlePackageReady(param);
    }

    /**
     * 框架回调入口：系统服务进程启动。直接分发业务回调。
     */
    @Override
    public final void onSystemServerStarting(@NonNull SystemServerStartingParam param) {
        handleSystemServerStarting(param);
    }

    /**
     * 框架回调入口：热更新准备阶段（旧代码执行）。收集状态并提交，允许/拒绝由 {@link #isHotReloadingAllowed(String)} 决定；状态保存成功后才清空注册表。
     */
    @Override
    public final boolean onHotReloading(@NonNull HotReloadingParam param) {
        try {
            // 允许/拒绝策略由子类覆写 isHotReloadingAllowed 控制；拒绝时直接返回，不做任何状态变更。
            if (!isHotReloadingAllowed(mPackageName)) {
                return false;
            }

            // 在收集/清空注册表之前先取出 ClassLoader，失败则提前拒绝且注册表不受影响。
            ClassLoader classLoader = ModuleData.getClassLoader();

            Map<String, Object> merged = new HashMap<>();
            Map<String, Object> moduleState = handleHotReloading(param.getExtras());
            Map<String, Object> hookState = HookRegistry.saveState(param.getExtras());

            // 跨来源键冲突检测：冲突时拒绝本次热重载。
            for (String key : hookState.keySet()) {
                if (moduleState.containsKey(key)) {
                    AndroidLog.logW(TAG,
                        "Hot reload state key collision, rejecting hot reload: " + key);
                    return false;
                }
            }
            merged.putAll(moduleState);
            merged.putAll(hookState);
            merged.put(ModuleData.MODULE_HOST_CLASSLOADER, classLoader);

            param.setSavedInstanceState(merged);
            HookRegistry.clear();
            return true;
        } catch (Throwable throwable) {
            try {
                handleHotReloadingFailed(throwable);
            } catch (Throwable failedError) {
                AndroidLog.logW(TAG,
                    "handleHotReloadingFailed() threw an exception, ignored to preserve the original exception.",
                    failedError);
            }
            Throwables.throwIt(throwable);
            return false;
        }
    }

    /**
     * 框架回调入口：热更新完成（新代码执行）。恢复 ClassLoader 与配置，先解除旧 Hook 再分发状态恢复。
     */
    @Override
    public final void onHotReloaded(@NonNull HotReloadedParam param) {
        List<HookHandle> oldHandles = param.getOldHookHandles(); // 预取，供先解除与 finally 兜底复用
        try {
            mProcessName = param.getProcessName();
            ModuleData.setWrapper(this);
            Host.set(new XposedHost(this));

            loadConfig();

            ClassLoader classLoader = null;
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) param.getSavedInstanceState();
            if (map != null) {
                Object cl = map.get(ModuleData.MODULE_HOST_CLASSLOADER);
                if (cl instanceof ClassLoader) {
                    classLoader = (ClassLoader) cl;
                }
            }
            // 带上下文消息的校验，缺键/类型不匹配时给出可诊断提示，而非裸 NPE。
            Objects.requireNonNull(classLoader,
                "Hot update status missing or key mismatch: " + ModuleData.MODULE_HOST_CLASSLOADER);

            // 回写默认 ClassLoader，使 handleHotReloaded 及后续反射默认类加载 API 可用。
            ModuleData.setClassLoader(classLoader);
            ReflectEnv.setDefaultClassLoader(classLoader);

            // 先解除旧 Hook，杜绝与新注册 Hook 的并存双执行窗口。
            unhookAll(oldHandles);

            handleHotReloaded(param, classLoader);
            HookRegistry.restoreState(param);
        } finally {
            // 任何路径都确保旧 Hook 被解除。
            unhookAll(oldHandles);
        }
    }

    /**
     * 批量解除旧 Hook 句柄。
     *
     * @param handles 待解除的 Hook 句柄列表
     */
    private static void unhookAll(@NonNull List<HookHandle> handles) {
        for (HookHandle handle : handles) {
            handle.unhook();
        }
    }

    private void hookApplication(@NonNull PackageLoadedParam param) {
        if (param.isFirstPackage()) {
            if (TextUtils.equals(param.getPackageName(), mProcessName)) {
                if (mHandle != null) {
                    mHandle.unhook();
                    mHandle = null;
                }

                try {
                    mHandle = Hook.hookMethod(Application.class, "attach", Context.class,
                        new AbsHook() {
                            @Override
                            public void before() {
                                Context context = (Context) getArg(0);
                                Objects.requireNonNull(context, "Application.attach() context must not be null.");
                                handleApplicationCreated(context);
                            }
                        }
                    );
                } catch (Throwable e) {
                    AndroidLog.logW(TAG, "Failed to hook Application.attach().", e);
                }
            }
        }
    }
}