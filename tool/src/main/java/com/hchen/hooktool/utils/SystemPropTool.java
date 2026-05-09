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
 * Prop 工具
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
     * 获取 boolean 类型的 prop
     */
    public static boolean getProp(@NonNull String key, boolean def) {
        if (propClass == null) return def;
        return Boolean.TRUE.equals(
            callStaticMethod(propClass, "getBoolean", new Class[]{String.class, boolean.class}, key, def)
        );
    }

    public static int getProp(@NonNull String key, int def) {
        if (propClass == null) return def;
        return (int) Optional.ofNullable(
            callStaticMethod(propClass, "getInt", new Class[]{String.class, int.class}, key, def)
        ).orElse(def);
    }

    public static long getProp(@NonNull String key, long def) {
        if (propClass == null) return def;
        return (long) Optional.ofNullable(
            callStaticMethod(propClass, "getLong", new Class[]{String.class, long.class}, key, def)
        ).orElse(def);
    }

    public static String getProp(@NonNull String key, String def) {
        if (propClass == null) return def;
        return (String) Optional.ofNullable(
            callStaticMethod(propClass, "get", new Class[]{String.class, String.class}, key, def)
        ).orElse(def);
    }

    public static String getProp(@NonNull String key) {
        if (propClass == null) return "";
        return callStaticMethod(propClass, "get", new Class[]{String.class}, key);
    }

    /**
     * 获取 String 类型的 prop，指定 ClassLoader
     */
    public static String getProp(@NonNull String key, ClassLoader classLoader) {
        return callStaticMethod(
            "android.os.SystemProperties",
            classLoader,
            "get",
            new Class[]{String.class},
            key
        );
    }

    /**
     * 只有系统框架才可能可以调用
     * <p>
     * 可设置的 prop 类型非常有限，一般情况下使用不到
     */
    public static void setProp(@NonNull String key, String vale) {
        if (propClass == null) return;
        callStaticMethod(propClass, "set", new Class[]{String.class, String.class}, key, vale);
    }
}
