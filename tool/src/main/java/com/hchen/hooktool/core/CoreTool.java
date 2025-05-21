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
package com.hchen.hooktool.core;

import static com.hchen.hooktool.helper.TryHelper.doTry;
import static com.hchen.hooktool.log.LogExpand.getTag;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.HCData;
import com.hchen.hooktool.callback.IAsyncPrefs;
import com.hchen.hooktool.callback.IMemberFilter;
import com.hchen.hooktool.callback.IPrefsApply;
import com.hchen.hooktool.exception.HookException;
import com.hchen.hooktool.exception.MissingParameterException;
import com.hchen.hooktool.exception.UnexpectedException;
import com.hchen.hooktool.helper.ConstructorHelper;
import com.hchen.hooktool.helper.MethodHelper;
import com.hchen.hooktool.hook.HookFactory;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.log.AndroidLog;
import com.hchen.hooktool.log.LogExpand;
import com.hchen.hooktool.log.XposedLog;
import com.hchen.hooktool.utils.PrefsTool;
import com.hchen.hooktool.utils.ResInjectTool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 核心工具
 *
 * @author 焕晨HChen
 */
public class CoreTool extends XposedLog {
    // -------------------------- Class ------------------------------

    /**
     * 是否存在指定类
     *
     * @param classPath 类引用路径
     */
    public static boolean existsClass(@NonNull String classPath) {
        return existsClass(classPath, HCData.getClassLoader());
    }

    /**
     * 是否存在指定类
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     */
    public static boolean existsClass(@NonNull String classPath, ClassLoader classLoader) {
        return doTry(
            () -> Objects.nonNull(XposedHelpers.findClassIfExists(classPath, classLoader))
        ).orElse(false);
    }

    /**
     * 查找获取指定类
     *
     * @param classPath 类引用路径
     */
    public static Class<?> findClass(@NonNull String classPath) {
        return findClass(classPath, HCData.getClassLoader());
    }

    /**
     * 查找获取指定类
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     */
    public static Class<?> findClass(@NonNull String classPath, ClassLoader classLoader) {
        return XposedHelpers.findClass(classPath, classLoader);
    }

    /**
     * 查找获取指定类，不存在返回 null，不会抛错
     *
     * @param classPath 类引用路径
     */
    @Nullable
    public static Class<?> findClassIfExists(@NonNull String classPath) {
        return findClassIfExists(classPath, HCData.getClassLoader());
    }

    /**
     * 查找获取指定类，不存在返回 null，不会抛错
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     */
    @Nullable
    public static Class<?> findClassIfExists(@NonNull String classPath, ClassLoader classLoader) {
        return doTry(
            () -> XposedHelpers.findClassIfExists(classPath, classLoader)
        ).get();
    }

    // -------------------------- Method ------------------------------

    /**
     * 查找是否存在指定方法
     *
     * @param classPath  类引用路径
     * @param methodName 方法名
     * @param params     方法参数
     */
    public static boolean existsMethod(@NonNull String classPath, @NonNull String methodName, @NonNull Object... params) {
        return existsMethod(classPath, HCData.getClassLoader(), methodName, params);
    }

    /**
     * 查找是否存在指定方法
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param methodName  方法名
     * @param params      方法参数
     */
    public static boolean existsMethod(@NonNull String classPath, ClassLoader classLoader, @NonNull String methodName, @NonNull Object... params) {
        return doTry(
            () -> Objects.nonNull(XposedHelpers.findMethodExactIfExists(classPath, classLoader, methodName, params))
        ).orElse(false);
    }

    /**
     * 查找是否存在指定方法
     *
     * @param clazz      类
     * @param methodName 方法名
     * @param params     方法参数
     */
    public static boolean existsMethod(@NonNull Class<?> clazz, @NonNull String methodName, @NonNull Object... params) {
        return doTry(
            () -> Objects.nonNull(XposedHelpers.findMethodExactIfExists(clazz, methodName, params))
        ).orElse(false);
    }

    /**
     * 查找是否存任意指定方法名的方法
     *
     * @param classPath  类引用路径
     * @param methodName 方法名
     */
    public static boolean existsAnyMethod(@NonNull String classPath, @NonNull String methodName) {
        return existsAnyMethod(classPath, HCData.getClassLoader(), methodName);
    }

    /**
     * 查找是否存任意指定方法名的方法
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param methodName  方法名
     */
    public static boolean existsAnyMethod(@NonNull String classPath, ClassLoader classLoader, @NonNull String methodName) {
        return doTry(() ->
            existsAnyMethod(findClass(classPath, classLoader), methodName)
        ).orElse(false);
    }

    /**
     * 查找是否存任意指定方法名的方法
     *
     * @param clazz      类
     * @param methodName 方法名
     */
    public static boolean existsAnyMethod(@NonNull Class<?> clazz, @NonNull String methodName) {
        return doTry(() ->
            Arrays.stream(clazz.getDeclaredMethods())
                .anyMatch(method -> Objects.equals(method.getName(), methodName))
        ).orElse(false);
    }

    /**
     * 查找指定的方法
     *
     * @param classPath  类引用路径
     * @param methodName 方法名
     * @param params     方法参数
     */
    public static Method findMethod(@NonNull String classPath, @NonNull String methodName, @NonNull Object... params) {
        return findMethod(classPath, HCData.getClassLoader(), methodName, params);
    }

    /**
     * 查找指定的方法
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param methodName  方法名
     * @param params      方法参数
     */
    public static Method findMethod(@NonNull String classPath, ClassLoader classLoader, @NonNull String methodName, @NonNull Object... params) {
        return XposedHelpers.findMethodExact(classPath, classLoader, methodName, params);
    }

    /**
     * 查找指定的方法
     *
     * @param clazz      类
     * @param methodName 方法名
     * @param params     方法参数
     */
    public static Method findMethod(@NonNull Class<?> clazz, @NonNull String methodName, @NonNull Object... params) {
        return XposedHelpers.findMethodExact(clazz, methodName, params);
    }

    /**
     * 使用 Pro 工具查找指定的方法
     *
     * @param classPath 类引用路径
     */
    public static MethodHelper findMethodPro(@NonNull String classPath) {
        return findMethodPro(classPath, HCData.getClassLoader());
    }

    /**
     * 使用 Pro 工具查找指定的方法
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     */
    public static MethodHelper findMethodPro(@NonNull String classPath, ClassLoader classLoader) {
        return findMethodPro(findClass(classPath, classLoader));
    }

    /**
     * 使用 Pro 工具查找指定的方法
     *
     * @param clazz 类
     */
    public static MethodHelper findMethodPro(@NonNull Class<?> clazz) {
        return new MethodHelper(clazz);
    }

    /**
     * 查找指定的方法，不存在则返回 null，不会抛错
     *
     * @param classPath  类引用路径
     * @param methodName 方法名
     * @param params     方法参数
     */
    @Nullable
    public static Method findMethodIfExists(@NonNull String classPath, @NonNull String methodName, @NonNull Object... params) {
        return findMethodIfExists(classPath, HCData.getClassLoader(), methodName, params);
    }

