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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 反射工具类。
 * <p>
 * 提供类、字段、方法、构造函数的查找与调用能力，所有查找结果按类缓存。
 *
 * @author 焕晨HChen
 */
public final class Reflect {
    private Reflect() {
    }

    // ------------------------------ class ------------------------------

    /**
     * 使用默认类加载器加载类。
     *
     * @param className 类名，支持 {@code int[]} 与内部类写法
     * @return 加载到的类
     */
    @NonNull
    public static Class<?> findClass(@NonNull String className) {
        return ClassCache.find(className, null);
    }

    /**
     * 加载类。
     *
     * @param className 类名
     * @param classLoader 类加载器，为 null 时使用默认值
     * @return 加载到的类
     */
    @NonNull
    public static Class<?> findClass(@NonNull String className, @Nullable ClassLoader classLoader) {
        return ClassCache.find(className, classLoader);
    }

    /**
     * 加载类，不存在时返回 null。
     *
     * @param className 类名
     * @return 类或 null
     */
    @Nullable
    public static Class<?> findClassIfExists(@NonNull String className) {
        return findClassIfExists(className, null);
    }

    /**
     * 加载类，不存在时返回 null。
     *
     * @param className 类名
     * @param classLoader 类加载器
     * @return 类或 null
     */
    @Nullable
    public static Class<?> findClassIfExists(@NonNull String className,
                                             @Nullable ClassLoader classLoader) {
        return ClassCache.lookup(className, classLoader).getOrNull();
    }

    /**
     * 加载类，失败时返回带原因的结果。
     *
     * @param className 类名
     * @param classLoader 类加载器
     * @return 查找结果
     */
    @NonNull
    public static Lookup<Class<?>> lookupClass(@NonNull String className,
                                               @Nullable ClassLoader classLoader) {
        return ClassCache.lookup(className, classLoader);
    }

    // ------------------------------ field ------------------------------

    /**
     * 沿继承链与接口树查找字段。
     *
     * @param clazz 目标类
     * @param fieldName 字段名
     * @return 已设置为可访问的字段
     */
    @NonNull
    public static Field findField(@NonNull Class<?> clazz, @NonNull String fieldName) {
        return FieldFinder.find(clazz, fieldName);
    }

    /**
     * 查找字段，不存在时返回 null。
     *
     * @param clazz 目标类
     * @param fieldName 字段名
     * @return 字段或 null
     */
    @Nullable
    public static Field findFieldIfExists(@NonNull Class<?> clazz, @NonNull String fieldName) {
        return FieldFinder.findOrNull(clazz, fieldName);
    }

    /**
     * 查找字段，失败时返回带原因的结果。
     *
     * @param clazz 目标类
     * @param fieldName 字段名
     * @return 查找结果
     */
    @NonNull
    public static Lookup<Field> lookupField(@NonNull Class<?> clazz, @NonNull String fieldName) {
        try {
            return Lookup.of(findField(clazz, fieldName));
        } catch (ReflectException e) {
            return Lookup.failed(e);
        }
    }

    // --------------------------- method exact ---------------------------

    /**
     * 按精确参数类型查找方法。
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param parameterTypes 参数类型，元素为 Class 或类名字符串
     * @return 已设置为可访问的方法
     */
    @NonNull
    public static Method findMethodExact(@NonNull Class<?> clazz, @NonNull String methodName,
                                    @NonNull Object... parameterTypes) {
        Class<?>[] resolved = ParameterTokens.resolve(clazz.getClassLoader(), parameterTypes);
        return MethodFinder.findExact(clazz, methodName, resolved);
    }

