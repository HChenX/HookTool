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
package com.hchen.hooktool.log;

import android.util.Log;

import com.hchen.hooktool.ModuleConfig;
import com.hchen.hooktool.ModuleData;

/**
 * Xposed 运行时环境专用日志输出工具类。
 * <p>
 * 与 {@link AndroidLog} 直接调用 {@link android.util.Log} 不同，
 * 本类通过 {@link ModuleData#getWrapper()} 获取 Xposed 运行时的日志代理接口进行输出，
 * 确保日志能够正确地出现在 Xposed 宿主环境（如 LSPosed 管理器）的日志面板中。
 * <p>
 * 支持四个日志优先级：ERROR（{@code E}）、WARN（{@code W}）、INFO（{@code I}）和 DEBUG（{@code D}），
 * 每个优先级均提供纯文本、附带调用栈字符串、附带异常对象及两者兼具等多种重载形式。
 * 所有输出均受 {@link ModuleConfig#getLogLevel()} 全局日志等级控制。
 *
 * @author 焕晨HChen
 * @see AndroidLog
 * @see ModuleData#getWrapper()
 */
public class XposedLog {
    protected XposedLog() {
    }

    // -------- logE -------------
    /**
     * 以 ERROR 级别输出一条纯文本日志至 Xposed 运行时日志系统。
     * <p>
     * 当全局日志等级低于 {@link ModuleConfig#LOG_E} 时，此调用将被静默跳过。
     *
     * @param tag 业务侧自定义标识，将传递给 Xposed 日志代理
     * @param log 待输出的日志正文
     */
    public static void logE(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        ModuleData.getWrapper().log(Log.ERROR, tag, "[E]: " + log);
    }

    /**
     * 以 ERROR 级别输出一条仅包含异常信息的日志至 Xposed 运行时日志系统。
     *
     * @param tag 业务侧自定义标识
     * @param e   待记录的异常实例
     */
    public static void logE(String tag, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        ModuleData.getWrapper().log(Log.ERROR, tag, "[E]: ", e);
    }

    /**
     * 以 ERROR 级别输出一条附带调用栈字符串的日志至 Xposed 运行时日志系统。
     *
     * @param tag        业务侧自定义标识
     * @param log        待输出的日志正文
     * @param stackTrace 以字符串形式提供的调用栈信息，将追加到消息末尾
     */
    public static void logE(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        ModuleData.getWrapper().log(Log.ERROR, tag, "[E]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    /**
     * 以 ERROR 级别输出一条同时包含文本描述和异常信息的日志至 Xposed 运行时日志系统。
     *
     * @param tag 业务侧自定义标识
     * @param log 待输出的日志正文
     * @param e   待记录的异常实例
     */
    public static void logE(String tag, String log, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        ModuleData.getWrapper().log(Log.ERROR, tag, "[E]: " + log, e);
    }

    // ----------- logW --------------
    /**
     * 以 WARN 级别输出一条纯文本日志至 Xposed 运行时日志系统。
     * <p>
     * 当全局日志等级低于 {@link ModuleConfig#LOG_W} 时，此调用将被静默跳过。
     *
     * @param tag 业务侧自定义标识
     * @param log 待输出的日志正文
     */
    public static void logW(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        ModuleData.getWrapper().log(Log.WARN, tag, "[W]: " + log);
    }

    /**
     * 以 WARN 级别输出一条仅包含异常信息的日志至 Xposed 运行时日志系统。
     *
     * @param tag 业务侧自定义标识
     * @param e   待记录的异常实例
     */
    public static void logW(String tag, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        ModuleData.getWrapper().log(Log.WARN, tag, "[W]: ", e);
    }

    /**
     * 以 WARN 级别输出一条附带调用栈字符串的日志至 Xposed 运行时日志系统。
     *
     * @param tag        业务侧自定义标识
     * @param log        待输出的日志正文
     * @param stackTrace 以字符串形式提供的调用栈信息
     */
    public static void logW(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        ModuleData.getWrapper().log(Log.WARN, tag, "[W]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    /**
     * 以 WARN 级别输出一条同时包含文本描述和异常信息的日志至 Xposed 运行时日志系统。
     *
     * @param tag 业务侧自定义标识
     * @param log 待输出的日志正文
     * @param e   待记录的异常实例
     */
    public static void logW(String tag, String log, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        ModuleData.getWrapper().log(Log.WARN, tag, "[W]: " + log, e);
    }

    // ----------- logI --------------
    /**
     * 以 INFO 级别输出一条纯文本日志至 Xposed 运行时日志系统。
     * <p>
     * 当全局日志等级低于 {@link ModuleConfig#LOG_I} 时，此调用将被静默跳过。
     *
     * @param tag 业务侧自定义标识
     * @param log 待输出的日志正文
     */
    public static void logI(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        ModuleData.getWrapper().log(Log.INFO, tag, "[I]: " + log);
    }

    /**
     * 以 INFO 级别输出一条附带调用栈字符串的日志至 Xposed 运行时日志系统。
     *
     * @param tag        业务侧自定义标识
     * @param log        待输出的日志正文
     * @param stackTrace 以字符串形式提供的调用栈信息
     */
    public static void logI(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        ModuleData.getWrapper().log(Log.INFO, tag, "[I]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    /**
     * 以 INFO 级别输出一条仅包含异常信息的日志至 Xposed 运行时日志系统。
     *
     * @param tag 业务侧自定义标识
     * @param e   待记录的异常实例
     */
    public static void logI(String tag, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        ModuleData.getWrapper().log(Log.INFO, tag, "[I]: ", e);
    }

    /**
     * 以 INFO 级别输出一条同时包含文本描述和异常信息的日志至 Xposed 运行时日志系统。
     *
     * @param tag 业务侧自定义标识
     * @param log 待输出的日志正文
     * @param e   待记录的异常实例
     */
    public static void logI(String tag, String log, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        ModuleData.getWrapper().log(Log.INFO, tag, "[I]: " + log, e);
    }

    // ------------ logD --------------
    /**
     * 以 DEBUG 级别输出一条纯文本日志至 Xposed 运行时日志系统。
     * <p>
     * 当全局日志等级低于 {@link ModuleConfig#LOG_D} 时，此调用将被静默跳过。
     *
     * @param tag 业务侧自定义标识
     * @param log 待输出的日志正文
     */
    public static void logD(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        ModuleData.getWrapper().log(Log.DEBUG, tag, "[D]: " + log);
    }

    /**
     * 以 DEBUG 级别输出一条仅包含异常信息的日志至 Xposed 运行时日志系统。
     *
     * @param tag 业务侧自定义标识
     * @param e   待记录的异常实例
     */
    public static void logD(String tag, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        ModuleData.getWrapper().log(Log.DEBUG, tag, "[D]: ", e);
    }

    /**
     * 以 DEBUG 级别输出一条附带调用栈字符串的日志至 Xposed 运行时日志系统。
     *
     * @param tag        业务侧自定义标识
     * @param log        待输出的日志正文
     * @param stackTrace 以字符串形式提供的调用栈信息
     */
    public static void logD(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        ModuleData.getWrapper().log(Log.DEBUG, tag, "[D]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    /**
     * 以 DEBUG 级别输出一条同时包含文本描述和异常信息的日志至 Xposed 运行时日志系统。
     *
     * @param tag 业务侧自定义标识
     * @param log 待输出的日志正文
     * @param e   待记录的异常实例
     */
    public static void logD(String tag, String log, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        ModuleData.getWrapper().log(Log.DEBUG, tag, "[D]: " + log, e);
    }
}
