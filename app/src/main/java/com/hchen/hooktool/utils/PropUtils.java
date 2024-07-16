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

import android.annotation.SuppressLint;
import android.content.Context;

/**
 * 本类为 prop 工具，可以获取或者写入系统 prop 条目
 */
@SuppressLint("PrivateApi")
public class PropUtils {
    private static final String TAG = "PropUtils";

    public static String getProp(Context context, String name) {
        try {
            return classLoaderMethod(context, name);
        } catch (Throwable e) {
            logE(TAG, "get string prop failed!", e);
            return "";
        }
    }

    public static boolean getProp(String key, boolean def) {
        try {
            @SuppressLint("PrivateApi") Class<?> cls = Class.forName("android.os.SystemProperties");
            return Boolean.TRUE.equals(invokeMethod(cls, "getBoolean", new Class[]{String.class, boolean.class}, key, def));
        } catch (Throwable e) {
            logE(TAG, "get boolean prop failed!", e);
            return false;
        }
    }

    public static int getProp(String key, int def) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return invokeMethod(cls, "getInt", new Class[]{String.class, int.class}, key, def);
        } catch (Throwable e) {
            logE(TAG, "get int prop failed!", e);
            return 0;
        }
    }

    public static long getProp(String key, long def) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return invokeMethod(cls, "getLong", new Class[]{String.class, long.class}, key, def);
        } catch (Throwable e) {
            logE(TAG, "get long prop failed!", e);
            return 0L;
        }
    }

    public static String getProp(String key, String def) {
        try {
            return invokeMethod(Class.forName("android.os.SystemProperties"),
                    "get", new Class[]{String.class, String.class}, key, def);
        } catch (Throwable e) {
            logE(TAG, "get string prop failed!", e);
            return "";
        }
    }

    public static String getProp(String key) {
        try {
            return invokeMethod(Class.forName("android.os.SystemProperties"),
                    "get", new Class[]{String.class}, key);
        } catch (Throwable e) {
            logE(TAG, "get string no def prop failed!", e);
            return "";
        }
    }

    /**
     * 只有系统核心可以调用。
     * 返回 true 表示成功。
     *
     * @return boolean
     */
    public static boolean setProp(String key, String vale) {
        try {
            invokeMethod(Class.forName("android.os.SystemProperties"),
                    "set", new Class[]{String.class, String.class}, key, vale);
            return true;
        } catch (Throwable e) {
            logE(TAG, "set prop failed!", e);
        }
        return false;
    }

    private static String classLoaderMethod(Context context, String name) {
        ClassLoader classLoader = context.getClassLoader();
        return InvokeUtils.callStaticMethod(InvokeUtils.findClass("android.os.SystemProperties", classLoader),
                "get", new Class[]{String.class}, name);
    }

    /**
     * @noinspection unchecked
     */
    private static <T> T invokeMethod(Class<?> cls, String str, Class<?>[] clsArr, Object... objArr) {
        return InvokeUtils.callStaticMethod(cls, str, clsArr, objArr);
    }
}
