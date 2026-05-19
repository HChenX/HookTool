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
 * 日志辅助功能扩展工具类。
 * <p>
 * 本类为 {@link AndroidLog} 和 {@link XposedLog} 提供底层辅助能力，
 * 主要包含以下功能：
 * <ul>
 *     <li>将异常堆栈跟踪序列化为字符串形式（{@link #printStackTrace(Throwable)}）</li>
 *     <li>获取当前线程的格式化调用栈信息（{@link #getStackTrace()}）</li>
 *     <li>基于调用栈自动推断并生成日志标签（{@link #getTag()}）</li>
 *     <li>将钩子的调用上下文格式化为结构化观测日志（{@link #observeCall(AbsHook)}）</li>
 * </ul>
 * <p>
 * 本类为纯工具类，不允许实例化。
 *
 * @author 焕晨HChen
 * @see AndroidLog
 * @see XposedLog
 */
public final class LogExpand {
    private LogExpand() {
    }

    /**
     * 将指定异常的完整堆栈跟踪序列化为字符串。
     * <p>
     * 实现原理：通过 {@link StringWriter} 创建内存缓冲区，
     * 配合 {@link PrintWriter} 调用 {@link Throwable#printStackTrace(PrintWriter)}
     * 将堆栈输出重定向至字符串。
     *
     * @param e 待序列化的异常实例，不得为 {@code null}
     * @return 包含完整堆栈跟踪信息的字符串，保证不为 {@code null}
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
     * 获取当前线程的完整调用栈，并格式化为可读字符串。
     * <p>
     * 遍历当前线程的堆栈帧数组，将每一帧格式化为
     * {@code at 完整类名.方法名(文件名:行号)} 的形式，
     * 各帧之间以换行符分隔。
     *
     * @return 格式化后的调用栈信息，保证不为 {@code null}
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
     * 根据调用栈自动推断当前日志应使用的标签名。
     * <p>
     * 从当前线程堆栈中向下扫描，查找首个类名匹配
     * {@link ModuleConfig#getLogExpandPaths()} 中任一路径前缀的栈帧，
     * 取其简单类名（去除 {@code $} 之后的内部类后缀）作为标签返回。
     * 堆栈遍历过程中会跳过类名命中 {@link ModuleConfig#getLogExpandIgnoreClassNames()}
     * 配置列表的帧。
     * <p>
     * 若没有任何栈帧匹配配置路径，则回退为 {@link ModuleConfig#getLogTag()} 的默认值。
     *
     * @return 推断得到的日志标签，保证不为 {@code null}
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
     * 生成钩子方法当前调用的结构化观测日志。
     * <p>
     * 输出内容以树形结构呈现，包含以下字段：
     * <ul>
     *     <li>{@code Class} — 被拦截方法所属类的全限定名</li>
     *     <li>{@code Method} — 被拦截方法的名称</li>
     *     <li>{@code Params} — 各参数的索引、类型简名及值</li>
     *     <li>{@code Return} — 方法的返回值</li>
     * </ul>
     * 参数中若包含数组类型，会自动进行深度格式化（基本类型数组与对象数组均支持）。
     *
     * @param hook 当前正在执行的钩子实例，不得为 {@code null}
     * @return 格式化后的结构化观测信息字符串，保证不为 {@code null}
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

    /**
     * 将方法参数对象转换为可读的字符串表示。
     * <p>
     * 对数组类型做特殊处理：对象数组调用 {@link Arrays#deepToString(Object[])}，
     * 各基本类型数组调用对应的 {@link Arrays#toString} 重载方法。
     * 非数组类型直接调用 {@link Object#toString()}。
     *
     * @param param 待转换的参数对象，允许为 {@code null}
     * @return 参数的字符串表示；入参为 {@code null} 时返回字面量 {@code "null"}，保证不为 {@code null}
     */
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
