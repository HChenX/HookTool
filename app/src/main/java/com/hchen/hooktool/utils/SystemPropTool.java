package com.hchen.hooktool.utils;

import java.util.Optional;

public class SystemPropTool {
    private static final Class<?> clazz = InvokeTool.findClass("android.os.SystemProperties");

    public static String getProp(String name, ClassLoader classLoader) {
        return classLoaderMethod(name, classLoader);
    }

    public static boolean getProp(String key, boolean def) {
        return Boolean.TRUE.equals(
            invokeMethod("getBoolean", new Class[]{String.class, boolean.class}, key, def)
        );
    }

    public static int getProp(String key, int def) {
        return (int) Optional.ofNullable(
            invokeMethod("getInt", new Class[]{String.class, int.class}, key, def)
        ).orElse(def);
    }

    public static long getProp(String key, long def) {
        return (long) Optional.ofNullable(
            invokeMethod("getLong", new Class[]{String.class, long.class}, key, def)
        ).orElse(def);
    }

    public static String getProp(String key, String def) {
        return (String) Optional.ofNullable(
            invokeMethod("get", new Class[]{String.class, String.class}, key, def)
        ).orElse(def);
    }

    public static String getProp(String key) {
        return (String) Optional.ofNullable(
            invokeMethod("get", new Class[]{String.class}, key)
        ).orElse("");
    }

    /**
     * 只有系统框架才可能可以调用。
     */
    public static void setProp(String key, String vale) {
        invokeMethod("set", new Class[]{String.class, String.class}, key, vale);
    }

    private static String classLoaderMethod(String name, ClassLoader classLoader) {
        return (String) Optional.ofNullable(
            InvokeTool.callStaticMethod(
                "android.os.SystemProperties",
                classLoader,
                "get",
                new Class[]{String.class},
                name
            )
        ).orElse("");
    }

    private static <T> T invokeMethod(String str, Class<?>[] clsArr, Object... objArr) {
        return InvokeTool.callStaticMethod(clazz, str, clsArr, objArr);
    }
}
