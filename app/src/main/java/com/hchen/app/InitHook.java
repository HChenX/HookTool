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

import androidx.annotation.NonNull;

import com.hchen.app.hook.TestHook;
import com.hchen.hooktool.ModuleConfig;
import com.hchen.hooktool.ModuleData;
import com.hchen.hooktool.ModuleEntrance;
import com.hchen.hooktool.log.AndroidLog;

public class InitHook extends ModuleEntrance {
    private static final String TAG = "InitHook";

    @Override
    public void initModuleConfig() {
        ModuleConfig.setLogTag("TestDemo");
        ModuleConfig.setLogLevel(ModuleConfig.LOG_D);
        ModuleConfig.setModulePackageName(BuildConfig.APPLICATION_ID);
        ModuleConfig.setPrefsName("test_demo_prefs");
        ModuleConfig.setLogExpandPaths(new String[]{
            "com.hchen.app.hook"
        });
        ModuleConfig.setShowHookSuccessLog(true);
    }

    @NonNull
    @Override
    public String[] ignorePackages() {
        return new String[]{"com.miui.contentcatcher"};
    }

    @Override
    public void handleModuleLoaded(@NonNull ModuleLoadedParam param) {
        AndroidLog.logD(TAG, "handleModuleLoaded: " + param.isSystemServer() + ", " + param.getProcessName() + ", " + param);
        super.handleModuleLoaded(param);
    }

    @Override
    public void handlePackageLoaded(@NonNull PackageLoadedParam param) {
        AndroidLog.logD(TAG, "handlePackageLoaded: " + param.isFirstPackage() + ", " + param.getPackageName() +
            ", " + param.getApplicationInfo() + ", " + param.getDefaultClassLoader() + ", " + param);
        super.handlePackageLoaded(param);
    }

    @Override
    public void handlePackageReady(@NonNull PackageReadyParam param) {
        AndroidLog.logD(TAG, "handlePackageReady: " + param.getClassLoader() + ", " + param.getAppComponentFactory() + ", " + param);
        super.handlePackageReady(param);

        ModuleData.setClassLoader(param.getClassLoader());
        new TestHook().handlePackageReady(param);
    }

    @Override
    public void handleSystemServerStarting(@NonNull SystemServerStartingParam param) {
        AndroidLog.logD(TAG, "handleSystemServerStarting: " + param.getClassLoader() + ", " + param);
        super.handleSystemServerStarting(param);
    }
}
