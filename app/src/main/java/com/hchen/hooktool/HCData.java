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

 * Copyright (C) 2023-2025 HChenX
 */
package com.hchen.hooktool;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 工具数据
 *
 * @author 焕晨HChen
 */
public class HCData {
    @NonNull
    private static String TAG = "Unknown";
    private static int logLevel = HCInit.LOG_I;
    @NonNull
    private static String modulePackageName = "";
    @NonNull
    private static String targetPackageName = "UnknownPackage";
    @NonNull
    private static String prefsName = "";
    private static boolean isAutoReload = true;
    private static boolean isXposed = false;
    @Nullable
    private static String[] logExpandPath = null;
    @Nullable
    private static String[] logExpandIgnoreClassNames = null;
    @NonNull
    private static ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    @Nullable
    private static XC_LoadPackage.LoadPackageParam loadPackageParam;
    @Nullable
    private static IXposedHookZygoteInit.StartupParam startupParam;

    private HCData() {
    }

    @NonNull
    public static String getTag() {
        return TAG;
    }

    public static int getLogLevel() {
        return logLevel;
    }

    @NonNull
    public static String getModulePackageName() {
        return modulePackageName;
    }

    @NonNull
    public static String getTargetPackageName() {
        return targetPackageName;
    }

    @NonNull
    public static String getPrefsName() {
        return prefsName;
    }

    public static boolean isAutoReload() {
        return isAutoReload;
    }

    public static boolean isXposed() {
        return isXposed;
    }

    @Nullable
    public static String[] getLogExpandPath() {
        return logExpandPath;
    }

    @Nullable
    public static String[] getLogExpandIgnoreClassNames() {
        return logExpandIgnoreClassNames;
    }

    @Nullable
    public static String getModulePath() {
        if (startupParam != null) {
            return startupParam.modulePath;
        }
        return null;
    }

    @NonNull
    public static ClassLoader getClassLoader() {
        return classLoader;
    }

    @Nullable
    public static XC_LoadPackage.LoadPackageParam getLoadPackageParam() {
        return loadPackageParam;
    }

    @Nullable
    public static IXposedHookZygoteInit.StartupParam getStartupParam() {
        return startupParam;
    }

    // ------------------------ 工具内部使用 -------------------------------
    protected static void setTag(@NonNull String tag) {
        HCData.TAG = tag;
    }

    protected static void setLogLevel(int logLevel) {
        HCData.logLevel = logLevel;
    }

    protected static void setModulePackageName(@NonNull String modulePackageName) {
        HCData.modulePackageName = modulePackageName;
    }

    protected static void setPrefsName(@NonNull String prefsName) {
        HCData.prefsName = prefsName;
    }

    protected static void setAutoReload(boolean isAutoReload) {
        HCData.isAutoReload = isAutoReload;
    }

    protected static void setIsXposed(boolean isXposed) {
        HCData.isXposed = isXposed;
    }

    protected static void setLogExpandPath(@NonNull String... logExpandPath) {
        HCData.logExpandPath = logExpandPath;
    }

    protected static void setLogExpandIgnoreClassNames(@NonNull String... logExpandIgnoreClassNames) {
        HCData.logExpandIgnoreClassNames = logExpandIgnoreClassNames;
    }

    protected static void setClassLoader(@NonNull ClassLoader classLoader) {
        HCData.classLoader = classLoader;
        HCBase.classLoader = classLoader;
    }

    protected static void setLoadPackageParam(@NonNull XC_LoadPackage.LoadPackageParam loadPackageParam) {
        HCData.loadPackageParam = loadPackageParam;
        HCData.targetPackageName = loadPackageParam.packageName;
        HCBase.loadPackageParam = loadPackageParam;
    }

    protected static void setStartupParam(@NonNull IXposedHookZygoteInit.StartupParam startupParam) {
        HCData.startupParam = startupParam;
    }
}