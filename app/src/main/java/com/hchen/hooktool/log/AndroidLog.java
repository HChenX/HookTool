package com.hchen.hooktool.log;

public class AndroidLog {
    public static final String hookMain = "[HChen]";
    public static final String mHook = "[HChen:";
    public static final String other = "]";
    public static void logSI(String name, String tag, String log) {
        android.util.Log.i(mHook + name + other, "[" + tag + "][I]: " + log);
    }

    public static void logSI(String tag, String log) {
        android.util.Log.i(hookMain, "[" + tag + "][I]: " + log);
    }

    public static void logSW(String tag, String log) {
        android.util.Log.w(hookMain, "[" + tag + "][W]: " + log);
    }

    public void logSE(String tag, String log) {
        android.util.Log.e(hookMain, "[" + tag + "][E]: " + log);
    }
}
