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
package com.hchen.hooktool.tool;

import static com.hchen.hooktool.helper.TryHelper.run;
import static com.hchen.hooktool.log.LogExpand.getTag;
import static com.hchen.hooktool.tool.CoreBase.baseCallMethod;
import static com.hchen.hooktool.tool.CoreBase.baseCallStaticMethod;
import static com.hchen.hooktool.tool.CoreBase.baseCallSuperPrivateMethod;
import static com.hchen.hooktool.tool.CoreBase.baseCallSuperStaticPrivateMethod;
import static com.hchen.hooktool.tool.CoreBase.baseFilterConstructor;
import static com.hchen.hooktool.tool.CoreBase.baseFilterMethod;
import static com.hchen.hooktool.tool.CoreBase.baseFindAllConstructor;
import static com.hchen.hooktool.tool.CoreBase.baseFindAllMethod;
import static com.hchen.hooktool.tool.CoreBase.baseFindClass;
import static com.hchen.hooktool.tool.CoreBase.baseFindConstructor;
import static com.hchen.hooktool.tool.CoreBase.baseFindField;
import static com.hchen.hooktool.tool.CoreBase.baseFindMethod;
import static com.hchen.hooktool.tool.CoreBase.baseGetAdditionalStaticField;
import static com.hchen.hooktool.tool.CoreBase.baseGetField;
import static com.hchen.hooktool.tool.CoreBase.baseGetStaticField;
import static com.hchen.hooktool.tool.CoreBase.baseHook;
import static com.hchen.hooktool.tool.CoreBase.baseHookAll;
import static com.hchen.hooktool.tool.CoreBase.baseNewInstance;
import static com.hchen.hooktool.tool.CoreBase.baseRemoveAdditionalStaticField;
import static com.hchen.hooktool.tool.CoreBase.baseSetAdditionalStaticField;
import static com.hchen.hooktool.tool.CoreBase.baseSetField;
import static com.hchen.hooktool.tool.CoreBase.baseSetStaticField;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.log.LogExpand;
import com.hchen.hooktool.log.XposedLog;
import com.hchen.hooktool.tool.itool.IMemberFilter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 核心工具
 *
 * @author 焕晨HChen
 * @noinspection unused
 */
public class CoreTool extends XposedLog {
    public static ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

    //------------ 检查指定类是否存在 --------------
    public static boolean existsClass(String clazz) {
        return baseFindClass(clazz).isSuccess();
    }

    public static boolean existsClass(String clazz, ClassLoader classLoader) {
        return baseFindClass(clazz, classLoader).isSuccess();
    }

    // --------------- 查找类 ----------------
    @Nullable
    public static Class<?> findClass(String path) {
        return baseFindClass(path).get();
    }

    @Nullable
    public static Class<?> findClass(String path, ClassLoader classLoader) {
        return baseFindClass(path, classLoader).get();
    }

    @Nullable
    public static Class<?> findClassIfExists(String path) {
        if (existsClass(path))
            return baseFindClass(path).get();
        return null;
    }

    @Nullable
    public static Class<?> findClassIfExists(String path, ClassLoader classLoader) {
        if (existsClass(path, classLoader))
            return baseFindClass(path, classLoader).get();
        return null;
    }

    //------------ 检查指定方法是否存在 --------------
    public static boolean existsMethod(String path, String name, Object... objs) {
        return baseFindMethod(baseFindClass(path), name, objs).isSuccess();
    }

    public static boolean existsMethod(String path, ClassLoader classLoader, String name, Object... objs) {
        return baseFindMethod(baseFindClass(path, classLoader), name, objs).isSuccess();
    }

    public static boolean existsMethod(Class<?> clazz, String name, Object... objs) {
        return baseFindMethod(new SingleMember<>(clazz), name, objs).isSuccess();
    }

    public static boolean existsAnyMethod(String path, String name) {
        return existsAnyMethod(baseFindClass(path).getNotReport(), name);
    }

    public static boolean existsAnyMethod(String path, ClassLoader classLoader, String name) {
        return existsAnyMethod(baseFindClass(path, classLoader).getNotReport(), name);
    }

    public static boolean existsAnyMethod(Class<?> clazz, String name) {
        return run(() ->
            Arrays.stream(clazz.getDeclaredMethods())
                .anyMatch(method -> Objects.equals(method.getName(), name))
        ).or(false);
    }

    // ------------ 查找方法 --------------
    @Nullable
    public static Method findMethod(String path, String name, Object... objs) {
        return baseFindMethod(baseFindClass(path), name, objs).get();
    }

