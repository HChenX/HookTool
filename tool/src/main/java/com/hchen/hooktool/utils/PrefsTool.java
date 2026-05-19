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

import com.hchen.hooktool.ModuleConfig;
import com.hchen.hooktool.ModuleData;
import com.hchen.hooktool.exception.UnexpectedException;
import com.hchen.hooktool.log.AndroidLog;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * SharedPreferences 封装工具类。
 * <p>
 * 为 Xposed 模块场景提供统一的 SharedPreferences 读写接口，分别支持宿主进程（通过
 * {@link ModuleData#getRemotePreferences} 跨进程访问）和模块进程（使用
 * {@code MODE_WORLD_READABLE} 或降级 {@code MODE_PRIVATE}）两种模式。
 * 内部使用 {@link ConcurrentHashMap} 对已创建的实例进行缓存，避免重复创建。
 *
 * @author 焕晨HChen
 */
public final class PrefsTool {
    private static final String TAG = "PrefsTool";
    private static final ConcurrentHashMap<String, SharedPreferences> xPreferences = new ConcurrentHashMap<>(); // 宿主端
    private static final ConcurrentHashMap<String, SharedPreferences> sPreferences = new ConcurrentHashMap<>(); // 模块端

    private PrefsTool() {
    }

    /**
     * 获取模块端默认名称的 {@link SharedPreferences} 实例。
     * <p>
     * 以空字符串作为名称调用 {@link #prefs(Context, String)}，从而使用默认偏好设置文件。
     *
     * @param context Android 上下文，不可为 {@code null}
     * @return 默认名称对应的 {@link SharedPreferences} 实例
     */
    @NonNull
    public static SharedPreferences prefs(@NonNull Context context) {
        return prefs(context, "");
    }

    /**
     * 获取模块端指定名称的 {@link SharedPreferences} 实例。
     * <p>
     * 优先尝试以 {@code MODE_WORLD_READABLE} 模式打开文件，使宿主进程可读取；若该模式
     * 不被支持则自动降级为 {@code MODE_PRIVATE}。相同 {@code context.getPackageName() + prefsName}
     * 组合的实例会被缓存，不会重复创建。
     *
     * @param context   Android 上下文，不可为 {@code null}
     * @param prefsName 偏好设置文件名称；为空字符串时使用由 {@link ModuleConfig} 或模块包名生成的默认名称
     * @return 指定名称对应的 {@link SharedPreferences} 实例
     */
    @NonNull
    public static SharedPreferences prefs(@NonNull Context context, @NonNull String prefsName) {
        return createSharedPreferences(context, prefsName);
    }

    /**
     * 在 Xposed 宿主进程中获取模块的默认 {@link SharedPreferences} 实例。
     * <p>
     * 内部通过 {@link ModuleData#getRemotePreferences} 跨进程读取模块端的配置数据，使用默认偏好设置名称。
     *
     * @return 跨进程访问的 {@link SharedPreferences} 实例
     */
    @NonNull
    public static SharedPreferences prefs() {
        return prefs("");
    }

    /**
     * 在 Xposed 宿主进程中获取模块指定名称的 {@link SharedPreferences} 实例。
     * <p>
     * 内部通过 {@link ModuleData#getRemotePreferences} 跨进程读取，实例会被缓存以避免重复创建。
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
        String key = ModuleData.getModulePackageName() + resolvedName;
        return xPreferences.computeIfAbsent(key, new Function<String, SharedPreferences>() {
            @Override
            public SharedPreferences apply(String k) {
                return ModuleData.getRemotePreferences(resolvedName);
            }
        });
    }

    @SuppressLint("WorldReadableFiles")
    private static SharedPreferences createSharedPreferences(@NonNull Context context, @NonNull String prefsName) {
        String resolvedName = initPrefsName(prefsName);
        String key = context.getPackageName() + resolvedName;
        return sPreferences.computeIfAbsent(key, new Function<String, SharedPreferences>() {
            @Override
            public SharedPreferences apply(String k) {
                SharedPreferences preferences;
                try {
                    // noinspection deprecation
                    preferences = context.getSharedPreferences(resolvedName, Context.MODE_WORLD_READABLE);
                } catch (Throwable ignored) {
                    preferences = context.getSharedPreferences(resolvedName, Context.MODE_PRIVATE);
                    AndroidLog.logW(TAG, "Maybe unsupported prefs.", getStackTrace());
                }
                return preferences;
            }
        });
    }

    /**
     * 解析并返回最终使用的偏好设置文件名称。
     * <p>
     * 名称的确定遵循以下优先级：
     * <ol>
     *     <li>非空的 {@code name} 参数直接使用</li>
     *     <li>{@link ModuleConfig#getPrefsName()} 中配置的名称</li>
     *     <li>基于模块包名自动生成（格式：{@code <包名小写>_prefs}）</li>
     * </ol>
     * 若以上方式均无法确定名称，则抛出 {@link UnexpectedException}。
     *
     * @param name 传入的偏好设置名称候选值
     * @return 解析后的偏好设置文件名称
     * @throws UnexpectedException 无法确定偏好设置文件名称时抛出
     */
    @NonNull
    private static String initPrefsName(@NonNull String name) {
        Objects.requireNonNull(name, "Prefs name must not be null.");

        if (name.isEmpty()) {
            if (ModuleConfig.getPrefsName().isEmpty()) {
                if (ModuleData.getModulePackageName().isEmpty())
                    throw new UnexpectedException("What prefs name you want use?");

                return ModuleData.getModulePackageName().toLowerCase() + "_prefs";
            }
            return ModuleConfig.getPrefsName();
        } else return name;
    }
}
