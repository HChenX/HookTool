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
package com.hchen.hooktool.config;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 模块全局配置。
 *
 * @author 焕晨HChen
 */
public final class HookConfig {
    @NonNull
    private static volatile String sLogTag;
    @LogLevel
    private static volatile int sLogLevel;
    @NonNull
    private static volatile String sPrefsName;
    @NonNull
    private static volatile Set<String> sLogExpandPaths;
    @NonNull
    private static volatile Set<String> sLogExpandIgnoreClassNames;
    private static volatile boolean sHookSuccessLogEnabled;

    /**
     * 错误等级，仅输出错误。
     */
    public static final int LOG_E = 1;
    /**
     * 警告等级，输出警告与错误。
     */
    public static final int LOG_W = 2;
    /**
     * 信息等级，默认值。
     */
    public static final int LOG_I = 3;
    /**
     * 调试等级，输出全部日志。
     */
    public static final int LOG_D = 4;

    @IntDef({LOG_E, LOG_W, LOG_I, LOG_D})
    @Retention(RetentionPolicy.SOURCE)
    private @interface LogLevel {
    }

    static {
        sLogTag = "Unknown";
        sLogLevel = LOG_I;
        sPrefsName = "";
        sLogExpandPaths = Collections.emptySet();
        sLogExpandIgnoreClassNames = Collections.emptySet();
        sHookSuccessLogEnabled = false;
    }

    private HookConfig() {
    }

    /**
     * 设置日志标签。
     *
     * @param logTag 日志标签
     */
    public static void setLogTag(@NonNull String logTag) {
        sLogTag = logTag;
    }

    /**
     * 设置日志等级阈值。
     *
     * @param logLevel 日志等级，取值为 {@link #LOG_E}、{@link #LOG_W}、{@link #LOG_I}、{@link #LOG_D}
     */
    public static void setLogLevel(@LogLevel int logLevel) {
        sLogLevel = logLevel;
    }

    /**
     * 设置配置文件名。
     *
     * @param prefsName 配置文件名
     */
    public static void setPrefsName(@NonNull String prefsName) {
        sPrefsName = prefsName;
    }

    /**
     * 设置用于推断日志标签的包路径。
     * <p>
     * 代码被混淆时需要添加对应的保留规则：
     * <pre>{@code
     * -keepnames class com.example.hook.**
     * -keepnames class com.example.hook.**$*
     * }</pre>
     *
     * @param paths 待扫描的包路径
     */
    public static void setLogExpandPaths(@NonNull Set<String> paths) {
        sLogExpandPaths = new HashSet<>(paths);
    }

    /**
     * 设置推断日志标签时忽略的类名。
     *
     * @param names 完整类名集合
     */
    public static void setLogExpandIgnoreClassNames(@NonNull Set<String> names) {
        sLogExpandIgnoreClassNames = new HashSet<>(names);
    }

    /**
     * 设置是否输出 Hook 成功日志。
     *
     * @param enabled 输出时传入 true
     */
    public static void setHookSuccessLogEnabled(boolean enabled) {
        sHookSuccessLogEnabled = enabled;
    }

    /**
     * 获取日志标签。
     *
     * @return 日志标签
     */
    @NonNull
    public static String getLogTag() {
        return sLogTag;
    }

    /**
     * 获取日志等级。
     *
     * @return 日志等级
     */
    @LogLevel
    public static int getLogLevel() {
        return sLogLevel;
    }

    /**
     * 获取配置文件名。
     *
     * @return 配置文件名
     */
    @NonNull
    public static String getPrefsName() {
        return sPrefsName;
    }

    /**
     * 获取用于推断日志标签的包路径。
     *
     * @return 包路径集合
     */
    @NonNull
    public static Set<String> getLogExpandPaths() {
        return new HashSet<>(sLogExpandPaths);
    }

    /**
     * 获取推断日志标签时忽略的类名。
     *
     * @return 类名集合
     */
    @NonNull
    public static Set<String> getLogExpandIgnoreClassNames() {
        return new HashSet<>(sLogExpandIgnoreClassNames);
    }

    /**
     * 判断是否输出 Hook 成功日志。
     *
     * @return 已开启时返回 true
     */
    public static boolean isHookSuccessLogEnabled() {
        return sHookSuccessLogEnabled;
    }
}
