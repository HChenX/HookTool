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

import static com.hchen.hooktool.utils.InvokeTool.callStaticMethod;

import androidx.annotation.NonNull;

import java.util.Optional;

/**
 * Android 系统属性工具类。
 * <p>
 * 提供读取和设置 Android 系统属性（SystemProperties）的便捷方法。
 *
 * @author 焕晨HChen
 */
public final class SystemPropTool {
    private static final Class<?> propClass;

    static {
        Class<?> clazz = null;
        try {
            clazz = InvokeTool.findClass("android.os.SystemProperties");
        } catch (Throwable ignore) {
        }
        propClass = clazz;
    }

    private SystemPropTool() {
    }

    /**
     * 获取布尔类型的系统属性值。
     *
     * @param key 属性名
     * @param def 默认值
     * @return 属性值，如果属性不存在或加载失败则返回默认值
     */
    public static boolean getProp(@NonNull String key, boolean def) {
        if (propClass == null) return def;
        return Boolean.TRUE.equals(
            callStaticMethod(propClass, "getBoolean", new Class[]{String.class, boolean.class}, key, def)
        );
    }

    /**
     * 获取整型的系统属性值。
     *
     * @param key 属性名
     * @param def 默认值
     * @return 属性值，如果属性不存在或加载失败则返回默认值
     */
    public static int getProp(@NonNull String key, int def) {
        if (propClass == null) return def;
        return (int) Optional.ofNullable(
            callStaticMethod(propClass, "getInt", new Class[]{String.class, int.class}, key, def)
        ).orElse(def);
    }

    /**
     * 获取长整型的系统属性值。
     *
     * @param key 属性名
     * @param def 默认值
     * @return 属性值，如果属性不存在或加载失败则返回默认值
     */
    public static long getProp(@NonNull String key, long def) {
        if (propClass == null) return def;
        return (long) Optional.ofNullable(
            callStaticMethod(propClass, "getLong", new Class[]{String.class, long.class}, key, def)
        ).orElse(def);
    }

    /**
     * 获取字符串类型的系统属性值。
     *
     * @param key 属性名
     * @param def 默认值
     * @return 属性值，如果属性不存在或加载失败则返回默认值
     */
    public static String getProp(@NonNull String key, String def) {
        if (propClass == null) return def;
        return (String) Optional.ofNullable(
            callStaticMethod(propClass, "get", new Class[]{String.class, String.class}, key, def)
        ).orElse(def);
    }

    /**
     * 获取字符串类型的系统属性值，如果不存在则返回空字符串。
     *
     * @param key 属性名
     * @return 属性值，如果属性不存在则返回空字符串
     */
    public static String getProp(@NonNull String key) {
        if (propClass == null) return "";
        return (String) Optional.ofNullable(
            callStaticMethod(propClass, "get", new Class[]{String.class}, key)
        ).orElse("");
    }

    /**
     * 使用指定的类加载器获取字符串类型的系统属性值。
     *
     * @param key         属性名
     * @param classLoader 类加载器
     * @return 属性值，如果属性不存在则返回空字符串
     */
    public static String getProp(@NonNull String key, ClassLoader classLoader) {
        return (String) Optional.ofNullable(
            callStaticMethod(
                "android.os.SystemProperties",
                classLoader,
                "get",
                new Class[]{String.class},
                key
            )
        ).orElse("");
    }

    /**
     * 设置系统属性值。
     * <p>
     * 只有系统框架才可能可以调用，可设置的属性类型非常有限，一般情况下使用不到。
     *
     * @param key   属性名
     * @param value 属性值
     */
    public static void setProp(@NonNull String key, String value) {
        if (propClass == null) return;
        callStaticMethod(propClass, "set", new Class[]{String.class, String.class}, key, value);
    }
}