    @Nullable
    public static Method findMethod(String path, ClassLoader classLoader, String name, Object... objs) {
        return baseFindMethod(baseFindClass(path, classLoader), name, objs).get();
    }

    @Nullable
    public static Method findMethod(Class<?> clazz, String name, Object... objs) {
        return baseFindMethod(new SingleMember<>(clazz), name, objs).get();
    }

    @Nullable
    public static Method findMethodIfExists(String path, String name, Object... objs) {
        if (existsMethod(path, name, objs))
            return baseFindMethod(baseFindClass(path), name, objs).get();
        return null;
    }

    @Nullable
    public static Method findMethodIfExists(String path, ClassLoader classLoader, String name, Object... objs) {
        if (existsMethod(path, classLoader, name, objs))
            return baseFindMethod(baseFindClass(path, classLoader), name, objs).get();
        return null;
    }

    @Nullable
    public static Method findMethodIfExists(Class<?> clazz, String name, Object... objs) {
        if (existsMethod(clazz, name, objs))
            return baseFindMethod(new SingleMember<>(clazz), name, objs).get();
        return null;
    }

    public static Method[] findAllMethod(String path, String name) {
        return baseFindAllMethod(baseFindClass(path), name);
    }

    public static Method[] findAllMethod(String path, ClassLoader classLoader, String name) {
        return baseFindAllMethod(baseFindClass(path, classLoader), name);
    }

    public static Method[] findAllMethod(Class<?> clazz, String name) {
        return baseFindAllMethod(new SingleMember<>(clazz), name);
    }

    //------------ 检查指定构造函数是否存在 --------------
    public static boolean existsConstructor(String path, Object... objs) {
        return baseFindConstructor(baseFindClass(path), objs).isSuccess();
    }

    public static boolean existsConstructor(String path, ClassLoader classLoader, Object... objs) {
        return baseFindConstructor(baseFindClass(path, classLoader), objs).isSuccess();
    }

    public static boolean existsConstructor(Class<?> clazz, Object... objs) {
        return baseFindConstructor(new SingleMember<>(clazz), objs).isSuccess();
    }

    // --------- 查找构造函数 -----------
    @Nullable
    public static Constructor<?> findConstructor(String path, Object... objs) {
        return baseFindConstructor(baseFindClass(path), objs).get();
    }

    @Nullable
    public static Constructor<?> findConstructor(String path, ClassLoader classLoader, Object... objs) {
        return baseFindConstructor(baseFindClass(path, classLoader), objs).get();
    }

    @Nullable
    public static Constructor<?> findConstructor(Class<?> clazz, Object... objs) {
        return baseFindConstructor(new SingleMember<>(clazz), objs).get();
    }

    @Nullable
    public static Constructor<?> findConstructorIfExists(String path, Object... objs) {
        if (existsConstructor(path, objs))
            return baseFindConstructor(baseFindClass(path), objs).get();
        return null;
    }

    @Nullable
    public static Constructor<?> findConstructorIfExists(String path, ClassLoader classLoader, Object... objs) {
        if (existsConstructor(path, classLoader, objs))
            return baseFindConstructor(baseFindClass(path, classLoader), objs).get();
        return null;
    }

    @Nullable
    public static Constructor<?> findConstructorIfExists(Class<?> clazz, Object... objs) {
        if (existsConstructor(clazz, objs))
            return baseFindConstructor(new SingleMember<>(clazz), objs).get();
        return null;
    }

    public static Constructor<?>[] findAllConstructor(String path) {
        return baseFindAllConstructor(baseFindClass(path));
    }

    public static Constructor<?>[] findAllConstructor(String path, ClassLoader classLoader) {
        return baseFindAllConstructor(baseFindClass(path, classLoader));
    }

    public static Constructor<?>[] findAllConstructor(Class<?> clazz) {
        return baseFindAllConstructor(new SingleMember<>(clazz));
    }

    //------------ 检查指定字段是否存在 --------------
    public static boolean existsField(String clazz, String name) {
        return baseFindField(baseFindClass(clazz), name).isSuccess();
    }

    public static boolean existsField(String clazz, ClassLoader classLoader, String name) {
        return baseFindField(baseFindClass(clazz, classLoader), name).isSuccess();
    }

    public static boolean existsField(Class<?> clazz, String name) {
        return baseFindField(new SingleMember<>(clazz), name).isSuccess();
    }

    // --------- 查找字段 -----------
    @Nullable
    public static Field findField(String path, String name) {
        return baseFindField(baseFindClass(path), name).get();
    }

    @Nullable
    public static Field findField(String path, ClassLoader classLoader, String name) {
        return baseFindField(baseFindClass(path, classLoader), name).get();
    }

