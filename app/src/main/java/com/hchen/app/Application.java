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

import com.hchen.hooktool.ModuleConfig;
import com.hchen.hooktool.log.AndroidLog;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

/**
 * 示例应用 Application 类。实现 {@link XposedServiceHelper.OnServiceListener} 接口以监听 Xposed 服务绑定状态。
 */
public class Application extends android.app.Application
    implements XposedServiceHelper.OnServiceListener {
    private static final String TAG = "Application";

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        super.onCreate();
        ModuleConfig.setLogTag("TestDemo");
        ModuleConfig.setLogLevel(ModuleConfig.LOG_D);
        ModuleConfig.setPrefsName("test_demo_prefs");

        XposedServiceHelper.registerListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceBind(@NonNull XposedService service) {
        AndroidLog.logD(TAG, "onServiceBind: " + service);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceDied(@NonNull XposedService service) {
        AndroidLog.logD(TAG, "onServiceDied: " + service);
    }
}
