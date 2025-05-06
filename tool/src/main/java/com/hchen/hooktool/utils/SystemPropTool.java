/*
 * This file is part of HookTool.

 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HChenX
 */
package com.hchen.hooktool.utils;

import java.util.Optional;

/**
 * Prop 工具
 *
 * @author 焕晨HChen
 */
public class SystemPropTool {
    private static final Class<?> propClass = InvokeTool.findClass("android.os.SystemProperties");

    private SystemPropTool() {
    }

    public static String getProp(String key, ClassLoader classLoader) {
        return invokePropMethod(key, classLoader);
    }

    /**
     * 获取 boolean 类型的 prop
     */
    public static boolean getProp(String key, boolean def) {
        return Boolean.TRUE.equals(
            invokePropMethod("getBoolean", new Class[]{String.class, boolean.class}, key, def)
        );
    }

    /**
     * 获取 int 类型的 prop
     */
    public static int getProp(String key, int def) {
        return (int) Optional.ofNullable(
            invokePropMethod("getInt", new Class[]{String.class, int.class}, key, def)
        ).orElse(def);
    }

    /**
     * 获取 long 类型的 prop
     */
    public static long getProp(String key, long def) {
        return (long) Optional.ofNullable(
            invokePropMethod("getLong", new Class[]{String.class, long.class}, key, def)
        ).orElse(def);
    }

    /**
     * 获取 String 类型的 prop
     */
    public static String getProp(String key, String def) {
        return (String) Optional.ofNullable(
            invokePropMethod("get", new Class[]{String.class, String.class}, key, def)
        ).orElse(def);
    }

    /**
     * 获取 String 类型的 prop，无默认值
     */
    public static String getProp(String key) {
        return (String) Optional.ofNullable(
            invokePropMethod("get", new Class[]{String.class}, key)
        ).orElse("");
    }

    /**
     * 只有系统框架才可能可以调用
     */
    public static void setProp(String key, String vale) {
        invokePropMethod("set", new Class[]{String.class, String.class}, key, vale);
    }

    private static String invokePropMethod(String key, ClassLoader classLoader) {
        return (String) Optional.ofNullable(
            InvokeTool.callStaticMethod(
                "android.os.SystemProperties",
                classLoader,
                "get",
                new Class[]{String.class},
                key
            )
        ).orElse("");
    }

    private static <T> T invokePropMethod(String methodName, Class<?>[] classes, Object... objs) {
        return InvokeTool.callStaticMethod(propClass, methodName, classes, objs);
    }
}
