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
package com.hchen.hooktool.hook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.config.HookConfig;
import com.hchen.hooktool.entry.ModuleData;
import com.hchen.hooktool.log.AndroidLog;
import com.hchen.hooktool.log.LogExpand;
import com.hchen.hooktool.reflect.Reflect;
import com.hchen.hooktool.reflect.ReflectException;
import com.hchen.hooktool.reflect.Throwables;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import io.github.libxposed.api.XposedInterface;

/**
 * Hook 安装门面。
 * <p>
 * 参数列表的最后一个元素固定为 {@link AbsHook} 实例。
 *
 * @author 焕晨HChen
 */
public final class Hook {
    private Hook() {
    }

    // ------------------------------ install ------------------------------

    /**
     * 对已解析的可执行对象安装 Hook。
     *
     * @param executable 目标方法或构造函数
     * @param absHook Hook 实现
     * @return 已安装的 Hook 句柄
     */
    @NonNull
    public static XposedInterface.HookHandle install(@NonNull Executable executable,
                                                     @NonNull AbsHook absHook) {
        XposedInterface.HookHandle handle;
        try {
            handle = new HookBridge(
                ModuleData.getWrapper().hook(executable),
                executable.getDeclaringClass().getName(),
                Modifier.isStatic(executable.getModifiers())
            ).intercept(absHook);
        } catch (Throwable throwable) {
            return Throwables.throwIt(throwable);
        }
        logSuccess(executable);
        return handle;
    }

    /**
     * 对类的静态初始化器安装 Hook。
     *
     * @param clazz 目标类
     * @param absHook Hook 实现
     * @return 已安装的 Hook 句柄
     */
    @NonNull
    public static XposedInterface.HookHandle installClassInitializer(@NonNull Class<?> clazz,
                                                                     @NonNull AbsHook absHook) {
        XposedInterface.HookHandle handle;
        try {
            handle = new HookBridge(
                ModuleData.getWrapper().hookClassInitializer(clazz),
                clazz.getName(), true
            ).intercept(absHook);
        } catch (Throwable throwable) {
            return Throwables.throwIt(throwable);
        }
        logSuccess(clazz);
        return handle;
    }

    private static void logSuccess(Object target) {
        if (!HookConfig.isShowHookSuccessLog()) return;
        try {
            AndroidLog.logI(LogExpand.getTag(), "Success to hook: " + target);
        } catch (Throwable ignored) {
            // 日志失败不影响 Hook 安装
        }
    }

    // ------------------------------ methods ------------------------------

    /**
     * 对方法安装 Hook。
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param parameterTypesAndHook 参数类型，最后一个元素为 {@link AbsHook}
     * @return 已安装的 Hook 句柄
     */
    @NonNull
    public static XposedInterface.HookHandle hookMethod(@NonNull Class<?> clazz,
                                                        @NonNull String methodName,
                                                        @NonNull Object... parameterTypesAndHook) {
        AbsHook hook = extractHook(parameterTypesAndHook);
        Object[] tokens = dropLast(parameterTypesAndHook);
        return install(Reflect.findMethod(clazz, methodName, tokens), hook);
    }

    /**
     * 对指定类名中的方法安装 Hook。
     *
     * @param className 目标类名
     * @param classLoader 解析类名使用的类加载器
     * @param methodName 方法名
     * @param parameterTypesAndHook 参数类型，最后一个元素为 {@link AbsHook}
     * @return 已安装的 Hook 句柄
     */
    @NonNull
    public static XposedInterface.HookHandle hookMethod(@NonNull String className,
                                                        @Nullable ClassLoader classLoader,
                                                        @NonNull String methodName,
                                                        @NonNull Object... parameterTypesAndHook) {
        return hookMethod(Reflect.findClass(className, classLoader), methodName,
            parameterTypesAndHook);
    }

    /**
     * 对方法安装 Hook，目标不存在时返回 null。
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param parameterTypesAndHook 参数类型，最后一个元素为 {@link AbsHook}
     * @return Hook 句柄或 null
     */
    @Nullable
    public static XposedInterface.HookHandle hookMethodIfExists(@NonNull Class<?> clazz,
                                                                @NonNull String methodName,
                                                                @NonNull Object... parameterTypesAndHook) {
        try {
            return hookMethod(clazz, methodName, parameterTypesAndHook);
        } catch (ReflectException e) {
            return null;
        }
    }

    /**
     * 对指定类名中的方法安装 Hook，目标不存在时返回 null。
     *
     * @param className 目标类名
     * @param classLoader 解析类名使用的类加载器
     * @param methodName 方法名
     * @param parameterTypesAndHook 参数类型，最后一个元素为 {@link AbsHook}
     * @return Hook 句柄或 null
     */
    @Nullable
    public static XposedInterface.HookHandle hookMethodIfExists(@NonNull String className,
                                                                @Nullable ClassLoader classLoader,
                                                                @NonNull String methodName,
                                                                @NonNull Object... parameterTypesAndHook) {
        try {
            return hookMethod(className, classLoader, methodName, parameterTypesAndHook);
        } catch (ReflectException e) {
            return null;
        }
    }

    // --------------------------- constructors ----------------------------

    /**
     * 对构造函数安装 Hook。
     *
     * @param clazz 目标类
     * @param parameterTypesAndHook 参数类型，最后一个元素为 {@link AbsHook}
     * @return 已安装的 Hook 句柄
     */
    @NonNull
    public static XposedInterface.HookHandle hookConstructor(@NonNull Class<?> clazz,
                                                             @NonNull Object... parameterTypesAndHook) {
        AbsHook hook = extractHook(parameterTypesAndHook);
        Object[] tokens = dropLast(parameterTypesAndHook);
        return install(Reflect.findConstructor(clazz, tokens), hook);
    }

