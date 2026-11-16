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

import com.hchen.hooktool.ModuleConfig;
import com.hchen.hooktool.ModuleData;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Xposed 运行时环境专用日志输出工具类。
 *
 * @author 焕晨HChen
 */
public class XposedLog extends AbstractLog {
    private static final String TAG = "XposedLog";
    /**
     * 本类输出目标单例，供静态门面委托。
     */
    private static final AbstractLog IMPL = new XposedLog();
    /**
     * 标记是否已发生回落，保证回落提示仅输出一次。
     */
    private static final AtomicBoolean sFallbackUsed = new AtomicBoolean(false);

    protected XposedLog() {
    }

    // -------- logE -------------

    /**
     * 以 ERROR 级别输出一条纯文本日志至 Xposed 运行时日志系统。
     *
     * @param tag     业务侧自定义标识，将传递给 Xposed 日志代理
     * @param message 待输出的日志正文
     */
    public static void logE(String tag, String message) {
        logAt(IMPL, ModuleConfig.LOG_E, tag, message, null);
    }

    /**
     * 以 ERROR 级别输出一条仅包含异常信息的日志至 Xposed 运行时日志系统。
     *
     * @param tag 业务侧自定义标识
     * @param e   待记录的异常实例
     */
    public static void logE(String tag, Throwable e) {
        logAt(IMPL, ModuleConfig.LOG_E, tag, null, e);
    }

    /**
     * 以 ERROR 级别输出一条同时包含文本描述和异常信息的日志至 Xposed 运行时日志系统。
     *
     * @param tag     业务侧自定义标识，将传递给 Xposed 日志代理
     * @param message 待输出的日志正文
     * @param e       待记录的异常实例
     */
    public static void logE(String tag, String message, Throwable e) {
        logAt(IMPL, ModuleConfig.LOG_E, tag, message, e);
    }

    // ----------- logW --------------

    /**
     * 以 WARN 级别输出一条纯文本日志至 Xposed 运行时日志系统。
     *
     * @param tag     业务侧自定义标识，将传递给 Xposed 日志代理
     * @param message 待输出的日志正文
     */
    public static void logW(String tag, String message) {
        logAt(IMPL, ModuleConfig.LOG_W, tag, message, null);
    }

    /**
     * 以 WARN 级别输出一条仅包含异常信息的日志至 Xposed 运行时日志系统。
     *
     * @param tag 业务侧自定义标识
     * @param e   待记录的异常实例
     */
    public static void logW(String tag, Throwable e) {
        logAt(IMPL, ModuleConfig.LOG_W, tag, null, e);
    }

    /**
     * 以 WARN 级别输出一条同时包含文本描述和异常信息的日志至 Xposed 运行时日志系统。
     *
     * @param tag     业务侧自定义标识，将传递给 Xposed 日志代理
     * @param message 待输出的日志正文
     * @param e       待记录的异常实例
     */
    public static void logW(String tag, String message, Throwable e) {
        logAt(IMPL, ModuleConfig.LOG_W, tag, message, e);
    }

    // ----------- logI --------------

    /**
     * 以 INFO 级别输出一条纯文本日志至 Xposed 运行时日志系统。
     *
     * @param tag     业务侧自定义标识，将传递给 Xposed 日志代理
     * @param message 待输出的日志正文
     */
    public static void logI(String tag, String message) {
        logAt(IMPL, ModuleConfig.LOG_I, tag, message, null);
    }

    /**
     * 以 INFO 级别输出一条仅包含异常信息的日志至 Xposed 运行时日志系统。
     *
     * @param tag 业务侧自定义标识
     * @param e   待记录的异常实例
     */
    public static void logI(String tag, Throwable e) {
        logAt(IMPL, ModuleConfig.LOG_I, tag, null, e);
    }

    /**
     * 以 INFO 级别输出一条同时包含文本描述和异常信息的日志至 Xposed 运行时日志系统。
     *
     * @param tag     业务侧自定义标识，将传递给 Xposed 日志代理
     * @param message 待输出的日志正文
     * @param e       待记录的异常实例
     */
    public static void logI(String tag, String message, Throwable e) {
        logAt(IMPL, ModuleConfig.LOG_I, tag, message, e);
    }

    // ------------ logD --------------

    /**
     * 以 DEBUG 级别输出一条纯文本日志至 Xposed 运行时日志系统。
     *
     * @param tag     业务侧自定义标识，将传递给 Xposed 日志代理
     * @param message 待输出的日志正文
     */
    public static void logD(String tag, String message) {
        logAt(IMPL, ModuleConfig.LOG_D, tag, message, null);
    }

    /**
     * 以 DEBUG 级别输出一条仅包含异常信息的日志至 Xposed 运行时日志系统。
     *
     * @param tag 业务侧自定义标识
     * @param e   待记录的异常实例
     */
    public static void logD(String tag, Throwable e) {
        logAt(IMPL, ModuleConfig.LOG_D, tag, null, e);
    }

    /**
     * 以 DEBUG 级别输出一条同时包含文本描述和异常信息的日志至 Xposed 运行时日志系统。
     *
     * @param tag     业务侧自定义标识，将传递给 Xposed 日志代理
     * @param message 待输出的日志正文
     * @param e       待记录的异常实例
     */
    public static void logD(String tag, String message, Throwable e) {
        logAt(IMPL, ModuleConfig.LOG_D, tag, message, e);
    }

    /**
     * 抽象输出点：优先通过 Xposed 运行时日志代理输出；失败或环境不满足时回落 {@link AndroidLog}。
     * 回落提示仅输出一次，Xposed 环境就绪后自动恢复 Xposed 通道。
     */
    @Override
    protected final void log(int priority, String tag, String message, Throwable throwable) {
        if (sFallbackUsed.get() || !ModuleData.isXposedEnvironment()) {
            if (ModuleData.isXposedEnvironment()) {
                // 环境已就绪但处于回落态：尝试重新走 Xposed 通道，成功则解除回落。
                try {
                    ModuleData.getWrapper().log(priority, tag, message, throwable);
                    sFallbackUsed.set(false);
                    return;
                } catch (Throwable t) {
                    // 仍不可用，继续回落（不再重复打印提示）；保留 D 级痕迹便于排查持续回落。
                    AndroidLog.logD(TAG, "Xposed log proxy still unavailable, staying on fallback.", t);
                }
            }
            AndroidLog.output(priority, tag, message, throwable);
            return;
        }
        try {
            ModuleData.getWrapper().log(priority, tag, message, throwable);
        } catch (Throwable t) {
            markFallback(t);
            AndroidLog.output(priority, tag, message, throwable);
        }
    }

    /**
     * 记录回落状态并输出一次性提示。
     *
     * @param t 导致回落的异常
     */
    private static void markFallback(Throwable t) {
        if (sFallbackUsed.compareAndSet(false, true)) {
            AndroidLog.logW(TAG, "Falling back to AndroidLog.", t);
        }
    }
}
