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

import com.hchen.hooktool.config.HookConfig;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志辅助工具类。
 * <p>
 * 提供堆栈信息格式化与日志标签推断能力。
 *
 * @author 焕晨HChen
 */
public final class LogExpand {
    private static volatile Set<String> sCachedPaths = Collections.emptySet();
    private static volatile Set<String> sCachedIgnores = Collections.emptySet();
    private static final ConcurrentHashMap<String, String> TAG_CACHE = new ConcurrentHashMap<>();

    private LogExpand() {
    }

    /**
     * 将异常的完整堆栈信息格式化为字符串。
     *
     * @param throwable 待格式化的异常
     * @return 堆栈信息
     */
    @NonNull
    public static String printStackTrace(@NonNull Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            throwable.printStackTrace(printWriter);
        }
        return stringWriter.toString();
    }

    /**
     * 获取当前线程的调用栈。
     *
     * @return 格式化后的调用栈
     */
    @NonNull
    public static String getStackTrace() {
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            builder.append("\nat ").append(element);
        }
        return builder.toString();
    }

    /**
     * 根据调用栈推断日志标签。
     * <p>
     * 未命中配置路径时返回 {@link HookConfig#getLogTag()}。
     *
     * @return 日志标签
     */
    @NonNull
    public static String getTag() {
        Set<String> logExpandPaths = HookConfig.getLogExpandPaths();
        Set<String> ignoreClassNames = HookConfig.getLogExpandIgnoreClassNames();
        refreshConfigCache(logExpandPaths, ignoreClassNames);
        if (logExpandPaths.isEmpty()) return HookConfig.getLogTag();

        StackTraceElement[] elements = Thread.currentThread().getStackTrace();

        main:
        for (StackTraceElement element : elements) {
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
        return HookConfig.getLogTag();
    }

    private static boolean isPrefixMatch(@NonNull String className, @NonNull String prefix) {
        return className.startsWith(prefix)
            && (className.length() == prefix.length()
            || className.charAt(prefix.length()) == '.');
    }

    private static String deriveTag(@NonNull String className) {
        int dotIndex = className.lastIndexOf('.');
        String tag = dotIndex == -1 ? className : className.substring(dotIndex + 1);
        int dollarIndex = tag.indexOf('$');
        if (dollarIndex != -1) {
            tag = tag.substring(0, dollarIndex);
        }
        return tag;
    }

    private static void refreshConfigCache(@NonNull Set<String> paths, @NonNull Set<String> ignores) {
        if (!sCachedPaths.equals(paths) || !sCachedIgnores.equals(ignores)) {
            sCachedPaths = paths;
            sCachedIgnores = ignores;
            TAG_CACHE.clear();
        }
    }
}
