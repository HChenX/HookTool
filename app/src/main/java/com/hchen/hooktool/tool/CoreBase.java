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

import static com.hchen.hooktool.helper.TryHelper.run;
import static com.hchen.hooktool.helper.TryHelper.runDump;
import static com.hchen.hooktool.hook.HookFactory.createHook;
import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.LogExpand.getTag;
import static com.hchen.hooktool.log.XposedLog.logD;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;
import static com.hchen.hooktool.tool.CoreTool.findClass;
import static com.hchen.hooktool.tool.CoreTool.findConstructor;
import static com.hchen.hooktool.tool.CoreTool.findMethod;

import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.tool.itool.IMemberFilter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class CoreBase {
    protected CoreBase() {
    }

    protected static MemberData<Method> baseFindMethod(MemberData<Class<?>> clazz, String name, Class<?>... classes) {
        return runDump(() -> XposedHelpers.findMethodExact(clazz.getIfExists(), name, classes))
                .setErrMsg("Failed to find method!")
                .spiltThrowableMsg(clazz.getThrowable());
    }

    protected static ArrayList<Method> baseFindAllMethod(MemberData<Class<?>> clazz, String name) {
        return runDump(() -> Arrays.stream(clazz.getIfExists().getDeclaredMethods())
                .filter(method -> name.equals(method.getName()))
                .collect(Collectors.toCollection(ArrayList::new)))
                .setErrMsg("Failed to find all method!")
                .spiltThrowableMsg(clazz.getThrowable())
                .or(new ArrayList<>());
    }

    protected static MemberData<Constructor<?>> baseFindConstructor(MemberData<Class<?>> clazz, Class<?>... classes) {
        return (MemberData<Constructor<?>>) (MemberData<?>) runDump(() -> XposedHelpers.findConstructorExact(clazz.getIfExists(), classes))
                .setErrMsg("Failed to find constructor!")
                .spiltThrowableMsg(clazz.getThrowable());
    }

    protected static ArrayList<Constructor<?>> baseFindAllConstructor(MemberData<Class<?>> clazz) {
        return runDump(() -> new ArrayList<>(Arrays.asList(clazz.getIfExists().getDeclaredConstructors())))
                .setErrMsg("Failed to find constructor!")
                .spiltThrowableMsg(clazz.getThrowable())
                .or(new ArrayList<>());
    }

    protected static MemberData<Field> baseFindField(MemberData<Class<?>> clazz, String name) {
        return runDump(() -> XposedHelpers.findField(clazz.getIfExists(), name))
                .setErrMsg("Failed to find field!")
                .spiltThrowableMsg(clazz.getThrowable());
    }

    protected static CoreTool.UnHook baseHook(MemberData<Class<?>> clazz, ClassLoader classLoader, String method, Object... params) {
        String debug = (method != null ? "METHOD" : "CONSTRUCTOR") + "#" + (clazz.getIfExists() == null ? "null" : clazz.getIfExists().getName())
                + "#" + method + "#" + Arrays.toString(params);
        String tag = getTag();
        if (params == null || params.length == 0 || !(params[params.length - 1] instanceof IHook iHook)) {
            logW(tag, "Hook params is null or length is 0 or last param not is IAction! \ndebug: " + debug + getStackTrace());
            return new CoreTool.UnHook(null);
        }

        if (clazz.getThrowable() != null) {
            logE(tag, "Failed to hook! \ndebug: " + debug, clazz.getThrowable());
            return new CoreTool.UnHook(null);
        }

        final MemberData<?>[] member = new MemberData[]{null};
        run(() -> {
            Class<?>[] classes = Arrays.stream(params)
                    .limit(params.length - 1)
                    .map(o -> {
                        if (o instanceof String s) {
                            MemberData<Class<?>> classMemberData = findClass(s, classLoader);
                            if (classMemberData.getThrowable() != null)
                                throw new RuntimeException(classMemberData.getThrowable());
                            return classMemberData.get();
                        } else if (o instanceof Class<?> c) return c;
                        else throw new RuntimeException("Unknown type: " + o);
                    }).toArray(Class<?>[]::new);

            if (method != null)
                member[0] = findMethod(clazz.getIfExists(), method, classes);
            else
                member[0] = findConstructor(clazz.getIfExists(), classes);
            return null;
        }).orErrMag(null, "Failed to hook! \ndebug: " + debug);

        if (member[0] == null) return new CoreTool.UnHook(null); // 上方必抛错
        if (member[0].getThrowable() != null) {
            logE(tag, "Failed to hook! \ndebug: " + debug, member[0].getThrowable());
            return new CoreTool.UnHook(null);
        }

        return runDump(() -> {
            CoreTool.UnHook unHook = new CoreTool.UnHook(XposedBridge.hookMethod(((MemberData<Member>) member[0]).getIfExists(), createHook(tag, iHook)));
            logD(tag, "Success to hook: " + member[0].getIfExists());
            return unHook;
        })
                .setErrMsg("Failed to hook! \ndebug: " + debug)
                .or(new CoreTool.UnHook(null));
    }

    protected static ArrayList<Method> baseFilterMethod(MemberData<Class<?>> clazz, IMemberFilter<Method> iMemberFilter) {
        return runDump(() -> Arrays.stream(clazz.getIfExists().getDeclaredMethods()).filter(iMemberFilter::test)
                .collect(Collectors.toCollection(ArrayList::new)))
                .setErrMsg("Failed to filter method!")
                .spiltThrowableMsg(clazz.getThrowable())
                .or(new ArrayList<>());
    }

    protected static ArrayList<Constructor<?>> baseFilterConstructor(MemberData<Class<?>> clazz, IMemberFilter<Constructor<?>> iMemberFilter) {
        return runDump(() -> Arrays.stream(clazz.getIfExists().getDeclaredConstructors()).filter(iMemberFilter::test)
                .collect(Collectors.toCollection(ArrayList::new)))
                .setErrMsg("Failed to filter constructor!")
                .spiltThrowableMsg(clazz.getThrowable())
                .or(new ArrayList<>());
    }

    protected static <T> T baseNewInstance(MemberData<Class<?>> clz, Object... objects) {
        return runDump(() -> (T) XposedHelpers.newInstance(clz.getIfExists(), objects))
                .setErrMsg("Failed to create new instance!")
                .spiltThrowableMsg(clz.getThrowable())
                .or(null);
    }

    protected static <T> T baseCallStaticMethod(MemberData<Class<?>> clz, String name, Object... objs) {
        return runDump(() -> (T) XposedHelpers.callStaticMethod(clz.getIfExists(), name, objs))
                .setErrMsg("Failed to call static method!")
                .spiltThrowableMsg(clz.getThrowable())
                .or(null);
    }

    protected static <T> T baseGetStaticField(MemberData<Class<?>> clz, String name) {
        return runDump(() -> (T) XposedHelpers.getStaticObjectField(clz.getIfExists(), name))
                .setErrMsg("Failed to get static field!")
                .spiltThrowableMsg(clz.getThrowable())
                .or(null);
    }

    protected static boolean baseSetStaticField(MemberData<Class<?>> clz, String name, Object value) {
        return runDump(() -> {
            XposedHelpers.setStaticObjectField(clz.getIfExists(), name, value);
            return true;
        })
                .setErrMsg("Failed to set static field!")
                .spiltThrowableMsg(clz.getThrowable())
                .or(false);
    }

    protected static <T> T baseSetAdditionalStaticField(MemberData<Class<?>> clz, String key, Object value) {
        return runDump(() -> (T) XposedHelpers.setAdditionalStaticField(clz.getIfExists(), key, value))
                .setErrMsg("Failed to set static additional instance!")
                .spiltThrowableMsg(clz.getThrowable())
                .or(null);
    }

    protected static <T> T baseGetAdditionalStaticField(MemberData<Class<?>> clz, String key) {
        return runDump(() -> (T) XposedHelpers.getAdditionalStaticField(clz.getIfExists(), key))
                .setErrMsg("Failed to get static additional instance!")
                .spiltThrowableMsg(clz.getThrowable())
                .or(null);
    }

    protected static <T> T baseRemoveAdditionalStaticField(MemberData<Class<?>> clz, String key) {
        return runDump(() -> (T) XposedHelpers.removeAdditionalStaticField(clz.getIfExists(), key))
                .setErrMsg("Failed to remove static additional instance!")
                .spiltThrowableMsg(clz.getThrowable())
                .or(null);
    }
}
