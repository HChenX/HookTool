package com.hchen.hooktool.log;

import android.util.Log;

/**
 * 没啥用的安卓日志类
 */
public class AndroidLog {
    public static void logI(String tag, String pkg, String log) {
        Log.i(tag, "[" + pkg + "][I]: " + log);
    }

    public static void logI(String tag, String log) {
        Log.i(tag, "[I]: " + log);
    }

    public static void logW(String tag, String log) {
        Log.w(tag, "[W]: " + log);
    }

    public void logE(String tag, String log) {
        Log.e(tag, "[E]: " + log);
    }
}
