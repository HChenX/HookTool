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

 * Copyright (C) 2023-2024 HookTool Contributions
 */
package com.hchen.hooktool.tool;

import static com.hchen.hooktool.helper.ConvertHelper.arrayToClass;
import static com.hchen.hooktool.helper.TryHelper.createSingleMember;
import static com.hchen.hooktool.helper.TryHelper.run;
import static com.hchen.hooktool.hook.HookFactory.createHook;
import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.LogExpand.getTag;
import static com.hchen.hooktool.log.XposedLog.logD;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;
import static com.hchen.hooktool.tool.CoreTool.findConstructor;
import static com.hchen.hooktool.tool.CoreTool.findMethod;

import androidx.annotation.Nullable;

import com.hchen.hooktool.helper.TryHelper;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.tool.itool.IMemberFilter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 核心类
 *
 * @author 焕晨HChen
 */
public final class CoreBase {
    private CoreBase() {
    }

    static SingleMember<Class<?>> baseFindClass(String name, ClassLoader classLoader) {
        return TryHelper.<Class<?>>createSingleMember(() -> {
            Class<?> clazz = MemberCache.readClassCache(name, classLoader);
            if (clazz == null) {
                clazz = XposedHelpers.findClass(name, classLoader);
                MemberCache.writeClassCache(clazz);
            }
            return clazz;
        }).setErrMsg("Failed to find class!");
    }

    static SingleMember<Method> baseFindMethod(SingleMember<Class<?>> clazz, String name, Object... objs) {
        return clazz.reportOrRun(member ->
                        createSingleMember(
                                () -> XposedHelpers.findMethodExact(member, name, arrayToClass(member.getClassLoader(), objs))
                        ).setErrMsg("Failed to find method!"),
                new SingleMember<>(null));
    }

    static ArrayList<Method> baseFindAllMethod(SingleMember<Class<?>> clazz, String name) {
        return clazz.reportOrRun(member ->
                        createSingleMember(
                                () -> Arrays.stream(member.getDeclaredMethods())
                                        .filter(method -> name.equals(method.getName()))
                                        .collect(Collectors.toCollection(ArrayList::new))
                        ).setErrMsg("Failed to find all method!")
                                .or(new ArrayList<>()),
                new ArrayList<>());
    }

    static SingleMember<Constructor<?>> baseFindConstructor(SingleMember<Class<?>> clazz, Object... objs) {
        return clazz.reportOrRun(member ->
                        createSingleMember(
                                () -> XposedHelpers.findConstructorExact(member, arrayToClass(member.getClassLoader(), objs))
                        ).setErrMsg("Failed to find constructor!"),
                new SingleMember<>(null));
    }

    static ArrayList<Constructor<?>> baseFindAllConstructor(SingleMember<Class<?>> clazz) {
        return clazz.reportOrRun(member ->
                        createSingleMember(
                                () -> new ArrayList<>(Arrays.asList(member.getDeclaredConstructors()))
                        ).setErrMsg("Failed to find constructor!")
                                .or(new ArrayList<>()),
                new ArrayList<>());
    }

    static SingleMember<Field> baseFindField(SingleMember<Class<?>> clazz, String name) {
        return clazz.reportOrRun(member ->
                        createSingleMember(
                                () -> XposedHelpers.findField(member, name)
                        ).setErrMsg("Failed to find field!"),
                new SingleMember<>(null));
    }

    static XC_MethodHook.Unhook baseHook(SingleMember<Class<?>> clazz, String method, Object... params) {
        String tag = getTag();
        String debug = (method != null ? "METHOD" : "CONSTRUCTOR") + "#" + (clazz.getNoReport() == null ? "null" : clazz.getNoReport().getName())
                + "#" + method + "#" + Arrays.toString(params);
        if (params == null || params.length == 0 || !(params[params.length - 1] instanceof IHook iHook)) {
            logW(tag, "Hook params is null or length is 0 or last param not is IAction! \ndebug: " + debug + getStackTrace());
            return null;
        }

        if (clazz.getThrowable() != null) {
            logE(tag, "Failed to hook! \ndebug: " + debug, clazz.getThrowable());
            return null;
        }

        SingleMember<?> member;
        if (method != null)
            member = findMethod(clazz.getNoReport(), method, params);
        else
            member = findConstructor(clazz.getNoReport(), params);
        if (member.getThrowable() != null) {
            logE(tag, "Failed to hook! \ndebug: " + debug, member.getThrowable());
            return null;
        }

        return run(() -> {
            XC_MethodHook.Unhook unHook = XposedBridge.hookMethod(((SingleMember<Member>) member).getNoReport(), createHook(tag, iHook));
            logD(tag, "Success to hook: " + member.getNoReport());
            return unHook;
        }).orErrMag(null, "Failed to hook! \ndebug: " + debug);
    }

