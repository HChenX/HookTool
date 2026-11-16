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
import androidx.annotation.Nullable;

import com.hchen.hooktool.ModuleConfig;
import com.hchen.hooktool.hook.AbsHook;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志辅助功能扩展工具类。
 *
 * @author 焕晨HChen
 */
public final class LogExpand {
    private LogExpand() {
    }

    /**
     * 日志增强路径配置快照，用于检测配置变更并失效 TAG 缓存。
     */
    private static volatile Set<String> sCachedPaths = Collections.emptySet();
    /**
     * 日志增强忽略类名配置快照。
     */
    private static volatile Set<String> sCachedIgnores = Collections.emptySet();
    /**
     * TAG 解析结果缓存：键为命中的栈帧全限定类名，值为解析出的标签。
     */
    private static final ConcurrentHashMap<String, String> TAG_CACHE = new ConcurrentHashMap<>();

    /**
     * 将指定异常的完整堆栈跟踪序列化为字符串。
     *
     * @param e 待序列化的异常实例
     * @return 包含完整堆栈跟踪信息的字符串
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
     *
     * @return 格式化后的调用栈信息
     */
    @NonNull
    public static String getStackTrace() {
        StringBuilder stringBuilder = new StringBuilder();
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            stringBuilder.append("\nat ").append(element.toString());
        }
        return stringBuilder.toString();
    }

    /**
     * 根据调用栈自动推断当前日志应使用的标签名。
     * 无栈帧匹配配置路径时回退为 {@link ModuleConfig#getLogTag()}。
     *
     * @return 推断得到的日志标签
     */
    @NonNull
    public static String getTag() {
        Set<String> logExpandPaths = ModuleConfig.getLogExpandPaths();
        Set<String> ignoreClassNames = ModuleConfig.getLogExpandIgnoreClassNames();
        refreshConfigCache(logExpandPaths, ignoreClassNames);
        if (logExpandPaths.isEmpty()) return ModuleConfig.getLogTag();

        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        main:
        for (StackTraceElement element : stackTraceElements) {
            String className = element.getClassName();
            for (String name : ignoreClassNames) {
                if (isPrefixMatch(className, name)) {
                    continue main;
                }
            }

            for (String path : logExpandPaths) {
                if (isPrefixMatch(className, path)) {
                    return TAG_CACHE.computeIfAbsent(className, LogExpand::deriveTag);
                }
            }
        }

        return ModuleConfig.getLogTag();
    }

    /**
     * 判断类名是否匹配指定的包前缀（含包边界）。
     * 前缀后须紧跟 {@code .} 或与前缀等长，避免同类名前缀误匹配。
     *
     * @param className 全限定类名
     * @param prefix    待匹配的前缀
     * @return 精确包前缀匹配时返回 {@code true}
     */
    private static boolean isPrefixMatch(@NonNull String className, @NonNull String prefix) {
        return className.startsWith(prefix)
            && (className.length() == prefix.length()
            || className.charAt(prefix.length()) == '.');
    }

    /**
     * 从命中帧类名中推导简单类名标签（去除包路径与 {@code $} 内部类后缀）。
     *
     * @param className 命中的栈帧全限定类名
     * @return 推导出的简单类名标签
     */
    private static String deriveTag(@NonNull String className) {
        int dotIndex = className.lastIndexOf('.');
        String tag = dotIndex == -1 ? className : className.substring(dotIndex + 1);
        int dollarIndex = tag.indexOf('$');
        if (dollarIndex != -1) {
            tag = tag.substring(0, dollarIndex);
        }
        return tag;
    }

    /**
     * 检测配置是否变更；变更时更新快照并清空 TAG 缓存。
     *
     * @param paths   当前日志增强路径配置
     * @param ignores 当前忽略类名配置
     */
    private static void refreshConfigCache(@NonNull Set<String> paths, @NonNull Set<String> ignores) {
        if (!sCachedPaths.equals(paths) || !sCachedIgnores.equals(ignores)) {
            sCachedPaths = paths;
            sCachedIgnores = ignores;
            TAG_CACHE.clear();
        }
    }

    /**
     * 生成钩子方法当前调用的结构化观测日志。
     *
     * @param hook 当前正在执行的钩子实例
     * @return 格式化后的结构化观测信息字符串
     */
    @NonNull
    @SuppressWarnings("StringBufferReplaceableByString")
    public static String observeCall(@NonNull AbsHook hook) {
        try {
            Object[] args = hook.getArgs();
            String declaringClass = hook.getExecutable().getDeclaringClass().getName();
            String methodName = hook.getExecutable().getName();

            if (args.length == 0) {
                StringBuilder sb = new StringBuilder(128);
                sb.append("→ Called Method\n")
                    .append("├─ Class:  ").append(declaringClass).append("\n")
                    .append("├─ Method: ").append(methodName).append("\n")
                    .append("├─ Params: { }\n")
                    .append("└─ Return: ").append(safeToString(hook.getResult()));
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
                .append("└─ Return: ").append(safeToString(hook.getResult()));

            return log.toString();
        } catch (Throwable t) {
            return "observeCall failed: " + t;
        }
    }

    /**
     * 安全地获取对象的字符串表示，防止 {@code toString()} 抛异常或自引用数组引发堆栈溢出。
     *
     * @param obj 目标对象，可为 {@code null}
     * @return 对象的字符串表示；无法安全转换时返回占位描述
     */
    @NonNull
    private static String safeToString(@Nullable Object obj) {
        if (obj == null) return "null";
        try {
            return obj.toString();
        } catch (Throwable t) {
            return "<unprintable " + obj.getClass().getSimpleName() + ">";
        }
    }

    /**
     * 将方法参数对象转换为可读的字符串表示。
     *
     * @param param 待转换的参数对象，允许为 {@code null}
     * @return 参数的字符串表示；入参为 {@code null} 时返回字面量 {@code "null"}
     */
    @NonNull
    private static String paramToString(Object param) {
        if (param == null) {
            return "null";
        }

        Class<?> clazz = param.getClass();
        if (!clazz.isArray()) {
            return safeToString(param);
        }

        try {
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

            return safeToString(param);
        } catch (Throwable t) {
            return "<unprintable " + clazz.getSimpleName() + ">";
        }
    }
}
