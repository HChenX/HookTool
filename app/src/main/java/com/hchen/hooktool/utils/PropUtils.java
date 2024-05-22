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
package com.hchen.hooktool.utils;

import static com.hchen.hooktool.log.AndroidLog.logE;

import android.content.Context;

/**
 * 本类为 prop 工具，可以获取或者写入系统 prop 条目。
 */
public class PropUtils {
    private static final String TAG = "PropUtils";

    public static String getProp(Context context, String name) {
        try {
            return classLoaderMethod(context, name);
        } catch (Throwable e) {
            logE(TAG, "get prop string", e);
            return "";
        }
    }

    public static boolean getProp(String name, boolean def) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return Boolean.TRUE.equals(invokeMethod(cls, "getBoolean", new Class[]{String.class, boolean.class}, name, def));
        } catch (Throwable e) {
            logE(TAG, "get prop int", e);
            return false;
        }
    }

    public static int getProp(String name, int def) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return invokeMethod(cls, "getInt", new Class[]{String.class, int.class}, name, def);
        } catch (Throwable e) {
            logE(TAG, "get prop int", e);
            return 0;
        }
    }

    public static long getProp(String name, long def) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return invokeMethod(cls, "getLong", new Class[]{String.class, long.class}, name, def);
        } catch (Throwable e) {
            logE(TAG, "get prop long", e);
            return 0L;
        }
    }

    public static String getProp(String name, String def) {
        try {
            return invokeMethod(Class.forName("android.os.SystemProperties"),
                    "get", new Class[]{String.class, String.class}, name, def);
        } catch (Throwable e) {
            logE(TAG, "get prop String", e);
            return "";
        }
    }

    public static String getProp(String name) {
        try {
            return invokeMethod(Class.forName("android.os.SystemProperties"),
                    "get", new Class[]{String.class}, name);
        } catch (Throwable e) {
            logE(TAG, "get prop string no def", e);
            return "";
        }
    }

    /**
     * 只有系统核心可以调用。
     * 返回 true 表示成功。
     *
     * @return boolean
     */
    public static boolean setProp(String name, String vale) {
        try {
            invokeMethod(Class.forName("android.os.SystemProperties"),
                    "set", new Class[]{String.class, String.class}, name, vale);
            return true;
        } catch (Throwable e) {
            logE(TAG, "set", e);
        }
        return false;
    }

    private static String classLoaderMethod(Context context, String name) throws Throwable {
        ClassLoader classLoader = context.getClassLoader();
        return InvokeUtils.callStaticMethod("android.os.SystemProperties", classLoader,
                "get", new Class[]{String.class}, name);
    }

    /**
     * @noinspection unchecked
     */
    private static <T> T invokeMethod(Class<?> cls, String str, Class<?>[] clsArr, Object... objArr) throws Throwable {
        return InvokeUtils.callStaticMethod(cls, str, clsArr, objArr);
    }
}
