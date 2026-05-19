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
 * Java 反射调用工具类。
 * <p>
 * 封装了基于反射机制的方法调用与字段读写操作，并使用 {@link ConcurrentHashMap} 对已解析的
 * {@link Method} 和 {@link Field} 进行缓存，以避免重复查找带来的性能开销。
 * <p>
 * 支持以下操作：
 * <ul>
 *     <li>调用实例方法与静态方法（支持自定义类加载器）</li>
 *     <li>读取和设置实例字段与静态字段（支持自定义类加载器）</li>
 *     <li>通过全限定名加载类（支持自定义类加载器）</li>
 * </ul>
 * <p>
 * 该类为纯工具类，所有方法均为静态方法，不允许实例化。
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
     * 通过反射调用指定对象实例的方法。
     *
     * @param instance       目标对象实例，不得为 {@code null}
     * @param methodName     方法名称
     * @param parameterTypes 参数类型数组，用于精确匹配目标方法签名
     * @param args           传入方法的实际参数值
     * @param <T>            返回值的泛型类型
     * @return 方法执行后的返回值
     */
    public static <T> T callMethod(@NonNull Object instance, @NonNull String methodName, @NonNull Object[] parameterTypes, @NonNull Object... args) {
        return baseInvokeMethod(null, instance, methodName, getParameterTypes(instance.getClass().getClassLoader(), parameterTypes), args);
    }

    /**
     * 通过反射调用指定类的静态方法。
     *
     * @param clazz          目标类，不得为 {@code null}
     * @param methodName     方法名称
     * @param parameterTypes 参数类型数组，用于精确匹配目标方法签名
     * @param args           传入方法的实际参数值
     * @param <T>            返回值的泛型类型
     * @return 方法执行后的返回值
     */
    public static <T> T callStaticMethod(@NonNull Class<?> clazz, @NonNull String methodName, @NonNull Object[] parameterTypes, @NonNull Object... args) {
        return baseInvokeMethod(clazz, null, methodName, getParameterTypes(clazz.getClassLoader(), parameterTypes), args);
    }

    /**
     * 通过类全限定名反射调用静态方法，使用系统类加载器加载目标类。
     *
     * @param classPath      目标类的全限定名
     * @param methodName     方法名称
     * @param parameterTypes 参数类型数组
     * @param args           传入方法的实际参数值
     * @param <T>            返回值的泛型类型
     * @return 方法执行后的返回值
     */
    public static <T> T callStaticMethod(@NonNull String classPath, @NonNull String methodName, @NonNull Object[] parameterTypes, @NonNull Object... args) {
        return baseInvokeMethod(findClass(classPath), null, methodName, getParameterTypes(parameterTypes), args);
    }

    /**
     * 通过类全限定名和自定义类加载器反射调用静态方法。
     *
     * @param classPath      目标类的全限定名
     * @param classLoader    用于加载目标类的类加载器，不得为 {@code null}
     * @param methodName     方法名称
     * @param parameterTypes 参数类型数组
     * @param args           传入方法的实际参数值
     * @param <T>            返回值的泛型类型
     * @return 方法执行后的返回值
     */
    public static <T> T callStaticMethod(@NonNull String classPath, ClassLoader classLoader, @NonNull String methodName, @NonNull Object[] parameterTypes, @NonNull Object... args) {
        return baseInvokeMethod(findClass(classPath, classLoader), null, methodName, getParameterTypes(classLoader, parameterTypes), args);
    }

    // ---------------------------- 设置字段 --------------------------------

    /**
     * 通过反射设置指定对象实例的字段值。
     *
     * @param instance  目标对象实例，不得为 {@code null}
     * @param fieldName 字段名称
     * @param value     要设置的字段值
     */
    public static void setField(@NonNull Object instance, @NonNull String fieldName, Object value) {
        baseInvokeField(null, instance, fieldName, true, value);
    }

    /**
     * 通过反射获取指定对象实例的字段值。
     *
     * @param instance  目标对象实例，不得为 {@code null}
     * @param fieldName 字段名称
     * @param <T>       返回值的泛型类型
     * @return 字段的当前值
     */
    public static <T> T getField(@NonNull Object instance, @NonNull String fieldName) {
        return baseInvokeField(null, instance, fieldName, false, null);
    }

    /**
     * 通过反射设置指定类的静态字段值。
     *
     * @param clazz     目标类，不得为 {@code null}
     * @param fieldName 字段名称
     * @param value     要设置的字段值
     */
    public static void setStaticField(@NonNull Class<?> clazz, @NonNull String fieldName, Object value) {
        baseInvokeField(clazz, null, fieldName, true, value);
    }

    /**
     * 通过类全限定名反射设置静态字段值，使用系统类加载器加载目标类。
     *
     * @param classPath 目标类的全限定名
     * @param fieldName 字段名称
     * @param value     要设置的字段值
     */
    public static void setStaticField(@NonNull String classPath, @NonNull String fieldName, Object value) {
        baseInvokeField(findClass(classPath), null, fieldName, true, value);
    }

    /**
     * 通过类全限定名和自定义类加载器反射设置静态字段值。
     *
     * @param classPath   目标类的全限定名
     * @param classLoader 用于加载目标类的类加载器
     * @param fieldName   字段名称
     * @param value       要设置的字段值
     */
    public static void setStaticField(@NonNull String classPath, ClassLoader classLoader, @NonNull String fieldName, Object value) {
        baseInvokeField(findClass(classPath, classLoader), null, fieldName, true, value);
    }

    /**
     * 通过反射获取指定类的静态字段值。
     *
     * @param clazz     目标类，不得为 {@code null}
     * @param fieldName 字段名称
     * @param <T>       返回值的泛型类型
     * @return 字段的当前值
     */
    public static <T> T getStaticField(@NonNull Class<?> clazz, @NonNull String fieldName) {
        return baseInvokeField(clazz, null, fieldName, false, null);
    }

    /**
     * 通过类全限定名反射获取静态字段值，使用系统类加载器加载目标类。
     *
     * @param classPath 目标类的全限定名
     * @param fieldName 字段名称
     * @param <T>       返回值的泛型类型
     * @return 字段的当前值
     */
    public static <T> T getStaticField(@NonNull String classPath, @NonNull String fieldName) {
        return baseInvokeField(findClass(classPath), null, fieldName, false, null);
    }

    /**
     * 通过类全限定名和自定义类加载器反射获取静态字段值。
     *
     * @param classPath   目标类的全限定名
     * @param classLoader 用于加载目标类的类加载器
     * @param fieldName   字段名称
     * @param <T>         返回值的泛型类型
     * @return 字段的当前值
     */
    public static <T> T getStaticField(@NonNull String classPath, ClassLoader classLoader, @NonNull String fieldName) {
        return baseInvokeField(findClass(classPath, classLoader), null, fieldName, false, null);
    }

    /**
     * @noinspection unchecked
     */
    private static <T> T baseInvokeMethod(Class<?> clazz, Object instance, @NonNull String methodName, @NonNull Class<?>[] parameterTypes, @NonNull Object... args) {
        if (clazz == null) {
            if (instance == null) {
                throw new IllegalArgumentException("Both clazz and instance cannot be null.");
            }
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
     * 在指定类及其父类链中递归查找匹配的方法。
     * <p>
     * 优先从当前类中查找，未找到则沿继承链向上逐级查找，直至 {@code Object} 类。
     *
     * @param clazz          起始查找的类
     * @param methodName     方法名称
     * @param parameterTypes 参数类型数组
     * @return 匹配的 {@link Method} 对象
     * @throws NoSuchMethodException 若在类及整条父类链中均未找到匹配方法
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
     * 生成方法缓存键。
     * <p>
     * 格式为 {@code "类全限定名#方法名#参数类型1全限定名#参数类型2全限定名..."}。
     *
     * @param clazz          目标类
     * @param methodName     方法名称
     * @param parameterTypes 参数类型数组
     * @return 用于缓存查找的唯一键字符串
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
            if (instance == null) {
                throw new IllegalArgumentException("Both clazz and instance cannot be null.");
            }
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
     * 在指定类及其父类链中递归查找匹配的字段。
     * <p>
     * 优先从当前类中查找，未找到则沿继承链向上逐级查找，直至 {@code Object} 类。
     *
     * @param clazz     起始查找的类
     * @param fieldName 字段名称
     * @return 匹配的 {@link Field} 对象
     * @throws NoSuchFieldException 若在类及整条父类链中均未找到匹配字段
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
     * 生成字段缓存键。
     * <p>
     * 格式为 {@code "类全限定名#字段名"}。
     *
     * @param clazz     目标类
     * @param fieldName 字段名称
     * @return 用于缓存查找的唯一键字符串
     */
    @NonNull
    private static String generateFieldCacheKey(@NonNull Class<?> clazz, @NonNull String fieldName) {
        return clazz.getName() + "#" + fieldName;
    }

    /**
     * 通过类全限定名查找并加载类，使用系统类加载器。
     *
     * @param classPath 类的全限定名
     * @return 加载成功的 {@link Class} 对象
     * @throws RuntimeException 若指定类未找到
     */
    @NonNull
    public static Class<?> findClass(@NonNull String classPath) {
        return findClass(classPath, null);
    }

    /**
     * 通过类全限定名和指定的类加载器查找并加载类。
     * <p>
     * 若 {@code classLoader} 参数为 {@code null}，则回退使用系统类加载器。
     *
     * @param classPath   类的全限定名
     * @param classLoader 用于加载类的类加载器，为 {@code null} 时使用系统类加载器
     * @return 加载成功的 {@link Class} 对象
     * @throws RuntimeException 若指定类未找到
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
