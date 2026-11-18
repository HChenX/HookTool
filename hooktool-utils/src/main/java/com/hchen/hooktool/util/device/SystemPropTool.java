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
package com.hchen.hooktool.util.device;

import androidx.annotation.NonNull;

import com.hchen.hooktool.log.AndroidLog;
import com.hchen.hooktool.reflect.Reflect;

/**
 * Android 系统属性（SystemProperties）操作工具类。
 *
 * @author 焕晨HChen
 */
public final class SystemPropTool {
    private static final String TAG = "SystemPropTool";
    private static final Class<?> PROP_CLASS;

    static {
        Class<?> clazz = null;
        try {
            clazz = Reflect.findClass("android.os.SystemProperties");
        } catch (Throwable e) {
            AndroidLog.logW(TAG, "android.os.SystemProperties unavailable; property APIs will be no-ops.", e);
        }
        PROP_CLASS = clazz;
    }

    private SystemPropTool() {
    }

    /**
     * 统一的反射调用入口：任何反射失败（方法缺失、访问受限等）都捕获并降级为 {@code null}，由各 getProp 方法回退到默认值，不向上抛出。
     *
     * @param methodName 目标 SystemProperties 方法名
     * @param types      参数类型数组
     * @param args       实际参数
     * @return 反射调用返回值；失败或 void 方法返回 {@code null}
     */
    private static Object callProp(@NonNull String methodName, @NonNull Class<?>[] types, @NonNull Object... args) {
        try {
            return Reflect.callStaticMethodWithTypes(PROP_CLASS, methodName, types, args);
        } catch (Throwable e) {
            AndroidLog.logW(TAG, "SystemProperties." + methodName + "() reflection failed.", e);
            return null;
        }
    }

    /**
     * 获取布尔类型的系统属性值。
     *
     * @param key 属性名称
     * @param def 属性不存在或读取失败时的默认值
     * @return 属性的布尔值
     */
    public static boolean getProp(@NonNull String key, boolean def) {
        if (PROP_CLASS == null) return def;
        Object v = callProp("getBoolean", new Class[]{String.class, boolean.class}, key, def);
        return v instanceof Boolean b ? b : def;
    }

    /**
     * 获取整型的系统属性值。
     *
     * @param key 属性名称
     * @param def 属性不存在或读取失败时的默认值
     * @return 属性的整型值
     */
    public static int getProp(@NonNull String key, int def) {
        if (PROP_CLASS == null) return def;
        Object v = callProp("getInt", new Class[]{String.class, int.class}, key, def);
        return v instanceof Integer i ? i : def;
    }

    /**
     * 获取长整型的系统属性值。
     *
     * @param key 属性名称
     * @param def 属性不存在或读取失败时的默认值
     * @return 属性的长整型值
     */
    public static long getProp(@NonNull String key, long def) {
        if (PROP_CLASS == null) return def;
        Object v = callProp("getLong", new Class[]{String.class, long.class}, key, def);
        return v instanceof Long l ? l : def;
    }

    /**
     * 获取字符串类型的系统属性值，支持自定义默认值。
     *
     * @param key 属性名称
     * @param def 属性不存在或读取失败时的默认值
     * @return 属性的字符串值
     */
    public static String getProp(@NonNull String key, String def) {
        if (PROP_CLASS == null) return def;
        Object v = callProp("get", new Class[]{String.class, String.class}, key, def);
        return v instanceof String s ? s : def;
    }

    /**
     * 获取字符串类型的系统属性值，属性不存在时返回空字符串。
     *
     * @param key 属性名称
     * @return 属性的字符串值，不存在时为空字符串
     */
    public static String getProp(@NonNull String key) {
        if (PROP_CLASS == null) return "";
        Object v = callProp("get", new Class[]{String.class}, key);
        return v instanceof String s ? s : "";
    }

    /**
     * 使用指定的类加载器获取字符串类型的系统属性值。用于在特定类加载上下文中访问系统属性，加载或调用失败时返回空字符串。
     *
     * @param key         属性名称
     * @param classLoader 用于加载 {@code android.os.SystemProperties} 类的类加载器
     * @return 属性的字符串值，不存在时为空字符串
     */
    public static String getProp(@NonNull String key, ClassLoader classLoader) {
        try {
            Class<?> propClass = Reflect.findClass("android.os.SystemProperties", classLoader);
            Object v = Reflect.callStaticMethodWithTypes(
                propClass,
                "get",
                new Class[]{String.class},
                key
            );
            return v instanceof String s ? s : "";
        } catch (Throwable e) {
            AndroidLog.logW(TAG, "SystemProperties.get(String) reflection failed with custom class loader.", e);
            return "";
        }
    }

    /**
     * 设置系统属性值。普通应用无法生效，只在系统框架进程中可能成功。
     *
     * @param key   属性名称
     * @param value 要设置的属性值
     */
    public static void setProp(@NonNull String key, String value) {
        if (PROP_CLASS == null) return;
        callProp("set", new Class[]{String.class, String.class}, key, value);
    }
}