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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按类加载器隔离的类缓存。
 * <p>
 * 仅缓存查找成功的结果。
 *
 * @author 焕晨HChen
 */
final class ClassCache {
    private static final Map<String, Class<?>> PRIMITIVES = new HashMap<>();

    static {
        PRIMITIVES.put("boolean", boolean.class);
        PRIMITIVES.put("byte", byte.class);
        PRIMITIVES.put("char", char.class);
        PRIMITIVES.put("short", short.class);
        PRIMITIVES.put("int", int.class);
        PRIMITIVES.put("long", long.class);
        PRIMITIVES.put("float", float.class);
        PRIMITIVES.put("double", double.class);
        PRIMITIVES.put("void", void.class);
    }

    private static final ConcurrentHashMap<CacheKey, WeakReference<Class<?>>> CACHE =
        new ConcurrentHashMap<>();

    private ClassCache() {
    }

    private static final class CacheKey {
        private final int loaderId;
        private final String className;

        private CacheKey(int loaderId, String className) {
            this.loaderId = loaderId;
            this.className = className;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof CacheKey other)) return false;
            return loaderId == other.loaderId && className.equals(other.className);
        }

        @Override
        public int hashCode() {
            return 31 * loaderId + className.hashCode();
        }
    }

    /**
     * 加载指定名字的类。
     *
     * @param className 类名，支持基本类型与数组写法
     * @param classLoader 类加载器，为 null 时使用默认值
     * @return 加载到的类
     * @throws ReflectException 类不存在时抛出
     */
    static Class<?> find(String className, ClassLoader classLoader) {
        ClassLoader loader = classLoader != null ? classLoader : ReflectEnv.defaultClassLoader();
        CacheKey key = new CacheKey(System.identityHashCode(loader), className);
        WeakReference<Class<?>> cached = CACHE.get(key);
        if (cached != null) {
            Class<?> value = cached.get();
            if (value != null && value.getClassLoader() == loader) return value;
        }
        Class<?> clazz = doFindClass(className, loader);
        CACHE.put(key, new WeakReference<>(clazz));
        return clazz;
    }

    /**
     * 加载指定名字的类，失败时返回空结果。
     *
     * @param className 类名
     * @param classLoader 类加载器，为 null 时使用默认值
     * @return 查找结果
     */
    static Lookup<Class<?>> lookup(String className, ClassLoader classLoader) {
        try {
            return Lookup.of(find(className, classLoader));
        } catch (ReflectException e) {
            return Lookup.failed(e);
        }
    }

    private static Class<?> doFindClass(String className, ClassLoader classLoader) {
        String next = className;
        int lastDotIndex;
        do {
            Class<?> primitive = PRIMITIVES.get(next);
            if (primitive != null) return primitive;
            try {
                return Class.forName(toCanonicalName(next), false, classLoader);
            } catch (ClassNotFoundException e) {
                lastDotIndex = next.lastIndexOf('.');
                if (lastDotIndex != -1) {
                    next = next.substring(0, lastDotIndex) + '$' + next.substring(lastDotIndex + 1);
                }
            }
        } while (lastDotIndex != -1);
        throw new ReflectException(ReflectException.Kind.CLASS_NOT_FOUND, className,
            "Class not found: " + className);
    }

    private static String toCanonicalName(String className) {
        String name = className == null ? "" : className.trim();
        if (name.endsWith("[]")) {
            StringBuilder sb = new StringBuilder();
            while (name.endsWith("[]")) {
                sb.append('[');
                name = name.substring(0, name.length() - 2);
            }
            switch (name) {
                case "boolean":
                    sb.append('Z');
                    break;
                case "byte":
                    sb.append('B');
                    break;
                case "char":
                    sb.append('C');
                    break;
                case "short":
                    sb.append('S');
                    break;
                case "int":
                    sb.append('I');
                    break;
                case "long":
                    sb.append('J');
                    break;
                case "float":
                    sb.append('F');
                    break;
                case "double":
                    sb.append('D');
                    break;
                default:
                    sb.append('L').append(name).append(';');
                    break;
            }
            return sb.toString();
        }
        return name;
    }
}