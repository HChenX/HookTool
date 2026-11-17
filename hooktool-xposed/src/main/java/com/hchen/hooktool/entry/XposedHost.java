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

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.host.HostEnv;
import com.hchen.hooktool.log.AndroidLog;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedInterfaceWrapper;

/**
 * 基于框架包装器的宿主环境实现。
 * <p>
 * 日志优先写入框架通道，不可用时回落到系统日志。
 *
 * @author 焕晨HChen
 */
final class XposedHost implements HostEnv {
    private static final String TAG = "XposedHost";
    private static final AtomicBoolean sFallbackUsed = new AtomicBoolean(false);

    private final XposedInterfaceWrapper mWrapper;

    /**
     * 创建宿主环境。
     *
     * @param wrapper 模块包装器，通常为入口实例
     */
    XposedHost(XposedInterfaceWrapper wrapper) {
        mWrapper = Objects.requireNonNull(wrapper, "wrapper");
    }

    @Override
    public boolean isXposedEnvironment() {
        return true;
    }

    @Override
    public void log(int priority, @NonNull String tag, @NonNull String message,
                    @Nullable Throwable throwable) {
        if (sFallbackUsed.get()) {
            try {
                mWrapper.log(priority, tag, message, throwable);
                sFallbackUsed.set(false);
                return;
            } catch (Throwable t) {
                AndroidLog.logD(TAG, "Xposed log proxy still unavailable, staying on fallback.", t);
            }
            AndroidLog.output(priority, tag, message, throwable);
            return;
        }
        try {
            mWrapper.log(priority, tag, message, throwable);
        } catch (Throwable t) {
            markFallback(t);
            AndroidLog.output(priority, tag, message, throwable);
        }
    }

    private static void markFallback(Throwable throwable) {
        if (sFallbackUsed.compareAndSet(false, true)) {
            AndroidLog.logW(TAG, "Falling back to AndroidLog.", throwable);
        }
    }

    @Override
    @Nullable
    public ClassLoader defaultClassLoader() {
        return ModuleData.getClassLoaderOrNull();
    }

    @Override
    @NonNull
    public String modulePath() {
        return mWrapper.getModuleApplicationInfo().sourceDir;
    }

    @Override
    @NonNull
    public String modulePackageName() {
        return mWrapper.getModuleApplicationInfo().packageName;
    }

    @Override
    @NonNull
    public SharedPreferences remotePrefs(@NonNull String name) {
        return mWrapper.getRemotePreferences(name);
    }

    @Override
    public int apiVersion() {
        return mWrapper.getApiVersion();
    }
}