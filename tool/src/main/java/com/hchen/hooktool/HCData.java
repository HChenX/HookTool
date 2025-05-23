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
    private static String targetPackageName = "Unknown";
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

    /**
     * 获取当前日志等级
     */
    @NonNull
    public static String getTag() {
        return TAG;
    }

    /**
     * 获取当前日志 TAG
     */
    public static int getLogLevel() {
        return logLevel;
    }

    /**
     * 获取当前设置的模块包名
     */
    @NonNull
    public static String getModulePackageName() {
        return modulePackageName;
    }

    /**
     * 获取当前注入的软件包名
     */
    @NonNull
    public static String getTargetPackageName() {
        return targetPackageName;
    }

    /**
     * 获取当前使用的共享首选项名称
     */
    @NonNull
    public static String getPrefsName() {
        return prefsName;
    }

    /**
     * 是否自动更新 Xprefs
     */
    public static boolean isAutoReload() {
        return isAutoReload;
    }

    /**
     * 是否处于 注入/Xposed 环境
     */
    public static boolean isXposed() {
        return isXposed;
    }

    /**
     * 获取当前使用日志增强的路径
     */
    @Nullable
    public static String[] getLogExpandPath() {
        return logExpandPath;
    }

    /**
     * 获取日志增强时应忽略的类名列表
     */
    @Nullable
    public static String[] getLogExpandIgnoreClassNames() {
        return logExpandIgnoreClassNames;
    }

    /**
     * 获取模块 Apk 的路径
     */
    @Nullable
    public static String getModulePath() {
        if (startupParam != null) {
            return startupParam.modulePath;
        }
        return null;
    }

    /**
     * 获取当前的类加载器
     */
    @NonNull
    public static ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * 获取当前的 LoadPackageParam
     */
    @Nullable
    public static XC_LoadPackage.LoadPackageParam getLoadPackageParam() {
        return loadPackageParam;
    }

    /**
     * 获取当前的 StartupParam
     */
    @Nullable
    public static IXposedHookZygoteInit.StartupParam getStartupParam() {
        return startupParam;
    }

    // ------------------------ 工具内部使用 -------------------------------

    /**
     * 设置工具日志 TAG
     */
    public static void setTag(@NonNull String tag) {
        HCData.TAG = tag;
    }

    /**
     * 设置日志等级
     */
    public static void setLogLevel(@HCInit.LogLevel int logLevel) {
        HCData.logLevel = logLevel;
    }

    /**
     * 设置本模块包名
     */
    public static void setModulePackageName(@NonNull String modulePackageName) {
        HCData.modulePackageName = modulePackageName;
    }

    /**
     * 设置通用共享首选项名称
     * <p>
     * 不设置默认使用：模块包名_prefs
     */
    public static void setPrefsName(@NonNull String prefsName) {
        HCData.prefsName = prefsName;
    }

    /**
     * 是否自动更新 Xprefs
     * <p>
     * 默认开启
     */
    public static void setAutoReload(boolean isAutoReload) {
        HCData.isAutoReload = isAutoReload;
    }

    /**
     * 指定使用日志增强功能的路径
     * <p>
     * 开启的路径可正常获取类名作为日志 TAG
     * <p>
     * 请注意本功能存在性能消耗，介意勿开
     * <p>
     * 同时加入类似的混淆规则:
     * <pre>{@code
     *     -keep class com.hchen.demo.hook.**
     *     -keep class com.hchen.demo.hook.**$*
     * }
     */
    public static void setLogExpandPath(@NonNull String... logExpandPath) {
        HCData.logExpandPath = logExpandPath;
    }

    /**
     * 设置使用日志增强时应忽略的类名
     */
    public static void setLogExpandIgnoreClassNames(@NonNull String... logExpandIgnoreClassNames) {
        HCData.logExpandIgnoreClassNames = logExpandIgnoreClassNames;
    }

    /**
     * 设置类加载器
     */
    public static void setClassLoader(@NonNull ClassLoader classLoader) {
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

    /**
     * @noinspection SameParameterValue
     */
    protected static void setIsXposed(boolean isXposed) {
        HCData.isXposed = isXposed;
    }
}