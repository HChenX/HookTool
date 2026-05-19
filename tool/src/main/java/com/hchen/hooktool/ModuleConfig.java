/*
 * This file is part of HookTool.
 *
 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * HookTool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with HookTool. If not, see <https://www.gnu.org/licenses/lgpl-2.1>.
 *
 * Copyright (C) 2024–2026 HChenX
 */
package com.hchen.hooktool;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * ModuleConfig
 * <p>
 * 此类存储模块的基本配置。
 *
 * @author 焕晨HChen
 */
public final class ModuleConfig {
    @NonNull
    private static volatile String logTag;
    @LogLevel
    private static volatile int logLevel;
    @NonNull
    private static volatile String prefsName;
    @NonNull
    private static volatile String[] logExpandPaths;
    @NonNull
    private static volatile String[] logExpandIgnoreClassNames;
    private static volatile boolean isShowHookSuccessLog;

    // -------- 可选日志等级 --------

    /** 错误日志等级。 */
    public static final int LOG_E = 1;

    /** 警告日志等级。 */
    public static final int LOG_W = 2;

    /** 信息日志等级。 */
    public static final int LOG_I = 3;

    /** 调试日志等级。 */
    public static final int LOG_D = 4;

    @IntDef(value = {
        LOG_I,
        LOG_W,
        LOG_E,
        LOG_D
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface LogLevel {
    }

    static {
        logTag = "Unknown";
        logLevel = LOG_I;
        prefsName = "";
        logExpandPaths = new String[]{};
        logExpandIgnoreClassNames = new String[]{};
        isShowHookSuccessLog = false;
    }

    private ModuleConfig() {
    }

    // ------------------------ setter ------------------------

    /**
     * 设置模块日志 tag，推荐设置。
     *
     * @param logTag 日志标签
     */
    public static void setLogTag(@NonNull String logTag) {
        ModuleConfig.logTag = logTag;
    }

    /**
     * 设置模块可输出日志等级，推荐设置。
     *
     * @param logLevel 日志等级
     */
    public static void setLogLevel(@LogLevel int logLevel) {
        ModuleConfig.logLevel = logLevel;
    }

    /**
     * 设置模块默认的共享首选项名称，推荐设置。
     *
     * @param prefsName 共享首选项名称
     */
    public static void setPrefsName(@NonNull String prefsName) {
        ModuleConfig.prefsName = prefsName;
    }

    /**
     * 设置日志增强功能，将帮助工具在代码被混淆时正确获取日志 tag。
     * <p>
     * 此功能可能带来性能影响，请谨慎使用。
     * <p>
     * 需要配置如下混淆规则：
     * <pre>{@code
     * // 包名改成自己的，此配置的含义就是不混淆指定目录下的文件名
     * -keepnames class com.hchen.demo.hook.**
     * -keepnames class com.hchen.demo.hook.**$*
     * }
     *
     * @param logExpandPaths 日志增强路径数组
     */
    public static void setLogExpandPaths(@NonNull String... logExpandPaths) {
        ModuleConfig.logExpandPaths = logExpandPaths;
    }

    /**
     * 使用日志增强功能时应该忽略的类目列表。
     * <p>
     * 帮助工具忽略干扰项。
     *
     * @param logExpandIgnoreClassNames 忽略的类名数组
     */
    public static void setLogExpandIgnoreClassNames(@NonNull String... logExpandIgnoreClassNames) {
        ModuleConfig.logExpandIgnoreClassNames = logExpandIgnoreClassNames;
    }

    /**
     * 是否在 Hook 成功时打印日志，默认关闭。
     * <p>
     * 使用此功能时务必设置 {@link ModuleConfig#setLogExpandPaths(String[])}。
     *
     * @param isShowHookSuccessLog 是否显示 Hook 成功日志
     */
    public static void setShowHookSuccessLog(boolean isShowHookSuccessLog) {
        ModuleConfig.isShowHookSuccessLog = isShowHookSuccessLog;
    }

    // -------------------- getter ----------------------

    /** 获取模块日志标签。 */
    @NonNull
    public static String getLogTag() {
        return logTag;
    }

    /** 获取模块日志等级。 */
    @LogLevel
    public static int getLogLevel() {
        return logLevel;
    }

    /** 获取模块共享首选项名称。 */
    @NonNull
    public static String getPrefsName() {
        return prefsName;
    }

    /** 获取日志增强路径列表。 */
    @NonNull
    public static String[] getLogExpandPaths() {
        return logExpandPaths;
    }

    /** 获取日志增强忽略类名列表。 */
    @NonNull
    public static String[] getLogExpandIgnoreClassNames() {
        return logExpandIgnoreClassNames;
    }

    /** 判断是否显示 Hook 成功日志。 */
    public static boolean isShowHookSuccessLog() {
        return isShowHookSuccessLog;
    }
}