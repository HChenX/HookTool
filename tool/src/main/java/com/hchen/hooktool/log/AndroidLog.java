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
 *
 * @author 焕晨HChen
 */
public class AndroidLog extends AbstractLog {
    /**
     * 本类输出目标单例，供静态门面委托。
     */
    private static final AbstractLog IMPL = new AndroidLog();

    private AndroidLog() {
    }

    // ----------- logE ----------

    /**
     * 以 ERROR 级别输出一条纯文本日志。
     *
     * @param tag     业务侧自定义标识，将嵌入消息头部的方括号中
     * @param message 待输出的日志正文
     */
    public static void logE(String tag, String message) {
        logAt(IMPL, ModuleConfig.LOG_E, tag, message, null);
    }

    /**
     * 以 ERROR 级别输出一条仅包含异常信息的日志。
     *
     * @param tag       业务侧自定义标识
     * @param throwable 待记录的异常实例
     */
    public static void logE(String tag, Throwable throwable) {
        logAt(IMPL, ModuleConfig.LOG_E, tag, null, throwable);
    }

    /**
     * 以 ERROR 级别输出一条同时包含文本描述和异常信息的日志。
     *
     * @param tag       业务侧自定义标识，将嵌入消息头部的方括号中
     * @param message   待输出的日志正文
     * @param throwable 待记录的异常实例
     */
    public static void logE(String tag, String message, Throwable throwable) {
        logAt(IMPL, ModuleConfig.LOG_E, tag, message, throwable);
    }

    // -------- logW --------------

    /**
     * 以 WARN 级别输出一条纯文本日志。
     *
     * @param tag     业务侧自定义标识，将嵌入消息头部的方括号中
     * @param message 待输出的日志正文
     */
    public static void logW(String tag, String message) {
        logAt(IMPL, ModuleConfig.LOG_W, tag, message, null);
    }

    /**
     * 以 WARN 级别输出一条仅包含异常信息的日志。
     *
     * @param tag       业务侧自定义标识
     * @param throwable 待记录的异常实例
     */
    public static void logW(String tag, Throwable throwable) {
        logAt(IMPL, ModuleConfig.LOG_W, tag, null, throwable);
    }

    /**
     * 以 WARN 级别输出一条同时包含文本描述和异常信息的日志。
     *
     * @param tag       业务侧自定义标识，将嵌入消息头部的方括号中
     * @param message   待输出的日志正文
     * @param throwable 待记录的异常实例
     */
    public static void logW(String tag, String message, Throwable throwable) {
        logAt(IMPL, ModuleConfig.LOG_W, tag, message, throwable);
    }

    // ------------ logI -------------

    /**
     * 以 INFO 级别输出一条纯文本日志。
     *
     * @param tag     业务侧自定义标识，将嵌入消息头部的方括号中
     * @param message 待输出的日志正文
     */
    public static void logI(String tag, String message) {
        logAt(IMPL, ModuleConfig.LOG_I, tag, message, null);
    }

    /**
     * 以 INFO 级别输出一条仅包含异常信息的日志。
     *
     * @param tag       业务侧自定义标识
     * @param throwable 待记录的异常实例
     */
    public static void logI(String tag, Throwable throwable) {
        logAt(IMPL, ModuleConfig.LOG_I, tag, null, throwable);
    }

    /**
     * 以 INFO 级别输出一条同时包含文本描述和异常信息的日志。
     *
     * @param tag       业务侧自定义标识，将嵌入消息头部的方括号中
     * @param message   待输出的日志正文
     * @param throwable 待记录的异常实例
     */
    public static void logI(String tag, String message, Throwable throwable) {
        logAt(IMPL, ModuleConfig.LOG_I, tag, message, throwable);
    }

    // ---------- logD ---------------

    /**
     * 以 DEBUG 级别输出一条纯文本日志。
     *
     * @param tag     业务侧自定义标识，将嵌入消息头部的方括号中
     * @param message 待输出的日志正文
     */
    public static void logD(String tag, String message) {
        logAt(IMPL, ModuleConfig.LOG_D, tag, message, null);
    }

    /**
     * 以 DEBUG 级别输出一条仅包含异常信息的日志。
     *
     * @param tag       业务侧自定义标识
     * @param throwable 待记录的异常实例
     */
    public static void logD(String tag, Throwable throwable) {
        logAt(IMPL, ModuleConfig.LOG_D, tag, null, throwable);
    }

    /**
     * 以 DEBUG 级别输出一条同时包含文本描述和异常信息的日志。
     *
     * @param tag       业务侧自定义标识，将嵌入消息头部的方括号中
     * @param message   待输出的日志正文
     * @param throwable 待记录的异常实例
     */
    public static void logD(String tag, String message, Throwable throwable) {
        logAt(IMPL, ModuleConfig.LOG_D, tag, message, throwable);
    }

    /**
     * 包级私有输出桥：将格式化消息输出到 {@link android.util.Log}。
     * 调用者 {@code tag} 以 {@code [tag]} 嵌入消息体，因为原生 Log 只能按全局 {@link ModuleConfig#getLogTag()} 过滤。
     *
     * @param priority  {@link android.util.Log} 优先级常量
     * @param tag       调用方自定义标识
     * @param message   已格式化的消息体（含等级前缀）
     * @param throwable 待记录的异常，可为 {@code null}
     */
    static void output(int priority, String tag, String message, Throwable throwable) {
        String fullTag = ModuleConfig.getLogTag();
        String fullMsg = "[" + tag + "]" + message;
        if (throwable == null) {
            Log.println(priority, fullTag, fullMsg);
        } else {
            switch (priority) {
                case Log.ERROR:
                    Log.e(fullTag, fullMsg, throwable);
                    break;
                case Log.WARN:
                    Log.w(fullTag, fullMsg, throwable);
                    break;
                case Log.INFO:
                    Log.i(fullTag, fullMsg, throwable);
                    break;
                default:
                    Log.d(fullTag, fullMsg, throwable);
                    break;
            }
        }
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable throwable) {
        output(priority, tag, message, throwable);
    }
}
