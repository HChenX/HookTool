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

import java.lang.reflect.Constructor;

/**
 * 构造函数查找工具。
 *
 * @author 焕晨HChen
 */
final class ConstructorFinder {
    private static final MemberCache<Constructor<?>> CACHE = new MemberCache<>();

    private ConstructorFinder() {
    }

    /**
     * 按精确参数类型查找构造函数。
     *
     * @param clazz          目标类
     * @param parameterTypes 精确参数类型
     * @return 已设置为可访问的构造函数
     */
    static Constructor<?> findExact(Class<?> clazz, Class<?>[] parameterTypes) {
        String sig = exactSignature(parameterTypes);
        return CACHE.get(clazz, sig,
            key -> {
                try {
                    Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
                    constructor.setAccessible(true);
                    return MemberCache.Slot.of(constructor);
                } catch (NoSuchMethodException e) {
                    return MemberCache.Slot.empty();
                }
            },
            () -> notFound(clazz, parameterTypes));
    }

    /**
     * 按最匹配的参数类型查找构造函数。
     *
     * @param clazz          目标类
     * @param parameterTypes 目标参数类型，元素可为 null
     * @return 已设置为可访问的构造函数
     */
    static Constructor<?> findBest(Class<?> clazz, Class<?>[] parameterTypes) {
        // 精确签名已知不存在时直接按最佳匹配查找，避免每次调用都构造一次失败异常。
        if (allPresent(parameterTypes) && !CACHE.isMissing(clazz, exactSignature(parameterTypes))) {
            try {
                return findExact(clazz, parameterTypes);
            } catch (ReflectException ignored) {
                // 继续按最佳匹配查找
            }
        }
        String sig = Types.signature(parameterTypes) + "#best";
        return CACHE.get(clazz, sig,
            key -> {
                Constructor<?> best = null;
                for (Constructor<?> candidate : clazz.getDeclaredConstructors()) {
                    if (!Types.isAssignable(parameterTypes, candidate.getParameterTypes()))
                        continue;
                    if (best == null || Types.compareFit(candidate.getParameterTypes(),
                        best.getParameterTypes(), parameterTypes) < 0) {
                        best = candidate;
                    }
                }
                if (best == null) return MemberCache.Slot.empty();
                best.setAccessible(true);
                return MemberCache.Slot.of(best);
            },
            () -> notFound(clazz, parameterTypes));
    }

    private static String exactSignature(Class<?>[] parameterTypes) {
        return Types.signature(parameterTypes) + "#exact";
    }

    private static boolean allPresent(Class<?>[] parameterTypes) {
        if (parameterTypes == null) return true;
        for (Class<?> type : parameterTypes) {
            if (type == null) return false;
        }
        return true;
    }

    private static ReflectException notFound(Class<?> clazz, Class<?>[] parameterTypes) {
        StringBuilder target = new StringBuilder(clazz.getName()).append("<init>(");
        if (parameterTypes != null) {
            for (int i = 0; i < parameterTypes.length; i++) {
                if (i > 0) target.append(',');
                target.append(parameterTypes[i] == null ? "null" : parameterTypes[i].getName());
            }
        }
        target.append(')');
        return new ReflectException(ReflectException.Kind.CONSTRUCTOR_NOT_FOUND, target.toString(),
            "No constructor " + target + ".");
    }
}