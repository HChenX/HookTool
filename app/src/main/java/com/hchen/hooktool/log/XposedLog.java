package com.hchen.hooktool.log;

import com.hchen.hooktool.HCData;
import com.hchen.hooktool.HCInit;

import de.robv.android.xposed.XposedBridge;

public class XposedLog {
    // -------- logE -------------
    public static void logE(String tag, String log) {
        if (HCData.getLogLevel() < HCInit.LOG_E) return;
        XposedBridge.log(getXposedTag() + "[" + tag + "][" + HCData.getTargetPackageName() + "][E]: " + log);
    }

    public static void logE(String tag, Throwable e) {
        if (HCData.getLogLevel() < HCInit.LOG_E) return;
        XposedBridge.log(getXposedTag() + "[" + tag + "][" + HCData.getTargetPackageName() + "][E]:\n" + LogExpand.printStackTrace(e));
    }

    public static void logE(String tag, String log, String stackTrace) {
        if (HCData.getLogLevel() < HCInit.LOG_E) return;
        XposedBridge.log(getXposedTag() + "[" + tag + "][" + HCData.getTargetPackageName() + "][E]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    public static void logE(String tag, String log, Throwable e) {
        if (HCData.getLogLevel() < HCInit.LOG_E) return;
        XposedBridge.log(getXposedTag() + "[" + tag + "][" + HCData.getTargetPackageName() + "][E]: " + log + "\n[Stack Info]: " + LogExpand.printStackTrace(e));
    }

    // ----------- logW --------------
    public static void logW(String tag, String log) {
        if (HCData.getLogLevel() < HCInit.LOG_W) return;
        XposedBridge.log(getXposedTag() + "[" + tag + "][" + HCData.getTargetPackageName() + "][W]: " + log);
    }

    public static void logW(String tag, Throwable e) {
        if (HCData.getLogLevel() < HCInit.LOG_W) return;
        XposedBridge.log(getXposedTag() + "[" + tag + "][" + HCData.getTargetPackageName() + "][W]:\n" + LogExpand.printStackTrace(e));
    }

    public static void logW(String tag, String log, String stackTrace) {
        if (HCData.getLogLevel() < HCInit.LOG_W) return;
        XposedBridge.log(getXposedTag() + "[" + tag + "][" + HCData.getTargetPackageName() + "][W]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    public static void logW(String tag, String log, Throwable e) {
        if (HCData.getLogLevel() < HCInit.LOG_W) return;
        XposedBridge.log(getXposedTag() + "[" + tag + "][" + HCData.getTargetPackageName() + "][W]: " + log + "\n[Stack Info]: " + LogExpand.printStackTrace(e));
    }

    // ----------- logI --------------
    public static void logI(String log) {
        if (HCData.getLogLevel() < HCInit.LOG_I) return;
        XposedBridge.log(getXposedTag() + "[I]: " + log);
    }

    public static void logI(String tag, String log) {
        if (HCData.getLogLevel() < HCInit.LOG_I) return;
        XposedBridge.log(getXposedTag() + "[" + tag + "][" + HCData.getTargetPackageName() + "][I]: " + log);
    }

    public static void logI(String tag, String log, String stackTrace) {
        if (HCData.getLogLevel() < HCInit.LOG_I) return;
        XposedBridge.log(getXposedTag() + "[" + tag + "][" + HCData.getTargetPackageName() + "][I]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    public static void logI(String tag, String log, Throwable e) {
        if (HCData.getLogLevel() < HCInit.LOG_I) return;
        XposedBridge.log(getXposedTag() + "[" + tag + "][" + HCData.getTargetPackageName() + "][I]: " + log + "\n[Stack Info]: " + LogExpand.printStackTrace(e));
    }

    // ------------ logD --------------
    public static void logD(String tag, String log) {
        if (HCData.getLogLevel() < HCInit.LOG_D) return;
        XposedBridge.log(getXposedTag() + "[" + tag + "][" + HCData.getTargetPackageName() + "][D]: " + log);
    }

    public static void logD(String tag, Throwable e) {
        if (HCData.getLogLevel() < HCInit.LOG_D) return;
        XposedBridge.log(getXposedTag() + "[" + tag + "][" + HCData.getTargetPackageName() + "][D]:\n" + LogExpand.printStackTrace(e));
    }

    public static void logD(String tag, String log, String stackTrace) {
        if (HCData.getLogLevel() < HCInit.LOG_D) return;
        XposedBridge.log(getXposedTag() + "[" + tag + "][" + HCData.getTargetPackageName() + "][D]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    public static void logD(String tag, String log, Throwable e) {
        if (HCData.getLogLevel() < HCInit.LOG_D) return;
        XposedBridge.log(getXposedTag() + "[" + tag + "][" + HCData.getTargetPackageName() + "][D]: " + log + "\n[Stack Info]: " + LogExpand.printStackTrace(e));
    }

    private static String getXposedTag() {
        return "[" + HCData.getTag() + "]";
    }
}