    @Nullable
    public static Field findField(Class<?> clazz, String name) {
        return baseFindField(new SingleMember<>(clazz), name).get();
    }

    @Nullable
    public static Field findFieldIfExists(String path, String name) {
        if (existsField(path, name))
            return baseFindField(baseFindClass(path), name).get();
        return null;
    }

    @Nullable
    public static Field findFieldIfExists(String path, ClassLoader classLoader, String name) {
        if (existsField(path, classLoader, name))
            return baseFindField(baseFindClass(path, classLoader), name).get();
        return null;
    }

    @Nullable
    public static Field findFieldIfExists(Class<?> clazz, String name) {
        if (existsField(clazz, name))
            return baseFindField(new SingleMember<>(clazz), name).get();
        return null;
    }

    // --------- 执行 hook -----------
    // --------- 普通方法 -------------
    @Nullable
    public static XC_MethodHook.Unhook hookMethod(String path, String method, Object... params) {
        return baseHook(baseFindClass(path), method, params);
    }

    @Nullable
    public static XC_MethodHook.Unhook hookMethod(String path, ClassLoader classLoader, String method, Object... params) {
        return baseHook(baseFindClass(path, classLoader), method, params);
    }

    @Nullable
    public static XC_MethodHook.Unhook hookMethod(Class<?> clazz, String method, Object... params) {
        return baseHook(new SingleMember<>(clazz), method, params);
    }

    @Nullable
    public static XC_MethodHook.Unhook hookMethodIfExists(String path, String method, Object... params) {
        if (existsMethod(path, method, params))
            return baseHook(baseFindClass(path), method, params);
        return null;
    }

    @Nullable
    public static XC_MethodHook.Unhook hookMethodIfExists(String path, ClassLoader classLoader, String method, Object... params) {
        if (existsMethod(path, classLoader, method, params))
            return baseHook(baseFindClass(path, classLoader), method, params);
        return null;
    }

    @Nullable
    public static XC_MethodHook.Unhook hookMethodIfExists(Class<?> clazz, String method, Object... params) {
        if (existsMethod(clazz, method, params))
            return baseHook(new SingleMember<>(clazz), method, params);
        return null;
    }

    public static XC_MethodHook.Unhook[] hookAllMethod(String path, String method, IHook iHook) {
        return baseHookAll(baseFindAllMethod(baseFindClass(path), method), iHook);
    }

    public static XC_MethodHook.Unhook[] hookAllMethod(String path, ClassLoader classLoader, String method, IHook iHook) {
        return baseHookAll(baseFindAllMethod(baseFindClass(path, classLoader), method), iHook);
    }

    public static XC_MethodHook.Unhook[] hookAllMethod(Class<?> clazz, String method, IHook iHook) {
        return baseHookAll(baseFindAllMethod(new SingleMember<>(clazz), method), iHook);
    }

    // --------- 构造函数 ------------
    @Nullable
    public static XC_MethodHook.Unhook hookConstructor(String path, Object... params) {
        return baseHook(baseFindClass(path), null, params);
    }

    @Nullable
    public static XC_MethodHook.Unhook hookConstructor(String path, ClassLoader classLoader, Object... params) {
        return baseHook(baseFindClass(path, classLoader), null, params);
    }

    @Nullable
    public static XC_MethodHook.Unhook hookConstructor(Class<?> clazz, Object... params) {
        return baseHook(new SingleMember<>(clazz), null, params);
    }

    @Nullable
    public static XC_MethodHook.Unhook hookConstructorIfExists(String path, Object... params) {
        if (existsConstructor(path, params))
            return baseHook(baseFindClass(path), null, params);
        return null;
    }

    @Nullable
    public static XC_MethodHook.Unhook hookConstructorIfExists(String path, ClassLoader classLoader, Object... params) {
        if (existsConstructor(path, classLoader, params))
            return baseHook(baseFindClass(path, classLoader), null, params);
        return null;
    }

    @Nullable
    public static XC_MethodHook.Unhook hookConstructorIfExists(Class<?> clazz, Object... params) {
        if (existsConstructor(clazz, params))
            return baseHook(new SingleMember<>(clazz), null, params);
        return null;
    }

    public static XC_MethodHook.Unhook[] hookAllConstructor(String path, IHook iHook) {
        return baseHookAll(baseFindAllConstructor(baseFindClass(path)), iHook);
    }

    public static XC_MethodHook.Unhook[] hookAllConstructor(String path, ClassLoader classLoader, IHook iHook) {
        return baseHookAll(baseFindAllConstructor(baseFindClass(path, classLoader)), iHook);
    }

