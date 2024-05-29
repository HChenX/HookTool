/*
 * This file is part of HookTool.

 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2024 HookTool Contributions
 */
package com.hchen.hooktool;

import static com.hchen.hooktool.log.XposedLog.logI;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 初始化类，请在 Hook 入口处初始化本类。
 */
public class HCInit {
    private static XC_LoadPackage.LoadPackageParam lpparam = null;
    private static ClassLoader classLoader = null;
    private static boolean canUseSystemClassLoader = false;
    private static String packageName;
    private static String TAG = "Unknown";
    public static String spareTag = "Unknown";
    private static int logLevel = 0;

    public static void setTAG(String tag) {
        TAG = "[" + tag + "]";
        spareTag = tag;
    }

    public static String getTAG() {
        return TAG;
    }

    /* 日志等级 */
    public static void setLogLevel(int level) {
        logLevel = level;
    }

    public static int getLogLevel() {
        return logLevel;
    }

    /**
     * 设置在 classLoader 为 null 时允许使用系统 classLoader
     */
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
        logI(spareTag, "init lpparam: [" + lpparam + "] classLoader: [" + classLoader + "] pkgName: " + packageName);
    }

    protected static XC_LoadPackage.LoadPackageParam getLoadPackageParam() throws Throwable {
        if (lpparam != null) return lpparam;
        throw new Throwable("Failed to obtain LoadPackageParam, it is null!");
    }

    protected static ClassLoader getClassLoader() {
        if (classLoader != null) return classLoader;
        if (canUseSystemClassLoader) {
            return getSystemClassLoader();
        }
        throw new RuntimeException("Failed to obtain ClassLoader! It is null!");
    }

    private static ClassLoader getClassLoaderIfExists() {
        return classLoader;
    }

    private static ClassLoader getSystemClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }

    public static boolean isInitDone() {
        // if (lpparam == null) return false;
        if (canUseSystemClassLoader) return true;
        return classLoader != null;
    }
}