    /**
     * 按精确参数类型在指定类名中查找方法。
     *
     * @param className 目标类名
     * @param classLoader 用于解析类名与参数字符串的类加载器
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     * @return 已设置为可访问的方法
     */
    @NonNull
    public static Method findMethodExact(@NonNull String className, @Nullable ClassLoader classLoader,
                                    @NonNull String methodName,
                                    @NonNull Object... parameterTypes) {
        Class<?> clazz = findClass(className, classLoader);
        Class<?>[] resolved = ParameterTokens.resolve(
            classLoader != null ? classLoader : clazz.getClassLoader(), parameterTypes);
        return MethodFinder.findExact(clazz, methodName, resolved);
    }

    /**
     * 按精确参数类型查找方法，参数类型已解析为 Class。
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param parameterTypes 精确参数类型
     * @return 已设置为可访问的方法
     */
    @NonNull
    static Method findMethodExactWithTypes(@NonNull Class<?> clazz, @NonNull String methodName,
                                      @NonNull Class<?>... parameterTypes) {
        return MethodFinder.findExact(clazz, methodName,
            parameterTypes == null ? new Class<?>[0] : parameterTypes);
    }

    /**
     * 按精确参数类型查找方法，不存在时返回 null。
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     * @return 方法或 null
     */
    @Nullable
    public static Method findMethodExactIfExists(@NonNull Class<?> clazz, @NonNull String methodName,
                                            @NonNull Object... parameterTypes) {
        try {
            return findMethodExact(clazz, methodName, parameterTypes);
        } catch (ReflectException e) {
            return null;
        }
    }

    /**
     * 按精确参数类型在指定类名中查找方法，不存在时返回 null。
     *
     * @param className 目标类名
     * @param classLoader 类加载器
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     * @return 方法或 null
     */
    @Nullable
    public static Method findMethodExactIfExists(@NonNull String className,
                                            @Nullable ClassLoader classLoader,
                                            @NonNull String methodName,
                                            @NonNull Object... parameterTypes) {
        try {
            return findMethodExact(className, classLoader, methodName, parameterTypes);
        } catch (ReflectException e) {
            return null;
        }
    }

    /**
     * 按精确参数类型查找方法，失败时返回带原因的结果。
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param parameterTypes 精确参数类型
     * @return 查找结果
     */
    @NonNull
    public static Lookup<Method> lookupMethod(@NonNull Class<?> clazz, @NonNull String methodName,
                                              @NonNull Class<?>... parameterTypes) {
        try {
            return Lookup.of(findMethodExactWithTypes(clazz, methodName, parameterTypes));
        } catch (ReflectException e) {
            return Lookup.failed(e);
        }
    }

    // --------------------------- method best ----------------------------

    /**
     * 查找与给定参数类型最匹配的方法。
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param parameterTypes 目标参数类型，元素可为 null
     * @return 已设置为可访问的方法
     */
    @NonNull
    public static Method findMethodBestMatch(@NonNull Class<?> clazz, @NonNull String methodName,
                                             @NonNull Class<?>... parameterTypes) {
        return MethodFinder.findBest(clazz, methodName,
            parameterTypes == null ? new Class<?>[0] : parameterTypes);
    }

    /**
     * 根据实参值查找最匹配的方法。
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param args 实参，元素为 null 表示任意类型
     * @return 已设置为可访问的方法
     */
    @NonNull
    public static Method findMethodByArgs(@NonNull Class<?> clazz, @NonNull String methodName,
                                          @NonNull Object... args) {
        return MethodFinder.findBest(clazz, methodName, typesOf(args));
    }

    /**
     * 查找最匹配的方法，参数类型中的 null 由实参类型补全。
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param parameterTypes 声明的参数类型，元素可为 null
     * @param args 实参
     * @return 已设置为可访问的方法
     */
    @NonNull
    public static Method findMethodBestMatch(@NonNull Class<?> clazz,
                                             @NonNull String methodName,
                                             @NonNull Class<?>[] parameterTypes,
                                             @NonNull Object[] args) {
        if (parameterTypes.length != args.length) {
            throw new IllegalArgumentException("parameterTypes size (" + parameterTypes.length
                + ") must match args size (" + args.length + ")");
        }
        Class<?>[] resolved = parameterTypes.clone();
        for (int i = 0; i < resolved.length; i++) {
            if (resolved[i] == null && args[i] != null) {
                resolved[i] = args[i].getClass();
            }
        }
        return MethodFinder.findBest(clazz, methodName, resolved);
    }

