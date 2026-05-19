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
 * Android 系统属性（SystemProperties）操作工具类。
 * <p>
 * 通过反射调用 {@code android.os.SystemProperties} 的隐藏 API，提供读取和设置系统属性的便捷方法。
 * 支持布尔、整型、长整型和字符串四种属性值类型。若目标类加载失败，所有读取方法将安全地返回默认值。
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
     * <p>
     * 通过反射调用 {@code SystemProperties.getBoolean(String, boolean)} 实现。
     * 若 {@code android.os.SystemProperties} 类未加载成功，直接返回默认值。
     *
     * @param key 属性名称
     * @param def 属性不存在或读取失败时的默认值
     * @return 属性的布尔值
     */
    public static boolean getProp(@NonNull String key, boolean def) {
        if (propClass == null) return def;
        return Boolean.TRUE.equals(
            callStaticMethod(propClass, "getBoolean", new Class[]{String.class, boolean.class}, key, def)
        );
    }

    /**
     * 获取整型的系统属性值。
     * <p>
     * 通过反射调用 {@code SystemProperties.getInt(String, int)} 实现。
     * 若返回值为 {@code null} 或类加载失败，返回默认值。
     *
     * @param key 属性名称
     * @param def 属性不存在或读取失败时的默认值
     * @return 属性的整型值
     */
    public static int getProp(@NonNull String key, int def) {
        if (propClass == null) return def;
        return (int) Optional.ofNullable(
            callStaticMethod(propClass, "getInt", new Class[]{String.class, int.class}, key, def)
        ).orElse(def);
    }

    /**
     * 获取长整型的系统属性值。
     * <p>
     * 通过反射调用 {@code SystemProperties.getLong(String, long)} 实现。
     * 若返回值为 {@code null} 或类加载失败，返回默认值。
     *
     * @param key 属性名称
     * @param def 属性不存在或读取失败时的默认值
     * @return 属性的长整型值
     */
    public static long getProp(@NonNull String key, long def) {
        if (propClass == null) return def;
        return (long) Optional.ofNullable(
            callStaticMethod(propClass, "getLong", new Class[]{String.class, long.class}, key, def)
        ).orElse(def);
    }

    /**
     * 获取字符串类型的系统属性值，支持自定义默认值。
     * <p>
     * 通过反射调用 {@code SystemProperties.get(String, String)} 实现。
     * 若返回值为 {@code null} 或类加载失败，返回默认值。
     *
     * @param key 属性名称
     * @param def 属性不存在或读取失败时的默认值
     * @return 属性的字符串值
     */
    public static String getProp(@NonNull String key, String def) {
        if (propClass == null) return def;
        return (String) Optional.ofNullable(
            callStaticMethod(propClass, "get", new Class[]{String.class, String.class}, key, def)
        ).orElse(def);
    }

    /**
     * 获取字符串类型的系统属性值，属性不存在时返回空字符串。
     * <p>
     * 通过反射调用 {@code SystemProperties.get(String)} 实现。
     * 若属性不存在或类加载失败，返回空字符串。
     *
     * @param key 属性名称
     * @return 属性的字符串值，不存在时为空字符串
     */
    public static String getProp(@NonNull String key) {
        if (propClass == null) return "";
        return (String) Optional.ofNullable(
            callStaticMethod(propClass, "get", new Class[]{String.class}, key)
        ).orElse("");
    }

    /**
     * 使用指定的类加载器获取字符串类型的系统属性值。
     * <p>
     * 适用于需要在特定类加载上下文中访问系统属性的场景（如不同进程或自定义类加载器环境）。
     * 内部通过传入的 {@code classLoader} 重新加载 {@code android.os.SystemProperties} 类并调用其方法。
     *
     * @param key         属性名称
     * @param classLoader 用于加载 {@code android.os.SystemProperties} 类的类加载器
     * @return 属性的字符串值，不存在时为空字符串
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
     * 通过反射调用 {@code SystemProperties.set(String, String)} 实现。
     * 注意：此方法仅在系统框架进程中可能成功调用，可修改的属性类型非常有限，普通应用无法使用。
     * 若 {@code android.os.SystemProperties} 类加载失败，此方法不执行任何操作。
     *
     * @param key   属性名称
     * @param value 要设置的属性值
     */
    public static void setProp(@NonNull String key, String value) {
        if (propClass == null) return;
        callStaticMethod(propClass, "set", new Class[]{String.class, String.class}, key, value);
    }
}
