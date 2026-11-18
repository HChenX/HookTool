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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.config.HookConfig;
import com.hchen.hooktool.host.Host;

/**
 * 日志输出门面。
 * <p>
 * 输出统一交给 {@link Host} 处理，处于 Xposed 环境时写入框架日志，否则写入系统日志。
 *
 * @author 焕晨HChen
 */
public final class AndroidLog {
    private AndroidLog() {
    }

    /**
     * 直接将日志写入系统日志。
     * <p>
     * 供默认宿主环境与框架日志回落使用，业务代码请改用 {@code logX} 系列方法。
     *
     * @param priority  日志优先级
     * @param tag       自定义标签
     * @param message   已格式化的消息
     * @param throwable 异常信息
     */
    public static void output(int priority, @NonNull String tag, @NonNull String message,
                              @Nullable Throwable throwable) {
        String fullTag = HookConfig.getLogTag();
        String fullMessage = "[" + tag + "]" + message;
        if (throwable == null) {
            Log.println(priority, fullTag, fullMessage);
        } else {
            switch (priority) {
                case Log.ERROR:
                    Log.e(fullTag, fullMessage, throwable);
                    break;
                case Log.WARN:
                    Log.w(fullTag, fullMessage, throwable);
                    break;
                case Log.INFO:
                    Log.i(fullTag, fullMessage, throwable);
                    break;
                default:
                    Log.d(fullTag, fullMessage, throwable);
                    break;
            }
        }
    }

    // -------------------------------- error --------------------------------

    /**
     * 输出错误日志。
     *
     * @param tag     自定义标签
     * @param message 日志正文
     */
    public static void logE(@NonNull String tag, @NonNull String message) {
        logAt(HookConfig.LOG_E, tag, message, null);
    }

    /**
     * 输出带异常的错误日志。
     *
     * @param tag       自定义标签
     * @param throwable 异常信息
     */
    public static void logE(@NonNull String tag, @NonNull Throwable throwable) {
        logAt(HookConfig.LOG_E, tag, null, throwable);
    }

    /**
     * 输出带正文与异常的错误日志。
     *
     * @param tag       自定义标签
     * @param message   日志正文
     * @param throwable 异常信息
     */
    public static void logE(@NonNull String tag, @NonNull String message,
                            @NonNull Throwable throwable) {
        logAt(HookConfig.LOG_E, tag, message, throwable);
    }

    // -------------------------------- warn ---------------------------------

    /**
     * 输出警告日志。
     *
     * @param tag     自定义标签
     * @param message 日志正文
     */
    public static void logW(@NonNull String tag, @NonNull String message) {
        logAt(HookConfig.LOG_W, tag, message, null);
    }

    /**
     * 输出带异常的警告日志。
     *
     * @param tag       自定义标签
     * @param throwable 异常信息
     */
    public static void logW(@NonNull String tag, @NonNull Throwable throwable) {
        logAt(HookConfig.LOG_W, tag, null, throwable);
    }

    /**
     * 输出带正文与异常的警告日志。
     *
     * @param tag       自定义标签
     * @param message   日志正文
     * @param throwable 异常信息
     */
    public static void logW(@NonNull String tag, @NonNull String message,
                            @NonNull Throwable throwable) {
        logAt(HookConfig.LOG_W, tag, message, throwable);
    }

    // -------------------------------- info ---------------------------------

    /**
     * 输出信息日志。
     *
     * @param tag     自定义标签
     * @param message 日志正文
     */
    public static void logI(@NonNull String tag, @NonNull String message) {
        logAt(HookConfig.LOG_I, tag, message, null);
    }

    /**
     * 输出带异常的信息日志。
     *
     * @param tag       自定义标签
     * @param throwable 异常信息
     */
    public static void logI(@NonNull String tag, @NonNull Throwable throwable) {
        logAt(HookConfig.LOG_I, tag, null, throwable);
    }

    /**
     * 输出带正文与异常的信息日志。
     *
     * @param tag       自定义标签
     * @param message   日志正文
     * @param throwable 异常信息
     */
    public static void logI(@NonNull String tag, @NonNull String message,
                            @NonNull Throwable throwable) {
        logAt(HookConfig.LOG_I, tag, message, throwable);
    }

    // -------------------------------- debug --------------------------------

    /**
     * 输出调试日志。
     *
     * @param tag     自定义标签
     * @param message 日志正文
     */
    public static void logD(@NonNull String tag, @NonNull String message) {
        logAt(HookConfig.LOG_D, tag, message, null);
    }

    /**
     * 输出带异常的调试日志。
     *
     * @param tag       自定义标签
     * @param throwable 异常信息
     */
    public static void logD(@NonNull String tag, @NonNull Throwable throwable) {
        logAt(HookConfig.LOG_D, tag, null, throwable);
    }

    /**
     * 输出带正文与异常的调试日志。
     *
     * @param tag       自定义标签
     * @param message   日志正文
     * @param throwable 异常信息
     */
    public static void logD(@NonNull String tag, @NonNull String message,
                            @NonNull Throwable throwable) {
        logAt(HookConfig.LOG_D, tag, message, throwable);
    }

    // ------------------------------- internal ------------------------------

    private static void logAt(int level, @NonNull String tag, @Nullable String message,
                              @Nullable Throwable throwable) {
        if (!isLoggable(level)) return;
        Host.get().log(toPriority(level), tag, buildMessage(level, message), throwable);
    }

    private static boolean isLoggable(int level) {
        return HookConfig.getLogLevel() >= level;
    }

    private static int toPriority(int level) {
        switch (level) {
            case HookConfig.LOG_E:
                return Log.ERROR;
            case HookConfig.LOG_W:
                return Log.WARN;
            case HookConfig.LOG_I:
                return Log.INFO;
            default:
                return Log.DEBUG;
        }
    }

    @NonNull
    private static String buildMessage(int level, @Nullable String message) {
        String prefix;
        switch (level) {
            case HookConfig.LOG_E:
                prefix = "[E]: ";
                break;
            case HookConfig.LOG_W:
                prefix = "[W]: ";
                break;
            case HookConfig.LOG_I:
                prefix = "[I]: ";
                break;
            default:
                prefix = "[D]: ";
                break;
        }
        return prefix + (message == null ? "" : message);
    }
}
