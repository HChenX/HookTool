/*
 * This file is part of HookTool.
 *
 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or
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
package com.hchen.app;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.app.hook.TestHook;
import com.hchen.app.hook.TestHookKt;
import com.hchen.hooktool.ModuleConfig;
import com.hchen.hooktool.ModuleData;
import com.hchen.hooktool.ModuleEntrance;
import com.hchen.hooktool.log.AndroidLog;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * HookTool 模块的主入口类。
 *
 * @author 焕晨HChen
 */
public class HookEntrance extends ModuleEntrance {
    private static final String TAG = "InitHook";
    private final TestHook mTestHook = new TestHook();
    private final TestHookKt mTestHookKt = new TestHookKt();

    /**
     * 配置模块的全局参数。
     */
    @Override
    public void loadConfig() {
        ModuleConfig.setLogTag("TestDemo");
        ModuleConfig.setLogLevel(ModuleConfig.LOG_D);
        ModuleConfig.setPrefsName("test_demo_prefs");
        ModuleConfig.setLogExpandPaths(Set.of("com.hchen.app.hook"));
        ModuleConfig.setShowHookSuccessLog(true);
    }

    /**
     * 返回需要跳过的目标包名。
     *
     * @return 需要跳过的包名集合
     */
    @NonNull
    @Override
    public Set<String> skipPackages() {
        return Set.of();
    }

    /**
     * 模块加载完成时的回调。
     *
     * @param param 模块加载参数
     */
    @Override
    public void handleModuleLoaded(@NonNull ModuleLoadedParam param) {
        AndroidLog.logD(TAG, "handleModuleLoaded: " + param.isSystemServer() + ", " + param.getProcessName() + ", " + param);
        super.handleModuleLoaded(param);
    }

    /**
     * 目标应用包加载时的回调。
     *
     * @param param 包加载参数
     */
    @Override
    public void handlePackageLoaded(@NonNull PackageLoadedParam param) {
        AndroidLog.logD(TAG, "handlePackageLoaded: " + param.isFirstPackage() + ", " + param.getPackageName() +
            ", " + param.getApplicationInfo() + ", " + param.getDefaultClassLoader() + ", " + param);
        super.handlePackageLoaded(param);
    }

    /**
     * 目标应用包资源就绪时的回调。
     *
     * @param param 包就绪参数
     */
    @Override
    public void handlePackageReady(@NonNull PackageReadyParam param) {
        AndroidLog.logD(TAG, "handlePackageReady: " + param.getClassLoader() + ", " + param.getAppComponentFactory() + ", " + param);
        super.handlePackageReady(param);

        ModuleData.setClassLoader(param.getClassLoader());
        mTestHook.handlePackageReady(param);
        mTestHookKt.handlePackageReady(param);
    }

    /**
     * 系统服务器启动时的回调。
     *
     * @param param 系统服务器启动参数
     */
    @Override
    public void handleSystemServerStarting(@NonNull SystemServerStartingParam param) {
        AndroidLog.logD(TAG, "handleSystemServerStarting: " + param.getClassLoader() + ", " + param);
        super.handleSystemServerStarting(param);
    }

    /**
     * 目标应用 Application 创建时的回调。
     *
     * @param context 目标应用的上下文对象
     */
    @Override
    public void handleApplicationCreated(@NonNull Context context) {
        AndroidLog.logD(TAG, "handleApplicationCreated: " + context);
        super.handleApplicationCreated(context);
    }

    /**
     * 模块即将被热更新时触发的回调（在旧代码中执行）。
     *
     * @param extras 热更新触发的附加数据 {@link Bundle}，可能为 {@code null}（当框架未传递额外数据时）
     * @return 子模块级状态键值对合并后的 {@link Map}；异常时返回空 {@link HashMap}
     */
    @NonNull
    @Override
    public Map<String, Object> handleHotReloading(@Nullable Bundle extras) {
        AndroidLog.logD(TAG, "handleHotReloading: " + extras);

        Map<String, Object> map = new HashMap<>();
        map.putAll(mTestHook.handleHotReloading(extras));
        map.putAll(mTestHookKt.handleHotReloading(extras));
        return map;
    }

    /**
     * 热更新准备阶段发生异常时的回调。
     *
     * @param throwable 在热更新准备阶段被捕获的异常实例
     */
    @Override
    public void handleHotReloadingFailed(@NonNull Throwable throwable) {
        AndroidLog.logE(TAG, "handleHotReloadingFailed", throwable);
        super.handleHotReloadingFailed(throwable);
    }

    /**
     * 模块已完成热更新时触发的回调（在新代码中执行）。
     *
     * @param param       热更新完成参数
     * @param classLoader 从旧代码保存的状态中恢复的宿主应用 ClassLoader
     */
    @Override
    public void handleHotReloaded(@NonNull HotReloadedParam param, @NonNull ClassLoader classLoader) {
        AndroidLog.logD(TAG, "handleHotReloaded: " + param);
        super.handleHotReloaded(param, classLoader);

        mTestHook.handleHotReloaded(param);
        mTestHookKt.handleHotReloaded(param);
    }
}