    /**
     * 对指定类名中的构造函数安装 Hook。
     *
     * @param className 目标类名
     * @param classLoader 解析类名使用的类加载器
     * @param parameterTypesAndHook 参数类型，最后一个元素为 {@link AbsHook}
     * @return 已安装的 Hook 句柄
     */
    @NonNull
    public static XposedInterface.HookHandle hookConstructor(@NonNull String className,
                                                             @Nullable ClassLoader classLoader,
                                                             @NonNull Object... parameterTypesAndHook) {
        return hookConstructor(Reflect.findClass(className, classLoader), parameterTypesAndHook);
    }

    /**
     * 对构造函数安装 Hook，目标不存在时返回 null。
     *
     * @param clazz 目标类
     * @param parameterTypesAndHook 参数类型，最后一个元素为 {@link AbsHook}
     * @return Hook 句柄或 null
     */
    @Nullable
    public static XposedInterface.HookHandle hookConstructorIfExists(@NonNull Class<?> clazz,
                                                                     @NonNull Object... parameterTypesAndHook) {
        try {
            return hookConstructor(clazz, parameterTypesAndHook);
        } catch (ReflectException e) {
            return null;
        }
    }

    /**
     * 对指定类名中的构造函数安装 Hook，目标不存在时返回 null。
     *
     * @param className 目标类名
     * @param classLoader 解析类名使用的类加载器
     * @param parameterTypesAndHook 参数类型，最后一个元素为 {@link AbsHook}
     * @return Hook 句柄或 null
     */
    @Nullable
    public static XposedInterface.HookHandle hookConstructorIfExists(@NonNull String className,
                                                                     @Nullable ClassLoader classLoader,
                                                                     @NonNull Object... parameterTypesAndHook) {
        try {
            return hookConstructor(className, classLoader, parameterTypesAndHook);
        } catch (ReflectException e) {
            return null;
        }
    }

    // ------------------------------- batch -------------------------------

    /**
     * 对同名方法全部安装 Hook。
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param absHook 所有重载共用的 Hook 实现
     * @return 已安装的 Hook 句柄数组
     */
    @NonNull
    public static XposedInterface.HookHandle[] hookAllMethod(@NonNull Class<?> clazz,
                                                             @NonNull String methodName,
                                                             @NonNull AbsHook absHook) {
        List<XposedInterface.HookHandle> handles = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isBridge() || method.isSynthetic()) continue;
            if (!method.getName().equals(methodName)) continue;
            handles.add(install(method, absHook));
        }
        return handles.toArray(new XposedInterface.HookHandle[0]);
    }

    /**
     * 对类中声明的构造函数全部安装 Hook。
     *
     * @param clazz 目标类
     * @param absHook 所有构造函数共用的 Hook 实现
     * @return 已安装的 Hook 句柄数组
     */
    @NonNull
    public static XposedInterface.HookHandle[] hookAllConstructor(@NonNull Class<?> clazz,
                                                                  @NonNull AbsHook absHook) {
        List<XposedInterface.HookHandle> handles = new ArrayList<>();
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            handles.add(install(constructor, absHook));
        }
        return handles.toArray(new XposedInterface.HookHandle[0]);
    }

    /**
     * 对类的静态初始化器安装 Hook。
     *
     * @param clazz 目标类
     * @param absHook Hook 实现
     * @return 已安装的 Hook 句柄
     */
    @NonNull
    public static XposedInterface.HookHandle hookClassInitializer(@NonNull Class<?> clazz,
                                                                  @NonNull AbsHook absHook) {
        return installClassInitializer(clazz, absHook);
    }

    // ----------------------------- factories -----------------------------

    /**
     * 创建跳过原方法并返回指定结果的 Hook。
     * <p>
     * 该 Hook 不会执行 {@code after} 阶段。
     *
     * @param result 返回给调用方的结果
     * @return Hook 实现
     */
    @NonNull
    public static AbsHook returnResult(@Nullable Object result) {
        return new AbsHook() {
            @Override
            public void before() {
                setResult(result);
            }
        };
    }

    /**
     * 创建跳过原方法并返回 null 的 Hook。
     *
     * @return Hook 实现
     */
    @NonNull
    public static AbsHook doNothing() {
        return returnResult(null);
    }

    /**
     * 创建在调用前替换指定参数值的 Hook。
     *
     * @param index 参数下标
     * @param value 替换后的值
     * @return Hook 实现
     */
    @NonNull
    public static AbsHook setArg(int index, @Nullable Object value) {
        return new AbsHook() {
            @Override
            public void before() {
                setArg(index, value);
            }
        };
    }

    // ------------------------------ helpers ------------------------------

    private static AbsHook extractHook(Object[] parameterTypesAndHook) {
        if (parameterTypesAndHook.length == 0
            || !(parameterTypesAndHook[parameterTypesAndHook.length - 1] instanceof AbsHook hook)) {
            throw new IllegalArgumentException("The last element of the parameter list must be an AbsHook instance.");
        }
        return hook;
    }

    private static Object[] dropLast(Object[] array) {
        Object[] result = new Object[array.length - 1];
        System.arraycopy(array, 0, result, 0, result.length);
        return result;
    }
}
