/*
 * This file is part of HookTool.

 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HChenX
 */
package com.hchen.hooktool.utils;

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
    public static <T> T callMethod(Object instance, String methodName, Class<?>[] classes, Object... params) {
        return baseInvokeMethod(null, instance, methodName, classes, params);
    }

    public static <T> T callStaticMethod(Class<?> clazz, String methodName, Class<?>[] classes, Object... params) {
        return baseInvokeMethod(clazz, null, methodName, classes, params);
    }

    public static <T> T callStaticMethod(String clazz, String methodName, Class<?>[] classes, Object... params) {
        return baseInvokeMethod(findClass(clazz), null, methodName, classes, params);
    }

    public static <T> T callStaticMethod(String clazz, ClassLoader classLoader, String methodName, Class<?>[] classes, Object... params) {
        return baseInvokeMethod(findClass(clazz, classLoader), null, methodName, classes, params);
    }

    // ---------------------------- 设置字段 --------------------------------
    public static <T> T setField(Object instance, String fieldName, Object value) {
        return baseInvokeField(null, instance, fieldName, true, value);
    }

    public static <T> T setStaticField(Class<?> clazz, String fieldName, Object value) {
        return baseInvokeField(clazz, null, fieldName, true, value);
    }

    public static <T> T setStaticField(String clazz, String fieldName, Object value) {
        return baseInvokeField(findClass(clazz), null, fieldName, true, value);
    }

    public static <T> T setStaticField(String clazz, ClassLoader classLoader, String fieldName, Object value) {
        return baseInvokeField(findClass(clazz, classLoader), null, fieldName, true, value);
    }

    public static <T> T getField(Object instance, String fieldName) {
        return baseInvokeField(null, instance, fieldName, false, null);
    }

    public static <T> T getStaticField(Class<?> clazz, String fieldName) {
        return baseInvokeField(clazz, null, fieldName, false, null);
    }

    public static <T> T getStaticField(String clazz, String fieldName) {
        return baseInvokeField(findClass(clazz), null, fieldName, false, null);
    }

    public static <T> T getStaticField(String clazz, ClassLoader classLoader, String fieldName) {
        return baseInvokeField(findClass(clazz, classLoader), null, fieldName, false, null);
    }

    /**
     * @noinspection unchecked
     */
    private static <T> T baseInvokeMethod(Class<?> clazz /* 类 */, Object instance /* 实例 */, String methodName /* 方法名 */,
                                          Class<?>[] classes /* 方法参数 */, Object... params /* 值 */) {
        Method declaredMethod;
        if (clazz == null && instance == null) {
            throw new NullPointerException("[InvokeTool]: Class or instance must not is null, can't invoke method: " + methodName);
        } else if (clazz == null) {
            clazz = instance.getClass();
        }
        try {
            String methodTag = clazz.getName() + "#" + methodName + "#" + Arrays.toString(classes);
            declaredMethod = mMethodCache.get(methodTag);
            if (declaredMethod == null) {
                declaredMethod = clazz.getDeclaredMethod(methodName, classes);
                mMethodCache.put(methodTag, declaredMethod);
            }
            declaredMethod.setAccessible(true);
            return (T) declaredMethod.invoke(instance, params);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * @noinspection unchecked
     */
    private static <T> T baseInvokeField(Class<?> clazz /* 类 */, Object instance /* 实例 */, String fieldName /* 字段名 */,
                                         boolean isSetMode /* 是否为 set 模式 */, Object value /* 指定值 */) {
        Field declaredField = null;
        if (clazz == null && instance == null) {
            throw new NullPointerException("[InvokeTool]: Class or instance must not is null, can't invoke field: " + fieldName);
        } else if (clazz == null) {
            clazz = instance.getClass();
        }
        try {
            String fieldTag = clazz.getName() + "#" + fieldName;
            declaredField = mFieldCache.get(fieldTag);
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
                mFieldCache.put(fieldTag, declaredField);
            }
            declaredField.setAccessible(true);
            if (isSetMode) {
                declaredField.set(instance, value);
                return null;
            } else
                return (T) declaredField.get(instance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new UnexpectedException(e);
        }
    }

    @NonNull
    public static Class<?> findClass(String className) {
        return findClass(className, null);
    }

    @NonNull
    public static Class<?> findClass(String className, ClassLoader classLoader) {
        try {
            if (classLoader == null)
                classLoader = ClassLoader.getSystemClassLoader();

            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
