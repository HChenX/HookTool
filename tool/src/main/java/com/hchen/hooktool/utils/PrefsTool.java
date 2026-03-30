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
package com.hchen.hooktool.utils;

import static com.hchen.hooktool.log.LogExpand.getStackTrace;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.ModuleConfig;
import com.hchen.hooktool.ModuleData;
import com.hchen.hooktool.exception.NoXposedEnvironmentException;
import com.hchen.hooktool.exception.UnexpectedException;
import com.hchen.hooktool.log.AndroidLog;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 共享首选项工具
 *
 * @author 焕晨HChen
 */
public final class PrefsTool {
    private static final String TAG = "PrefsTool";
    private final static ConcurrentHashMap<String, SharedPreferences> xPreferences = new ConcurrentHashMap<>(); // 宿主端
    private final static ConcurrentHashMap<String, SharedPreferences> sPreferences = new ConcurrentHashMap<>(); // 模块端

    private PrefsTool() {
    }

    /**
     * 从应用私有目录读取/写入共享首选项数据
     */
    @NonNull
    public static SharedPreferences prefs(@NonNull Context context) {
        return prefs(context, "");
    }

    /**
     * 从应用私有目录读取/写入共享首选项数据
     */
    @NonNull
    public static SharedPreferences prefs(@NonNull Context context, @NonNull String prefsName) {
        return createSharedPreferences(context, prefsName);
    }

    /**
     * Xposed 环境中读取模块的共享首选项，非 Xposed 环境中使用会引发异常
     */
    @NonNull
    public static SharedPreferences prefs() {
        if (!ModuleData.isXposedEnvironment())
            throw new NoXposedEnvironmentException("Must be in the xposed environment.");
        return prefs("");
    }

    /**
     * Xposed 环境中读取模块的共享首选项，非 Xposed 环境中使用会引发异常
     */
    @NonNull
    public static SharedPreferences prefs(@NonNull String prefsName) {
        if (!ModuleData.isXposedEnvironment())
            throw new NoXposedEnvironmentException("Must be in the xposed environment.");
        return createSharedPreferences(null, prefsName);
    }

    @SuppressLint("WorldReadableFiles")
    private static SharedPreferences createSharedPreferences(@Nullable Context context, @NonNull String prefsName) {
        prefsName = initPrefsName(prefsName);

        if (ModuleData.isXposedEnvironment()) {
            if (xPreferences.get(prefsName) == null) {
                SharedPreferences preferences = ModuleData.getRemotePreferences(prefsName);
                xPreferences.put(prefsName, preferences);
                return preferences;
            } else {
                return xPreferences.get(prefsName);
            }
        } else {
            Objects.requireNonNull(context);

            if (sPreferences.get(prefsName) == null) {
                SharedPreferences preferences;
                try {
                    // noinspection deprecation
                    preferences = context.getSharedPreferences(prefsName, Context.MODE_WORLD_READABLE);
                } catch (Throwable ignored) {
                    preferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
                    AndroidLog.logW(TAG, "Maybe unsupported prefs.", getStackTrace());
                }

                sPreferences.put(prefsName, preferences);
                return preferences;
            } else {
                return sPreferences.get(prefsName);
            }
        }
    }

    @NonNull
    private static String initPrefsName(@NonNull String name) {
        Objects.requireNonNull(name, "Prefs name must not be null.");

        if (name.isEmpty()) {
            if (ModuleConfig.getPrefsName().isEmpty()) {
                if (ModuleConfig.getModulePackageName().isEmpty())
                    throw new UnexpectedException("What prefs name you want use?");

                return ModuleConfig.getModulePackageName().toLowerCase() + "_prefs";
            }
            return ModuleConfig.getPrefsName();
        } else return name;
    }
}
