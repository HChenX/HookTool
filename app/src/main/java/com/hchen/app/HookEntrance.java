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
package com.hchen.app;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.app.hook.DemoHooks;
import com.hchen.app.hook.InteropHooks;
import com.hchen.app.hook.UtilitySamples;
import com.hchen.hooktool.config.HookConfig;
import com.hchen.hooktool.entry.ModuleData;
import com.hchen.hooktool.entry.ModuleEntrance;
import com.hchen.hooktool.log.AndroidLog;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 示例模块入口。
 *
 * @author 焕晨HChen
 */
public class HookEntrance extends ModuleEntrance {
    private static final String TAG = "HookEntrance";

    @Override
    public void loadConfig() {
        HookConfig.setLogTag("HookToolDemo");
        HookConfig.setLogLevel(HookConfig.LOG_D);
        HookConfig.setPrefsName("hooktool_demo_prefs");
        HookConfig.setLogExpandPaths(Set.of("com.hchen.app.hook"));
        HookConfig.setShowHookSuccessLog(true);
    }

    @NonNull
    @Override
    public Set<String> skipPackages() {
        return Set.of();
    }

    @Override
    public void handleModuleLoaded(@NonNull ModuleLoadedParam param) {
        AndroidLog.logI(TAG, "模块已加载: " + param.getProcessName());
    }

    @Override
    public void handlePackageLoaded(@NonNull PackageLoadedParam param) {
        AndroidLog.logI(TAG, "包已加载: " + param.getPackageName());
    }

    @Override
    public void handlePackageReady(@NonNull PackageReadyParam param) {
        AndroidLog.logI(TAG, "包已就绪: " + param.getPackageName());
        DemoHooks.installAll();
        InteropHooks.run();
    }

    @Override
    public void handleSystemServerStarting(@NonNull SystemServerStartingParam param) {
        AndroidLog.logI(TAG, "系统服务进程启动");
    }

    @Override
    public void handleApplicationCreated(@NonNull Context context) {
        AndroidLog.logI(TAG, "应用已创建: " + context.getPackageName());
        UtilitySamples.run(context);
    }

    @NonNull
    @Override
    public Map<String, Object> handleHotReloading(@Nullable Bundle extras) {
        DemoHooks.uninstallAll();
        return new HashMap<>();
    }

    @Override
    public void handleHotReloadingFailed(@NonNull Throwable throwable) {
        AndroidLog.logE(TAG, "热更新准备失败", throwable);
    }

    @Override
    public void handleHotReloaded(@NonNull HotReloadedParam param,
                                  @NonNull ClassLoader classLoader) {
        AndroidLog.logI(TAG, "热更新完成，重新安装 Hook");
        ModuleData.setClassLoader(classLoader);
        DemoHooks.installAll();
        InteropHooks.run();
    }
}