    // ------------------------- constructor exact ------------------------

    /**
     * 按精确参数类型查找构造函数。
     *
     * @param clazz 目标类
     * @param parameterTypes 参数类型，元素为 Class 或类名字符串
     * @return 已设置为可访问的构造函数
     */
    @NonNull
    public static Constructor<?> findConstructorExact(@NonNull Class<?> clazz,
                                                 @NonNull Object... parameterTypes) {
        Class<?>[] resolved = ParameterTokens.resolve(clazz.getClassLoader(), parameterTypes);
        return ConstructorFinder.findExact(clazz, resolved);
    }

    /**
     * 按精确参数类型在指定类名中查找构造函数。
     *
     * @param className 目标类名
     * @param classLoader 类加载器
     * @param parameterTypes 参数类型
     * @return 已设置为可访问的构造函数
     */
    @NonNull
    public static Constructor<?> findConstructorExact(@NonNull String className,
                                                 @Nullable ClassLoader classLoader,
                                                 @NonNull Object... parameterTypes) {
        Class<?> clazz = findClass(className, classLoader);
        Class<?>[] resolved = ParameterTokens.resolve(
            classLoader != null ? classLoader : clazz.getClassLoader(), parameterTypes);
        return ConstructorFinder.findExact(clazz, resolved);
    }

    /**
     * 按精确参数类型查找构造函数，参数类型已解析为 Class。
     *
     * @param clazz 目标类
     * @param parameterTypes 精确参数类型
     * @return 已设置为可访问的构造函数
     */
    @NonNull
    static Constructor<?> findConstructorExactWithTypes(@NonNull Class<?> clazz,
                                                   @NonNull Class<?>... parameterTypes) {
        return ConstructorFinder.findExact(clazz,
            parameterTypes == null ? new Class<?>[0] : parameterTypes);
    }

    /**
     * 按精确参数类型查找构造函数，不存在时返回 null。
     *
     * @param clazz 目标类
     * @param parameterTypes 参数类型
     * @return 构造函数或 null
     */
    @Nullable
    public static Constructor<?> findConstructorExactIfExists(@NonNull Class<?> clazz,
                                                         @NonNull Object... parameterTypes) {
        try {
            return findConstructorExact(clazz, parameterTypes);
        } catch (ReflectException e) {
            return null;
        }
    }

    /**
     * 按精确参数类型在指定类名中查找构造函数，不存在时返回 null。
     *
     * @param className 目标类名
     * @param classLoader 类加载器
     * @param parameterTypes 参数类型
     * @return 构造函数或 null
     */
    @Nullable
    public static Constructor<?> findConstructorExactIfExists(@NonNull String className,
                                                         @Nullable ClassLoader classLoader,
                                                         @NonNull Object... parameterTypes) {
        try {
            return findConstructorExact(className, classLoader, parameterTypes);
        } catch (ReflectException e) {
            return null;
        }
    }

    /**
     * 按精确参数类型查找构造函数，失败时返回带原因的结果。
     *
     * @param clazz 目标类
     * @param parameterTypes 精确参数类型
     * @return 查找结果
     */
    @NonNull
    public static Lookup<Constructor<?>> lookupConstructor(@NonNull Class<?> clazz,
                                                           @NonNull Class<?>... parameterTypes) {
        try {
            return Lookup.of(findConstructorExactWithTypes(clazz, parameterTypes));
        } catch (ReflectException e) {
            return Lookup.failed(e);
        }
    }

    // ------------------------- constructor best -------------------------

