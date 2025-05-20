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

    /**
     * 调用指定方法
     *
     * @param instance   方法所属的实例对象
     * @param methodName 方法名
     * @param classes    方法参数类型数组
     * @param params     方法参数值数组
     * @param <T>        返回值
     * @return 方法执行后的返回值
     */
    public static <T> T callMethod(@NonNull Object instance, @NonNull String methodName, @NonNull Class<?>[] classes, @NonNull Object... params) {
        return baseInvokeMethod(null, instance, methodName, classes, params);
    }

    /**
     * 调用静态方法
     *
     * @param clazz      方法所属的类
     * @param methodName 方法名
     * @param classes    方法参数类型数组
     * @param params     方法参数值数组
     * @param <T>        返回值
     * @return 方法执行后的返回值
     */
    public static <T> T callStaticMethod(@NonNull Class<?> clazz, @NonNull String methodName, @NonNull Class<?>[] classes, @NonNull Object... params) {
        return baseInvokeMethod(clazz, null, methodName, classes, params);
    }

    /**
     * 调用静态方法
     *
     * @param classPath  类引用路径
     * @param methodName 方法名
     * @param classes    方法参数类型数组
     * @param params     方法参数值数组
     * @param <T>        返回值
     * @return 方法执行后的返回值
     */
    public static <T> T callStaticMethod(@NonNull String classPath, @NonNull String methodName, @NonNull Class<?>[] classes, @NonNull Object... params) {
        return baseInvokeMethod(findClass(classPath), null, methodName, classes, params);
    }

    /**
     * 调用静态方法
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param methodName  方法名
     * @param classes     方法参数类型数组
     * @param params      方法参数值数组
     * @param <T>         返回值
     * @return 方法执行后的返回值
     */
    public static <T> T callStaticMethod(@NonNull String classPath, ClassLoader classLoader, @NonNull String methodName, @NonNull Class<?>[] classes, @NonNull Object... params) {
        return baseInvokeMethod(findClass(classPath, classLoader), null, methodName, classes, params);
    }

    // ---------------------------- 设置字段 --------------------------------

    /**
     * 设置实例指定字段的值
     *
     * @param instance  字段所属的实例对象
     * @param fieldName 字段名
     * @param value     要设置的值
     */
    public static void setField(@NonNull Object instance, @NonNull String fieldName, Object value) {
        baseInvokeField(null, instance, fieldName, true, value);
    }

    /**
     * 设置静态字段的值
     *
     * @param clazz     字段所属的类
     * @param fieldName 字段名
     * @param value     要设置的值
     */
    public static void setStaticField(@NonNull Class<?> clazz, @NonNull String fieldName, Object value) {
        baseInvokeField(clazz, null, fieldName, true, value);
    }

    /**
     * 设置静态字段的值
     *
     * @param classPath 类引用路径
     * @param fieldName 字段名
     * @param value     要设置的值
     */
    public static void setStaticField(@NonNull String classPath, @NonNull String fieldName, Object value) {
        baseInvokeField(findClass(classPath), null, fieldName, true, value);
    }

    /**
     * 设置静态字段的值
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param fieldName   字段名
     * @param value       要设置的值
     */
    public static void setStaticField(@NonNull String classPath, ClassLoader classLoader, @NonNull String fieldName, Object value) {
        baseInvokeField(findClass(classPath, classLoader), null, fieldName, true, value);
    }

    /**
     * 获取指定实例的字段的值
     *
     * @param instance  字段所属的实例对象
     * @param fieldName 字段名
     * @param <T>       返回值类型
     * @return 字段的值
     */
    public static <T> T getField(@NonNull Object instance, @NonNull String fieldName) {
        return baseInvokeField(null, instance, fieldName, false, null);
    }

    /**
     * 获取静态字段的值
     *
     * @param clazz     字段所属的类
     * @param fieldName 字段名
     * @param <T>       返回值类型
     * @return 字段的值
     */
    public static <T> T getStaticField(@NonNull Class<?> clazz, @NonNull String fieldName) {
        return baseInvokeField(clazz, null, fieldName, false, null);
    }

    /**
     * 获取静态字段的值
     *
     * @param classPath 类引用路径
     * @param fieldName 字段名
     * @param <T>       返回值类型
     * @return 字段的值
     */
    public static <T> T getStaticField(@NonNull String classPath, @NonNull String fieldName) {
        return baseInvokeField(findClass(classPath), null, fieldName, false, null);
    }

    /**
     * 获取静态字段的值
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param fieldName   字段名
     * @param <T>         返回值类型
     * @return 字段的值
     */
    public static <T> T getStaticField(@NonNull String classPath, ClassLoader classLoader, @NonNull String fieldName) {
        return baseInvokeField(findClass(classPath, classLoader), null, fieldName, false, null);
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

    /**
     * 根据类名查找类
     *
     * @param classPath 类引用路径
     * @return 找到的类
     */
    @NonNull
    public static Class<?> findClass(@NonNull String classPath) {
        return findClass(classPath, null);
    }


    /**
     * 根据类名和类加载器查找类
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @return 找到的类
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
