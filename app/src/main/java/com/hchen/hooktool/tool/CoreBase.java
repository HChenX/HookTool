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
import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.LogExpand.getTag;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logI;
import static com.hchen.hooktool.log.XposedLog.logW;
import static com.hchen.hooktool.tool.SingleMember.createSingleMember;

import androidx.annotation.Nullable;

import com.hchen.hooktool.HCData;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.log.LogExpand;
import com.hchen.hooktool.tool.itool.IMemberFilter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
    static SingleMember<Class<?>> baseFindClass(String name) {
        return baseFindClass(name, HCData.getClassLoader());
    }

    static SingleMember<Class<?>> baseFindClass(String name, ClassLoader classLoader) {
        return SingleMember.<Class<?>>createSingleMember(() ->
            XposedHelpers.findClass(name, classLoader)
        ).setErrorMsg("Failed to find class!! classLoader: " + classLoader);
    }

    static SingleMember<Method> baseFindMethod(SingleMember<Class<?>> clazz, String name, Object... objs) {
        return clazz.exec(new SingleMember<>(),
            member ->
                createSingleMember(
                    () -> {
                        Class<?>[] classes = arrayToClass(member.getClassLoader(), objs);
                        if (classes == null) return null;
                        return XposedHelpers.findMethodExact(member, name, classes);
                    }
                ).setErrorMsg("Failed to find method!")
        );
    }

    static Method[] baseFindAllMethod(SingleMember<Class<?>> clazz, String name) {
        return clazz.exec(new Method[0],
            member ->
                createSingleMember(
                    () -> Arrays.stream(member.getDeclaredMethods())
                        .filter(method -> name.equals(method.getName()))
                        .toArray(Method[]::new)
                ).setErrorMsg("Failed to find all method!")
                    .or(new Method[0])
        );
    }

    static SingleMember<Constructor<?>> baseFindConstructor(SingleMember<Class<?>> clazz, Object... objs) {
        return clazz.exec(new SingleMember<>(),
            member ->
                SingleMember.<Constructor<?>>createSingleMember(
                    () -> {
                        Class<?>[] classes = arrayToClass(member.getClassLoader(), objs);
                        if (classes == null) return null;
                        return XposedHelpers.findConstructorExact(member, classes);
                    }
                ).setErrorMsg("Failed to find constructor!")
        );
    }

    static Constructor<?>[] baseFindAllConstructor(SingleMember<Class<?>> clazz) {
        return clazz.exec(new Constructor[0],
            member ->
                createSingleMember(
                    member::getDeclaredConstructors
                ).setErrorMsg("Failed to find constructor!")
                    .or(new Constructor[0])
        );
    }

    static SingleMember<Field> baseFindField(SingleMember<Class<?>> clazz, String name) {
        return clazz.exec(new SingleMember<>(),
            member ->
                createSingleMember(
                    () -> XposedHelpers.findField(member, name)
                ).setErrorMsg("Failed to find field!")
        );
    }

    static XC_MethodHook.Unhook baseHook(SingleMember<Class<?>> clazz, String method, Object... params) {
        String tag = getTag();
        String debug = (method != null ? "METHOD" : "CONSTRUCTOR") + "#"
            + (clazz.getNotReport() == null ? "null" : clazz.getNotReport().getName())
            + "#" + method + "#" + Arrays.toString(params);

        if (params == null || params.length == 0 || !(params[params.length - 1] instanceof IHook iHook)) {
            logW(tag, "Hook params is null or length is 0 or last param not is IHook! \ndebug: " + debug, getStackTrace());
            return null;
        }

        if (clazz.getNotReport() == null) {
            logE(tag, "Failed to hook! Class is null! \ndebug: " + debug);
            return null;
        }
        if (clazz.getThrowable() != null) {
            logE(tag, "Failed to hook! \ndebug: " + debug, clazz.getThrowable());
            return null;
        }

        SingleMember<Member> member;
        if (method != null)
            member = (SingleMember<Member>) (SingleMember<?>) baseFindMethod(new SingleMember<>(clazz.getNotReport()), method, params);
        else
            member = (SingleMember<Member>) (SingleMember<?>) baseFindConstructor(new SingleMember<>(clazz.getNotReport()), params);

        if (member.getNotReport() == null) {
            logE(tag, "Failed to hook! Member is null! \ndebug: " + debug);
            return null;
        }
        if (member.getThrowable() != null) {
            logE(tag, "Failed to hook! \ndebug: " + debug, member.getThrowable());
            return null;
        }

        return run(() -> {
            XC_MethodHook.Unhook unHook = XposedBridge.hookMethod(member.getNotReport(), createHook(tag, iHook));
            logI(tag, "Success to hook: " + member.getNotReport());
            return unHook;
        }).orErrorMsg(null, "Failed to hook! \ndebug: " + debug);
    }

    static XC_MethodHook.Unhook[] baseHookAll(Member[] members, IHook iHook) {
        if (members == null || members.length == 0) return new XC_MethodHook.Unhook[0];
        String tag = getTag();

        return Arrays.stream(members).map(member ->
                run(() -> {
                        XC_MethodHook.Unhook unhook = XposedBridge.hookMethod(member, createHook(tag, iHook));
                        logI(tag, "Success to hook: " + member);
                        return unhook;
                    }
                ).orErrorMsg(null, "Failed to hook: " + member)
            )
            .filter(Objects::nonNull)
            .toArray(XC_MethodHook.Unhook[]::new);
    }

    static XC_MethodHook.Unhook baseFirstUnhook(XC_MethodHook.Unhook[] unhooks) {
        if (unhooks == null || unhooks.length == 0) return null;
        return unhooks[0];
    }

    static Method[] baseFilterMethod(SingleMember<Class<?>> clazz, IMemberFilter<Method> iMemberFilter) {
        return clazz.exec(new Method[0],
            member ->
                createSingleMember(
                    () -> Arrays.stream(member.getDeclaredMethods())
                        .filter(iMemberFilter::test)
                        .toArray(Method[]::new)
                ).setErrorMsg("Failed to filter method!")
                    .or(new Method[0])
        );
    }

    static Constructor<?>[] baseFilterConstructor(SingleMember<Class<?>> clazz, IMemberFilter<Constructor<?>> iMemberFilter) {
        return clazz.exec(new Constructor[0],
            member ->
                createSingleMember(
                    () -> Arrays.stream(member.getDeclaredConstructors())
                        .filter(iMemberFilter::test)
                        .toArray(Constructor<?>[]::new)
                ).setErrorMsg("Failed to filter constructor!")
                    .or(new Constructor[0])
        );
    }

    static Object baseCallMethod(Object instance, String name, Object... objs) {
        return run(
            () -> XposedHelpers.callMethod(instance, name, objs)
        ).orErrorMsg(null, "Failed to call method!");
    }

    static Object baseCallMethod(Object instance, Method method, Object... objs) {
        return run(() -> {
            method.setAccessible(true);
            return method.invoke(instance, objs);
        }).orErrorMsg(null, "Failed to call method!");
    }

    static Object baseCallSuperPrivateMethod(Object instance, String name, Object... objs) {
        return run(() -> {
            Method method = baseGetSuperPrivateMethod(instance.getClass(), name, objs);
            if (method == null) return null;

            return baseCallMethod(instance, method, objs);
        }).orErrorMsg(null, "Failed to call super private method!");
    }

    static Object baseGetField(Object instance, String name) {
        return run(
            () -> XposedHelpers.getObjectField(instance, name)
        ).orErrorMsg(null, "Failed to get field!");
    }

    static Object baseGetField(Object instance, Field field) {
        return run(() -> {
            field.setAccessible(true);
            return field.get(instance);
        }).orErrorMsg(null, "Failed to get field!");
    }

    static boolean baseSetField(Object instance, Field field, Object value) {
        return run(() -> {
            field.setAccessible(true);
            field.set(instance, value);
            return true;
        }).orErrorMsg(false, "Failed to set field!");
    }

    static boolean baseSetField(Object instance, String name, Object value) {
        return run(() -> {
            XposedHelpers.setObjectField(instance, name, value);
            return true;
        }).orErrorMsg(false, "Failed to set field!");
    }

    static Object baseNewInstance(SingleMember<Class<?>> clz, Object... objs) {
        return clz.exec(null,
            member ->
                createSingleMember(
                    () -> XposedHelpers.newInstance(member, objs)
                ).setErrorMsg("Failed to create new instance!")
                    .or(null)
        );
    }

    static Object baseCallStaticMethod(SingleMember<Class<?>> clz, String name, Object... objs) {
        return clz.exec(null,
            member ->
                createSingleMember(
                    () -> XposedHelpers.callStaticMethod(member, name, objs)
                ).setErrorMsg("Failed to call static method!")
                    .or(null)
        );
    }

    static Object baseCallStaticMethod(Method method, Object... objs) {
        return run(() -> {
            method.setAccessible(true);
            return method.invoke(null, objs);
        }).orErrorMsg(null, "Failed to call static method!");
    }

    static Object baseCallSuperStaticPrivateMethod(SingleMember<Class<?>> clz, String name, Object... objs) {
        return clz.exec(null,
            member ->
                createSingleMember(() -> {
                        Method method = baseGetSuperPrivateMethod(member, name, objs);
                        if (method == null) return null;

                        return baseCallStaticMethod(method, objs);
                    }
                ).setErrorMsg("Failed to call super private static field!")
                    .or(null)
        );
    }

    static Object baseGetStaticField(SingleMember<Class<?>> clz, String name) {
        return clz.exec(null,
            member ->
                createSingleMember(
                    () -> XposedHelpers.getStaticObjectField(member, name)
                ).setErrorMsg("Failed to get static field!")
                    .or(null)
        );
    }

    static Object baseGetStaticField(Field field) {
        return run(() -> {
            field.setAccessible(true);
            return field.get(null);
        }).orErrorMsg(null, "Failed to get static field!");
    }

    static boolean baseSetStaticField(SingleMember<Class<?>> clz, String name, Object value) {
        return clz.exec(false,
            member ->
                createSingleMember(() -> {
                    XposedHelpers.setStaticObjectField(member, name, value);
                    return true;
                }).setErrorMsg("Failed to set static field!")
                    .or(false)
        );
    }

    static boolean baseSetStaticField(Field field, Object value) {
        return run(() -> {
            field.setAccessible(true);
            field.set(null, value);
            return true;
        }).orErrorMsg(false, "Failed to set static field!");
    }

    static Object baseSetAdditionalStaticField(SingleMember<Class<?>> clz, String key, Object value) {
        return clz.exec(null,
            member ->
                createSingleMember(
                    () -> XposedHelpers.setAdditionalStaticField(member, key, value)
                ).setErrorMsg("Failed to set static additional instance!")
                    .or(null)
        );
    }

    static Object baseGetAdditionalStaticField(SingleMember<Class<?>> clz, String key) {
        return clz.exec(null,
            member ->
                createSingleMember(
                    () -> XposedHelpers.getAdditionalStaticField(member, key)
                ).setErrorMsg("Failed to get static additional instance!")
                    .or(null)
        );
    }

    static Object baseRemoveAdditionalStaticField(SingleMember<Class<?>> clz, String key) {
        return clz.exec(null,
            member ->
                createSingleMember(
                    () -> XposedHelpers.removeAdditionalStaticField(member, key)
                ).setErrorMsg("Failed to remove static additional instance!")
                    .or(null)
        );
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
        }).orErrorMsg(null, "Failed to get super private method!");
    }

    static XC_MethodHook createHook(String tag, IHook iHook) {
        iHook.PRIVATETAG = tag;
        XC_MethodHook xcMethodHook = new XC_MethodHook(iHook.PRIORITY) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    iHook.MethodHookParam(param);
                    iHook.before();
                } catch (Throwable e) {
                    logE(iHook.PRIVATETAG + ":before", e);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    iHook.MethodHookParam(param);
                    iHook.after();
                } catch (Throwable e) {
                    logE(iHook.PRIVATETAG + ":after", e);
                }
            }
        };
        iHook.XCMethodHook(xcMethodHook);
        return xcMethodHook;
    }

    /**
     * 数组参数转为类。
     */
    @Nullable
    private static Class<?>[] arrayToClass(ClassLoader classLoader, Object... objs) {
        if (classLoader == null || objs == null) return null;
        if (objs.length == 0) return new Class<?>[]{};

        List<Class<?>> classes = new ArrayList<>();
        for (Object o : objs) {
            if (o instanceof Class<?> c) {
                classes.add(c);
            } else if (o instanceof String s) {
                Class<?> ct = CoreTool.findClass(s, classLoader);
                if (ct == null) return null;
                classes.add(ct);
            } else if (o instanceof IHook) {
                break; // 一定为最后一个参数
            } else {
                logW(LogExpand.getTag(), "Unknown type: " + o, getStackTrace());
                return null;
            }
        }
        return classes.toArray(new Class<?>[0]);
    }
}