    /**
     * 查找与给定参数类型最匹配的构造函数。
     *
     * @param clazz 目标类
     * @param parameterTypes 目标参数类型，元素可为 null
     * @return 已设置为可访问的构造函数
     */
    @NonNull
    public static Constructor<?> findConstructorBestMatch(@NonNull Class<?> clazz,
                                                          @NonNull Class<?>... parameterTypes) {
        return ConstructorFinder.findBest(clazz,
            parameterTypes == null ? new Class<?>[0] : parameterTypes);
    }

    /**
     * 根据实参值查找最匹配的构造函数。
     *
     * @param clazz 目标类
     * @param args 实参
     * @return 已设置为可访问的构造函数
     */
    @NonNull
    public static Constructor<?> findConstructorByArgs(@NonNull Class<?> clazz,
                                                       @NonNull Object... args) {
        return ConstructorFinder.findBest(clazz, typesOf(args));
    }

    // ------------------------------ calls -------------------------------

    /**
     * 调用实例方法，参数类型取自实参。
     *
     * @param target 目标对象
     * @param methodName 方法名
     * @param args 实参
     * @return 调用结果，void 方法返回 null
     */
    @Nullable
    public static Object callMethod(@NonNull Object target, @NonNull String methodName,
                                    @NonNull Object... args) {
        Method method = MethodFinder.findBest(target.getClass(), methodName, typesOf(args));
        return invoke(method, target, args);
    }

    /**
     * 使用显式参数类型调用实例方法。
     *
     * @param target 目标对象
     * @param methodName 方法名
     * @param parameterTypes 声明的参数类型，元素可为 null
     * @param args 实参
     * @return 调用结果
     */
    @Nullable
    public static Object callMethodWithTypes(@NonNull Object target, @NonNull String methodName,
                                             @NonNull Class<?>[] parameterTypes,
                                             @NonNull Object... args) {
        Method method = MethodFinder.findBest(target.getClass(), methodName, parameterTypes);
        return invoke(method, target, args);
    }

    /**
     * 调用静态方法，参数类型取自实参。
     *
     * @param clazz 声明类
     * @param methodName 方法名
     * @param args 实参
     * @return 调用结果
     */
    @Nullable
    public static Object callStaticMethod(@NonNull Class<?> clazz, @NonNull String methodName,
                                          @NonNull Object... args) {
        Method method = MethodFinder.findBest(clazz, methodName, typesOf(args));
        return invoke(method, null, args);
    }

    /**
     * 调用已解析的实例方法。
     *
     * @param method 已解析的方法
     * @param target 目标对象
     * @param args 实参
     * @return 调用结果，void 方法返回 null
     */
    @Nullable
    public static Object callMethod(@NonNull Method method, @Nullable Object target,
                                    @NonNull Object... args) {
        method.setAccessible(true);
        return invoke(method, target, args);
    }

    /**
     * 调用已解析的静态方法。
     *
     * @param method 已解析的静态方法
     * @param args 实参
     * @return 调用结果，void 方法返回 null
     */
    @Nullable
    public static Object callStaticMethod(@NonNull Method method, @NonNull Object... args) {
        method.setAccessible(true);
        return invoke(method, null, args);
    }

    /**
     * 使用显式参数类型调用静态方法。
     *
     * @param clazz 声明类
     * @param methodName 方法名
     * @param parameterTypes 声明的参数类型
     * @param args 实参
     * @return 调用结果
     */
    @Nullable
    public static Object callStaticMethodWithTypes(@NonNull Class<?> clazz,
                                                   @NonNull String methodName,
                                                   @NonNull Class<?>[] parameterTypes,
                                                   @NonNull Object... args) {
        Method method = MethodFinder.findBest(clazz, methodName, parameterTypes);
        return invoke(method, null, args);
    }