    /**
     * 查找指定的方法，不存在则返回 null，不会抛错
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param methodName  方法名
     * @param params      方法参数
     */
    @Nullable
    public static Method findMethodIfExists(@NonNull String classPath, ClassLoader classLoader, @NonNull String methodName, @NonNull Object... params) {
        return doTry(
            () -> XposedHelpers.findMethodExactIfExists(classPath, classLoader, methodName, params)
        ).get();
    }

    /**
     * 查找指定的方法，不存在则返回 null，不会抛错
     *
     * @param clazz      类
     * @param methodName 方法名
     * @param params     方法参数
     */
    @Nullable
    public static Method findMethodIfExists(@NonNull Class<?> clazz, @NonNull String methodName, @NonNull Object... params) {
        return doTry(
            () -> XposedHelpers.findMethodExactIfExists(clazz, methodName, params)
        ).get();
    }

    /**
     * 查找全部指定名称的方法
     *
     * @param classPath  类引用路径
     * @param methodName 方法名
     */
    public static Method[] findAllMethod(@NonNull String classPath, @NonNull String methodName) {
        return findAllMethod(classPath, HCData.getClassLoader(), methodName);
    }

    /**
     * 查找全部指定名称的方法
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param methodName  方法名
     */
    public static Method[] findAllMethod(@NonNull String classPath, ClassLoader classLoader, @NonNull String methodName) {
        return findAllMethod(findClass(classPath, classLoader), methodName);
    }

    /**
     * 查找全部指定名称的方法
     *
     * @param clazz      类
     * @param methodName 方法名
     */
    public static Method[] findAllMethod(@NonNull Class<?> clazz, @NonNull String methodName) {
        return Arrays.stream(clazz.getDeclaredMethods())
            .filter(method -> Objects.equals(method.getName(), methodName))
            .toArray(Method[]::new);
    }

    // -------------------------- Constructor ------------------------------

    /**
     * 是否存在指定的构造函数
     *
     * @param classPath 类引用路径
     * @param params    构造函数参数
     */
    public static boolean existsConstructor(@NonNull String classPath, @NonNull Object... params) {
        return existsConstructor(classPath, HCData.getClassLoader(), params);
    }

    /**
     * 是否存在指定的构造函数
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param params      构造函数参数
     */
    public static boolean existsConstructor(@NonNull String classPath, ClassLoader classLoader, @NonNull Object... params) {
        return doTry(
            () -> Objects.nonNull(XposedHelpers.findConstructorExactIfExists(classPath, classLoader, params))
        ).orElse(false);
    }

    /**
     * 是否存在指定的构造函数
     *
     * @param clazz  类
     * @param params 构造函数参数
     */
    public static boolean existsConstructor(@NonNull Class<?> clazz, @NonNull Object... params) {
        return doTry(
            () -> Objects.nonNull(XposedHelpers.findConstructorExactIfExists(clazz, params))
        ).orElse(false);
    }

    /**
     * 查找指定类的构造函数
     *
     * @param classPath 类引用路径
     * @param params    构造函数参数
     */
    public static Constructor<?> findConstructor(@NonNull String classPath, @NonNull Object... params) {
        return findConstructor(classPath, HCData.getClassLoader(), params);
    }

    /**
     * 查找指定类的构造函数
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param params      构造函数参数
     */
    public static Constructor<?> findConstructor(@NonNull String classPath, ClassLoader classLoader, @NonNull Object... params) {
        return XposedHelpers.findConstructorExact(classPath, classLoader, params);
    }

    /**
     * 查找指定类的构造函数
     *
     * @param clazz  类
     * @param params 构造函数参数
     */
    public static Constructor<?> findConstructor(@NonNull Class<?> clazz, @NonNull Object... params) {
        return XposedHelpers.findConstructorExact(clazz, params);
    }

    /**
     * 使用 Pro 工具查找指定的构造函数
     *
     * @param classPath 类路径
     */
    public static ConstructorHelper findConstructorPro(@NonNull String classPath) {
        return findConstructorPro(classPath, HCData.getClassLoader());
    }


    /**
     * 使用 Pro 工具查找指定的构造函数
     *
     * @param classPath   类路径
     * @param classLoader 类加载器
     */
    public static ConstructorHelper findConstructorPro(@NonNull String classPath, ClassLoader classLoader) {
        return findConstructorPro(findClass(classPath, classLoader));
    }

    /**
     * 使用 Pro 工具查找指定的构造函数
     *
     * @param clazz 类
     */
    public static ConstructorHelper findConstructorPro(@NonNull Class<?> clazz) {
        return new ConstructorHelper(clazz);
    }

    /**
     * 查找指定类的构造函数，不存在则返回 null，不会抛错
     *
     * @param classPath 类引用路径
     * @param params    构造函数的参数列表
     */
    @Nullable
    public static Constructor<?> findConstructorIfExists(@NonNull String classPath, @NonNull Object... params) {
        return findConstructorIfExists(classPath, HCData.getClassLoader(), params);
    }

    /**
     * 查找指定类的构造函数，不存在则返回 null，不会抛错
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param params      构造函数的参数列表
     */
    @Nullable
    public static Constructor<?> findConstructorIfExists(@NonNull String classPath, ClassLoader classLoader, @NonNull Object... params) {
        return doTry(
            () -> XposedHelpers.findConstructorExactIfExists(classPath, classLoader, params)
        ).get();
    }

    /**
     * 查找指定类的构造函数，不存在则返回 null，不会抛错
     *
     * @param clazz  类
     * @param params 构造函数的参数列表
     */
    @Nullable
    public static Constructor<?> findConstructorIfExists(@NonNull Class<?> clazz, @NonNull Object... params) {
        return doTry(
            () -> XposedHelpers.findConstructorExactIfExists(clazz, params)
        ).get();
    }

    /**
     * 查找指定类的所有构造函数
     *
     * @param classPath 类引用路径
     */
    public static Constructor<?>[] findAllConstructor(@NonNull String classPath) {
        return findAllConstructor(classPath, HCData.getClassLoader());
    }

    /**
     * 查找指定类的所有构造函数
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     */
    public static Constructor<?>[] findAllConstructor(@NonNull String classPath, ClassLoader classLoader) {
        return findAllConstructor(findClass(classPath, classLoader));
    }

    /**
     * 查找指定类的所有构造函数
     *
     * @param clazz 类
     */
    public static Constructor<?>[] findAllConstructor(@NonNull Class<?> clazz) {
        return clazz.getDeclaredConstructors();
    }

    // -------------------------- Field ------------------------------

    /**
     * 检查指定类的字段是否存在
     *
     * @param classPath 类引用路径
     * @param fieldName 字段名
     */
    public static boolean existsField(@NonNull String classPath, @NonNull String fieldName) {
        return existsField(classPath, HCData.getClassLoader(), fieldName);
    }

    /**
     * 检查指定类的字段是否存在
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param fieldName   字段名
     */
    public static boolean existsField(@NonNull String classPath, ClassLoader classLoader, @NonNull String fieldName) {
        return doTry(
            () -> existsField(findClass(classPath, classLoader), fieldName)
        ).orElse(false);
    }

