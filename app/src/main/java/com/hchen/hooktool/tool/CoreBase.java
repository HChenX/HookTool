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
import static com.hchen.hooktool.helper.TryHelper.createMemberData;
import static com.hchen.hooktool.helper.TryHelper.run;
import static com.hchen.hooktool.hook.HookFactory.createHook;
import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.LogExpand.getTag;
import static com.hchen.hooktool.log.XposedLog.logD;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;
import static com.hchen.hooktool.tool.CoreTool.findClass;
import static com.hchen.hooktool.tool.CoreTool.findConstructor;
import static com.hchen.hooktool.tool.CoreTool.findMethod;

import androidx.annotation.Nullable;

import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.tool.itool.IMemberFilter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

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

    static MemberData<Class<?>> baseFindClass(String name, ClassLoader classLoader) {
        return (MemberData<Class<?>>) (MemberData<?>) createMemberData(() -> {
            Class<?> c = CoreMemberCache.readClassCache(name, classLoader);
            if (c == null) {
                c = XposedHelpers.findClass(name, classLoader);
                CoreMemberCache.writeClassCache(c);
            }
            return c;
        }).setErrMsg("Failed to find class!");
    }

    static MemberData<Method> baseFindMethod(MemberData<Class<?>> clazz, String name, Object... objs) {
        return createMemberData(() -> XposedHelpers.findMethodExact(clazz.getIfExists(), name, arrayToClass(clazz.getClassLoaderIfExists(), objs)))
                .setErrMsg("Failed to find method!")
                .spiltThrowableMsg(clazz.getThrowable());
    }

    static MemberListData<Method> baseFindAllMethod(MemberData<Class<?>> clazz, String name) {
        return createMemberData(() -> Arrays.stream(clazz.getIfExists().getDeclaredMethods())
                .filter(method -> name.equals(method.getName()))
                .collect(Collectors.toCollection(MemberListData::new)))
                .setErrMsg("Failed to find all method!")
                .spiltThrowableMsg(clazz.getThrowable())
                .or(new MemberListData<>());
    }

    static MemberData<Constructor<?>> baseFindConstructor(MemberData<Class<?>> clazz, Object... objs) {
        return (MemberData<Constructor<?>>) (MemberData<?>) createMemberData(() ->
                XposedHelpers.findConstructorExact(clazz.getIfExists(), arrayToClass(clazz.getClassLoaderIfExists(), objs)))
                .setErrMsg("Failed to find constructor!")
                .spiltThrowableMsg(clazz.getThrowable());
    }

    static MemberListData<Constructor<?>> baseFindAllConstructor(MemberData<Class<?>> clazz) {
        return createMemberData(() -> new MemberListData<>(Arrays.asList(clazz.getIfExists().getDeclaredConstructors())))
                .setErrMsg("Failed to find constructor!")
                .spiltThrowableMsg(clazz.getThrowable())
                .or(new MemberListData<>());
    }

    static MemberData<Field> baseFindField(MemberData<Class<?>> clazz, String name) {
        return createMemberData(() -> XposedHelpers.findField(clazz.getIfExists(), name))
                .setErrMsg("Failed to find field!")
                .spiltThrowableMsg(clazz.getThrowable());
    }

    static CoreTool.UnHook baseHook(MemberData<Class<?>> clazz, String method, Object... params) {
        String tag = getTag();
        String debug = (method != null ? "METHOD" : "CONSTRUCTOR") + "#" + (clazz.getIfExists() == null ? "null" : clazz.getIfExists().getName())
                + "#" + method + "#" + Arrays.toString(params);
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
                            MemberData<Class<?>> classMemberData = findClass(s, clazz.getClassLoaderIfExists());
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

        return run(() -> {
            CoreTool.UnHook unHook = new CoreTool.UnHook(XposedBridge.hookMethod(((MemberData<Member>) member[0]).getIfExists(), createHook(tag, iHook)));
            logD(tag, "Success to hook: " + member[0].getIfExists());
            return unHook;
        }).orErrMag(new CoreTool.UnHook(null), "Failed to hook! \ndebug: " + debug);
    }

    static MemberListData<Method> baseFilterMethod(MemberData<Class<?>> clazz, IMemberFilter<Method> iMemberFilter) {
        return createMemberData(() -> Arrays.stream(clazz.getIfExists().getDeclaredMethods()).filter(iMemberFilter::test)
                .collect(Collectors.toCollection(MemberListData::new)))
                .setErrMsg("Failed to filter method!")
                .spiltThrowableMsg(clazz.getThrowable())
                .or(new MemberListData<>());
    }

    static MemberListData<Constructor<?>> baseFilterConstructor(MemberData<Class<?>> clazz, IMemberFilter<Constructor<?>> iMemberFilter) {
        return createMemberData(() -> Arrays.stream(clazz.getIfExists().getDeclaredConstructors()).filter(iMemberFilter::test)
                .collect(Collectors.toCollection(MemberListData::new)))
                .setErrMsg("Failed to filter constructor!")
                .spiltThrowableMsg(clazz.getThrowable())
                .or(new MemberListData<>());
    }

    static <T> T baseNewInstance(MemberData<Class<?>> clz, Object... objs) {
        return createMemberData(() -> (T) XposedHelpers.newInstance(clz.getIfExists(), objs))
                .setErrMsg("Failed to create new instance!")
                .spiltThrowableMsg(clz.getThrowable())
                .or(null);
    }

    static <T> T baseCallStaticMethod(MemberData<Class<?>> clz, String name, Object... objs) {
        return createMemberData(() -> (T) XposedHelpers.callStaticMethod(clz.getIfExists(), name, objs))
                .setErrMsg("Failed to call static method!")
                .spiltThrowableMsg(clz.getThrowable())
                .or(null);
    }

    static <T> T baseGetStaticField(MemberData<Class<?>> clz, String name) {
        return createMemberData(() -> (T) XposedHelpers.getStaticObjectField(clz.getIfExists(), name))
                .setErrMsg("Failed to get static field!")
                .spiltThrowableMsg(clz.getThrowable())
                .or(null);
    }

    static boolean baseSetStaticField(MemberData<Class<?>> clz, String name, Object value) {
        return createMemberData(() -> {
            XposedHelpers.setStaticObjectField(clz.getIfExists(), name, value);
            return true;
        })
                .setErrMsg("Failed to set static field!")
                .spiltThrowableMsg(clz.getThrowable())
                .or(false);
    }

    static <T> T baseSetAdditionalStaticField(MemberData<Class<?>> clz, String key, Object value) {
        return createMemberData(() -> (T) XposedHelpers.setAdditionalStaticField(clz.getIfExists(), key, value))
                .setErrMsg("Failed to set static additional instance!")
                .spiltThrowableMsg(clz.getThrowable())
                .or(null);
    }

    static <T> T baseGetAdditionalStaticField(MemberData<Class<?>> clz, String key) {
        return createMemberData(() -> (T) XposedHelpers.getAdditionalStaticField(clz.getIfExists(), key))
                .setErrMsg("Failed to get static additional instance!")
                .spiltThrowableMsg(clz.getThrowable())
                .or(null);
    }

    static <T> T baseRemoveAdditionalStaticField(MemberData<Class<?>> clz, String key) {
        return createMemberData(() -> (T) XposedHelpers.removeAdditionalStaticField(clz.getIfExists(), key))
                .setErrMsg("Failed to remove static additional instance!")
                .spiltThrowableMsg(clz.getThrowable())
                .or(null);
    }

    private final static class CoreMemberCache {
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
