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
import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 日志扩展
 *
 * @author 焕晨HChen
 */
public class LogExpand {
    private LogExpand() {
    }

    public static String printStackTrace(@NonNull Throwable e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        return stringWriter.toString();
    }

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

    public static String getTag() {
        String[] logExpandPaths = ModuleConfig.getLogExpandPaths();
        String[] ignoreClassNames = ModuleConfig.getLogExpandIgnoreClassNames();
        if (logExpandPaths == null || logExpandPaths.length == 0) return ModuleConfig.getLogTag();

        String tag = null;
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        main:
        for (int i = stackTraceElements.length - 1; i >= 0; i--) {
            if (tag != null) {
                break;
            }

            StackTraceElement element = stackTraceElements[i];
            String className = element.getClassName();
            if (ignoreClassNames != null) {
                for (String name : ignoreClassNames) {
                    if (className.contains(name)) {
                        continue main;
                    }
                }
            }

            for (String path : logExpandPaths) {
                if (className.contains(path)) {
                    int index = className.lastIndexOf(".");
                    if (index == -1) {
                        continue main;
                    }

                    tag = className.substring(index + 1);
                    if (tag.contains("$")) {
                        while (tag.lastIndexOf("$") != -1) {
                            index = tag.lastIndexOf("$");
                            tag = tag.substring(0, index);
                        }
                    }
                }
            }
        }

        return tag != null ? tag : ModuleConfig.getLogTag();
    }

    public static String observeCall(@NonNull AbsHook hook) {
        if (hook.getArgs().isEmpty()) {
            return "→ Called Method\n"
                + "├─ Class:  " + hook.getExecutable().getDeclaringClass().getName() + "\n"
                + "├─ Method: " + hook.getExecutable().getName() + "\n"
                + "├─ Params: { }\n"
                + "└─ Return: " + hook.getResult();
        }

        StringBuilder log = new StringBuilder();
        for (int i = 0; i < hook.getArgs().size(); i++) {
            Object arg = hook.getArgs().get(i);
            log.append("    [").append(i).append("] ");
            log.append(arg == null ? "(null)" : arg.getClass().getSimpleName());
            log.append(" = ").append(paramToString(arg)).append("\n");
        }

        return "→ Called Method\n"
            + "├─ Class:  " + hook.getExecutable().getDeclaringClass().getName() + "\n"
            + "├─ Method: " + hook.getExecutable().getName() + "\n"
            + "├─ Params: {\n"
            + log
            + "├─ }\n"
            + "└─ Return: " + hook.getResult();
    }

    private static String paramToString(Object param) {
        if (param == null) {
            return "null";
        }

        Class<?> clazz = param.getClass();
        if (!clazz.isArray()) {
            return param.toString();
        }

        class Frame {
            final Object array;
            final int length;
            int index;

            Frame(Object array) {
                this.array = array;
                this.length = Array.getLength(array);
                this.index = 0;
            }
        }

        StringBuilder sb = new StringBuilder();
        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(param));
        sb.append("[");

        while (!stack.isEmpty()) {
            Frame top = stack.peek();
            assert top != null;
            if (top.index >= top.length) {
                stack.pop();
                sb.append("]");
                if (!stack.isEmpty()) {
                    Frame parent = stack.peek();
                    assert parent != null;
                    if (parent.index < parent.length) {
                        sb.append(", ");
                    }
                }
                continue;
            }

            Object element = Array.get(top.array, top.index);
            top.index++;

            if (element != null && element.getClass().isArray()) {
                sb.append("[");
                stack.push(new Frame(element));
            } else {
                sb.append(element == null ? "null" : element.toString());
                if (top.index < top.length) {
                    sb.append(", ");
                }
            }
        }

        return sb.toString();
    }
}
