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

import android.content.pm.ApplicationInfo;

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
    private static String mPackageName = null;
    private static String mProcessName = null;
    private static String mModulePackageName = null;
    private static String mModulePath = null;
    private static String mPrefsName = null;
    private static String[] mLogExpandPath = null;
    private static boolean isAutoReload = true;
    private static boolean isXposed = false;
    private static boolean isFirstApplication = false;
    private static ApplicationInfo mAppInfo = null;
    private static ClassLoader mClassLoader = null;
    private static XC_LoadPackage.LoadPackageParam mLpparam = null;
    private static IXposedHookZygoteInit.StartupParam mStartupParam = null;

    public static String getInitTag() {
        return mInitTag;
    }

    static void setInitTag(String mInitTag) {
        HCData.mInitTag = mInitTag;
    }

    public static String getSpareTag() {
        return mSpareTag;
    }

    static void setSpareTag(String mSpareTag) {
        HCData.mSpareTag = mSpareTag;
    }

    public static int getInitLogLevel() {
        return mInitLogLevel;
    }

    static void setInitLogLevel(int mInitLogLevel) {
        HCData.mInitLogLevel = mInitLogLevel;
    }

    public static String getModulePackageName() {
        return mModulePackageName;
    }

    static void setModulePackageName(String mModulePackageName) {
        HCData.mModulePackageName = mModulePackageName;
    }

    public static String getPrefsName() {
        return mPrefsName;
    }

    static void setPrefsName(String mPrefsName) {
        HCData.mPrefsName = mPrefsName;
    }

    public static String[] getLogExpandPath() {
        return mLogExpandPath;
    }

    static void setLogExpandPath(String[] mLogExpandPath) {
        HCData.mLogExpandPath = mLogExpandPath;
    }

    public static boolean isIsAutoReload() {
        return isAutoReload;
    }

    static void setIsAutoReload(boolean isAutoReload) {
        HCData.isAutoReload = isAutoReload;
    }

    public static boolean isXposed() {
        return isXposed;
    }

    static void setIsXposed(boolean isXposed) {
        HCData.isXposed = isXposed;
    }

    public static ClassLoader getClassLoader() {
        return mClassLoader;
    }

    static void setClassLoader(ClassLoader mClassLoader) {
        HCData.mClassLoader = mClassLoader;
    }

    public static XC_LoadPackage.LoadPackageParam getLpparam() {
        return mLpparam;
    }

    static void setLpparam(XC_LoadPackage.LoadPackageParam mLpparam) {
        HCData.mLpparam = mLpparam;
    }

    public static IXposedHookZygoteInit.StartupParam getStartupParam() {
        return mStartupParam;
    }

    static void setStartupParam(IXposedHookZygoteInit.StartupParam mStartupParam) {
        HCData.mStartupParam = mStartupParam;
    }

    public static String getModulePath() {
        return mModulePath;
    }

    static void setModulePath(String mModulePath) {
        HCData.mModulePath = mModulePath;
    }

    public static String getPackageName() {
        return mPackageName;
    }

    static void setPackageName(String mPackageName) {
        HCData.mPackageName = mPackageName;
    }

    public static ApplicationInfo getAppInfo() {
        return mAppInfo;
    }

    static void setAppInfo(ApplicationInfo mAppInfo) {
        HCData.mAppInfo = mAppInfo;
    }

    public static boolean isFirstApplication() {
        return isFirstApplication;
    }

    static void setIsFirstApplication(boolean isFirstApplication) {
        HCData.isFirstApplication = isFirstApplication;
    }

    public static String getProcessName() {
        return mProcessName;
    }

    static void setProcessName(String mProcessName) {
        HCData.mProcessName = mProcessName;
    }
}