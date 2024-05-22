package com.hchen.hooktool.log;

import android.util.Log;

import com.hchen.hooktool.HCInit;

/**
 * 没啥用的安卓日志类
 */
public class AndroidLog {
    private static final String rootTag = HCInit.getTAG();

    public static void logI(String tag, String pkg, String log) {
        Log.i(rootTag, "[" + tag + "]" + "[" + pkg + "][I]: " + log);
    }

    public static void logI(String tag, String log) {
        Log.i(rootTag, "[" + tag + "]" + "[I]: " + log);
    }

    public static void logW(String tag, String log) {
        Log.w(rootTag, "[" + tag + "]" + "[W]: " + log);
    }

    public static void logE(String tag, String log) {
        Log.e(rootTag, "[" + tag + "]" + "[E]: " + log);
    }

    public static void logE(String tag, String log, Throwable throwable) {
        Log.e(rootTag, "[" + tag + "]" + "[E]: " + log, throwable);
    }
}