    public static XC_MethodHook.Unhook[] hookAllConstructor(Class<?> clazz, IHook iHook) {
        return baseHookAll(baseFindAllConstructor(new SingleMember<>(clazz)), iHook);
    }

    // --------------- Member -----------
    public static XC_MethodHook.Unhook hook(Member member, IHook iHook) {
        return baseHookAll(new Member[]{member}, iHook)[0];
    }

    public static <T extends Member> XC_MethodHook.Unhook[] hookAll(T[] members, IHook iHook) {
        return baseHookAll(members, iHook);
    }

    // --------- 快捷方法 -----------
    public static IHook returnResult(final Object result) {
        return new IHook() {
            @Override
            public void before() {
                setResult(result);
            }
        };
    }

    public static IHook doNothing() {
        return new IHook() {
            @Override
            public void before() {
                setResult(null);
            }
        };
    }

    public static IHook setArgs(int index, Object value) {
        return new IHook() {
            @Override
            public void before() {
                setArgs(index, value);
            }
        };
    }

    // --------- 解除 hook ---------
    public static void unHook(Member member, XC_MethodHook xcMethodHook) {
        XposedBridge.unhookMethod(member, xcMethodHook);
    }

    // --------- 过滤方法 -----------
    public static Method[] filterMethod(String path, IMemberFilter<Method> iMemberFilter) {
        return baseFilterMethod(baseFindClass(path), iMemberFilter);
    }

    public static Method[] filterMethod(String path, ClassLoader classLoader, IMemberFilter<Method> iMemberFilter) {
        return baseFilterMethod(baseFindClass(path, classLoader), iMemberFilter);
    }

    public static Method[] filterMethod(Class<?> clazz, IMemberFilter<Method> iMemberFilter) {
        return baseFilterMethod(new SingleMember<>(clazz), iMemberFilter);
    }

    public static Constructor<?>[] filterConstructor(String path, IMemberFilter<Constructor<?>> iMemberFilter) {
        return baseFilterConstructor(baseFindClass(path), iMemberFilter);
    }

    public static Constructor<?>[] filterConstructor(String path, ClassLoader classLoader, IMemberFilter<Constructor<?>> iMemberFilter) {
        return baseFilterConstructor(baseFindClass(path, classLoader), iMemberFilter);
    }

    public static Constructor<?>[] filterConstructor(Class<?> clazz, IMemberFilter<Constructor<?>> iMemberFilter) {
        return baseFilterConstructor(new SingleMember<>(clazz), iMemberFilter);
    }

    // --------- 打印堆栈 ----------
    public static String getStackTrace(boolean autoLog) {
        String task = getStackTrace();
        if (autoLog) logI(getTag(), task);
        return task;
    }

    public static String getStackTrace() {
        return LogExpand.getStackTrace();
    }

    // --------- 耗时检查 -----------
    public static long timeConsumption(Runnable runnable) {
        return run(() -> {
            Instant start = Instant.now();
            runnable.run();
            Instant end = Instant.now();
            return Duration.between(start, end).toMillis();
        }).orErrorMsg(-1L, "Failed to check code time consumption!");
    }

    // ---------- NonNull ---------
    @NonNull
    public static Object getNonNull(Object value, Object def) {
        return Optional.ofNullable(value).orElse(def);
    }

    // ---------- 非静态 -----------
    public static Object callMethod(Object instance, String name, Object... objs) {
        return baseCallMethod(instance, name, objs);
    }

    public static Object callMethod(Object instance, Method method, Object... objs) {
        return baseCallMethod(instance, method, objs);
    }

    public static Object callSuperPrivateMethod(Object instance, String name, Object... objs) {
        return baseCallSuperPrivateMethod(instance, name, objs);
    }

    public static Object getField(Object instance, String name) {
        return baseGetField(instance, name);
    }

    public static Object getField(Object instance, Field field) {
        return baseGetField(instance, field);
    }

    public static boolean setField(Object instance, String name, Object value) {
        return baseSetField(instance, name, value);
    }

    public static boolean setField(Object instance, Field field, Object value) {
        return baseSetField(instance, field, value);
    }

    public static Object setAdditionalInstanceField(Object instance, String key, Object value) {
        return run(() -> XposedHelpers.setAdditionalInstanceField(instance, key, value))
            .orErrorMsg(null, "Failed to set additional instance!");
    }

    public static Object getAdditionalInstanceField(Object instance, String key) {
        return run(() -> XposedHelpers.getAdditionalInstanceField(instance, key))
            .orErrorMsg(null, "Failed to get additional instance!");
    }

