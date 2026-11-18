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
package com.hchen.hooktool.hook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

/**
 * Hook 调用信息格式化工具类。
 *
 * @author 焕晨HChen
 */
final class HookLog {
    private HookLog() {
    }

    /**
     * 生成当前 Hook 调用的描述信息。
     *
     * @param hook 当前执行的 Hook
     * @return 格式化后的描述信息
     */
    @NonNull
    static String describe(@NonNull AbsHook hook) {
        try {
            Object[] args = hook.getArgs();
            String declaringClass = hook.getExecutable().getDeclaringClass().getName();
            String methodName = hook.getExecutable().getName();

            if (args.length == 0) {
                return "\u2192 Called Method\n"
                    + "\u251c\u2500 Class:  " + declaringClass + "\n"
                    + "\u251c\u2500 Method: " + methodName + "\n"
                    + "\u251c\u2500 Params: { }\n"
                    + "\u2514\u2500 Return: " + safeToString(hook.getResult());
            }

            StringBuilder log = new StringBuilder(256);
            log.append("\u2192 Called Method\n")
                .append("\u251c\u2500 Class:  ").append(declaringClass).append("\n")
                .append("\u251c\u2500 Method: ").append(methodName).append("\n")
                .append("\u251c\u2500 Params: {\n");

            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                log.append("    [").append(i).append("] ");
                log.append(arg == null ? "(null)" : arg.getClass().getSimpleName());
                log.append(" = ").append(paramToString(arg)).append("\n");
            }

            log.append("\u251c\u2500 }\n")
                .append("\u2514\u2500 Return: ").append(safeToString(hook.getResult()));
            return log.toString();
        } catch (Throwable t) {
            return "describeCall failed: " + t;
        }
    }

    @NonNull
    private static String safeToString(@Nullable Object obj) {
        if (obj == null) return "null";
        try {
            return obj.toString();
        } catch (Throwable t) {
            return "<unprintable " + obj.getClass().getSimpleName() + ">";
        }
    }

    @NonNull
    private static String paramToString(@Nullable Object param) {
        if (param == null) return "null";
        Class<?> clazz = param.getClass();
        if (!clazz.isArray()) return safeToString(param);
        try {
            if (param instanceof Object[] array) return Arrays.deepToString(array);
            if (param instanceof int[] array) return Arrays.toString(array);
            if (param instanceof byte[] array) return Arrays.toString(array);
            if (param instanceof boolean[] array) return Arrays.toString(array);
            if (param instanceof long[] array) return Arrays.toString(array);
            if (param instanceof float[] array) return Arrays.toString(array);
            if (param instanceof double[] array) return Arrays.toString(array);
            if (param instanceof char[] array) return Arrays.toString(array);
            if (param instanceof short[] array) return Arrays.toString(array);
            return safeToString(param);
        } catch (Throwable t) {
            return "<unprintable " + clazz.getSimpleName() + ">";
        }
    }
}