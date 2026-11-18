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
package com.hchen.hooktool.host;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 宿主环境接口。
 * <p>
 * 由框架模块实现并注入，未注入时使用空实现。
 *
 * @author 焕晨HChen
 */
public interface HostEnv {
    /**
     * 判断是否处于 Xposed 环境。
     *
     * @return 处于 Xposed 环境时返回 true
     */
    boolean isXposedEnvironment();

    /**
     * 输出一行日志。
     *
     * @param priority  日志优先级
     * @param tag       自定义标签
     * @param message   已格式化的消息
     * @param throwable 异常信息
     */
    void log(int priority, @NonNull String tag, @NonNull String message,
             @Nullable Throwable throwable);

    /**
     * 获取目标应用的类加载器。
     *
     * @return 类加载器
     */
    @Nullable
    ClassLoader defaultClassLoader();

    /**
     * 获取模块安装包路径。
     *
     * @return 模块路径，不可用时为空字符串
     */
    @NonNull
    String modulePath();

    /**
     * 获取模块包名。
     *
     * @return 模块包名，不可用时为空字符串
     */
    @NonNull
    String modulePackageName();

    /**
     * 获取与模块应用共享的远程配置。
     *
     * @param name 配置文件名
     * @return 远程配置
     */
    @NonNull
    SharedPreferences remotePrefs(@NonNull String name);

    /**
     * 获取框架 API 版本。
     *
     * @return API 版本，不在框架中时为 0
     */
    int apiVersion();
}