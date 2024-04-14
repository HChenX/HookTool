package com.hchen.hooktool;

import android.util.Log;

import de.robv.android.xposed.XposedBridge;

public class HookLog {
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

    public static void logSI(String name, String tag, String log) {
        Log.i(mHook + name + other, "[" + tag + "][I]: " + log);
    }

    public static void logSI(String tag, String log) {
        Log.i(hookMain, "[" + tag + "][I]: " + log);
    }

    public static void logSW(String tag, String log) {
        Log.w(hookMain, "[" + tag + "][W]: " + log);
    }

    public void logSE(String tag, String log) {
        Log.e(hookMain, "[" + tag + "][E]: " + log);
    }
}
