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

import androidx.annotation.NonNull;

import com.hchen.hooktool.ModuleConfig;
import com.hchen.hooktool.hook.AbsHook;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

/**
 * 日志扩展工具类。提供堆栈跟踪获取、日志标签自动识别以及方法调用观察等增强日志功能。
 *
 * @author 焕晨HChen
 */
public final class LogExpand {
    private LogExpand() {
    }

    /**
     * 将异常堆栈跟踪转换为字符串。
     *
     * @param e 异常对象
     * @return 堆栈跟踪字符串
     */
    @NonNull
    public static String printStackTrace(@NonNull Throwable e) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            e.printStackTrace(printWriter);
        }
        return stringWriter.toString();
    }

    /**
     * 获取当前线程的堆栈跟踪信息。
     *
     * @return 堆栈跟踪信息字符串
     */
    @NonNull
    public static String getStackTrace() {
        StringBuilder stringBuilder = new StringBuilder();
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            stringBuilder.append("\nat ").append(element.getClassName()).append(".")
                .append(element.getMethodName()).append("(")
                .append(element.getFileName()).append(":")
                .append(element.getLineNumber()).append(")");
        }
        return stringBuilder.toString();
    }

    /**
     * 根据调用栈自动识别日志标签。
     *
     * @return 日志标签
     */
    @NonNull
    public static String getTag() {
        String[] logExpandPaths = ModuleConfig.getLogExpandPaths();
        String[] ignoreClassNames = ModuleConfig.getLogExpandIgnoreClassNames();
        if (logExpandPaths.length == 0) return ModuleConfig.getLogTag();

        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        main:
        for (StackTraceElement element : stackTraceElements) {
            String className = element.getClassName();
            for (String name : ignoreClassNames) {
                if (className.contains(name)) {
                    continue main;
                }
            }

            for (String path : logExpandPaths) {
                if (className.contains(path)) {
                    int dotIndex = className.lastIndexOf(".");
                    if (dotIndex == -1) continue main;

                    String tag = className.substring(dotIndex + 1);
                    int dollarIndex = tag.indexOf('$');
                    if (dollarIndex != -1) {
                        tag = tag.substring(0, dollarIndex);
                    }
                    return tag;
                }
            }
        }

        return ModuleConfig.getLogTag();
    }

    /**
     * 生成当前钩子调用的详细信息字符串。
     *
     * @param hook 钩子对象
     * @return 调用信息字符串
     */
    @NonNull
    @SuppressWarnings("StringBufferReplaceableByString")
    public static String observeCall(@NonNull AbsHook hook) {
        Object[] args = hook.getArgs();
        String declaringClass = hook.getExecutable().getDeclaringClass().getName();
        String methodName = hook.getExecutable().getName();

        if (args.length == 0) {
            StringBuilder sb = new StringBuilder(128);
            sb.append("→ Called Method\n")
                .append("├─ Class:  ").append(declaringClass).append("\n")
                .append("├─ Method: ").append(methodName).append("\n")
                .append("├─ Params: { }\n")
                .append("└─ Return: ").append(hook.getResult());
            return sb.toString();
        }

        StringBuilder log = new StringBuilder(256);
        log.append("→ Called Method\n")
            .append("├─ Class:  ").append(declaringClass).append("\n")
            .append("├─ Method: ").append(methodName).append("\n")
            .append("├─ Params: {\n");

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            log.append("    [").append(i).append("] ");
            log.append(arg == null ? "(null)" : arg.getClass().getSimpleName());
            log.append(" = ").append(paramToString(arg)).append("\n");
        }

        log.append("├─ }\n")
            .append("└─ Return: ").append(hook.getResult());

        return log.toString();
    }

    @NonNull
    private static String paramToString(Object param) {
        if (param == null) {
            return "null";
        }

        Class<?> clazz = param.getClass();
        if (!clazz.isArray()) {
            return param.toString();
        }

        // noinspection IfCanBeSwitch
        if (param instanceof Object[]) {
            return Arrays.deepToString((Object[]) param);
        }

        if (param instanceof int[]) return Arrays.toString((int[]) param);
        if (param instanceof byte[]) return Arrays.toString((byte[]) param);
        if (param instanceof boolean[]) return Arrays.toString((boolean[]) param);
        if (param instanceof long[]) return Arrays.toString((long[]) param);
        if (param instanceof float[]) return Arrays.toString((float[]) param);
        if (param instanceof double[]) return Arrays.toString((double[]) param);
        if (param instanceof char[]) return Arrays.toString((char[]) param);
        if (param instanceof short[]) return Arrays.toString((short[]) param);

        return param.toString();
    }
}