    /**
     * 检查指定类的字段是否存在
     *
     * @param clazz     类
     * @param fieldName 字段名
     */
    public static boolean existsField(@NonNull Class<?> clazz, @NonNull String fieldName) {
        return doTry(
            () -> Objects.nonNull(XposedHelpers.findFieldIfExists(clazz, fieldName))
        ).orElse(false);
    }

    /**
     * 查找指定类的字段
     *
     * @param classPath 类引用路径
     * @param fieldName 字段名
     */
    public static Field findField(@NonNull String classPath, @NonNull String fieldName) {
        return findField(classPath, HCData.getClassLoader(), fieldName);
    }

    /**
     * 查找指定类的字段
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param fieldName   字段名
     */
    public static Field findField(@NonNull String classPath, ClassLoader classLoader, @NonNull String fieldName) {
        return findField(findClass(classPath, classLoader), fieldName);
    }

    /**
     * 查找指定类的字段
     *
     * @param clazz     类
     * @param fieldName 字段名
     */
    public static Field findField(@NonNull Class<?> clazz, @NonNull String fieldName) {
        return XposedHelpers.findField(clazz, fieldName);
    }

    /**
     * 查找指定类的字段，不存在则返回 null，不会抛错
     *
     * @param classPath 类引用路径
     * @param fieldName 字段名
     */
    @Nullable
    public static Field findFieldIfExists(@NonNull String classPath, @NonNull String fieldName) {
        return findFieldIfExists(classPath, HCData.getClassLoader(), fieldName);
    }

    /**
     * 查找指定类的字段，不存在则返回 null，不会抛错
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param fieldName   字段名
     */
    @Nullable
    public static Field findFieldIfExists(@NonNull String classPath, ClassLoader classLoader, @NonNull String fieldName) {
        return doTry(
            () -> findFieldIfExists(findClass(classPath, classLoader), fieldName)
        ).get();
    }

    /**
     * 查找指定类的字段，不存在则返回 null，不会抛错
     *
     * @param clazz     类
     * @param fieldName 字段名
     */
    @Nullable
    public static Field findFieldIfExists(@NonNull Class<?> clazz, @NonNull String fieldName) {
        return doTry(
            () -> XposedHelpers.findFieldIfExists(clazz, fieldName)
        ).get();
    }

    // -------------------------- Hook ------------------------------

    /**
     * Hook 指定类的方法
     *
     * @param classPath  类引用路径
     * @param methodName 方法名
     * @param params     方法参数列表
     */
    public static XC_MethodHook.Unhook hookMethod(@NonNull String classPath, @NonNull String methodName, @NonNull Object... params) {
        return hookMethod(classPath, HCData.getClassLoader(), methodName, params);
    }

    /**
     * Hook 指定类的方法
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param methodName  方法名
     * @param params      方法参数列表
     */
    public static XC_MethodHook.Unhook hookMethod(@NonNull String classPath, ClassLoader classLoader, @NonNull String methodName, @NonNull Object... params) {
        return hookMethod(findClass(classPath, classLoader), methodName, params);
    }

    /**
     * Hook 指定类的方法
     *
     * @param clazz      类
     * @param methodName 方法名
     * @param params     方法参数列表
     */
    public static XC_MethodHook.Unhook hookMethod(@NonNull Class<?> clazz, @NonNull String methodName, @NonNull Object... params) {
        return hook(findMethod(clazz, methodName, filterParams(params)), filterIHook(params));
    }

    /**
     * 如果存在则 Hook 指定类的方法
     *
     * @param classPath  类引用路径
     * @param methodName 方法名
     * @param params     方法参数列表
     */
    @Nullable
    public static XC_MethodHook.Unhook hookMethodIfExists(@NonNull String classPath, @NonNull String methodName, @NonNull Object... params) {
        return hookMethod(classPath, HCData.getClassLoader(), methodName, params);
    }

    /**
     * 如果存在则 Hook 指定类的方法
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param methodName  方法名
     * @param params      方法参数列表
     */
    @Nullable
    public static XC_MethodHook.Unhook hookMethodIfExists(@NonNull String classPath, ClassLoader classLoader, @NonNull String methodName, @NonNull Object... params) {
        return doTry(
            () -> hookMethodIfExists(findClass(classPath, classLoader), methodName, params)
        ).get();
    }

    /**
     * 如果存在则 Hook 指定类的方法
     *
     * @param clazz      类
     * @param methodName 方法名
     * @param params     方法参数列表
     */
    @Nullable
    public static XC_MethodHook.Unhook hookMethodIfExists(@NonNull Class<?> clazz, @NonNull String methodName, @NonNull Object... params) {
        return doTry(
            () -> hook(findMethod(clazz, methodName, filterParams(params)), filterIHook(params))
        ).get();
    }

    /**
     * Hook 指定类的所有方法
     *
     * @param classPath  类引用路径
     * @param methodName 方法名
     * @param iHook      Hook 回调接口
     */
    public static XC_MethodHook.Unhook[] hookAllMethod(@NonNull String classPath, @NonNull String methodName, @NonNull IHook iHook) {
        return hookAllMethod(classPath, HCData.getClassLoader(), methodName, iHook);
    }

    /**
     * Hook 指定类的所有方法
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param methodName  方法名
     * @param iHook       Hook 回调接口
     */
    public static XC_MethodHook.Unhook[] hookAllMethod(@NonNull String classPath, ClassLoader classLoader, @NonNull String methodName, @NonNull IHook iHook) {
        return hookAllMethod(findClass(classPath, classLoader), methodName, iHook);
    }

    /**
     * Hook 指定类的所有方法
     *
     * @param clazz      类
     * @param methodName 方法名
     * @param iHook      Hook 回调接口
     */
    public static XC_MethodHook.Unhook[] hookAllMethod(@NonNull Class<?> clazz, @NonNull String methodName, @NonNull IHook iHook) {
        return hookAll(findAllMethod(clazz, methodName), iHook);
    }

    /**
     * Hook 指定类的构造函数
     *
     * @param classPath 类引用路径
     * @param params    构造函数的参数列表
     */
    public static XC_MethodHook.Unhook hookConstructor(@NonNull String classPath, @NonNull Object... params) {
        return hookConstructor(classPath, HCData.getClassLoader(), params);
    }

    /**
     * Hook 指定类的构造函数
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param params      构造函数的参数列表
     */
    public static XC_MethodHook.Unhook hookConstructor(@NonNull String classPath, ClassLoader classLoader, @NonNull Object... params) {
        return hookConstructor(findClass(classPath, classLoader), params);
    }

    /**
     * Hook 指定类的构造函数
     *
     * @param clazz  类
     * @param params 构造函数的参数列表
     */
    public static XC_MethodHook.Unhook hookConstructor(@NonNull Class<?> clazz, @NonNull Object... params) {
        return hook(findConstructor(clazz, filterParams(params)), filterIHook(params));
    }

    /**
     * 如果存在则 Hook 指定类的构造函数
     *
     * @param classPath 类引用路径
     * @param params    构造函数的参数列表
     */
    @Nullable
    public static XC_MethodHook.Unhook hookConstructorIfExists(@NonNull String classPath, @NonNull Object... params) {
        return hookConstructorIfExists(classPath, HCData.getClassLoader(), params);
    }

