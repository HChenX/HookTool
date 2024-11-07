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

 * Copyright (C) 2023-2024 HookTool Contributions
 */
package com.hchen.hooktool.tool.additional;

import android.annotation.SuppressLint;

import java.util.Optional;

/**
 * Prop 工具，可以获取或者写入系统 prop 条目
 *
 * @author 焕晨HChen
 */
@SuppressLint("PrivateApi")
public class SystemPropTool {
    private static final Class<?> clazz = InvokeTool.findClass("android.os.SystemProperties");

    public static String getProp(ClassLoader classLoader, String name) {
        return classLoaderMethod(classLoader, name);
    }

    public static boolean getProp(String key, boolean def) {
        return Boolean.TRUE.equals(invokeMethod("getBoolean", new Class[]{String.class, boolean.class}, key, def));
    }

    public static int getProp(String key, int def) {
        return (int) Optional.ofNullable(invokeMethod("getInt", new Class[]{String.class, int.class}, key, def))
                .orElse(def);
    }

    public static long getProp(String key, long def) {
        return (long) Optional.ofNullable(invokeMethod("getLong", new Class[]{String.class, long.class}, key, def))
                .orElse(def);
    }

    public static String getProp(String key, String def) {
        return (String) Optional.ofNullable(invokeMethod("get", new Class[]{String.class, String.class}, key, def))
                .orElse(def);
    }

    public static String getProp(String key) {
        return (String) Optional.ofNullable(invokeMethod("get", new Class[]{String.class}, key))
                .orElse("");
    }

    /**
     * 只有系统核心可以调用。
     */
    public static void setProp(String key, String vale) {
        invokeMethod("set", new Class[]{String.class, String.class}, key, vale);
    }

    private static String classLoaderMethod(ClassLoader classLoader, String name) {
        return (String) Optional.ofNullable(InvokeTool.callStaticMethod(
                InvokeTool.findClass("android.os.SystemProperties", classLoader),
                "get", new Class[]{String.class}, name)).orElse("");
    }

    private static <T> T invokeMethod(String str, Class<?>[] clsArr, Object... objArr) {
        return InvokeTool.callStaticMethod(clazz, str, clsArr, objArr);
    }
}