    @Nullable
    private static Object invoke(@NonNull Method method, @Nullable Object target,
                                 @NonNull Object[] args) {
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        if (target != null && isStatic) {
            throw new IllegalArgumentException("Method " + method.getName()
                + " is static, call with callStaticMethod instead.");
        }
        if (target == null && !isStatic) {
            throw new IllegalArgumentException("Method " + method.getName()
                + " is not static, call with callMethod instead.");
        }
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException e) {
            throw new ReflectException(ReflectException.Kind.OTHER, method.getName(),
                "Cannot access " + method.getName() + ": " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            return Throwables.throwIt(cause);
        }
    }

    // ----------------------------- fields -------------------------------

    /**
     * 读取实例字段。
     *
     * @param target 目标对象
     * @param fieldName 字段名
     * @return 字段值
     */
    @Nullable
    public static Object getObjectField(@NonNull Object target, @NonNull String fieldName) {
        Field field = FieldFinder.find(target.getClass(), fieldName);
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new ReflectException(ReflectException.Kind.OTHER, fieldName,
                "Cannot read field " + fieldName + ": " + e.getMessage(), e);
        }
    }

    /**
     * 写入实例字段。
     *
     * @param target 目标对象
     * @param fieldName 字段名
     * @param value 字段值
     */
    public static void setObjectField(@NonNull Object target, @NonNull String fieldName,
                                      @Nullable Object value) {
        Field field = FieldFinder.find(target.getClass(), fieldName);
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new ReflectException(ReflectException.Kind.OTHER, fieldName,
                "Cannot write field " + fieldName + ": " + e.getMessage(), e);
        }
    }

    /**
     * 读取已解析的实例字段。
     *
     * @param field 已解析的字段
     * @param target 目标对象
     * @return 字段值
     */
    @Nullable
    public static Object getObjectField(@NonNull Field field, @Nullable Object target) {
        Field resolved = requireInstanceField(field);
        try {
            return resolved.get(target);
        } catch (IllegalAccessException e) {
            throw new ReflectException(ReflectException.Kind.OTHER, resolved.getName(),
                "Cannot read field " + resolved.getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * 写入已解析的实例字段。
     *
     * @param field 已解析的字段
     * @param target 目标对象
     * @param value 字段值
     */
    public static void setObjectField(@NonNull Field field, @Nullable Object target,
                                      @Nullable Object value) {
        Field resolved = requireInstanceField(field);
        try {
            resolved.set(target, value);
        } catch (IllegalAccessException e) {
            throw new ReflectException(ReflectException.Kind.OTHER, resolved.getName(),
                "Cannot write field " + resolved.getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * 读取已解析的静态字段。
     *
     * @param field 已解析的字段
     * @return 字段值
     */
    @Nullable
    public static Object getStaticObjectField(@NonNull Field field) {
        Field resolved = requireStaticField(field);
        try {
            return resolved.get(null);
        } catch (IllegalAccessException e) {
            throw new ReflectException(ReflectException.Kind.OTHER, resolved.getName(),
                "Cannot read field " + resolved.getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * 写入已解析的静态字段。
     *
     * @param field 已解析的字段
     * @param value 字段值
     */
    public static void setStaticObjectField(@NonNull Field field, @Nullable Object value) {
        Field resolved = requireStaticField(field);
        try {
            resolved.set(null, value);
        } catch (IllegalAccessException e) {
            throw new ReflectException(ReflectException.Kind.OTHER, resolved.getName(),
                "Cannot write field " + resolved.getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * 读取静态字段。
     *
     * @param clazz 声明类
     * @param fieldName 字段名
     * @return 字段值
     */
    @Nullable
    public static Object getStaticObjectField(@NonNull Class<?> clazz,
                                              @NonNull String fieldName) {
        Field field = requireStatic(clazz, fieldName);
        try {
            return field.get(null);
        } catch (IllegalAccessException e) {
            throw new ReflectException(ReflectException.Kind.OTHER, fieldName,
                "Cannot read field " + fieldName + ": " + e.getMessage(), e);
        }
    }

    /**
     * 写入静态字段。
     *
     * @param clazz 声明类
     * @param fieldName 字段名
     * @param value 字段值
     */
    public static void setStaticObjectField(@NonNull Class<?> clazz, @NonNull String fieldName,
                                            @Nullable Object value) {
        Field field = requireStatic(clazz, fieldName);
        try {
            field.set(null, value);
        } catch (IllegalAccessException e) {
            throw new ReflectException(ReflectException.Kind.OTHER, fieldName,
                "Cannot write field " + fieldName + ": " + e.getMessage(), e);
        }
    }

    @NonNull
    private static Field requireInstanceField(@NonNull Field field) {
        if (Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException("Expected instance field, but " + field + " is static.");
        }
        field.setAccessible(true);
        return field;
    }

    @NonNull
    private static Field requireStaticField(@NonNull Field field) {
        if (!Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException("Expected static field, but " + field + " is an instance field.");
        }
        field.setAccessible(true);
        return field;
    }

    @NonNull
    private static Field requireStatic(@NonNull Class<?> clazz, @NonNull String fieldName) {
        Field field = FieldFinder.find(clazz, fieldName);
        if (!Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException("Expected static field, but " + clazz.getName()
                + "#" + fieldName + " is an instance field.");
        }
        return field;
    }

    /**
     * 读取实例字段，字段不存在时返回 null。
     *
     * @param target 目标对象
     * @param fieldName 字段名
     * @return 字段值或 null
     */
    @Nullable
    public static Object getObjectFieldIfExists(@NonNull Object target,
                                                @NonNull String fieldName) {
        Field field = FieldFinder.findOrNull(target.getClass(), fieldName);
        if (field == null) return null;
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 读取静态字段，字段不存在或不是静态字段时返回 null。
     *
     * @param clazz 声明类
     * @param fieldName 字段名
     * @return 字段值或 null
     */
    @Nullable
    public static Object getStaticObjectFieldIfExists(@NonNull Class<?> clazz,
                                                      @NonNull String fieldName) {
        Field field = FieldFinder.findOrNull(clazz, fieldName);
        if (field == null || !Modifier.isStatic(field.getModifiers())) return null;
        try {
            return field.get(null);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 按类名读取静态字段，类或字段不存在时返回 null。
     *
     * @param className 类名
     * @param fieldName 字段名
     * @return 字段值或 null
     */
    @Nullable
    public static Object getStaticObjectFieldIfExists(@NonNull String className,
                                                      @NonNull String fieldName) {
        Class<?> clazz = findClassIfExists(className, null);
        return clazz == null ? null : getStaticObjectFieldIfExists(clazz, fieldName);
    }

    /**
     * 获取类中声明的全部方法，跳过桥接方法与合成方法。
     *
     * @param clazz 目标类
     * @param methodName 方法名过滤，为 null 时不过滤
     * @return 匹配的方法数组
     */
    @NonNull
    public static Method[] findAllMethod(@NonNull Class<?> clazz, @Nullable String methodName) {
        List<Method> methods = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isBridge() || method.isSynthetic()) continue;
            if (methodName != null && !method.getName().equals(methodName)) continue;
            methods.add(method);
        }
        return methods.toArray(new Method[0]);
    }

    /**
     * 获取类中声明的全部构造函数。
     *
     * @param clazz 目标类
     * @return 构造函数数组
     */
    @NonNull
    public static Constructor<?>[] findAllConstructor(@NonNull Class<?> clazz) {
        return clazz.getDeclaredConstructors();
    }

    // --------------------------- new instance ---------------------------

    /**
     * 使用最匹配的构造函数创建实例。
     *
     * @param clazz 目标类
     * @param args 构造参数
     * @return 新实例
     */
    @NonNull
    public static Object newInstance(@NonNull Class<?> clazz, @NonNull Object... args) {
        Constructor<?> constructor = ConstructorFinder.findBest(clazz, typesOf(args));
        return instantiate(constructor, args);
    }

    /**
     * 使用显式参数类型创建实例。
     *
     * @param clazz 目标类
     * @param parameterTypes 声明的参数类型
     * @param args 构造参数
     * @return 新实例
     */
    @NonNull
    public static Object newInstanceWithTypes(@NonNull Class<?> clazz,
                                              @NonNull Class<?>[] parameterTypes,
                                              @NonNull Object... args) {
        Constructor<?> constructor = ConstructorFinder.findBest(clazz, parameterTypes);
        return instantiate(constructor, args);
    }

    /**
     * 使用已解析的构造函数创建实例。
     *
     * @param constructor 已解析的构造函数
     * @param args 构造参数
     * @return 新实例
     */
    @NonNull
    public static Object newInstance(@NonNull Constructor<?> constructor,
                                     @NonNull Object... args) {
        constructor.setAccessible(true);
        return instantiate(constructor, args);
    }

    @NonNull
    private static Object instantiate(@NonNull Constructor<?> constructor,
                                      @NonNull Object[] args) {
        try {
            return constructor.newInstance(args);
        } catch (IllegalAccessException e) {
            throw new ReflectException(ReflectException.Kind.OTHER, constructor.getName(),
                "Cannot access constructor: " + e.getMessage(), e);
        } catch (InstantiationException e) {
            throw new ReflectException(ReflectException.Kind.OTHER, constructor.getName(),
                "Cannot instantiate " + constructor.getDeclaringClass().getName() + ": "
                    + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            return Throwables.throwIt(cause);
        }
    }

    // ------------------------- additional fields ------------------------

    /**
     * 设置附加实例字段。
     *
     * @param target 目标对象
     * @param key 字段键
     * @param value 字段值
     * @return 该键原有的值
     */
    @Nullable
    public static Object setAdditionalInstanceField(@NonNull Object target, @NonNull String key,
                                                    @Nullable Object value) {
        return AdditionalFields.set(target, key, value);
    }

    /**
     * 读取附加实例字段。
     *
     * @param target 目标对象
     * @param key 字段键
     * @return 字段值或 null
     */
    @Nullable
    public static Object getAdditionalInstanceField(@NonNull Object target, @NonNull String key) {
        return AdditionalFields.get(target, key);
    }

    /**
     * 移除附加实例字段。
     *
     * @param target 目标对象
     * @param key 字段键
     * @return 被移除的值或 null
     */
    @Nullable
    public static Object removeAdditionalInstanceField(@NonNull Object target,
                                                       @NonNull String key) {
        return AdditionalFields.remove(target, key);
    }

    /**
     * 设置附加静态字段，以声明类作为键。
     *
     * @param clazz 声明类
     * @param key 字段键
     * @param value 字段值
     * @return 该键原有的值
     */
    @Nullable
    public static Object setAdditionalStaticField(@NonNull Class<?> clazz, @NonNull String key,
                                                  @Nullable Object value) {
        return AdditionalFields.set(clazz, key, value);
    }

    /**
     * 读取附加静态字段。
     *
     * @param clazz 声明类
     * @param key 字段键
     * @return 字段值或 null
     */
    @Nullable
    public static Object getAdditionalStaticField(@NonNull Class<?> clazz, @NonNull String key) {
        return AdditionalFields.get(clazz, key);
    }

    /**
     * 移除附加静态字段。
     *
     * @param clazz 声明类
     * @param key 字段键
     * @return 被移除的值或 null
     */
    @Nullable
    public static Object removeAdditionalStaticField(@NonNull Class<?> clazz,
                                                     @NonNull String key) {
        return AdditionalFields.remove(clazz, key);
    }

    // ------------------------------ helper ------------------------------

    @NonNull
    private static Class<?>[] typesOf(@Nullable Object[] args) {
        if (args == null) return new Class<?>[0];
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] == null ? null : args[i].getClass();
        }
        return types;
    }
}
