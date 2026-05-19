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
 * HookTool 模块的全局配置中心。
 * <p>
 * 以静态方式集中管理模块的各项运行时配置，包括日志标签、日志等级阈值、
 * SharedPreferences 名称、日志增强扫描路径以及 Hook 成功日志开关等。
 * 所有配置项均通过静态 setter 方法写入、静态 getter 方法读取。
 * <p>
 * 建议在 {@link com.hchen.hooktool.ModuleEntrance#initModuleConfig()} 中
 * 完成全部配置的初始化工作。
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

    /** 错误级别，仅输出错误信息。 */
    public static final int LOG_E = 1;

    /** 警告级别，输出警告及以上等级信息。 */
    public static final int LOG_W = 2;

    /** 信息级别，输出一般信息及以上等级信息。 */
    public static final int LOG_I = 3;

    /** 调试级别，输出全部等级的详细调试信息。 */
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
     * 设置日志输出标签。
     * <p>
     * 推荐在模块初始化时调用，设置一个有意义的标签以便在 Logcat 中过滤日志。
     *
     * @param logTag 日志标签，不可为 null
     */
    public static void setLogTag(@NonNull String logTag) {
        ModuleConfig.logTag = logTag;
    }

    /**
     * 设置日志输出等级阈值。
     * <p>
     * 低于此阈值的日志将被过滤，不会实际输出。可选值：
     * {@link #LOG_E}、{@link #LOG_W}、{@link #LOG_I}、{@link #LOG_D}。
     *
     * @param logLevel 日志等级常量
     */
    public static void setLogLevel(@LogLevel int logLevel) {
        ModuleConfig.logLevel = logLevel;
    }

    /**
     * 设置 SharedPreferences 文件名称。
     * <p>
     * 该名称用于通过远程 SharedPreferences 接口存取模块的持久化配置。
     *
     * @param prefsName SharedPreferences 文件名称，不可为 null
     */
    public static void setPrefsName(@NonNull String prefsName) {
        ModuleConfig.prefsName = prefsName;
    }

    /**
     * 配置日志增强功能的扫描路径。
     * <p>
     * 启用后，工具会在代码被 ProGuard/R8 混淆的情况下通过遍历指定路径
     * 来正确获取日志 TAG。此功能会引入额外性能开销，请按需启用。
     * <p>
     * 使用前需在混淆规则中添加如下配置（将包名替换为实际值）：
     * <pre>{@code
     * -keepnames class com.hchen.demo.hook.**
     * -keepnames class com.hchen.demo.hook.**$*
     * }</pre>
     *
     * @param logExpandPaths 需要扫描的包路径数组，支持可变参数
     */
    public static void setLogExpandPaths(@NonNull String... logExpandPaths) {
        ModuleConfig.logExpandPaths = logExpandPaths;
    }

    /**
     * 设置日志增强扫描时应忽略的类名列表。
     * <p>
     * 扫描调用栈时会跳过此处指定的类，以排除干扰项并提高 TAG 识别准确性。
     *
     * @param logExpandIgnoreClassNames 需要忽略的完整类名数组，支持可变参数
     */
    public static void setLogExpandIgnoreClassNames(@NonNull String... logExpandIgnoreClassNames) {
        ModuleConfig.logExpandIgnoreClassNames = logExpandIgnoreClassNames;
    }

    /**
     * 设置是否在 Hook 成功时输出日志，默认关闭。
     * <p>
     * 启用前须先通过 {@link #setLogExpandPaths(String...)} 配置日志增强路径，
     * 否则在代码混淆场景下可能无法正确输出日志标签。
     *
     * @param isShowHookSuccessLog {@code true} 开启，{@code false} 关闭
     */
    public static void setShowHookSuccessLog(boolean isShowHookSuccessLog) {
        ModuleConfig.isShowHookSuccessLog = isShowHookSuccessLog;
    }

    // -------------------- getter ----------------------

    /**
     * 获取当前日志标签。
     *
     * @return 日志标签字符串
     */
    @NonNull
    public static String getLogTag() {
        return logTag;
    }

    /**
     * 获取当前日志输出等级。
     *
     * @return 日志等级常量
     */
    @LogLevel
    public static int getLogLevel() {
        return logLevel;
    }

    /**
     * 获取当前 SharedPreferences 文件名称。
     *
     * @return SharedPreferences 名称字符串
     */
    @NonNull
    public static String getPrefsName() {
        return prefsName;
    }

    /**
     * 获取日志增强扫描路径列表。
     *
     * @return 扫描路径数组
     */
    @NonNull
    public static String[] getLogExpandPaths() {
        return logExpandPaths;
    }

    /**
     * 获取日志增强扫描时忽略的类名列表。
     *
     * @return 忽略类名数组
     */
    @NonNull
    public static String[] getLogExpandIgnoreClassNames() {
        return logExpandIgnoreClassNames;
    }

    /**
     * 查询 Hook 成功日志是否已启用。
     *
     * @return {@code true} 表示已启用，{@code false} 表示已关闭
     */
    public static boolean isShowHookSuccessLog() {
        return isShowHookSuccessLog;
    }
}
