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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 方法查找工具。
 * <p>
 * 支持精确匹配、最佳匹配，以及继承链与接口默认方法的查找。
 *
 * @author 焕晨HChen
 */
final class MethodFinder {
    private static final MemberCache<Method> CACHE = new MemberCache<>();

    private MethodFinder() {
    }

    /**
     * 按精确参数类型查找方法。
     *
     * @param clazz          查找起点类
     * @param name           方法名
     * @param parameterTypes 精确参数类型
     * @return 已设置为可访问的方法
     */
    static Method findExact(Class<?> clazz, String name, Class<?>[] parameterTypes) {
        String sig = exactSignature(name, parameterTypes);
        return CACHE.get(clazz, sig,
            key -> {
                try {
                    Method method = findExactRecursive(clazz, name, parameterTypes);
                    method.setAccessible(true);
                    return MemberCache.Slot.of(method);
                } catch (NoSuchMethodException e) {
                    return MemberCache.Slot.empty();
                }
            },
            () -> notFound(clazz, name, parameterTypes));
    }

    /**
     * 按最匹配的参数类型查找方法。
     *
     * @param clazz          查找起点类
     * @param name           方法名
     * @param parameterTypes 目标参数类型，元素可为 null 表示任意
     * @return 已设置为可访问的方法
     */
    static Method findBest(Class<?> clazz, String name, Class<?>[] parameterTypes) {
        // 精确签名已知不存在时直接按最佳匹配查找，避免每次调用都构造一次失败异常。
        if (allPresent(parameterTypes) && !CACHE.isMissing(clazz, exactSignature(name, parameterTypes))) {
            try {
                return findExact(clazz, name, parameterTypes);
            } catch (ReflectException ignored) {
                // 继续按最佳匹配查找
            }
        }
        String sig = name + "#" + Types.signature(parameterTypes) + "#best";
        return CACHE.get(clazz, sig,
            key -> {
                List<Method> candidates = new ArrayList<>();
                collectBestMatch(clazz, name, parameterTypes, candidates);
                Method best = null;
                for (Method candidate : candidates) {
                    if (best == null || Types.compareFit(candidate.getParameterTypes(),
                        best.getParameterTypes(), parameterTypes) < 0) {
                        best = candidate;
                    }
                }
                if (best == null) return MemberCache.Slot.empty();
                best.setAccessible(true);
                return MemberCache.Slot.of(best);
            },
            () -> notFound(clazz, name, parameterTypes));
    }


    private static ReflectException notFound(Class<?> clazz, String name, Class<?>[] parameterTypes) {
        String target = clazz.getName() + "#" + name + describe(parameterTypes);
        return new ReflectException(ReflectException.Kind.METHOD_NOT_FOUND, target,
            "No method " + target + " in the class hierarchy or its interfaces.");
    }

    private static String describe(Class<?>[] parameterTypes) {
        StringBuilder sb = new StringBuilder("(");
        if (parameterTypes != null) {
            for (int i = 0; i < parameterTypes.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(parameterTypes[i] == null ? "null" : parameterTypes[i].getName());
            }
        }
        return sb.append(')').toString();
    }

    private static String exactSignature(String name, Class<?>[] parameterTypes) {
        return name + "#" + Types.signature(parameterTypes) + "#exact";
    }

    private static boolean allPresent(Class<?>[] parameterTypes) {
        if (parameterTypes == null) return true;
        for (Class<?> type : parameterTypes) {
            if (type == null) return false;
        }
        return true;
    }

    private static Method findExactRecursive(Class<?> clazz, String name, Class<?>[] parameterTypes)
        throws NoSuchMethodException {
        Class<?> clz = clazz;
        while (clz != null) {
            try {
                return clz.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                // 继续向上查找
            }
            clz = clz.getSuperclass();
        }

        clz = clazz;
        while (clz != null) {
            for (Class<?> iface : clz.getInterfaces()) {
                Method method = findInterfaceMethodExactRecursive(iface, name, parameterTypes);
                if (method != null) return method;
            }
            clz = clz.getSuperclass();
        }
        throw new NoSuchMethodException(name);
    }

    private static Method findInterfaceMethodExactRecursive(Class<?> iface, String name,
                                                            Class<?>[] parameterTypes) {
        try {
            Method method = iface.getDeclaredMethod(name, parameterTypes);
            int modifiers = method.getModifiers();
            if (!Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)
                && !Modifier.isAbstract(modifiers)) {
                return method;
            }
        } catch (NoSuchMethodException ignored) {
            // 继续查找父接口
        }
        for (Class<?> superInterface : iface.getInterfaces()) {
            Method method = findInterfaceMethodExactRecursive(superInterface, name, parameterTypes);
            if (method != null) return method;
        }
        return null;
    }

    private static void collectBestMatch(Class<?> clazz, String name, Class<?>[] parameterTypes,
                                         List<Method> result) {
        Set<String> seen = new HashSet<>();

        Class<?> clz = clazz;
        boolean considerPrivate = true;
        while (clz != null) {
            for (Method method : clz.getDeclaredMethods()) {
                if (method.isBridge() || method.isSynthetic()) continue;
                if (!considerPrivate && Modifier.isPrivate(method.getModifiers())) continue;
                if (method.getName().equals(name)
                    && Types.isAssignable(parameterTypes, method.getParameterTypes())) {
                    String sig = method.getName() + Arrays.toString(method.getParameterTypes());
                    if (seen.add(sig)) result.add(method);
                }
            }
            considerPrivate = false;
            clz = clz.getSuperclass();
        }

        clz = clazz;
        while (clz != null) {
            for (Class<?> iface : clz.getInterfaces()) {
                collectInterfaceMethodsRecursive(iface, name, parameterTypes, seen, result);
            }
            clz = clz.getSuperclass();
        }
    }

    private static void collectInterfaceMethodsRecursive(Class<?> iface, String name,
                                                         Class<?>[] parameterTypes, Set<String> seen,
                                                         List<Method> result) {
        for (Method method : iface.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers)
                || Modifier.isAbstract(modifiers) || method.isBridge() || method.isSynthetic()) {
                continue;
            }
            if (method.getName().equals(name)
                && Types.isAssignable(parameterTypes, method.getParameterTypes())) {
                String sig = method.getName() + Arrays.toString(method.getParameterTypes());
                if (seen.add(sig)) result.add(method);
            }
        }
        for (Class<?> superInterface : iface.getInterfaces()) {
            collectInterfaceMethodsRecursive(superInterface, name, parameterTypes, seen, result);
        }
    }
}