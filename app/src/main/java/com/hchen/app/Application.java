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
 * <p>本类实现了 {@link XposedServiceHelper.OnServiceListener} 接口，
 * 负责在应用启动时完成模块的基础配置（日志标签、日志等级、偏好设置文件名），
 * 并通过 {@link XposedServiceHelper#registerListener} 注册 Xposed 服务的
 * 生命周期监听，以便在服务绑定或异常断开时执行相应处理。</p>
 *
 * @see XposedServiceHelper
 * @see ModuleConfig
 */
public class Application extends android.app.Application
    implements XposedServiceHelper.OnServiceListener {
    private static final String TAG = "Application";

    /**
     * 应用进程启动时调用的初始化方法。
     *
     * <p>完成以下初始化操作：</p>
     * <ul>
     *   <li>将模块日志标签设置为 {@code "TestDemo"}</li>
     *   <li>将日志输出等级设置为 {@link ModuleConfig#LOG_D}（调试级别）</li>
     *   <li>指定偏好设置文件名为 {@code "test_demo_prefs"}</li>
     *   <li>向 {@link XposedServiceHelper} 注册当前实例作为服务监听器</li>
     * </ul>
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
     * <p>当 {@link XposedServiceHelper} 成功连接到 Xposed 服务后，
     * 会触发此方法，当前实现仅输出调试级别的绑定日志。</p>
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
     * <p>当 {@link XposedService} 服务连接异常断开或进程死亡时，
     * 会触发此方法，当前实现仅输出调试级别的断开日志。</p>
     *
     * @param service 已断开连接的 {@link XposedService} 服务实例
     */
    @Override
    public void onServiceDied(@NonNull XposedService service) {
        AndroidLog.logD(TAG, "onServiceDied: " + service);
    }
}
