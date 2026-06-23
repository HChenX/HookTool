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

/**
 * Android 平台原生日志输出工具类。
 * <p>
 * 该类封装了 {@link android.util.Log} 的系统级 API，为 HookTool 框架提供统一的日志输出通道。
 * 内部支持四个日志优先级：ERROR（{@code E}）、WARN（{@code W}）、INFO（{@code I}）和 DEBUG（{@code D}），
 * 每个优先级均提供纯文本、附带调用栈字符串、附带异常对象及两者兼具等多种重载形式。
 * <p>
 * 所有日志输出行为受全局配置约束：日志等级由 {@link ModuleConfig#getLogLevel()} 控制，
 * 低于当前方法所对应等级的调用将被静默忽略；日志标签由 {@link ModuleConfig#getLogTag()} 统一指定，
 * 调用者提供的 {@code tag} 参数会以 {@code [tag]} 的格式嵌入日志消息体中。
 *
 * @author 焕晨HChen
 * @see ModuleConfig#getLogLevel()
 * @see ModuleConfig#getLogTag()
 */
public class AndroidLog {
    private AndroidLog() {
    }

    // ----------- logE ----------

    /**
     * 以 ERROR 级别输出一条纯文本日志。
     * <p>
     * 日志格式为：{@code [tag][E]: log}。
     * 当全局日志等级低于 {@link ModuleConfig#LOG_E} 时，此调用将被静默跳过。
     *
     * @param tag 业务侧自定义标识，将嵌入消息头部的方括号中
     * @param log 待输出的日志正文
     */
    public static void logE(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        Log.e(ModuleConfig.getLogTag(), "[" + tag + "]" + "[E]: " + log);
    }

