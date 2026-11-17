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

import com.hchen.hooktool.log.AndroidLog;

/**
 * 默认宿主环境实现。
 * <p>
 * 未注入框架时使用，日志直接写入系统日志。
 *
 * @author 焕晨HChen
 */
final class NoopHostEnv implements HostEnv {
    /**
     * 全局单例。
     */
    public static final NoopHostEnv INSTANCE = new NoopHostEnv();

    private NoopHostEnv() {
    }

    @Override
    public boolean isXposedEnvironment() {
        return false;
    }

    @Override
    public void log(int priority, @NonNull String tag, @NonNull String message,
                    @Nullable Throwable throwable) {
        AndroidLog.output(priority, tag, message, throwable);
    }

    @Override
    @Nullable
    public ClassLoader defaultClassLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader != null ? loader : ClassLoader.getSystemClassLoader();
    }

    @Override
    @NonNull
    public String modulePath() {
        return "";
    }

    @Override
    @NonNull
    public String modulePackageName() {
        return "";
    }

    @Override
    @NonNull
    public SharedPreferences remotePrefs(@NonNull String name) {
        throw new IllegalStateException("Remote preferences are only available inside a Xposed module process.");
    }

    @Override
    public int apiVersion() {
        return 0;
    }
}