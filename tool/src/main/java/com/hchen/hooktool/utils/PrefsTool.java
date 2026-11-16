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

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.hchen.hooktool.ModuleConfig;
import com.hchen.hooktool.ModuleData;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * SharedPreferences 封装工具类。
 *
 * @author 焕晨HChen
 */
public final class PrefsTool {
    private static final ConcurrentHashMap<String, SharedPreferences> sHostPreferences = new ConcurrentHashMap<>(); // 宿主端
    private static final ConcurrentHashMap<String, SharedPreferences> sModulePreferences = new ConcurrentHashMap<>(); // 模块端

    private PrefsTool() {
    }

    /**
     * 获取模块端默认名称的 SharedPreferences 实例。
     *
     * @param context 上下文
     * @return 默认名称对应的 SharedPreferences 实例
     */
    @NonNull
    public static SharedPreferences prefs(@NonNull Context context) {
        return prefs(context, "");
    }

    /**
     * 获取模块端指定名称的 {@link SharedPreferences} 实例。
     *
     * @param context   上下文
     * @param prefsName 偏好文件名称；为空字符串时使用由 {@link ModuleConfig} 或模块包名生成的默认名称
     * @return 指定名称对应的 {@link SharedPreferences} 实例
     */
    @NonNull
    public static SharedPreferences prefs(@NonNull Context context, @NonNull String prefsName) {
        return createSharedPreferences(context, prefsName);
    }

    /**
     * 在 Xposed 宿主进程中获取模块默认名称的 SharedPreferences 实例。
     *
     * @return 跨进程访问的 SharedPreferences 实例
     */
    @NonNull
    public static SharedPreferences prefs() {
        return prefs("");
    }

    /**
     * 在 Xposed 宿主进程中获取模块指定名称的 {@link SharedPreferences} 实例。
     *
     * @param prefsName 偏好设置文件名称；为空字符串时使用默认名称
     * @return 跨进程访问的 {@link SharedPreferences} 实例
     */
    @NonNull
    public static SharedPreferences prefs(@NonNull String prefsName) {
        return createSharedPreferences(prefsName);
    }

    private static SharedPreferences createSharedPreferences(@NonNull String prefsName) {
        String resolvedName = initPrefsName(prefsName);
        String key = ModuleData.getModulePackageName() + "|" + resolvedName;
        return sHostPreferences.computeIfAbsent(key, new Function<String, SharedPreferences>() {
            @Override
            public SharedPreferences apply(String k) {
                return ModuleData.getRemotePreferences(resolvedName);
            }
        });
    }

    private static SharedPreferences createSharedPreferences(@NonNull Context context, @NonNull String prefsName) {
        String resolvedName = initPrefsName(prefsName);
        String key = context.getPackageName() + "|" + resolvedName;
        return sModulePreferences.computeIfAbsent(key, new Function<String, SharedPreferences>() {
            @Override
            public SharedPreferences apply(String k) {
                return context.getSharedPreferences(resolvedName, Context.MODE_PRIVATE);
            }
        });
    }

    /**
     * 解析并返回最终使用的偏好设置文件名称。
     *
     * @param name 传入的偏好设置名称候选值
     * @return 解析后的偏好设置文件名称
     * @throws IllegalArgumentException 无法确定偏好设置文件名称时抛出
     */
    @NonNull
    private static String initPrefsName(@NonNull String name) {
        if (name.isEmpty()) {
            if (ModuleConfig.getPrefsName().isEmpty()) {
                if (ModuleData.getModulePackageName().isEmpty())
                    throw new IllegalArgumentException(
                        "Unable to determine prefs name: call ModuleConfig.setPrefsName() or run in the module process.");

                return ModuleData.getModulePackageName().toLowerCase(Locale.ROOT) + "_prefs";
            }
            return ModuleConfig.getPrefsName();
        } else return name;
    }
}
