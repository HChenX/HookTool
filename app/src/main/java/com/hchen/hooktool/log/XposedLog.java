package com.hchen.hooktool.log;

import de.robv.android.xposed.XposedBridge;

/**
 * 本工具的日志类。
 */
public class XposedLog {

    public static void logI(String tag, String log) {
        XposedBridge.log(tag + "[I]: " + log);
    }

    public static void logI(String tag, String pkg, String log) {
        XposedBridge.log(tag + "[" + pkg + "][I]: " + log);
    }

    public static void logW(String tag, String log) {
        XposedBridge.log(tag + "[W]: " + log);
    }

    public static void logE(String tag, String log) {
        XposedBridge.log(tag + "[E]: " + log);
    }

    public static void logE(String tag, Throwable e) {
        XposedBridge.log(tag + "[E]: " + e);
    }

    public static void logD(String tag, Throwable e) {
        XposedBridge.log(tag + "[D]: " + e);
    }

    public static void logD(String tag, String e) {
        XposedBridge.log(tag + "[D]: " + e);
    }
}
