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

import com.hchen.hooktool.entry.ModuleData;
import com.hchen.hooktool.reflect.Reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;

/**
 * 框架能力接口。
 * <p>
 * 提供调用器获取与去优化能力，需要框架包装器已就绪。
 *
 * @author 焕晨HChen
 */
public final class HookInterop {
    private HookInterop() {
    }

    // ------------------------------ invoker ------------------------------

    /**
     * 获取方法调用器。
     *
     * @param method 目标方法
     * @return 方法调用器
     */
    @NonNull
    public static XposedInterface.Invoker<?, Method> getMethodInvoker(@NonNull Method method) {
        return ModuleData.getWrapper().getInvoker(method);
    }

    /**
     * 按名称获取方法调用器。
     *
     * @param clazz 声明类
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     * @return 方法调用器
     */
    @NonNull
    public static XposedInterface.Invoker<?, Method> getMethodInvoker(@NonNull Class<?> clazz,
                                                                      @NonNull String methodName,
                                                                      @NonNull Object... parameterTypes) {
        return getMethodInvoker(Reflect.findMethod(clazz, methodName, parameterTypes));
    }

    /**
     * 获取构造函数调用器。
     *
     * @param constructor 目标构造函数
     * @param <T> 实例类型
     * @return 构造函数调用器
     */
    @NonNull
    public static <T> XposedInterface.CtorInvoker<T> getConstructorInvoker(
        @NonNull Constructor<T> constructor) {
        return ModuleData.getWrapper().getInvoker(constructor);
    }

    // ---------------------------- deoptimize -----------------------------

    /**
     * 对指定可执行对象执行去优化。
     *
     * @param executable 目标可执行对象
     * @return 框架报告的执行结果
     */
    public static boolean deoptimize(@NonNull Executable executable) {
        return ModuleData.getWrapper().deoptimize(executable);
    }

    /**
     * 按名称对方法执行去优化。
     *
     * @param clazz 声明类
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     */
    public static void deoptimizeMethod(@NonNull Class<?> clazz, @NonNull String methodName,
                                        @NonNull Object... parameterTypes) {
        deoptimize(Reflect.findMethod(clazz, methodName, parameterTypes));
    }

    /**
     * 对类中声明的方法全部执行去优化。
     *
     * @param clazz 声明类
     */
    public static void deoptimizeAllMethod(@NonNull Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isBridge() || method.isSynthetic()) continue;
            deoptimize(method);
        }
    }

    /**
     * 按参数类型对构造函数执行去优化。
     *
     * @param clazz 声明类
     * @param parameterTypes 参数类型
     */
    public static void deoptimizeConstructor(@NonNull Class<?> clazz,
                                             @NonNull Object... parameterTypes) {
        deoptimize(Reflect.findConstructor(clazz, parameterTypes));
    }

    /**
     * 对类中声明的构造函数全部执行去优化。
     *
     * @param clazz 声明类
     */
    public static void deoptimizeAllConstructor(@NonNull Class<?> clazz) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            deoptimize(constructor);
        }
    }
}