    static ArrayList<XC_MethodHook.Unhook> baseHookAll(Member[] members, IHook iHook) {
        if (members == null) return new ArrayList<>();
        String tag = getTag();

        return Arrays.stream(members).map(member ->
                        run(
                                () -> XposedBridge.hookMethod(member, createHook(tag, iHook))
                        ).orErrMag(null, "Failed to hook: " + member)
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    static XC_MethodHook.Unhook baseFirstUnhook(ArrayList<XC_MethodHook.Unhook> unhooks) {
        if (unhooks.isEmpty()) return null;
        return unhooks.get(0);
    }

    static ArrayList<Method> baseFilterMethod(SingleMember<Class<?>> clazz, IMemberFilter<Method> iMemberFilter) {
        return clazz.reportOrRun(member ->
                        createSingleMember(
                                () -> Arrays.stream(member.getDeclaredMethods())
                                        .filter(iMemberFilter::test)
                                        .collect(Collectors.toCollection(ArrayList::new))
                        ).setErrMsg("Failed to filter method!")
                                .or(new ArrayList<>()),
                new ArrayList<>());
    }

    static ArrayList<Constructor<?>> baseFilterConstructor(SingleMember<Class<?>> clazz, IMemberFilter<Constructor<?>> iMemberFilter) {
        return clazz.reportOrRun(member ->
                        createSingleMember(
                                () -> Arrays.stream(member.getDeclaredConstructors())
                                        .filter(iMemberFilter::test)
                                        .collect(Collectors.toCollection(ArrayList::new))
                        ).setErrMsg("Failed to filter constructor!")
                                .or(new ArrayList<>()),
                new ArrayList<>());
    }

    static Object baseNewInstance(SingleMember<Class<?>> clz, Object... objs) {
        return clz.reportOrRun(member ->
                        createSingleMember(
                                () -> XposedHelpers.newInstance(member, objs)
                        ).setErrMsg("Failed to create new instance!")
                                .or(null),
                null);
    }

    static Object baseCallStaticMethod(SingleMember<Class<?>> clz, String name, Object... objs) {
        return clz.reportOrRun(member ->
                        createSingleMember(
                                () -> XposedHelpers.callStaticMethod(member, name, objs)
                        ).setErrMsg("Failed to call static method!")
                                .or(null),
                null);
    }

    static Object baseGetStaticField(SingleMember<Class<?>> clz, String name) {
        return clz.reportOrRun(member ->
                        createSingleMember(
                                () -> XposedHelpers.getStaticObjectField(member, name)
                        ).setErrMsg("Failed to get static field!")
                                .or(null),
                null);
    }

    static boolean baseSetStaticField(SingleMember<Class<?>> clz, String name, Object value) {
        return clz.reportOrRun(member ->
                        createSingleMember(() -> {
                            XposedHelpers.setStaticObjectField(member, name, value);
                            return true;
                        }).setErrMsg("Failed to set static field!")
                                .or(false),
                false);
    }

    static Object baseSetAdditionalStaticField(SingleMember<Class<?>> clz, String key, Object value) {
        return clz.reportOrRun(member ->
                        createSingleMember(
                                () -> XposedHelpers.setAdditionalStaticField(member, key, value)
                        ).setErrMsg("Failed to set static additional instance!")
                                .or(null),
                null);
    }

    static Object baseGetAdditionalStaticField(SingleMember<Class<?>> clz, String key) {
        return clz.reportOrRun(member ->
                        createSingleMember(
                                () -> XposedHelpers.getAdditionalStaticField(member, key)
                        ).setErrMsg("Failed to get static additional instance!")
                                .or(null),
                null);
    }

    static Object baseRemoveAdditionalStaticField(SingleMember<Class<?>> clz, String key) {
        return clz.reportOrRun(member ->
                        createSingleMember(
                                () -> XposedHelpers.removeAdditionalStaticField(member, key)
                        ).setErrMsg("Failed to remove static additional instance!")
                                .or(null),
                null);
    }

    private final static class MemberCache {
        private static final HashMap<String, Class<?>> mClassMap = new HashMap<>();

        public static void writeClassCache(Class<?> clazz) {
            if (clazz != null) {
                ClassLoader classLoader = clazz.getClassLoader();
                if (classLoader == null) classLoader = CoreTool.systemClassLoader;
                String clazzId = classLoader + "#" + classLoader.hashCode() + "#" + clazz.getName();
                mClassMap.put(clazzId, clazz);
            }
        }

        @Nullable
        public static Class<?> readClassCache(String name, ClassLoader classLoader) {
            if (classLoader == null) classLoader = CoreTool.systemClassLoader;
            String clazzId = classLoader + "#" + classLoader.hashCode() + "#" + name;
            Class<?> cache = mClassMap.get(clazzId);
            if (cache == null) {
                classLoader = classLoader.getParent();
                clazzId = classLoader + "#" + classLoader.hashCode() + "#" + name;
                cache = mClassMap.get(clazzId);
            }
            return cache;
        }

        /*
        Xposed 有此实现，不重复实现。
        // private static final ConcurrentHashMap<String, Method> mMethodMap = new ConcurrentHashMap<>();
        // private static final ConcurrentHashMap<String, Field> mFieldMap = new ConcurrentHashMap<>();
        
        public void writeMethodCache(Method method) {
            if (method == null) return;
            Class<?> c = method.getDeclaringClass();
            String paramsId = "(" + Arrays.stream(method.getParameterTypes()).map(Class::getName).collect(Collectors.joining(", ")) + ")";
            method.setAccessible(true);
            mMethodMap.put(c.getName() + "#" + method.getName() + paramsId, method);
        }

        public void writeFieldCache(Field field) {
            if (field == null) return;
            field.setAccessible(true);
            mFieldMap.put(field.getDeclaringClass().getName() + field.getName(), field);
        }
        */
    }
}
