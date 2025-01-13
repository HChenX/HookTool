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

 * Copyright (C) 2023-2024 HChenX
 */
package com.hchen.hooktool.tool;

import static com.hchen.hooktool.helper.ConvertHelper.arrayToClass;
import static com.hchen.hooktool.helper.TryHelper.createSingleMember;
import static com.hchen.hooktool.helper.TryHelper.run;
import static com.hchen.hooktool.hook.HookFactory.createHook;
import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.LogExpand.getTag;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logI;
import static com.hchen.hooktool.log.XposedLog.logW;
import static com.hchen.hooktool.tool.CoreTool.callMethod;
import static com.hchen.hooktool.tool.CoreTool.callStaticMethod;
import static com.hchen.hooktool.tool.CoreTool.findConstructor;
import static com.hchen.hooktool.tool.CoreTool.findMethod;

import com.hchen.hooktool.helper.TryHelper;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.tool.itool.IMemberFilter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 核心类
 *
 * @author 焕晨HChen
 */
final class CoreBase {
    private CoreBase() {
    }

    static SingleMember<Class<?>> baseFindClass(String name, ClassLoader classLoader) {
        return TryHelper.<Class<?>>createSingleMember(() ->
                XposedHelpers.findClass(name, classLoader)
        ).setErrMsg("Failed to find class, classLoader: " + classLoader);
    }

    static SingleMember<Method> baseFindMethod(SingleMember<Class<?>> clazz, String name, Object... objs) {
        return clazz.reportOrRun(member ->
                        createSingleMember(
                                () -> XposedHelpers.findMethodExact(member, name, arrayToClass(member.getClassLoader(), objs))
                        ).setErrMsg("Failed to find method!"),
                new SingleMember<>(null));
    }

    static Method[] baseFindAllMethod(SingleMember<Class<?>> clazz, String name) {
        return clazz.reportOrRun(member ->
                        createSingleMember(
                                () -> Arrays.stream(member.getDeclaredMethods())
                                        .filter(method -> name.equals(method.getName()))
                                        .toArray(Method[]::new)
                        ).setErrMsg("Failed to find all method!").or(new Method[0]),
                new Method[0]);
    }

    static SingleMember<Constructor<?>> baseFindConstructor(SingleMember<Class<?>> clazz, Object... objs) {
        return clazz.reportOrRun(member ->
                        createSingleMember(
                                () -> XposedHelpers.findConstructorExact(member, arrayToClass(member.getClassLoader(), objs))
                        ).setErrMsg("Failed to find constructor!"),
                new SingleMember<>(null));
    }

