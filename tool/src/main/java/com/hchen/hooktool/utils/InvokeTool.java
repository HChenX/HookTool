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
package com.hchen.hooktool.utils;


import static com.hchen.hooktool.core.CoreTool.getParameterTypes;

import androidx.annotation.NonNull;

import com.hchen.hooktool.core.CoreTool;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反射工具
 *
 * @author 焕晨HChen
 */
public final class InvokeTool {
    private static final String TAG = "InvokeTool";
    private static final ConcurrentHashMap<String, Method> mMethodCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Field> mFieldCache = new ConcurrentHashMap<>();

    private InvokeTool() {
    }

    // ---------------------------- 调用方法 --------------------------------

    /**
     * 调用指定方法
     */
    public static <T> T callMethod(@NonNull Object instance, @NonNull String methodName, @NonNull Object[] parameterTypes, @NonNull Object... args) {
        return baseInvokeMethod(null, instance, methodName, getParameterTypes(instance.getClass().getClassLoader(), parameterTypes), args);
    }

    /**
     * 调用静态方法
     */
    public static <T> T callStaticMethod(@NonNull Class<?> clazz, @NonNull String methodName, @NonNull Object[] parameterTypes, @NonNull Object... args) {
        return baseInvokeMethod(clazz, null, methodName, getParameterTypes(clazz.getClassLoader(), parameterTypes), args);
    }

    /**
     * 调用静态方法
     */
    public static <T> T callStaticMethod(@NonNull String classPath, @NonNull String methodName, @NonNull Object[] parameterTypes, @NonNull Object... args) {
        return baseInvokeMethod(findClass(classPath), null, methodName, getParameterTypes(parameterTypes), args);
    }

    /**
     * 调用静态方法
     */
    public static <T> T callStaticMethod(@NonNull String classPath, ClassLoader classLoader, @NonNull String methodName, @NonNull Object[] parameterTypes, @NonNull Object... args) {
        return baseInvokeMethod(findClass(classPath, classLoader), null, methodName, getParameterTypes(classLoader, parameterTypes), args);
    }

    // ---------------------------- 设置字段 --------------------------------

    /**
     * 设置实例指定字段的值
     */
    public static void setField(@NonNull Object instance, @NonNull String fieldName, Object value) {
        baseInvokeField(null, instance, fieldName, true, value);
    }

    /**
     * 获取指定实例的字段的值
     */
    public static <T> T getField(@NonNull Object instance, @NonNull String fieldName) {
        return baseInvokeField(null, instance, fieldName, false, null);
    }

    /**
     * 设置静态字段的值
     */
    public static void setStaticField(@NonNull Class<?> clazz, @NonNull String fieldName, Object value) {
        baseInvokeField(clazz, null, fieldName, true, value);
    }

    /**
     * 设置静态字段的值
     */
    public static void setStaticField(@NonNull String classPath, @NonNull String fieldName, Object value) {
        baseInvokeField(findClass(classPath), null, fieldName, true, value);
    }

    /**
     * 设置静态字段的值
     */
    public static void setStaticField(@NonNull String classPath, ClassLoader classLoader, @NonNull String fieldName, Object value) {
        baseInvokeField(findClass(classPath, classLoader), null, fieldName, true, value);
    }

    /**
     * 获取静态字段的值
     */
    public static <T> T getStaticField(@NonNull Class<?> clazz, @NonNull String fieldName) {
        return baseInvokeField(clazz, null, fieldName, false, null);
    }

    /**
     * 获取静态字段的值
     */
    public static <T> T getStaticField(@NonNull String classPath, @NonNull String fieldName) {
        return baseInvokeField(findClass(classPath), null, fieldName, false, null);
    }

    /**
     * 获取静态字段的值
     */
    public static <T> T getStaticField(@NonNull String classPath, ClassLoader classLoader, @NonNull String fieldName) {
        return baseInvokeField(findClass(classPath, classLoader), null, fieldName, false, null);
    }

    /**
     * @noinspection unchecked
     */
    private static <T> T baseInvokeMethod(Class<?> clazz, Object instance, @NonNull String methodName, @NonNull Class<?>[] parameterTypes, @NonNull Object... args) {
        if (clazz == null) {
            clazz = instance.getClass();
        }

        try {
            Method method;
            String cacheKey = generateMethodCacheKey(clazz, methodName, parameterTypes);
            method = mMethodCache.get(cacheKey);
            if (method == null) {
                method = findMethod(clazz, methodName, parameterTypes);
                method.setAccessible(true);
                mMethodCache.put(cacheKey, method);
            }
            return (T) method.invoke(instance, args);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            CoreTool.throwIt(e);
            return null; // Not actually executed
        }
    }

    /**
     * 查找指定的方法，包括父类中的方法
     */
    @NonNull
    private static Method findMethod(@NonNull Class<?> clazz, @NonNull String methodName, @NonNull Class<?>[] parameterTypes) throws NoSuchMethodException {
        try {
            return clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            Class<?> superClass = clazz.getSuperclass();
            while (superClass != null && !superClass.equals(Object.class)) {
                try {
                    return superClass.getDeclaredMethod(methodName, parameterTypes);
                } catch (NoSuchMethodException ignored) {
                    superClass = superClass.getSuperclass();
                }
            }
            throw e;
        }
    }

    /**
     * 生成方法缓存的键
     */
    @NonNull
    private static String generateMethodCacheKey(@NonNull Class<?> clazz, @NonNull String methodName, @NonNull Class<?>[] parameterTypes) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(clazz.getName());
        keyBuilder.append("#").append(methodName);
        for (Class<?> paramType : parameterTypes) {
            keyBuilder.append("#").append(paramType.getName());
        }
        return keyBuilder.toString();
    }

    /**
     * @noinspection unchecked
     */
    private static <T> T baseInvokeField(Class<?> clazz, Object instance, @NonNull String fieldName, boolean isSetter, Object value) {
        if (clazz == null) {
            clazz = instance.getClass();
        }

        try {
            Field field;
            String cacheKey = generateFieldCacheKey(clazz, fieldName);
            field = mFieldCache.get(cacheKey);
            if (field == null) {
                field = findField(clazz, fieldName);
                field.setAccessible(true);
                mFieldCache.put(cacheKey, field);
            }

            if (isSetter) {
                field.set(instance, value);
                return null;
            } else {
                return (T) field.get(instance);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            CoreTool.throwIt(e);
            return null; // Not actually executed
        }
    }

    /**
     * 查找指定的字段，包括父类中的字段
     */
    @NonNull
    private static Field findField(@NonNull Class<?> clazz, @NonNull String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = clazz.getSuperclass();
            while (superClass != null && !superClass.equals(Object.class)) {
                try {
                    return superClass.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {
                    superClass = superClass.getSuperclass();
                }
            }
            throw e;
        }
    }

    /**
     * 生成字段缓存的键
     */
    @NonNull
    private static String generateFieldCacheKey(@NonNull Class<?> clazz, @NonNull String fieldName) {
        return clazz.getName() + "#" + fieldName;
    }

    /**
     * 根据类名查找类
     */
    @NonNull
    public static Class<?> findClass(@NonNull String classPath) {
        return findClass(classPath, null);
    }

    /**
     * 根据类名和类加载器查找类
     */
    @NonNull
    public static Class<?> findClass(@NonNull String classPath, ClassLoader classLoader) {
        try {
            if (classLoader == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            }

            return classLoader.loadClass(classPath);
        } catch (ClassNotFoundException e) {
            CoreTool.throwIt(e);
            // noinspection DataFlowIssue
            return null; // Not actually executed
        }
    }
}