    /**
     * 以 ERROR 级别输出一条附带调用栈字符串的日志。
     * <p>
     * 日志格式为：{@code [tag][E]: log[Stack Info]: stackTrace}。
     * 适用于需要手动传入预格式化堆栈信息的场景。
     *
     * @param tag        业务侧自定义标识
     * @param log        待输出的日志正文
     * @param stackTrace 以字符串形式提供的调用栈信息，将追加到消息末尾
     */
    public static void logE(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        Log.e(ModuleConfig.getLogTag(), "[" + tag + "]" + "[E]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    /**
     * 以 ERROR 级别输出一条仅包含异常信息的日志。
     * <p>
     * 异常对象的完整堆栈将由 {@link android.util.Log} 底层自动格式化输出。
     *
     * @param tag       业务侧自定义标识
     * @param throwable 待记录的异常实例
     */
    public static void logE(String tag, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        Log.e(ModuleConfig.getLogTag(), "[" + tag + "]" + "[E]: ", throwable);
    }

    /**
     * 以 ERROR 级别输出一条同时包含文本描述和异常信息的日志。
     * <p>
     * 文本消息作为日志主体输出，异常对象的堆栈信息将作为附加内容由底层引擎格式化。
     *
     * @param tag       业务侧自定义标识
     * @param log       待输出的日志正文
     * @param throwable 待记录的异常实例
     */
    public static void logE(String tag, String log, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        Log.e(ModuleConfig.getLogTag(), "[" + tag + "]" + "[E]: " + log, throwable);
    }

    // -------- logW --------------

    /**
     * 以 WARN 级别输出一条纯文本日志。
     * <p>
     * 当全局日志等级低于 {@link ModuleConfig#LOG_W} 时，此调用将被静默跳过。
     *
     * @param tag 业务侧自定义标识
     * @param log 待输出的日志正文
     */
    public static void logW(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        Log.w(ModuleConfig.getLogTag(), "[" + tag + "]" + "[W]: " + log);
    }

    /**
     * 以 WARN 级别输出一条附带调用栈字符串的日志。
     *
     * @param tag        业务侧自定义标识
     * @param log        待输出的日志正文
     * @param stackTrace 以字符串形式提供的调用栈信息
     */
    public static void logW(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        Log.w(ModuleConfig.getLogTag(), "[" + tag + "]" + "[W]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    /**
     * 以 WARN 级别输出一条仅包含异常信息的日志。
     *
     * @param tag       业务侧自定义标识
     * @param throwable 待记录的异常实例
     */
    public static void logW(String tag, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        Log.w(ModuleConfig.getLogTag(), "[" + tag + "]" + "[W]: ", throwable);
    }

    /**
     * 以 WARN 级别输出一条同时包含文本描述和异常信息的日志。
     *
     * @param tag       业务侧自定义标识
     * @param log       待输出的日志正文
     * @param throwable 待记录的异常实例
     */
    public static void logW(String tag, String log, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        Log.w(ModuleConfig.getLogTag(), "[" + tag + "]" + "[W]: " + log, throwable);
    }

    // ------------ logI -------------

    /**
     * 以 INFO 级别输出一条纯文本日志。
     * <p>
     * 当全局日志等级低于 {@link ModuleConfig#LOG_I} 时，此调用将被静默跳过。
     *
     * @param tag 业务侧自定义标识
     * @param log 待输出的日志正文
     */
    public static void logI(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        Log.i(ModuleConfig.getLogTag(), "[" + tag + "]" + "[I]: " + log);
    }

    /**
     * 以 INFO 级别输出一条附带调用栈字符串的日志。
     *
     * @param tag        业务侧自定义标识
     * @param log        待输出的日志正文
     * @param stackTrace 以字符串形式提供的调用栈信息
     */
    public static void logI(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        Log.i(ModuleConfig.getLogTag(), "[" + tag + "]" + "[I]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    /**
     * 以 INFO 级别输出一条仅包含异常信息的日志。
     *
     * @param tag       业务侧自定义标识
     * @param throwable 待记录的异常实例
     */
    public static void logI(String tag, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        Log.i(ModuleConfig.getLogTag(), "[" + tag + "]" + "[I]: ", throwable);
    }

    /**
     * 以 INFO 级别输出一条同时包含文本描述和异常信息的日志。
     *
     * @param tag       业务侧自定义标识
     * @param log       待输出的日志正文
     * @param throwable 待记录的异常实例
     */
    public static void logI(String tag, String log, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        Log.i(ModuleConfig.getLogTag(), "[" + tag + "]" + "[I]: " + log, throwable);
    }

    // ---------- logD ---------------

    /**
     * 以 DEBUG 级别输出一条纯文本日志。
     * <p>
     * 当全局日志等级低于 {@link ModuleConfig#LOG_D} 时，此调用将被静默跳过。
     *
     * @param tag 业务侧自定义标识
     * @param log 待输出的日志正文
     */
    public static void logD(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        Log.d(ModuleConfig.getLogTag(), "[" + tag + "]" + "[D]: " + log);
    }

    /**
     * 以 DEBUG 级别输出一条附带调用栈字符串的日志。
     *
     * @param tag        业务侧自定义标识
     * @param log        待输出的日志正文
     * @param stackTrace 以字符串形式提供的调用栈信息
     */
    public static void logD(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        Log.d(ModuleConfig.getLogTag(), "[" + tag + "]" + "[D]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    /**
     * 以 DEBUG 级别输出一条仅包含异常信息的日志。
     *
     * @param tag       业务侧自定义标识
     * @param throwable 待记录的异常实例
     */
    public static void logD(String tag, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        Log.d(ModuleConfig.getLogTag(), "[" + tag + "]" + "[D]: ", throwable);
    }

    /**
     * 以 DEBUG 级别输出一条同时包含文本描述和异常信息的日志。
     *
     * @param tag       业务侧自定义标识
     * @param log       待输出的日志正文
     * @param throwable 待记录的异常实例
     */
    public static void logD(String tag, String log, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        Log.d(ModuleConfig.getLogTag(), "[" + tag + "]" + "[D]: " + log, throwable);
    }
}
