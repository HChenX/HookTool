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
 * Android 原生日志工具类。通过 Android 标准 Log API 输出日志，支持 E/W/I/D 四个日志等级。
 * 日志输出受 {@link ModuleConfig#getLogLevel()} 控制。
 *
 * @author 焕晨HChen
 */
public class AndroidLog {
    private AndroidLog() {
    }

    // ----------- logE ----------
    /**
     * 输出错误级别日志。
     *
     * @param tag 日志标签
     * @param log 日志内容
     */
    public static void logE(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        Log.e(ModuleConfig.getLogTag(), "[" + tag + "]" + "[E]: " + log);
    }

    /**
     * 输出错误级别日志，附带堆栈跟踪信息。
     *
     * @param tag        日志标签
     * @param log        日志内容
     * @param stackTrace 堆栈跟踪信息
     */
    public static void logE(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        Log.e(ModuleConfig.getLogTag(), "[" + tag + "]" + "[E]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    /**
     * 输出错误级别日志。
     *
     * @param tag       日志标签
     * @param throwable 异常对象
     */
    public static void logE(String tag, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        Log.e(ModuleConfig.getLogTag(), "[" + tag + "]" + "[E]: ", throwable);
    }

    /**
     * 输出错误级别日志，附带异常信息。
     *
     * @param tag       日志标签
     * @param log       日志内容
     * @param throwable 异常对象
     */
    public static void logE(String tag, String log, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        Log.e(ModuleConfig.getLogTag(), "[" + tag + "]" + "[E]: " + log, throwable);
    }

    // -------- logW --------------
    /**
     * 输出警告级别日志。
     *
     * @param tag 日志标签
     * @param log 日志内容
     */
    public static void logW(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        Log.w(ModuleConfig.getLogTag(), "[" + tag + "]" + "[W]: " + log);
    }

    /**
     * 输出警告级别日志，附带堆栈跟踪信息。
     *
     * @param tag        日志标签
     * @param log        日志内容
     * @param stackTrace 堆栈跟踪信息
     */
    public static void logW(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        Log.w(ModuleConfig.getLogTag(), "[" + tag + "]" + "[W]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    /**
     * 输出警告级别日志。
     *
     * @param tag       日志标签
     * @param throwable 异常对象
     */
    public static void logW(String tag, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        Log.w(ModuleConfig.getLogTag(), "[" + tag + "]" + "[W]: ", throwable);
    }

    /**
     * 输出警告级别日志，附带异常信息。
     *
     * @param tag       日志标签
     * @param log       日志内容
     * @param throwable 异常对象
     */
    public static void logW(String tag, String log, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        Log.w(ModuleConfig.getLogTag(), "[" + tag + "]" + "[W]: " + log, throwable);
    }

    // ------------ logI -------------
    /**
     * 输出信息级别日志。
     *
     * @param tag 日志标签
     * @param log 日志内容
     */
    public static void logI(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        Log.i(ModuleConfig.getLogTag(), "[" + tag + "]" + "[I]: " + log);
    }

    /**
     * 输出信息级别日志，附带堆栈跟踪信息。
     *
     * @param tag        日志标签
     * @param log        日志内容
     * @param stackTrace 堆栈跟踪信息
     */
    public static void logI(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        Log.i(ModuleConfig.getLogTag(), "[" + tag + "]" + "[I]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    /**
     * 输出信息级别日志。
     *
     * @param tag       日志标签
     * @param throwable 异常对象
     */
    public static void logI(String tag, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        Log.i(ModuleConfig.getLogTag(), "[" + tag + "]" + "[I]: ", throwable);
    }

    /**
     * 输出信息级别日志，附带异常信息。
     *
     * @param tag       日志标签
     * @param log       日志内容
     * @param throwable 异常对象
     */
    public static void logI(String tag, String log, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        Log.i(ModuleConfig.getLogTag(), "[" + tag + "]" + "[I]: " + log, throwable);
    }

    // ---------- logD ---------------
    /**
     * 输出调试级别日志。
     *
     * @param tag 日志标签
     * @param log 日志内容
     */
    public static void logD(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        Log.d(ModuleConfig.getLogTag(), "[" + tag + "]" + "[D]: " + log);
    }

    /**
     * 输出调试级别日志，附带堆栈跟踪信息。
     *
     * @param tag        日志标签
     * @param log        日志内容
     * @param stackTrace 堆栈跟踪信息
     */
    public static void logD(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        Log.d(ModuleConfig.getLogTag(), "[" + tag + "]" + "[D]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    /**
     * 输出调试级别日志。
     *
     * @param tag       日志标签
     * @param throwable 异常对象
     */
    public static void logD(String tag, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        Log.d(ModuleConfig.getLogTag(), "[" + tag + "]" + "[D]: ", throwable);
    }

    /**
     * 输出调试级别日志，附带异常信息。
     *
     * @param tag       日志标签
     * @param log       日志内容
     * @param throwable 异常对象
     */
    public static void logD(String tag, String log, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        Log.d(ModuleConfig.getLogTag(), "[" + tag + "]" + "[D]: " + log, throwable);
    }
}
