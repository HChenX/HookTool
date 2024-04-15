package com.hchen.hooktool.log;

import de.robv.android.xposed.XposedBridge;

/**
 * 本工具的日志类。
 */
public class XposedLog {
    public static final String hookMain = "[HChen]";
    public static final String mHook = "[HChen:";
    public static final String other = "]";

    public static void logI(String tag, String Log) {
        XposedBridge.log(hookMain + "[" + tag + "][I]: " + Log);
    }

    public static void logI(String name, String tag, String Log) {
        XposedBridge.log(mHook + name + other + "[" + tag + "][I]: " + Log);
    }

    public static void logW(String tag, String Log) {
        XposedBridge.log(hookMain + "[" + tag + "][W]: " + Log);
    }

    public static void logE(String tag, String Log) {
        XposedBridge.log(hookMain + "[" + tag + "][E]: " + Log);
    }

    public static void logE(String tag, Throwable e) {
        XposedBridge.log(hookMain + "[" + tag + "][E]: " + e);
    }
}
