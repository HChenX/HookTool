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
 * Copyright (C) 2023–2025 HChenX
 */
package com.hchen.hooktool.utils;

import static com.hchen.hooktool.core.CoreTool.getParamTypes;

import androidx.annotation.NonNull;

import com.hchen.hooktool.exception.UnexpectedException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

/**
 * 反射工具
 *
 * @author 焕晨HChen
 */
public class InvokeTool {
    private static final String TAG = "InvokeTool";
    private static final HashMap<String, Method> mMethodCache = new HashMap<>();
    private static final HashMap<String, Field> mFieldCache = new HashMap<>();

    private InvokeTool() {
    }

    // ---------------------------- 调用方法 --------------------------------

    /**
     * 调用指定方法
     */
    public static <T> T callMethod(@NonNull Object instance, @NonNull String methodName, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return baseInvokeMethod(null, instance, methodName, getParamTypes(instance.getClass().getClassLoader(), paramTypes), params);
    }

    /**
     * 调用静态方法
     */
    public static <T> T callStaticMethod(@NonNull Class<?> clazz, @NonNull String methodName, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return baseInvokeMethod(clazz, null, methodName, getParamTypes(clazz.getClassLoader(), paramTypes), params);
    }

    /**
     * 调用静态方法
     */
    public static <T> T callStaticMethod(@NonNull String classPath, @NonNull String methodName, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return baseInvokeMethod(findClass(classPath), null, methodName, getParamTypes(null, paramTypes), params);
    }

    /**
     * 调用静态方法
     */
    public static <T> T callStaticMethod(@NonNull String classPath, ClassLoader classLoader, @NonNull String methodName, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return baseInvokeMethod(findClass(classPath, classLoader), null, methodName, getParamTypes(classLoader, paramTypes), params);
    }

    // ---------------------------- 设置字段 --------------------------------

    /**
     * 设置实例指定字段的值
     */
    public static void setField(@NonNull Object instance, @NonNull String fieldName, Object value) {
        baseInvokeField(null, instance, fieldName, true, value);
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
     * 获取指定实例的字段的值
     */
    public static <T> T getField(@NonNull Object instance, @NonNull String fieldName) {
        return baseInvokeField(null, instance, fieldName, false, null);
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
    private static <T> T baseInvokeMethod(Class<?> clazz, Object instance, String methodName, Class<?>[] paramTypes, Object... params) {
        if (clazz == null && instance == null) {
            throw new NullPointerException("[InvokeTool]: Class or Instance must not be null, can't invoke method: " + methodName);
        } else if (clazz == null) {
            clazz = instance.getClass();
        }

        try {
            Method declaredMethod;
            String id = clazz.getName() + "#" + methodName + "#" + Arrays.toString(paramTypes);
            declaredMethod = mMethodCache.get(id);
            if (declaredMethod == null) {
                try {
                    declaredMethod = clazz.getDeclaredMethod(methodName, paramTypes);
                } catch (NoSuchMethodException e) {
                    while (true) {
                        clazz = clazz.getSuperclass();
                        if (clazz == null || clazz.equals(Object.class))
                            break;

                        try {
                            declaredMethod = clazz.getDeclaredMethod(methodName, paramTypes);
                            break;
                        } catch (NoSuchMethodException ignored) {
                        }
                    }
                    if (declaredMethod == null) throw e;
                }
                declaredMethod.setAccessible(true);
                mMethodCache.put(id, declaredMethod);
            }
            return (T) declaredMethod.invoke(instance, params);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * @noinspection unchecked
     */
    private static <T> T baseInvokeField(Class<?> clazz, Object instance, String fieldName, boolean isSetter, Object value) {
        if (clazz == null && instance == null) {
            throw new NullPointerException("[InvokeTool]: Class or Instance must not be null, can't invoke field: " + fieldName);
        } else if (clazz == null) {
            clazz = instance.getClass();
        }

        try {
            Field declaredField;
            String id = clazz.getName() + "#" + fieldName;
            declaredField = mFieldCache.get(id);
            if (declaredField == null) {
                try {
                    declaredField = clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    while (true) {
                        clazz = clazz.getSuperclass();
                        if (clazz == null || clazz.equals(Object.class))
                            break;

                        try {
                            declaredField = clazz.getDeclaredField(fieldName);
                            break;
                        } catch (NoSuchFieldException ignored) {
                        }
                    }
                    if (declaredField == null) throw e;
                }
                declaredField.setAccessible(true);
                mFieldCache.put(id, declaredField);
            }

            if (isSetter) {
                declaredField.set(instance, value);
                return null;
            } else
                return (T) declaredField.get(instance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new UnexpectedException(e);
        }
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
            if (classLoader == null)
                classLoader = ClassLoader.getSystemClassLoader();

            return classLoader.loadClass(classPath);
        } catch (ClassNotFoundException e) {
            throw new UnexpectedException(e);
        }
    }
}
