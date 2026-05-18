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
 * Xposed 日志工具类。通过 XposedInterfaceWrapper 输出日志，支持 E/W/I/D 四个日志等级。
 * 日志输出受 {@link ModuleConfig#getLogLevel()} 控制。
 *
 * @author 焕晨HChen
 */
public class XposedLog {
    protected XposedLog() {
    }

    // -------- logE -------------
    /**
     * 输出错误级别日志。
     *
     * @param tag 日志标签
     * @param log 日志内容
     */
    public static void logE(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        ModuleData.getWrapper().log(Log.ERROR, tag, "[E]: " + log);
    }

    /**
     * 输出错误级别日志。
     *
     * @param tag 日志标签
     * @param e   异常对象
     */
    public static void logE(String tag, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        ModuleData.getWrapper().log(Log.ERROR, tag, "[E]: ", e);
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
        ModuleData.getWrapper().log(Log.ERROR, tag, "[E]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    /**
     * 输出错误级别日志，附带异常信息。
     *
     * @param tag 日志标签
     * @param log 日志内容
     * @param e   异常对象
     */
    public static void logE(String tag, String log, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        ModuleData.getWrapper().log(Log.ERROR, tag, "[E]: " + log, e);
    }

    // ----------- logW --------------
    /**
     * 输出警告级别日志。
     *
     * @param tag 日志标签
     * @param log 日志内容
     */
    public static void logW(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        ModuleData.getWrapper().log(Log.WARN, tag, "[W]: " + log);
    }

    /**
     * 输出警告级别日志。
     *
     * @param tag 日志标签
     * @param e   异常对象
     */
    public static void logW(String tag, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        ModuleData.getWrapper().log(Log.WARN, tag, "[W]: ", e);
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
        ModuleData.getWrapper().log(Log.WARN, tag, "[W]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    /**
     * 输出警告级别日志，附带异常信息。
     *
     * @param tag 日志标签
     * @param log 日志内容
     * @param e   异常对象
     */
    public static void logW(String tag, String log, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        ModuleData.getWrapper().log(Log.WARN, tag, "[W]: " + log, e);
    }

    // ----------- logI --------------
    /**
     * 输出信息级别日志。
     *
     * @param tag 日志标签
     * @param log 日志内容
     */
    public static void logI(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        ModuleData.getWrapper().log(Log.INFO, tag, "[I]: " + log);
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
        ModuleData.getWrapper().log(Log.INFO, tag, "[I]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    /**
     * 输出信息级别日志。
     *
     * @param tag 日志标签
     * @param e   异常对象
     */
    public static void logI(String tag, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        ModuleData.getWrapper().log(Log.INFO, tag, "[I]: ", e);
    }

    /**
     * 输出信息级别日志，附带异常信息。
     *
     * @param tag 日志标签
     * @param log 日志内容
     * @param e   异常对象
     */
    public static void logI(String tag, String log, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        ModuleData.getWrapper().log(Log.INFO, tag, "[I]: " + log, e);
    }

    // ------------ logD --------------
    /**
     * 输出调试级别日志。
     *
     * @param tag 日志标签
     * @param log 日志内容
     */
    public static void logD(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        ModuleData.getWrapper().log(Log.DEBUG, tag, "[D]: " + log);
    }

    /**
     * 输出调试级别日志。
     *
     * @param tag 日志标签
     * @param e   异常对象
     */
    public static void logD(String tag, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        ModuleData.getWrapper().log(Log.DEBUG, tag, "[D]: ", e);
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
        ModuleData.getWrapper().log(Log.DEBUG, tag, "[D]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    /**
     * 输出调试级别日志，附带异常信息。
     *
     * @param tag 日志标签
     * @param log 日志内容
     * @param e   异常对象
     */
    public static void logD(String tag, String log, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        ModuleData.getWrapper().log(Log.DEBUG, tag, "[D]: " + log, e);
    }
}