    /**
     * 如果存在则 Hook 指定类的构造函数
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param params      构造函数的参数列表
     */
    @Nullable
    public static XC_MethodHook.Unhook hookConstructorIfExists(@NonNull String classPath, ClassLoader classLoader, @NonNull Object... params) {
        return doTry(
            () -> hookConstructorIfExists(findClass(classPath, classLoader), params)
        ).get();
    }

    /**
     * 如果存在则 Hook 指定类的构造函数
     *
     * @param clazz  类
     * @param params 构造函数的参数列表
     */
    @Nullable
    public static XC_MethodHook.Unhook hookConstructorIfExists(@NonNull Class<?> clazz, @NonNull Object... params) {
        return doTry(
            () -> hook(findConstructor(clazz, filterParams(params)), filterIHook(params))
        ).get();
    }

    /**
     * Hook 指定类的所有构造函数
     *
     * @param classPath 类引用路径
     * @param iHook     Hook 回调接口
     */
    public static XC_MethodHook.Unhook[] hookAllConstructor(@NonNull String classPath, @NonNull IHook iHook) {
        return hookAllConstructor(classPath, HCData.getClassLoader(), iHook);
    }

    /**
     * Hook 指定类的所有构造函数
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param iHook       Hook 回调接口
     */
    public static XC_MethodHook.Unhook[] hookAllConstructor(@NonNull String classPath, ClassLoader classLoader, @NonNull IHook iHook) {
        return hookAllConstructor(findClass(classPath, classLoader), iHook);
    }

    /**
     * Hook 指定类的所有构造函数
     *
     * @param clazz 类
     * @param iHook Hook 回调接口
     */
    public static XC_MethodHook.Unhook[] hookAllConstructor(@NonNull Class<?> clazz, @NonNull IHook iHook) {
        return hookAll(findAllConstructor(clazz), iHook);
    }

    /**
     * Hook 指定的方法或构造函数
     *
     * @param member 方法或构造函数对象
     * @param iHook  Hook 回调接口
     */
    public static XC_MethodHook.Unhook hook(@NonNull Member member, @NonNull IHook iHook) {
        return hookAll(new Member[]{member}, iHook)[0];
    }

    /**
     * Hook 指定的方法或构造函数数组
     *
     * @param members 方法或构造函数数组
     * @param iHook   Hook 回调接口
     */
    public static XC_MethodHook.Unhook[] hookAll(@NonNull Member[] members, @NonNull IHook iHook) {
        String tag = getTag();
        return Arrays.stream(members).map(new Function<Member, XC_MethodHook.Unhook>() {
            @Override
            public XC_MethodHook.Unhook apply(Member member) {
                try {
                    XC_MethodHook.Unhook unhook = XposedBridge.hookMethod(member, HookFactory.createHook(tag, iHook));
                    XposedLog.logI(tag, "Success to hook: " + member);
                    return unhook;
                } catch (Throwable e) {
                    throw new HookException("[CoreTool]: Failed to hook: " + member, e);
                }
            }
        }).toArray(XC_MethodHook.Unhook[]::new);
    }

    /**
     * 取消 Hook 指定的方法或构造函数
     *
     * @param member       方法或构造函数对象
     * @param xcMethodHook Hook 回调对象
     */
    public static void unHook(@NonNull Member member, @NonNull XC_MethodHook xcMethodHook) {
        XposedBridge.unhookMethod(member, xcMethodHook);
    }

    /**
     * 拦截方法执行并返回指定值
     *
     * @param result 要返回的结果
     */
    public static IHook returnResult(final Object result) {
        return new IHook() {
            @Override
            public void before() {
                setResult(result);
            }
        };
    }

    /**
     * 拦截方法执行
     */
    public static IHook doNothing() {
        return new IHook() {
            @Override
            public void before() {
                returnNull();
            }
        };
    }

    /**
     * 设置方法的指定参数为指定值
     *
     * @param index 参数索引
     * @param value 参数值
     */
    public static IHook setArg(int index, Object value) {
        return new IHook() {
            @Override
            public void before() {
                setArg(index, value);
            }
        };
    }

    private static Object[] filterParams(@NonNull Object... params) {
        if (params.length == 0 || !(params[params.length - 1] instanceof IHook))
            throw new MissingParameterException("[CoreTool]: Missing IHook parameter!");

        return Arrays.copyOf(params, params.length - 1);
    }

    private static IHook filterIHook(@NonNull Object... params) {
        if (params.length == 0 || !(params[params.length - 1] instanceof IHook iHook))
            throw new MissingParameterException("[CoreTool]: Missing IHook parameter!");

        return iHook;
    }

    // -------------------------- Non static ------------------------------

    /**
     * 调用指定对象的实例方法
     *
     * @param instance   对象实例
     * @param methodName 方法名
     * @param params     方法参数列表
     */
    public static Object callMethod(@NonNull Object instance, @NonNull String methodName, @NonNull Object... params) {
        return XposedHelpers.callMethod(instance, methodName, params);
    }

    /**
     * 调用指定对象的实例方法，不会抛错
     *
     * @param instance   对象实例
     * @param methodName 方法名
     * @param params     方法参数列表
     */
    public static Object callMethodIfExists(@NonNull Object instance, @NonNull String methodName, @NonNull Object... params) {
        return doTry(
            () -> XposedHelpers.callMethod(instance, methodName, params)
        ).get();
    }

    /**
     * 调用指定对象的实例方法
     * <p>
     * 允许指定方法参数，这将在存在多个同名方法的情况下有所帮助，特别是传递 null 参数时
     *
     * @param instance   对象实例
     * @param methodName 方法名
     * @param paramTypes 方法参数列表 (只能是 String(类完整引用) 或者 Class 类型)
     * @param params     方法参数列表
     */
    public static Object callMethod(@NonNull Object instance, @NonNull String methodName, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return XposedHelpers.callMethod(instance, methodName, getParamTypes(instance.getClass().getClassLoader(), paramTypes), params);
    }

    /**
     * 调用指定对象的实例方法，不会抛错
     * <p>
     * 允许指定方法参数，这将在存在多个同名方法的情况下有所帮助，特别是传递 null 参数时
     *
     * @param instance   对象实例
     * @param methodName 方法名
     * @param paramTypes 方法参数列表 (只能是 String(类完整引用) 或者 Class 类型)
     * @param params     方法参数列表
     */
    public static Object callMethodIfExists(@NonNull Object instance, @NonNull String methodName, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return doTry(
            () -> XposedHelpers.callMethod(instance, methodName, getParamTypes(instance.getClass().getClassLoader(), paramTypes), params)
        ).get();
    }