    public static Object removeAdditionalInstanceField(Object instance, String key) {
        return run(() -> XposedHelpers.removeAdditionalInstanceField(instance, key))
            .orErrorMsg(null, "Failed to remove additional instance!");
    }

    // ---------- 静态 ------------
    @Nullable
    public static Object newInstance(Class<?> clazz, Object... objs) {
        return baseNewInstance(new SingleMember<>(clazz), objs);
    }

    @Nullable
    public static Object newInstance(String path, Object... objs) {
        return baseNewInstance(baseFindClass(path), objs);
    }

    @Nullable
    public static Object newInstance(String path, ClassLoader classLoader, Object... objs) {
        return baseNewInstance(baseFindClass(path, classLoader), objs);
    }

    public static Object callStaticMethod(Class<?> clazz, String name, Object... objs) {
        return baseCallStaticMethod(new SingleMember<>(clazz), name, objs);
    }

    public static Object callStaticMethod(String path, String name, Object... objs) {
        return baseCallStaticMethod(baseFindClass(path), name, objs);
    }

    public static Object callStaticMethod(String path, ClassLoader classLoader, String name, Object... objs) {
        return baseCallStaticMethod(baseFindClass(path, classLoader), name, objs);
    }

    public static Object callStaticMethod(Method method, Object... objs) {
        return baseCallStaticMethod(method, objs);
    }

    public static Object callSuperStaticPrivateMethod(String path, String name, Object... objs) {
        return baseCallSuperStaticPrivateMethod(baseFindClass(path), name, objs);
    }

    public static Object callSuperStaticPrivateMethod(String path, ClassLoader classLoader, String name, Object... objs) {
        return baseCallSuperStaticPrivateMethod(baseFindClass(path, classLoader), name, objs);
    }

    public static Object callSuperStaticPrivateMethod(Class<?> clazz, String name, Object... objs) {
        return baseCallSuperStaticPrivateMethod(new SingleMember<>(clazz), name, objs);
    }

    public static Object getStaticField(Class<?> clazz, String name) {
        return baseGetStaticField(new SingleMember<>(clazz), name);
    }

    public static Object getStaticField(String path, String name) {
        return baseGetStaticField(baseFindClass(path), name);
    }

    public static Object getStaticField(String path, ClassLoader classLoader, String name) {
        return baseGetStaticField(baseFindClass(path, classLoader), name);
    }

    public static Object getStaticField(Field field) {
        return baseGetStaticField(field);
    }

    public static boolean setStaticField(Class<?> clazz, String name, Object value) {
        return baseSetStaticField(new SingleMember<>(clazz), name, value);
    }

    public static boolean setStaticField(String path, String name, Object value) {
        return baseSetStaticField(baseFindClass(path), name, value);
    }

    public static boolean setStaticField(String path, ClassLoader classLoader, String name, Object value) {
        return baseSetStaticField(baseFindClass(path, classLoader), name, value);
    }

    public static boolean setStaticField(Field field, Object value) {
        return baseSetStaticField(field, value);
    }

    public static Object setAdditionalStaticField(Class<?> clazz, String key, Object value) {
        return baseSetAdditionalStaticField(new SingleMember<>(clazz), key, value);
    }

    public static Object setAdditionalStaticField(String path, String key, Object value) {
        return baseSetAdditionalStaticField(baseFindClass(path), key, value);
    }

    public static Object setAdditionalStaticField(String path, ClassLoader classLoader, String key, Object value) {
        return baseSetAdditionalStaticField(baseFindClass(path, classLoader), key, value);
    }

    public static Object getAdditionalStaticField(Class<?> clazz, String key) {
        return baseGetAdditionalStaticField(new SingleMember<>(clazz), key);
    }

    public static Object getAdditionalStaticField(String path, String key) {
        return baseGetAdditionalStaticField(baseFindClass(path), key);
    }

    public static Object getAdditionalStaticField(String path, ClassLoader classLoader, String key) {
        return baseGetAdditionalStaticField(baseFindClass(path, classLoader), key);
    }

    public static Object removeAdditionalStaticField(Class<?> clazz, String key) {
        return baseRemoveAdditionalStaticField(new SingleMember<>(clazz), key);
    }

    public static Object removeAdditionalStaticField(String path, String key) {
        return baseRemoveAdditionalStaticField(baseFindClass(path), key);
    }

    public static Object removeAdditionalStaticField(String path, ClassLoader classLoader, String key) {
        return baseRemoveAdditionalStaticField(baseFindClass(path, classLoader), key);
    }
}
