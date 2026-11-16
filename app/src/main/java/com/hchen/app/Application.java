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
 * HookTool 示例应用的全局 {@link android.app.Application} 入口。
 *
 * @author 焕晨HChen
 */
public class Application extends android.app.Application
    implements XposedServiceHelper.OnServiceListener {
    private static final String TAG = "Application";

    /**
     * 应用进程启动时调用的初始化方法。
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
     * Xposed 服务绑定成功时的回调。
     *
     * @param service 已成功绑定的 {@link XposedService} 服务实例
     */
    @Override
    public void onServiceBind(@NonNull XposedService service) {
        AndroidLog.logD(TAG, "onServiceBind: " + service);
    }

    /**
     * Xposed 服务意外终止时的回调。
     *
     * @param service 已断开连接的 {@link XposedService} 服务实例
     */
    @Override
    public void onServiceDied(@NonNull XposedService service) {
        AndroidLog.logD(TAG, "onServiceDied: " + service);
    }
}