    /**
     * 调用指定对象的实例方法
     *
     * @param instance 对象实例
     * @param method   方法对象
     * @param params   方法参数列表
     * @throws UnexpectedException 如果方法调用失败
     */
    public static Object callMethod(@NonNull Object instance, @NonNull Method method, @NonNull Object... params) {
        try {
            method.setAccessible(true);
            return method.invoke(instance, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * 获取指定对象的字段值
     *
     * @param instance  对象实例
     * @param fieldName 字段名
     */
    public static Object getField(@NonNull Object instance, @NonNull String fieldName) {
        return XposedHelpers.getObjectField(instance, fieldName);
    }

    /**
     * 获取指定对象的字段值，不会抛错
     *
     * @param instance  对象实例
     * @param fieldName 字段名
     */
    public static Object getFieldIfExists(@NonNull Object instance, @NonNull String fieldName) {
        return doTry(
            () -> XposedHelpers.getObjectField(instance, fieldName)
        ).get();
    }

    /**
     * 获取指定对象的字段值
     *
     * @param instance 对象实例
     * @param field    字段对象
     * @throws UnexpectedException 如果字段访问失败
     */
    public static Object getField(@NonNull Object instance, @NonNull Field field) {
        try {
            field.setAccessible(true);
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * 设置指定对象的字段值
     *
     * @param instance  对象实例
     * @param fieldName 字段名
     * @param value     字段的新值
     */
    public static void setField(@NonNull Object instance, @NonNull String fieldName, Object value) {
        XposedHelpers.setObjectField(instance, fieldName, value);
    }

    /**
     * 设置指定对象的字段值，不会抛错
     *
     * @param instance  对象实例
     * @param fieldName 字段名
     * @param value     字段的新值
     */
    public static void setFieldIfExists(@NonNull Object instance, @NonNull String fieldName, Object value) {
        doTry(() -> {
            XposedHelpers.setObjectField(instance, fieldName, value);
            return null;
        });
    }

    /**
     * 设置指定对象的字段值
     *
     * @param instance 对象实例
     * @param field    字段对象
     * @param value    字段的新值
     * @throws UnexpectedException 如果字段访问失败
     */
    public static void setField(@NonNull Object instance, @NonNull Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * 设置指定对象的附加字段值
     *
     * @param instance 对象实例
     * @param key      字段键
     * @param value    字段的值
     */
    public static Object setAdditionalInstanceField(@NonNull Object instance, @NonNull String key, Object value) {
        return XposedHelpers.setAdditionalInstanceField(instance, key, value);
    }

    /**
     * 获取指定对象的附加字段值
     *
     * @param instance 对象实例
     * @param key      字段键
     */
    public static Object getAdditionalInstanceField(@NonNull Object instance, @NonNull String key) {
        return XposedHelpers.getAdditionalInstanceField(instance, key);
    }


    /**
     * 移除指定对象的附加字段
     *
     * @param instance 对象实例
     * @param key      字段键
     */
    public static Object removeAdditionalInstanceField(@NonNull Object instance, @NonNull String key) {
        return XposedHelpers.removeAdditionalInstanceField(instance, key);
    }

    // -------------------------- Static ------------------------------

    /**
     * 创建指定类的新实例
     *
     * @param classPath 类引用路径
     * @param params    构造函数的参数列表
     */
    public static Object newInstance(@NonNull String classPath, @NonNull Object... params) {
        return newInstance(classPath, HCData.getClassLoader(), params);
    }

    /**
     * 创建指定类的新实例
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param params      构造函数的参数列表
     */
    public static Object newInstance(@NonNull String classPath, ClassLoader classLoader, @NonNull Object... params) {
        return newInstance(findClass(classPath, classLoader), params);
    }

    /**
     * 创建指定类的新实例
     *
     * @param clazz  类
     * @param params 构造函数的参数列表
     */
    public static Object newInstance(@NonNull Class<?> clazz, @NonNull Object... params) {
        return XposedHelpers.newInstance(clazz, params);
    }

    /**
     * 创建指定类的新实例，不会抛错
     *
     * @param classPath 类引用路径
     * @param params    构造函数的参数列表
     */
    public static Object newInstanceIfExists(@NonNull String classPath, @NonNull Object... params) {
        return newInstanceIfExists(classPath, HCData.getClassLoader(), params);
    }

    /**
     * 创建指定类的新实例，不会抛错
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param params      构造函数的参数列表
     */
    public static Object newInstanceIfExists(@NonNull String classPath, ClassLoader classLoader, @NonNull Object... params) {
        return doTry(
            () -> newInstanceIfExists(findClass(classPath, classLoader), params)
        ).get();
    }

    /**
     * 创建指定类的新实例，不会抛错
     *
     * @param clazz  类
     * @param params 构造函数的参数列表
     */
    public static Object newInstanceIfExists(@NonNull Class<?> clazz, @NonNull Object... params) {
        return doTry(
            () -> XposedHelpers.newInstance(clazz, params)
        ).get();
    }

    /**
     * 创建指定类的新实例
     *
     * @param classPath  类引用路径
     * @param paramTypes 方法参数列表 (只能是 String(类完整引用) 或者 Class 类型)
     * @param params     构造函数的参数列表
     */
    public static Object newInstance(@NonNull String classPath, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return newInstance(classPath, HCData.getClassLoader(), paramTypes, params);
    }

    /**
     * 创建指定类的新实例
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param paramTypes  方法参数列表 (只能是 String(类完整引用) 或者 Class 类型)
     * @param params      构造函数的参数列表
     */
    public static Object newInstance(@NonNull String classPath, ClassLoader classLoader, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return newInstance(findClass(classPath, classLoader), paramTypes, params);
    }

    /**
     * 创建指定类的新实例
     *
     * @param clazz      类
     * @param paramTypes 方法参数列表 (只能是 String(类完整引用) 或者 Class 类型)
     * @param params     构造函数的参数列表
     */
    public static Object newInstance(@NonNull Class<?> clazz, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return XposedHelpers.newInstance(clazz, getParamTypes(clazz.getClassLoader(), paramTypes), params);
    }

    /**
     * 创建指定类的新实例，不会抛错
     *
     * @param classPath  类引用路径
     * @param paramTypes 方法参数列表 (只能是 String(类完整引用) 或者 Class 类型)
     * @param params     构造函数的参数列表
     */
    public static Object newInstanceIfExists(@NonNull String classPath, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return newInstanceIfExists(classPath, HCData.getClassLoader(), paramTypes, params);
    }

    /**
     * 创建指定类的新实例，不会抛错
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param paramTypes  方法参数列表 (只能是 String(类完整引用) 或者 Class 类型)
     * @param params      构造函数的参数列表
     */
    public static Object newInstanceIfExists(@NonNull String classPath, ClassLoader classLoader, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return doTry(
            () -> newInstanceIfExists(findClass(classPath, classLoader), paramTypes, params)
        ).get();
    }

    /**
     * 创建指定类的新实例，不会抛错
     *
     * @param clazz      类
     * @param paramTypes 方法参数列表 (只能是 String(类完整引用) 或者 Class 类型)
     * @param params     构造函数的参数列表
     */
    public static Object newInstanceIfExists(@NonNull Class<?> clazz, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return doTry(
            () -> XposedHelpers.newInstance(clazz, getParamTypes(clazz.getClassLoader(), paramTypes), params)
        ).get();
    }

    /**
     * 调用指定类的静态方法
     *
     * @param classPath  类引用路径
     * @param methodName 方法名
     * @param params     方法参数列表
     */
    public static Object callStaticMethod(@NonNull String classPath, @NonNull String methodName, @NonNull Object... params) {
        return callStaticMethod(classPath, HCData.getClassLoader(), methodName, params);
    }

    /**
     * 调用指定类的静态方法
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param methodName  方法名
     * @param params      方法参数列表
     */
    public static Object callStaticMethod(@NonNull String classPath, ClassLoader classLoader, @NonNull String methodName, @NonNull Object... params) {
        return callStaticMethod(findClass(classPath, classLoader), methodName, params);
    }

    /**
     * 调用指定类的静态方法
     *
     * @param clazz      类
     * @param methodName 方法名
     * @param params     方法参数列表
     */
    public static Object callStaticMethod(@NonNull Class<?> clazz, @NonNull String methodName, @NonNull Object... params) {
        return XposedHelpers.callStaticMethod(clazz, methodName, params);
    }

    /**
     * 调用指定类的静态方法，不会抛错
     *
     * @param classPath  类引用路径
     * @param methodName 方法名
     * @param params     方法参数列表
     */
    public static Object callStaticMethodIfExists(@NonNull String classPath, @NonNull String methodName, @NonNull Object... params) {
        return callStaticMethodIfExists(classPath, HCData.getClassLoader(), methodName, params);
    }

    /**
     * 调用指定类的静态方法，不会抛错
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param methodName  方法名
     * @param params      方法参数列表
     */
    public static Object callStaticMethodIfExists(@NonNull String classPath, ClassLoader classLoader, @NonNull String methodName, @NonNull Object... params) {
        return doTry(
            () -> callStaticMethodIfExists(findClass(classPath, classLoader), methodName, params)
        ).get();
    }

    /**
     * 调用指定类的静态方法，不会抛错
     *
     * @param clazz      类
     * @param methodName 方法名
     * @param params     方法参数列表
     */
    public static Object callStaticMethodIfExists(@NonNull Class<?> clazz, @NonNull String methodName, @NonNull Object... params) {
        return doTry(
            () -> XposedHelpers.callStaticMethod(clazz, methodName, params)
        ).get();
    }

    /**
     * 调用指定类的静态方法
     *
     * @param classPath  类引用路径
     * @param methodName 方法名
     * @param paramTypes 方法参数列表 (只能是 String(类完整引用) 或者 Class 类型)
     * @param params     方法参数列表
     */
    public static Object callStaticMethod(@NonNull String classPath, @NonNull String methodName, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return callStaticMethod(classPath, HCData.getClassLoader(), methodName, paramTypes, params);
    }

    /**
     * 调用指定类的静态方法
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param methodName  方法名
     * @param paramTypes  方法参数列表 (只能是 String(类完整引用) 或者 Class 类型)
     * @param params      方法参数列表
     */
    public static Object callStaticMethod(@NonNull String classPath, ClassLoader classLoader, @NonNull String methodName, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return callStaticMethod(findClass(classPath, classLoader), methodName, paramTypes, params);
    }

    /**
     * 调用指定类的静态方法
     *
     * @param clazz      类
     * @param methodName 方法名
     * @param paramTypes 方法参数列表 (只能是 String(类完整引用) 或者 Class 类型)
     * @param params     方法参数列表
     */
    public static Object callStaticMethod(@NonNull Class<?> clazz, @NonNull String methodName, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return XposedHelpers.callStaticMethod(clazz, methodName, getParamTypes(clazz.getClassLoader(), paramTypes), params);
    }

    /**
     * 调用指定类的静态方法，不会抛错
     *
     * @param classPath  类引用路径
     * @param methodName 方法名
     * @param paramTypes 方法参数列表 (只能是 String(类完整引用) 或者 Class 类型)
     * @param params     方法参数列表
     */
    public static Object callStaticMethodIfExists(@NonNull String classPath, @NonNull String methodName, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return callStaticMethodIfExists(classPath, HCData.getClassLoader(), methodName, paramTypes, params);
    }

    /**
     * 调用指定类的静态方法，不会抛错
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param methodName  方法名
     * @param paramTypes  方法参数列表 (只能是 String(类完整引用) 或者 Class 类型)
     * @param params      方法参数列表
     */
    public static Object callStaticMethodIfExists(@NonNull String classPath, ClassLoader classLoader, @NonNull String methodName, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return doTry(
            () -> callStaticMethodIfExists(findClass(classPath, classLoader), methodName, paramTypes, params)
        ).get();
    }

    /**
     * 调用指定类的静态方法，不会抛错
     *
     * @param clazz      类
     * @param methodName 方法名
     * @param paramTypes 方法参数列表 (只能是 String(类完整引用) 或者 Class 类型)
     * @param params     方法参数列表
     */
    public static Object callStaticMethodIfExists(@NonNull Class<?> clazz, @NonNull String methodName, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return doTry(
            () -> XposedHelpers.callStaticMethod(clazz, methodName, getParamTypes(clazz.getClassLoader(), paramTypes), params)
        ).get();
    }

    /**
     * 调用指定的静态方法
     *
     * @param method 方法对象
     * @param params 方法参数列表
     * @throws UnexpectedException 如果方法调用失败
     */
    public static Object callStaticMethod(@NonNull Method method, @NonNull Object... params) {
        try {
            method.setAccessible(true);
            return method.invoke(null, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * 获取指定类的静态字段值
     *
     * @param classPath 类引用路径
     * @param fieldName 字段名
     */
    public static Object getStaticField(@NonNull String classPath, @NonNull String fieldName) {
        return getStaticField(classPath, HCData.getClassLoader(), fieldName);
    }

    /**
     * 获取指定类的静态字段值
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param fieldName   字段名
     */
    public static Object getStaticField(@NonNull String classPath, ClassLoader classLoader, @NonNull String fieldName) {
        return getStaticField(findClass(classPath, classLoader), fieldName);
    }

    /**
     * 获取指定类的静态字段值
     *
     * @param clazz     类
     * @param fieldName 字段名
     */
    public static Object getStaticField(@NonNull Class<?> clazz, @NonNull String fieldName) {
        return XposedHelpers.getStaticObjectField(clazz, fieldName);
    }

    /**
     * 获取指定类的静态字段值，不会抛错
     *
     * @param classPath 类引用路径
     * @param fieldName 字段名
     */
    public static Object getStaticFieldIfExists(@NonNull String classPath, @NonNull String fieldName) {
        return getStaticFieldIfExists(classPath, HCData.getClassLoader(), fieldName);
    }

    /**
     * 获取指定类的静态字段值，不会抛错
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param fieldName   字段名
     */
    public static Object getStaticFieldIfExists(@NonNull String classPath, ClassLoader classLoader, @NonNull String fieldName) {
        return doTry(
            () -> getStaticFieldIfExists(findClass(classPath, classLoader), fieldName)
        ).get();
    }

    /**
     * 获取指定类的静态字段值，不会抛错
     *
     * @param clazz     类
     * @param fieldName 字段名
     */
    public static Object getStaticFieldIfExists(@NonNull Class<?> clazz, @NonNull String fieldName) {
        return doTry(
            () -> XposedHelpers.getStaticObjectField(clazz, fieldName)
        ).get();
    }

    /**
     * 获取指定的静态字段值
     *
     * @param field 字段对象
     * @throws UnexpectedException 如果字段访问失败
     */
    public static Object getStaticField(@NonNull Field field) {
        try {
            field.setAccessible(true);
            return field.get(null);
        } catch (IllegalAccessException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * 设置指定类的静态字段值
     *
     * @param classPath 类引用路径
     * @param fieldName 字段名
     * @param value     字段的新值
     */
    public static void setStaticField(@NonNull String classPath, @NonNull String fieldName, Object value) {
        setStaticField(classPath, HCData.getClassLoader(), fieldName, value);
    }

    /**
     * 设置指定类的静态字段值
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param fieldName   字段名
     * @param value       字段的新值
     */
    public static void setStaticField(@NonNull String classPath, ClassLoader classLoader, @NonNull String fieldName, Object value) {
        setStaticField(findClass(classPath, classLoader), fieldName, value);
    }

    /**
     * 设置指定类的静态字段值
     *
     * @param clazz     类
     * @param fieldName 字段名
     * @param value     字段的新值
     */
    public static void setStaticField(@NonNull Class<?> clazz, @NonNull String fieldName, Object value) {
        XposedHelpers.setStaticObjectField(clazz, fieldName, value);
    }

    /**
     * 设置指定类的静态字段值，不会抛错
     *
     * @param classPath 类引用路径
     * @param fieldName 字段名
     * @param value     字段的新值
     */
    public static void setStaticFieldIfExists(@NonNull String classPath, @NonNull String fieldName, Object value) {
        setStaticFieldIfExists(classPath, HCData.getClassLoader(), fieldName, value);
    }

    /**
     * 设置指定类的静态字段值，不会抛错
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param fieldName   字段名
     * @param value       字段的新值
     */
    public static void setStaticFieldIfExists(@NonNull String classPath, ClassLoader classLoader, @NonNull String fieldName, Object value) {
        doTry(() -> {
            setStaticFieldIfExists(findClass(classPath, classLoader), fieldName, value);
            return null;
        });
    }

    /**
     * 设置指定类的静态字段值，不会抛错
     *
     * @param clazz     类
     * @param fieldName 字段名
     * @param value     字段的新值
     */
    public static void setStaticFieldIfExists(@NonNull Class<?> clazz, @NonNull String fieldName, Object value) {
        doTry(() -> {
            XposedHelpers.setStaticObjectField(clazz, fieldName, value);
            return null;
        });
    }

    /**
     * 设置指定静态字段的值
     *
     * @param field 目标静态字段
     * @param value 字段的新值
     */
    public static void setStaticField(@NonNull Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(null, value);
        } catch (IllegalAccessException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * 设置指定类的附加静态字段值
     *
     * @param classPath 类引用路径
     * @param key       附加字段的键
     * @param value     字段的值
     */
    public static Object setAdditionalStaticField(@NonNull String classPath, @NonNull String key, Object value) {
        return setAdditionalStaticField(classPath, HCData.getClassLoader(), key, value);
    }

    /**
     * 设置指定类的附加静态字段值
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param key         附加字段的键
     * @param value       字段的值
     */
    public static Object setAdditionalStaticField(@NonNull String classPath, ClassLoader classLoader, @NonNull String key, Object value) {
        return setAdditionalStaticField(findClass(classPath, classLoader), key, value);
    }

    /**
     * 设置指定类的附加静态字段值
     *
     * @param clazz 类
     * @param key   附加字段的键
     * @param value 字段的值
     */
    public static Object setAdditionalStaticField(@NonNull Class<?> clazz, @NonNull String key, Object value) {
        return XposedHelpers.setAdditionalStaticField(clazz, key, value);
    }

    /**
     * 获取指定类的附加静态字段值
     *
     * @param classPath 类引用路径
     * @param key       附加字段的键
     */
    public static Object getAdditionalStaticField(@NonNull String classPath, @NonNull String key) {
        return getAdditionalStaticField(classPath, HCData.getClassLoader(), key);
    }

    /**
     * 获取指定类的附加静态字段值
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param key         附加字段的键
     */
    public static Object getAdditionalStaticField(@NonNull String classPath, ClassLoader classLoader, @NonNull String key) {
        return getAdditionalStaticField(findClass(classPath, classLoader), key);
    }

    /**
     * 获取指定类的附加静态字段值
     *
     * @param clazz 类
     * @param key   附加字段的键
     */
    public static Object getAdditionalStaticField(@NonNull Class<?> clazz, @NonNull String key) {
        return XposedHelpers.getAdditionalStaticField(clazz, key);
    }

    /**
     * 移除指定类的附加静态字段
     *
     * @param classPath 类引用路径
     * @param key       附加字段的键
     */
    public static Object removeAdditionalStaticField(@NonNull String classPath, @NonNull String key) {
        return removeAdditionalStaticField(classPath, HCData.getClassLoader(), key);
    }

    /**
     * 移除指定类的附加静态字段
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     * @param key         附加字段的键
     */
    public static Object removeAdditionalStaticField(@NonNull String classPath, ClassLoader classLoader, @NonNull String key) {
        return removeAdditionalStaticField(findClass(classPath, classLoader), key);
    }

    /**
     * 移除指定类的附加静态字段
     *
     * @param clazz 类
     * @param key   附加字段的键
     */
    public static Object removeAdditionalStaticField(@NonNull Class<?> clazz, @NonNull String key) {
        return XposedHelpers.removeAdditionalStaticField(clazz, key);
    }

    // -------------------------- Invoke ------------------------------

    /**
     * 调用原始方法
     *
     * @param method     目标方法
     * @param thisObject 调用方法的对象实例
     * @param params     方法参数
     * @throws InvocationTargetException 如果目标方法抛出异常
     * @throws IllegalAccessException    如果无法访问目标方法
     */
    public static Object invokeOriginalMethod(@NonNull Member method, Object thisObject, @NonNull Object... params)
        throws InvocationTargetException, IllegalAccessException {
        return XposedBridge.invokeOriginalMethod(method, thisObject, params);
    }

    // -------------------------- Chain ------------------------------

    /**
     * 构建一个链式工具对象
     *
     * @param classPath 类引用路径
     */
    public static ChainTool buildChain(@NonNull String classPath) {
        return ChainTool.buildChain(classPath);
    }

    /**
     * 构建一个链式工具对象
     *
     * @param classPath   类引用路径
     * @param classLoader 类加载器
     */
    public static ChainTool buildChain(@NonNull String classPath, ClassLoader classLoader) {
        return ChainTool.buildChain(classPath, classLoader);
    }

    /**
     * 构建一个链式工具对象
     *
     * @param clazz 类
     */
    public static ChainTool buildChain(@NonNull Class<?> clazz) {
        return ChainTool.buildChain(clazz);
    }

    // -------------------------- Filter ------------------------------

    /**
     * 过滤指定类的方法
     *
     * @param classPath     类引用路径
     * @param iMemberFilter 方法过滤器
     */
    public static Method[] filterMethod(@NonNull String classPath, @NonNull IMemberFilter<Method> iMemberFilter) {
        return filterMethod(classPath, HCData.getClassLoader(), iMemberFilter);
    }

    /**
     * 过滤指定类的方法
     *
     * @param classPath     类引用路径
     * @param classLoader   类加载器
     * @param iMemberFilter 方法过滤器
     */
    public static Method[] filterMethod(@NonNull String classPath, ClassLoader classLoader, @NonNull IMemberFilter<Method> iMemberFilter) {
        return filterMethod(findClass(classPath, classLoader), iMemberFilter);
    }

    /**
     * 过滤指定类的方法
     *
     * @param clazz         类
     * @param iMemberFilter 方法过滤器
     */
    public static Method[] filterMethod(@NonNull Class<?> clazz, @NonNull IMemberFilter<Method> iMemberFilter) {
        return Arrays.stream(clazz.getDeclaredMethods())
            .filter(iMemberFilter::test)
            .toArray(Method[]::new);
    }

    /**
     * 过滤指定类的构造函数
     *
     * @param classPath     类引用路径
     * @param iMemberFilter 构造函数过滤器
     */
    public static Constructor<?>[] filterConstructor(@NonNull String classPath, @NonNull IMemberFilter<Constructor<?>> iMemberFilter) {
        return filterConstructor(classPath, HCData.getClassLoader(), iMemberFilter);
    }

    /**
     * 过滤指定类的构造函数
     *
     * @param classPath     类引用路径
     * @param classLoader   类加载器
     * @param iMemberFilter 构造函数过滤器
     */
    public static Constructor<?>[] filterConstructor(@NonNull String classPath, ClassLoader classLoader, @NonNull IMemberFilter<Constructor<?>> iMemberFilter) {
        return filterConstructor(findClass(classPath, classLoader), iMemberFilter);
    }

    /**
     * 过滤指定类的构造函数
     *
     * @param clazz         类
     * @param iMemberFilter 构造函数过滤器
     */
    public static Constructor<?>[] filterConstructor(@NonNull Class<?> clazz, @NonNull IMemberFilter<Constructor<?>> iMemberFilter) {
        return Arrays.stream(clazz.getDeclaredConstructors())
            .filter(iMemberFilter::test)
            .toArray(Constructor[]::new);
    }

    // -------------------------- ResTool ------------------------------

    /**
     * 创建一个假的资源 ID
     *
     * @param resName 资源名称
     */
    public static int createFakeResId(@NonNull String resName) {
        return ResInjectTool.createFakeResId(resName);
    }

    /**
     * 创建一个假的资源 ID
     *
     * @param res 资源对象
     * @param id  资源 ID
     */
    public static int createFakeResId(@NonNull Resources res, int id) {
        return ResInjectTool.createFakeResId(res, id);
    }

    /**
     * 设置资源替换
     *
     * @param packageName      包名
     * @param type             资源类型
     * @param resName          资源名称
     * @param replacementResId 替换的资源 ID
     */
    public static void setResReplacement(@NonNull String packageName, @NonNull String type, @NonNull String resName, int replacementResId) {
        ResInjectTool.setResReplacement(packageName, type, resName, replacementResId);
    }

    /**
     * 设置密度资源替换
     *
     * @param packageName         包名
     * @param type                资源类型
     * @param resName             资源名称
     * @param replacementResValue 替换的密度值
     */
    public static void setDensityReplacement(@NonNull String packageName, @NonNull String type, @NonNull String resName, float replacementResValue) {
        ResInjectTool.setDensityReplacement(packageName, type, resName, replacementResValue);
    }

    /**
     * 设置对象资源替换
     *
     * @param packageName         包名
     * @param type                资源类型
     * @param resName             资源名称
     * @param replacementResValue 替换的对象值
     */
    public static void setObjectReplacement(@NonNull String packageName, @NonNull String type, @NonNull String resName, Object replacementResValue) {
        ResInjectTool.setObjectReplacement(packageName, type, resName, replacementResValue);
    }

    // -------------------------- Prefs ------------------------------

    /**
     * 获取一个偏好设置应用对象
     *
     * @param context 上下文对象
     */
    @NonNull
    public static IPrefsApply prefs(@NonNull Context context) {
        return PrefsTool.prefs(context);
    }

    /**
     * 获取一个偏好设置应用对象
     *
     * @param context   上下文对象
     * @param prefsName 偏好设置名称
     */
    @NonNull
    public static IPrefsApply prefs(@NonNull Context context, @NonNull String prefsName) {
        return PrefsTool.prefs(context, prefsName);
    }

    /**
     * 获取一个偏好设置应用对象
     */
    @NonNull
    public static IPrefsApply prefs() {
        return PrefsTool.prefs();
    }

    /**
     * 获取一个偏好设置应用对象
     *
     * @param prefsName 偏好设置名称
     */
    @NonNull
    public static IPrefsApply prefs(@NonNull String prefsName) {
        return PrefsTool.prefs(prefsName);
    }

    /**
     * 异步应用偏好设置
     *
     * @param asyncPrefs 异步偏好设置接口
     */
    public static void asyncPrefs(@NonNull IAsyncPrefs asyncPrefs) {
        PrefsTool.asyncPrefs(asyncPrefs);
    }

    /**
     * 异步应用偏好设置
     *
     * @param prefsName  偏好设置名称
     * @param asyncPrefs 异步偏好设置接口
     */
    public static void asyncPrefs(@NonNull String prefsName, @NonNull IAsyncPrefs asyncPrefs) {
        PrefsTool.asyncPrefs(prefsName, asyncPrefs);
    }

    // -------------------------- Other ------------------------------

    /**
     * 获取当前线程的堆栈跟踪信息
     *
     * @param print 是否打印堆栈跟踪信息
     */
    public static String getStackTrace(boolean print) {
        String task = getStackTrace();
        if (print)
            AndroidLog.logD(getTag(), task);
        return task;
    }

    /**
     * 获取当前线程的堆栈跟踪信息
     */
    public static String getStackTrace() {
        return LogExpand.getStackTrace();
    }

    /**
     * 计算指定任务的执行时间
     *
     * @param runnable 要执行的任务
     */
    public static long timeConsumption(@NonNull Runnable runnable) {
        return doTry(() -> {
            Instant start = Instant.now();
            runnable.run();
            Instant end = Instant.now();
            return Duration.between(start, end).toMillis();
        }).orElse(-1L);
    }

    /**
     * 将参数列表转换为类参数列表
     *
     * @param classLoader 类加载器
     * @param paramTypes  参数列表
     * @noinspection IfCanBeSwitch
     */
    public static Class<?>[] getParamTypes(ClassLoader classLoader, @NonNull Object... paramTypes) {
        List<Class<?>> classes = new ArrayList<>();
        for (Object type : paramTypes) {
            if (type == null)
                throw new NullPointerException("Param type must not is null: " + Arrays.toString(paramTypes));
            else if (type instanceof String strType)
                classes.add(findClass(strType, classLoader));
            else if (type instanceof Class<?> clazz)
                classes.add(clazz);
            else
                throw new UnexpectedException("Unknown param type! param type must either be specified as Class or String: " + Arrays.toString(paramTypes));
        }
        return classes.toArray(new Class[0]);
    }
}
