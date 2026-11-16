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

/**
 * 日志输出抽象基类。
 *
 * @author 焕晨HChen
 */
@SuppressWarnings("EnhancedSwitchMigration")
public abstract class AbstractLog {
    /**
     * 受保护的空构造器，供子类继承链使用（如 {@code CoreTool : XposedLog : AbstractLog}）。
     */
    protected AbstractLog() {
    }

    /**
     * 判断指定日志等级是否达到全局日志等级阈值。
     *
     * @param level 目标日志等级（{@link ModuleConfig#LOG_E} 等常量）
     * @return 达到阈值时返回 {@code true}，否则返回 {@code false}
     */
    protected static boolean isLoggable(int level) {
        return ModuleConfig.getLogLevel() >= level;
    }

    /**
     * 将框架内部日志等级映射为 {@link android.util.Log} 的优先级常量。
     *
     * @param level 框架日志等级（{@link ModuleConfig#LOG_E} 等）
     * @return 对应的 {@link android.util.Log} 优先级常量
     */
    private static int toPriority(int level) {
        switch (level) {
            case ModuleConfig.LOG_E:
                return android.util.Log.ERROR;
            case ModuleConfig.LOG_W:
                return android.util.Log.WARN;
            case ModuleConfig.LOG_I:
                return android.util.Log.INFO;
            default:
                return android.util.Log.DEBUG;
        }
    }

    /**
     * 日志调度骨架：等级门控 → 消息格式化 → 委派给子类的抽象输出点。
     *
     * @param impl      承担输出的子类实例（通常为其私有静态单例）
     * @param level     框架日志等级
     * @param tag       调用方自定义标识，作参数传给输出目标
     * @param message   日志正文；为 {@code null} 时仅输出等级前缀
     * @param throwable 待记录的异常；为 {@code null} 时不传异常
     */
    protected static void logAt(AbstractLog impl, int level,
                                String tag, String message, Throwable throwable) {
        if (!isLoggable(level)) return;
        impl.log(toPriority(level), tag, buildMessage(level, message), throwable);
    }

    /**
     * 构建日志消息体：等级前缀 + 正文。
     *
     * @param level   框架日志等级，用于选取 {@code "[E]: "} 等前缀
     * @param message 日志正文，为 {@code null} 时视为空串
     * @return 格式化后的消息体
     */
    private static String buildMessage(int level, String message) {
        String prefix;
        switch (level) {
            case ModuleConfig.LOG_E:
                prefix = "[E]: ";
                break;
            case ModuleConfig.LOG_W:
                prefix = "[W]: ";
                break;
            case ModuleConfig.LOG_I:
                prefix = "[I]: ";
                break;
            default:
                prefix = "[D]: ";
                break;
        }
        String body = (message == null) ? "" : message;
        return prefix + body;
    }

    /**
     * 抽象输出点：将格式化后的消息交给子类的输出目标。
     *
     * @param priority  {@link android.util.Log} 优先级常量
     * @param tag       调用方自定义标识
     * @param message   已格式化的消息体（含等级前缀与调用栈）
     * @param throwable 待记录的异常，可为 {@code null}
     */
    protected abstract void log(int priority, String tag, String message, Throwable throwable);
}