    static Constructor<?>[] baseFindAllConstructor(SingleMember<Class<?>> clazz) {
        return clazz.reportOrRun(member ->
                        createSingleMember(
                                member::getDeclaredConstructors
                        ).setErrMsg("Failed to find constructor!").or(new Constructor<?>[0]),
                new Constructor<?>[0]);
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
            logI(tag, "Success to hook: " + member.getNoReport());
            return unHook;
        }).orErrMag(null, "Failed to hook! \ndebug: " + debug);
    }

    static <T extends Member> XC_MethodHook.Unhook[] baseHookAll(List<T> members, IHook iHook) {
        return baseHookAll(members.toArray(new Member[0]), iHook);
    }

    static XC_MethodHook.Unhook[] baseHookAll(Member[] members, IHook iHook) {
        if (members == null) return new XC_MethodHook.Unhook[0];
        String tag = getTag();

        return Arrays.stream(members).map(member ->
                        run(() -> {
                                    XC_MethodHook.Unhook unhook = XposedBridge.hookMethod(member, createHook(tag, iHook));
                                    logI(tag, "Success to hook: " + member);
                                    return unhook;
                                }
                        ).orErrMag(null, "Failed to hook: " + member)
                )
                .filter(Objects::nonNull)
                .toArray(XC_MethodHook.Unhook[]::new);
    }

    static XC_MethodHook.Unhook baseFirstUnhook(XC_MethodHook.Unhook[] unhooks) {
        if (unhooks == null || unhooks.length == 0) return null;
        return unhooks[0];
    }

    static Method[] baseFilterMethod(SingleMember<Class<?>> clazz, IMemberFilter<Method> iMemberFilter) {
        return clazz.reportOrRun(member ->
                        createSingleMember(
                                () -> Arrays.stream(member.getDeclaredMethods())
                                        .filter(iMemberFilter::test)
                                        .toArray(Method[]::new)
                        ).setErrMsg("Failed to filter method!").or(new Method[0]),
                new Method[0]);
    }

    static Constructor<?>[] baseFilterConstructor(SingleMember<Class<?>> clazz, IMemberFilter<Constructor<?>> iMemberFilter) {
        return clazz.reportOrRun(member ->
                        createSingleMember(
                                () -> Arrays.stream(member.getDeclaredConstructors())
                                        .filter(iMemberFilter::test)
                                        .toArray(Constructor<?>[]::new)
                        ).setErrMsg("Failed to filter constructor!").or(new Constructor<?>[0]),
                new Constructor<?>[0]);
    }

    static Object baseCallMethod(Object instance, Object type, Object... objs) {
        if (type instanceof String name) {
            return run(() -> XposedHelpers.callMethod(instance, name, objs))
                    .orErrMag(null, "Failed to call method!");
        } else if (type instanceof Method method) {
            return run(() -> {
                method.setAccessible(true);
                return method.invoke(instance, objs);
            }).orErrMag(null, "Failed to call method!");
        }
        return null;
    }

    static Object baseCallSuperPrivateMethod(Object instance, String name, Object... objs) {
        return run(() -> {
            Method method = baseGetSuperPrivateMethod(instance.getClass(), name, objs);
            if (method == null) return null;

            return callMethod(instance, method, objs);
        }).orErrMag(null, "Failed to call super private method!");
    }

    static Object baseGetField(Object instance, Object type) {
        if (type instanceof String name) {
            return run(() -> XposedHelpers.getObjectField(instance, name))
                    .orErrMag(null, "Failed to get field!");
        } else if (type instanceof Field field) {
            return run(() -> {
                field.setAccessible(true);
                return field.get(instance);
            }).orErrMag(null, "Failed to get field!");
        }
        return null;
    }

    static boolean baseSetField(Object instance, Object type, Object value) {
        if (type instanceof String name) {
            return run(() -> {
                XposedHelpers.setObjectField(instance, name, value);
                return true;
            }).orErrMag(false, "Failed to set field!");
        } else if (type instanceof Field field) {
            return run(() -> {
                field.setAccessible(true);
                field.set(instance, value);
                return true;
            }).orErrMag(false, "Failed to set field!");
        }
        return false;
    }

    static Object baseNewInstance(SingleMember<Class<?>> clz, Object... objs) {
        return clz.reportOrRun(member ->
                        createSingleMember(
                                () -> XposedHelpers.newInstance(member, objs)
                        ).setErrMsg("Failed to create new instance!").or(null),
                null);
    }

    static Object baseCallStaticMethod(SingleMember<Class<?>> clz, Method method, String name, Object... objs) {
        if (clz != null)
            return clz.reportOrRun(member ->
                            createSingleMember(
                                    () -> XposedHelpers.callStaticMethod(member, name, objs)
                            ).setErrMsg("Failed to call static method!").or(null),
                    null);
        else
            return run(() -> {
                method.setAccessible(true);
                return method.invoke(null, objs);
            }).orErrMag(null, "Failed to call static method!");
    }

    static Object baseCallSuperStaticPrivateMethod(SingleMember<Class<?>> clz, String name, Object... objs) {
        return clz.reportOrRun(member ->
                        createSingleMember(() -> {
                                    Method method = baseGetSuperPrivateMethod(member, name, objs);
                                    if (method == null) return null;
                                    return callStaticMethod(method, objs);
                                }
                        ).setErrMsg("Failed to call super private static field!").or(null),
                null);
    }

    static Object baseGetStaticField(SingleMember<Class<?>> clz, Field field, String name) {
        if (clz != null)
            return clz.reportOrRun(member ->
                            createSingleMember(
                                    () -> XposedHelpers.getStaticObjectField(member, name)
                            ).setErrMsg("Failed to get static field!").or(null),
                    null);
        else
            return run(() -> {
                field.setAccessible(true);
                return field.get(null);
            }).orErrMag(null, "Failed to get static field!");
    }

    static boolean baseSetStaticField(SingleMember<Class<?>> clz, Field field, String name, Object value) {
        if (clz != null)
            return clz.reportOrRun(member ->
                            createSingleMember(() -> {
                                XposedHelpers.setStaticObjectField(member, name, value);
                                return true;
                            }).setErrMsg("Failed to set static field!").or(false),
                    false);
        else
            return run(() -> {
                field.setAccessible(true);
                field.set(null, value);
                return true;
            }).orErrMag(false, "Failed to set static field!");
    }

    static Object baseSetAdditionalStaticField(SingleMember<Class<?>> clz, String key, Object value) {
        return clz.reportOrRun(member ->
                        createSingleMember(
                                () -> XposedHelpers.setAdditionalStaticField(member, key, value)
                        ).setErrMsg("Failed to set static additional instance!").or(null),
                null);
    }

    static Object baseGetAdditionalStaticField(SingleMember<Class<?>> clz, String key) {
        return clz.reportOrRun(member ->
                        createSingleMember(
                                () -> XposedHelpers.getAdditionalStaticField(member, key)
                        ).setErrMsg("Failed to get static additional instance!").or(null),
                null);
    }

    static Object baseRemoveAdditionalStaticField(SingleMember<Class<?>> clz, String key) {
        return clz.reportOrRun(member ->
                        createSingleMember(
                                () -> XposedHelpers.removeAdditionalStaticField(member, key)
                        ).setErrMsg("Failed to remove static additional instance!").or(null),
                null);
    }

    private static Method baseGetSuperPrivateMethod(Class<?> clz, String name, Object... objs) {
        return run(() -> {
            Method method = null;
            Class<?> clazz = clz;
            Class<?>[] params = XposedHelpers.getParameterTypes(objs);
            HashSet<Class<?>> paramsSet = new HashSet<>(Arrays.asList(params));
            superWhile:
            do {
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().equals(name) && paramsSet.containsAll(Arrays.asList(m.getParameterTypes()))) {
                        method = m;
                        break superWhile;
                    }
                }
            } while ((clazz = clazz.getSuperclass()) != null);

            return method;
        }).orErrMag(null, "Failed to get super private method!");
    }
}
