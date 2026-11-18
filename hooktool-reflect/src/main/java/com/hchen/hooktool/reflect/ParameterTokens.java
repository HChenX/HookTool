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
package com.hchen.hooktool.reflect;

/**
 * 参数类型标记解析工具。
 * <p>
 * 将 Class、类名字符串统一解析为 Class 数组。
 *
 * @author 焕晨HChen
 */
final class ParameterTokens {
    private ParameterTokens() {
    }

    /**
     * 将参数标记解析为 Class 数组。
     *
     * @param classLoader 解析类名时使用的类加载器，可为 null
     * @param tokens      参数标记，元素为 Class 或类名字符串
     * @return 解析后的 Class 数组
     * @throws ReflectException 标记为 null 或类型不受支持时抛出
     */
    static Class<?>[] resolve(ClassLoader classLoader, Object[] tokens) {
        if (tokens == null) return new Class<?>[0];
        Class<?>[] resolved = new Class<?>[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            Object token = tokens[i];
            if (token == null) {
                throw new ReflectException(ReflectException.Kind.PARAM_MISMATCH, "params[" + i + "]",
                    "Parameter type at index " + i + " must not be null.");
            }
            if (token instanceof Class<?> type) {
                resolved[i] = type;
            } else if (token instanceof String name) {
                resolved[i] = ClassCache.find(name, classLoader);
            } else {
                throw new ReflectException(ReflectException.Kind.PARAM_MISMATCH, "params[" + i + "]",
                    "Parameter type at index " + i + " must be a Class or a class name String, but was "
                        + token.getClass().getName() + "(" + token + ").");
            }
        }
        return resolved;
    }
}
