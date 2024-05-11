package com.hchen.hooktool;

import android.support.annotation.Nullable;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookInit {
    private static XC_LoadPackage.LoadPackageParam lpparam = null;
    private static ClassLoader classLoader = null;
    private static boolean canUseSystemClassLoader = false;
    private static String packageName;
    private static String thisTAG = "[HChen][HookInit]: ";
    private static String TAG = null;
    private static int logLevel = 0;

    public static void setTAG(String tag) {
        thisTAG = "[HChen]" + "[" + tag + "]: ";
        TAG = tag;
    }

    public static String getTAG() {
        return TAG;
    }

    public static void setLogLevel(int level) {
        logLevel = level;
    }

    public static int getLogLevel() {
        return logLevel;
    }

    public static void setCanUseSystemClassLoader(boolean use) {
        canUseSystemClassLoader = use;
    }

    /**
     * 请在初始化时调用。
     */
    public static void initLoadPackageParam(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        lpparam = loadPackageParam;
        classLoader = loadPackageParam.classLoader;
        packageName = lpparam.packageName;
    }

    public static XC_LoadPackage.LoadPackageParam getLoadPackageParam() throws Throwable {
        if (lpparam != null) return lpparam;
        throw new Throwable("Failed to obtain LoadPackageParam, it is null!");
    }

    public static ClassLoader getClassLoader() {
        if (classLoader != null) return classLoader;
        if (canUseSystemClassLoader) {
            return getSystemClassLoader();
        }
        throw new RuntimeException("Failed to obtain ClassLoader! It is null!");
    }

    @Nullable
    public static ClassLoader getClassLoaderIfExists() {
        return classLoader;
    }

    public static ClassLoader getSystemClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }

    public static boolean isInitDone() {
        // if (lpparam == null) return false;
        if (canUseSystemClassLoader) return true;
        return classLoader != null;
    }
}
