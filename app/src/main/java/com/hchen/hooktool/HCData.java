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

import android.content.pm.ApplicationInfo;

import androidx.annotation.Nullable;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 核心数据
 *
 * @author 焕晨HChen
 */
public final class HCData {
    private static String mInitTag = "[Unknown]";
    private static String mSpareTag = "Unknown";
    private static int mInitLogLevel = HCInit.LOG_I;
    private static String mModulePackageName = "";
    private static String mPrefsName = "";
    private static String[] mLogExpandPath = null;
    private static boolean isAutoReload = true;
    private static boolean isXposed = false;
    private static ClassLoader mClassLoader = null;
    private static XC_LoadPackage.LoadPackageParam mLpparam = null;
    private static IXposedHookZygoteInit.StartupParam mStartupParam = null;

    // ---------------------------- Getter -----------------------------------

    /**
     * 带 [] 的 TAG。
     * <p>
     * 例如: [HookTool]
     */
    public static String getInitTag() {
        return mInitTag;
    }

    /**
     * 原始的 TAG。
     */
    public static String getSpareTag() {
        return mSpareTag;
    }

    /**
     * 获取日志等级。
     */
    public static int getInitLogLevel() {
        return mInitLogLevel;
    }

    /**
     * 获取模块的包名。
     */
    public static String getModulePackageName() {
        return mModulePackageName;
    }

    /**
     * 获取自定义的 prefs 名。
     */
    public static String getPrefsName() {
        return mPrefsName;
    }

    /**
     * 获取日志增强 path。
     */
    public static String[] getLogExpandPath() {
        return mLogExpandPath;
    }

    /**
     * 是否自动更新 prefs。
     */
    public static boolean isAutoReload() {
        return isAutoReload;
    }

    /**
     * 是否处于 xposed 环境。
     */
    public static boolean isXposed() {
        return isXposed;
    }

    /**
     * 获取当前的 Classloader。
     */
    public static ClassLoader getClassLoader() {
        return mClassLoader;
    }

    /**
     * 获取当前的 LoadPackageParam。
     */
    public static XC_LoadPackage.LoadPackageParam getLoadPackageParam() {
        return mLpparam;
    }

    /**
     * 获取当前的 StartupParam。
     */
    public static IXposedHookZygoteInit.StartupParam getStartupParam() {
        return mStartupParam;
    }

    @Nullable
    public static String getModulePath() {
        if (mStartupParam != null)
            return mStartupParam.modulePath;
        return null;
    }

    public static String getPackageName() {
        if (mLpparam != null)
            return mLpparam.packageName;
        return "Unknown";
    }

    @Nullable
    public static ApplicationInfo getAppInfo() {
        if (mLpparam != null)
            return mLpparam.appInfo;
        return null;
    }

    public static boolean isFirstApplication() {
        if (mLpparam != null)
            return mLpparam.isFirstApplication;
        return false;
    }

    public static String getProcessName() {
        if (mLpparam != null)
            return mLpparam.processName;
        return "Unknown";
    }

    // ---------------------------- Setter ----------------------------------

    static void setInitTag(String mInitTag) {
        HCData.mInitTag = mInitTag;
    }

    static void setSpareTag(String mSpareTag) {
        HCData.mSpareTag = mSpareTag;
    }

    static void setInitLogLevel(int mInitLogLevel) {
        HCData.mInitLogLevel = mInitLogLevel;
    }

    static void setModulePackageName(String mModulePackageName) {
        HCData.mModulePackageName = mModulePackageName;
    }

    static void setPrefsName(String mPrefsName) {
        HCData.mPrefsName = mPrefsName;
    }

    static void setLogExpandPath(String[] mLogExpandPath) {
        HCData.mLogExpandPath = mLogExpandPath;
    }

    static void setAutoReload(boolean isAutoReload) {
        HCData.isAutoReload = isAutoReload;
    }

    static void setIsXposed(boolean isXposed) {
        HCData.isXposed = isXposed;
    }

    static void setClassLoader(ClassLoader mClassLoader) {
        HCData.mClassLoader = mClassLoader;
    }

    static void setLoadPackageParam(XC_LoadPackage.LoadPackageParam mLpparam) {
        HCData.mLpparam = mLpparam;
    }

    static void setStartupParam(IXposedHookZygoteInit.StartupParam mStartupParam) {
        HCData.mStartupParam = mStartupParam;
    }
}