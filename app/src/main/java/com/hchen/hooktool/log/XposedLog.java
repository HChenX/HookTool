package com.hchen.hooktool.log;

import com.hchen.hooktool.HCInit;

import de.robv.android.xposed.XposedBridge;

/**
 * 本工具的日志类。
 */
public class XposedLog {
    private static final String rootTag = HCInit.getTAG();

    public static void logI(String tag, String log) {
        XposedBridge.log(rootTag + "[" + tag + "]" + "[I]: " + log);
    }

    public static void logI(String tag, String pkg, String log) {
        XposedBridge.log(rootTag + "[" + tag + "]" + "[" + pkg + "][I]: " + log);
    }

    public static void logW(String tag, String log) {
        XposedBridge.log(rootTag + "[" + tag + "]" + "[W]: " + log);
    }

    public static void logE(String tag, String log) {
        XposedBridge.log(rootTag + "[" + tag + "]" + "[E]: " + log);
    }

    public static void logE(String tag, Throwable e) {
        XposedBridge.log(rootTag + "[" + tag + "]" + "[E]: " + e);
    }

    public static void logD(String tag, Throwable e) {
        XposedBridge.log(rootTag + "[" + tag + "]" + "[D]: " + e);
    }

    public static void logD(String tag, String e) {
        XposedBridge.log(rootTag + "[" + tag + "]" + "[D]: " + e);
    }
}
