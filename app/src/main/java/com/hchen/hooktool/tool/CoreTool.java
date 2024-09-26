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
import static com.hchen.hooktool.helper.ConvertHelper.toClassAsIAction;
import static com.hchen.hooktool.helper.Try.run;
import static com.hchen.hooktool.hook.HookFactory.createHook;
import static com.hchen.hooktool.log.XposedLog.logD;
import static com.hchen.hooktool.log.XposedLog.logI;
import static com.hchen.hooktool.log.XposedLog.logW;

import androidx.annotation.NonNull;

import com.hchen.hooktool.data.ToolData;
import com.hchen.hooktool.helper.Try;
import com.hchen.hooktool.hook.IAction;
import com.hchen.hooktool.log.LogExpand;
import com.hchen.hooktool.tool.itool.IFilter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 核心工具
 *
 * @author 焕晨HChen
 * @noinspection unused
 */
public class CoreTool {
    public static ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

    //------------ 检查指定类是否存在 --------------
    public static boolean existsClass(String clazz) {
        return existsClass(clazz, ToolData.classLoader);
    }

    public static boolean existsClass(String clazz, ClassLoader classLoader) {
        return Try.run(() -> XposedHelpers.findClassIfExists(clazz, classLoader)).get() != null;
    }

    // --------- 查找类 -----------
    public static Class<?> findClass(String name) {
        return findClass(name, ToolData.classLoader);
    }

    public static Class<?> findClass(String name, ClassLoader classLoader) {
        return run(() -> XposedHelpers.findClass(name, classLoader))
                .orErrMag(null, tag(), "Failed to find class!");
    }

    //------------ 检查指定方法是否存在 --------------
    public static boolean existsMethod(String clazz, String name, Object... objs) {
        return existsMethod(clazz, ToolData.classLoader, name, objs);
    }

    public static boolean existsMethod(String clazz, ClassLoader classLoader, String name, Object... objs) {
        return existsMethod(Try.run(() -> XposedHelpers.findClassIfExists(clazz, classLoader)).get(), classLoader, name, objs);
    }

    public static boolean existsMethod(Class<?> clazz, ClassLoader classLoader, String name, Object... objs) {
        return existsMethod(clazz, name, arrayToClass(objs, classLoader));
    }

    public static boolean existsMethod(Class<?> clazz, String name, Class<?>... classes) {
        return run(() -> clazz.getDeclaredMethod(name, classes)).isSuccess();
    }

    public static boolean existsAnyMethod(String clazz, String name) {
        return existsAnyMethod(clazz, ToolData.classLoader, name);
    }

    public static boolean existsAnyMethod(String clazz, ClassLoader classLoader, String name) {
        return existsAnyMethod(Try.run(() -> XposedHelpers.findClassIfExists(clazz, classLoader)).get(), name);
    }

    public static boolean existsAnyMethod(Class<?> clazz, String name) {
        if (clazz == null) return false;
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    // ------------ 查找方法 --------------
    public static Method findMethod(String clazz, String name, Object... objects) {
        return findMethod(findClass(clazz), name, arrayToClass(objects));
    }

    public static Method findMethod(String clazz, ClassLoader classLoader, String name, Object... objects) {
        return findMethod(findClass(clazz, classLoader), name, arrayToClass(classLoader, objects));
    }

    public static Method findMethod(Class<?> clazz, ClassLoader classLoader, String name, Object... objects) {
        return findMethod(clazz, name, arrayToClass(classLoader, objects));
    }

    public static Method findMethod(Class<?> clazz, String name, Class<?>... classes) {
        return run(() -> XposedHelpers.findMethodExact(clazz, name, classes))
                .orErrMag(null, tag(), "Failed to find method!");
    }

    public static ArrayList<Method> findAllMethod(String clazz, String name) {
        return findAllMethod(findClass(clazz), name);
    }

    public static ArrayList<Method> findAllMethod(String clazz, ClassLoader classLoader, String name) {
        return findAllMethod(findClass(clazz, classLoader), name);
    }

    public static ArrayList<Method> findAllMethod(Class<?> clazz, String name) {
        if (clazz == null) return new ArrayList<>();
        ArrayList<Method> methods = new ArrayList<>();
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name)) methods.add(m);
        }
        return methods;
    }

    //------------ 检查指定构造函数是否存在 --------------
    public static boolean existsConstructor(String clazz, Object... objs) {
        return existsConstructor(clazz, ToolData.classLoader, objs);
    }

    public static boolean existsConstructor(String clazz, ClassLoader classLoader, Object... objs) {
        return existsConstructor(Try.run(() -> XposedHelpers.findClassIfExists(clazz, classLoader)).get(), arrayToClass(classLoader, objs));
    }

    public static boolean existsConstructor(Class<?> clazz, ClassLoader classLoader, Object... objs) {
        return existsConstructor(clazz, arrayToClass(classLoader, objs));
    }

    public static boolean existsConstructor(Class<?> clazz, Class<?>... classes) {
        return run(() -> clazz.getDeclaredConstructor(classes)).isSuccess();
    }

    // --------- 查找构造函数 -----------
    public static Constructor<?> findConstructor(String clazz, Object... objects) {
        return findConstructor(findClass(clazz), arrayToClass(objects));
    }

    public static Constructor<?> findConstructor(String clazz, ClassLoader classLoader, Object... objects) {
        return findConstructor(findClass(clazz, classLoader), arrayToClass(classLoader, objects));
    }

    public static Constructor<?> findConstructor(Class<?> clazz, ClassLoader classLoader, Object... objects) {
        return findConstructor(clazz, arrayToClass(classLoader, objects));
    }

    public static Constructor<?> findConstructor(Class<?> clazz, Class<?>... classes) {
        return run(() -> XposedHelpers.findConstructorExact(clazz, classes))
                .orErrMag(null, tag(), "Failed to find constructor!");
    }

    public static ArrayList<Constructor<?>> findAllConstructor(String clazz) {
        return findAllConstructor(findClass(clazz));
    }

    public static ArrayList<Constructor<?>> findAllConstructor(String clazz, ClassLoader classLoader) {
        return findAllConstructor(findClass(clazz, classLoader));
    }

    public static ArrayList<Constructor<?>> findAllConstructor(Class<?> clazz) {
        return new ArrayList<>(Arrays.asList(clazz.getDeclaredConstructors()));
    }

    //------------ 检查指定字段是否存在 --------------
    public static boolean existsField(String clazz, String name) {
        return existsField(clazz, ToolData.classLoader, name);
    }

    public static boolean existsField(String clazz, ClassLoader classLoader, String name) {
        return existsField(Try.run(() -> XposedHelpers.findClassIfExists(clazz, classLoader)).get(), name);
    }

    public static boolean existsField(Class<?> clazz, String name) {
        return Try.run(() -> clazz.getDeclaredField(name)).isSuccess();
    }

    // --------- 查找字段 -----------
    public static Field findField(String clazz, String name) {
        return findField(findClass(clazz), name);
    }

    public static Field findField(String clazz, ClassLoader classLoader, String name) {
        return findField(findClass(clazz, classLoader), name);
    }

    public static Field findField(Class<?> clazz, String name) {
        return run(() -> XposedHelpers.findField(clazz, name))
                .orErrMag(null, tag(), "Failed to find field!");
    }

    // --------- 执行 hook -----------
    // --------- 普通方法 -------------
    public static UnHook hook(String clazz, String method, Object... params) {
        return hook(clazz, ToolData.classLoader, method, params);
    }

    public static UnHook hook(String clazz, ClassLoader classLoader, String method, Object... params) {
        return hook(findClass(clazz, classLoader), method, toClassAsIAction(classLoader, params));
    }

    public static UnHook hook(Class<?> clazz, ClassLoader classLoader, String method, Object... params) {
        return hook(clazz, method, toClassAsIAction(classLoader, params));
    }

    public static UnHook hook(Class<?> clazz, String method, Object... params) {
        return hook(clazz, method, HookType.METHOD, params);
    }

    public static UnHookList hookAll(String clazz, String method, IAction iAction) {
        return hookAll(findClass(clazz), method, iAction);
    }

    public static UnHookList hookAll(String clazz, ClassLoader classLoader, String method, IAction iAction) {
        return hookAll(findClass(clazz, classLoader), method, iAction);
    }

    public static UnHookList hookAll(Class<?> clazz, String method, IAction iAction) {
        return hookAll(findAllMethod(clazz, method), iAction);
    }

    // --------- 构造函数 ------------
    public static UnHook hook(String clazz, Object... params) {
        return hook(findClass(clazz), params);
    }

    public static UnHook hook(String clazz, ClassLoader classLoader, Object... params) {
        return hook(findClass(clazz, classLoader), toClassAsIAction(classLoader, params));
    }

    public static UnHook hook(Class<?> clazz, ClassLoader classLoader, Object... params) {
        return hook(clazz, toClassAsIAction(classLoader, params));
    }

    public static UnHook hook(Class<?> clazz, Object... params) {
        return hook(clazz, null, HookType.CONSTRUCTOR, params);
    }

    public static UnHookList hookAll(String clazz, IAction iAction) {
        return hookAll(findAllConstructor(clazz), iAction);
    }

    public static UnHookList hookAll(String clazz, ClassLoader classLoader, IAction iAction) {
        return hookAll(findAllConstructor(clazz, classLoader), iAction);
    }

    public static UnHookList hookAll(Class<?> clazz, IAction iAction) {
        return hookAll(findAllConstructor(clazz), iAction);
    }

    // ----------- 核心实现 ---------------
    private enum HookType {
        METHOD,
        CONSTRUCTOR
    }

    private static UnHook hook(Class<?> clazz, String method, HookType hookType, Object... params) {
        String debug = hookType.toString() + "#" + clazz.getName() + "#" + method + "#" + Arrays.toString(params);
        if (params == null || params.length == 0 || !(params[params.length - 1] instanceof IAction iAction)) {
            logW(tag(), "Params is null or length is 0 or last param not is IAction! \ndebug: " + debug + getStackTrace());
            return null;
        }

        return run(() -> {
            Class<?>[] classes = Arrays.stream(params)
                    .limit(params.length - 1)
                    .map(o -> {
                        if (o instanceof String s) {
                            Class<?> c = findClass(s);
                            if (c == null)
                                throw new RuntimeException("Found class is null, stop to hook!");
                            return c;
                        } else if (o instanceof Class<?> c) return c;
                        else throw new RuntimeException("Unknown type: " + o);
                    }).toArray(Class<?>[]::new);
            Member member = null;
            switch (hookType) {
                case METHOD -> member = findMethod(clazz, method, classes);
                case CONSTRUCTOR -> member = findConstructor(clazz, classes);
            }
            return hook(member, iAction);
        }).orErrMag(new UnHook(null), tag(), "Failed to hook! \ndebug: " + debug);
    }

    public static UnHook hook(Member member, IAction iAction) {
        String tag = tag();
        return run(() -> {
            UnHook unhook = new UnHook(XposedBridge.hookMethod(member, createHook(tag, iAction)));
            logD(tag, "Success to hook: " + member);
            return unhook;
        }).orErrMag(new UnHook(null), tag, "Failed to hook: " + member);
    }

    public static UnHookList hookAll(ArrayList<?> members, IAction iAction) {
        String tag = tag();
        UnHookList unhooks = new UnHookList();
        for (Object o : members) {
            if (o instanceof Method || o instanceof Constructor<?>) {
                run(() -> {
                    unhooks.add(XposedBridge.hookMethod((Member) o, createHook(tag, iAction)));
                    logD(tag, "Success to hook: " + o);
                    return null;
                }).orErrMag(null, tag, "Failed to hook: " + o);
            }
        }
        return unhooks;
    }

    // --------- 快捷方法 -----------
    public static IAction returnResult(final Object result) {
        return new IAction() {
            @Override
            public void before() {
                setResult(result);
            }
        };
    }

    public static IAction doNothing() {
        return new IAction() {
            @Override
            public void before() {
                setResult(null);
            }
        };
    }

    // --------- 解除 hook ---------
    public static void unHook(Member hookMember, XC_MethodHook xcMethodHook) {
        XposedBridge.unhookMethod(hookMember, xcMethodHook);
    }

    public static class UnHook {
        private XC_MethodHook.Unhook unhook;

        private UnHook(XC_MethodHook.Unhook unhook) {
            this.unhook = unhook;
        }

        public void unHook() {
            if (unhook != null) unhook.unhook();
            unhook = null;
        }
    }

    public static class UnHookList {
        private final List<XC_MethodHook.Unhook> unhooks = new ArrayList<>();

        private UnHookList() {
            unhooks.clear();
        }

        private void add(XC_MethodHook.Unhook unhook) {
            unhooks.add(unhook);
        }

        public void forEach(Consumer<? super XC_MethodHook.Unhook> action) {
            unhooks.forEach(action);
        }

        public XC_MethodHook.Unhook get(int index) {
            return unhooks.get(index);
        }

        public void unHookAll() {
            for (XC_MethodHook.Unhook unhook : unhooks) {
                unhook.unhook();
            }
            unhooks.clear();
        }

        @NonNull
        @Override
        public String toString() {
            return unhooks.toString();
        }
    }

    // --------- 过滤方法 -----------
    public static ArrayList<Method> filterMethod(String clazz, IFilter<Method> iFilter) {
        return filterMethod(findClass(clazz), iFilter);
    }

    public static ArrayList<Method> filterMethod(String clazz, ClassLoader classLoader, IFilter<Method> iFilter) {
        return filterMethod(findClass(clazz, classLoader), iFilter);
    }

    public static ArrayList<Method> filterMethod(Class<?> clazz, IFilter<Method> iFilter) {
        return run(() -> {
            ArrayList<Method> methods = new ArrayList<>();
            for (Method m : clazz.getDeclaredMethods()) {
                if (iFilter.test(m)) {
                    methods.add(m);
                }
            }
            return methods;
        }).orErrMag(new ArrayList<>(), tag(), "Failed to filter method!");
    }

    public static ArrayList<Constructor<?>> filterConstructor(String clazz, IFilter<Constructor<?>> iFilter) {
        return filterConstructor(findClass(clazz), iFilter);
    }

    public static ArrayList<Constructor<?>> filterConstructor(String clazz, ClassLoader classLoader, IFilter<Constructor<?>> iFilter) {
        return filterConstructor(findClass(clazz, classLoader), iFilter);
    }

    public static ArrayList<Constructor<?>> filterConstructor(Class<?> clazz, IFilter<Constructor<?>> iFilter) {
        return run(() -> {
            ArrayList<Constructor<?>> constructors = new ArrayList<>();
            for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                if (iFilter.test(c)) {
                    constructors.add(c);
                }
            }
            return constructors;
        }).orErrMag(new ArrayList<>(), tag(), "Failed to filter constructor!");
    }

    // --------- 打印堆栈 ----------
    public static String getStackTrace(boolean autoLog) {
        String task = getStackTrace();
        if (autoLog) logI(tag(), task);
        return task;
    }

    public static String getStackTrace() {
        return LogExpand.getStackTrace();
    }

    // --------- 耗时检查 -----------
    public static long timeConsumption(Runnable runnable) {
        return Try.run(() -> {
            Instant start = Instant.now();
            runnable.run();
            Instant end = Instant.now();
            return Duration.between(start, end).toMillis();
        }).orErrMag(-1L, tag(), "Failed to check code time consumption!");
    }

    // ---------- 非静态 -----------
    public static <T> T callMethod(Object instance, String name, Object... objs) {
        return run(() -> (T) XposedHelpers.callMethod(instance, name, objs))
                .orErrMag(null, tag(), "Failed to call method!");
    }

    public static <T> T getField(Object instance, String name) {
        return run(() -> (T) XposedHelpers.getObjectField(instance, name))
                .orErrMag(null, tag(), "Failed to get field!");
    }

    public static <T> T getField(Object instance, Field field) {
        return run(() -> {
            field.setAccessible(true);
            return (T) field.get(instance);
        }).orErrMag(null, tag(), "Failed to get field!");
    }

    public static boolean setField(Object instance, String name, Object value) {
        return run(() -> {
            XposedHelpers.setObjectField(instance, name, value);
            return true;
        }).orErrMag(false, tag(), "Failed to set field!");
    }

    public static boolean setField(Object instance, Field field, Object value) {
        return run(() -> {
            field.setAccessible(true);
            field.set(instance, value);
            return true;
        }).orErrMag(false, tag(), "Failed to set field!");
    }

    public static <T> T setAdditionalInstanceField(Object instance, String key, Object value) {
        return run(() -> (T) XposedHelpers.setAdditionalInstanceField(instance, key, value))
                .orErrMag(null, tag(), "Failed to set additional instance!");
    }

    public static <T> T getAdditionalInstanceField(Object instance, String key) {
        return run(() -> (T) XposedHelpers.getAdditionalInstanceField(instance, key))
                .orErrMag(null, tag(), "Failed to get additional instance!");
    }

    public static <T> T removeAdditionalInstanceField(Object instance, String key) {
        return run(() -> (T) XposedHelpers.removeAdditionalInstanceField(instance, key))
                .orErrMag(null, tag(), "Failed to remove additional instance!");
    }

    // ---------- 静态 ------------
    public static <T> T newInstance(Class<?> clz, Object... objects) {
        return run(() -> (T) XposedHelpers.newInstance(clz, objects))
                .orErrMag(null, tag(), "Failed to create new instance!");
    }

    public static <T> T newInstance(String clz, Object... objects) {
        return newInstance(findClass(clz), objects);
    }

    public static <T> T newInstance(String clz, ClassLoader classLoader, Object... objects) {
        return newInstance(findClass(clz, classLoader), objects);
    }

    public static <T> T callStaticMethod(Class<?> clz, String name, Object... objs) {
        return run(() -> (T) XposedHelpers.callStaticMethod(clz, name, objs))
                .orErrMag(null, tag(), "Failed to call static method!");
    }

    public static <T> T callStaticMethod(String clz, String name, Object... objs) {
        return callStaticMethod(findClass(clz), name, objs);
    }

    public static <T> T callStaticMethod(String clz, ClassLoader classLoader, String name, Object... objs) {
        return callStaticMethod(findClass(clz, classLoader), name, objs);
    }

    public static <T> T callStaticMethod(Method method, Object... objs) {
        return run(() -> {
            method.setAccessible(true);
            return (T) method.invoke(null, objs);
        }).orErrMag(null, tag(), "Failed to call static method!");
    }

    public static <T> T getStaticField(Class<?> clz, String name) {
        return run(() -> (T) XposedHelpers.getStaticObjectField(clz, name))
                .orErrMag(null, tag(), "Failed to get static field!");
    }

    public static <T> T getStaticField(String clz, String name) {
        return getStaticField(findClass(clz), name);
    }

    public static <T> T getStaticField(String clz, ClassLoader classLoader, String name) {
        return getStaticField(findClass(clz, classLoader), name);
    }

    public static <T> T getStaticField(Field field) {
        return run(() -> {
            field.setAccessible(true);
            return (T) field.get(null);
        }).orErrMag(null, tag(), "Failed to get static field!");
    }

    public static boolean setStaticField(Class<?> clz, String name, Object value) {
        return run(() -> {
            XposedHelpers.setStaticObjectField(clz, name, value);
            return true;
        }).orErrMag(false, tag(), "Failed to set static field!");
    }

    public static boolean setStaticField(Field field, Object value) {
        return run(() -> {
            field.setAccessible(true);
            field.set(null, value);
            return true;
        }).orErrMag(false, tag(), "Failed to set static field!");
    }

    public static boolean setStaticField(String clz, String name, Object value) {
        return setStaticField(findClass(clz), name, value);
    }

    public static boolean setStaticField(String clz, ClassLoader classLoader, String name, Object value) {
        return setStaticField(findClass(clz, classLoader), name, value);
    }

    public static <T> T setAdditionalStaticField(Class<?> clz, String key, Object value) {
        return run(() -> (T) XposedHelpers.setAdditionalStaticField(clz, key, value))
                .orErrMag(null, tag(), "Failed to set static additional instance!");
    }

    public static <T> T getAdditionalStaticField(Class<?> clz, String key) {
        return run(() -> (T) XposedHelpers.getAdditionalStaticField(clz, key))
                .orErrMag(null, tag(), "Failed to get static additional instance!");
    }

    public static <T> T removeAdditionalStaticField(Class<?> clz, String key) {
        return run(() -> (T) XposedHelpers.removeAdditionalStaticField(clz, key))
                .orErrMag(null, tag(), "Failed to remove static additional instance!");
    }

    public static <T> T setAdditionalStaticField(String clz, String key, Object value) {
        return setAdditionalStaticField(findClass(clz), key, value);
    }

    public static <T> T setAdditionalStaticField(String clz, ClassLoader classLoader, String key, Object value) {
        return setAdditionalStaticField(findClass(clz, classLoader), key, value);
    }

    public static <T> T getAdditionalStaticField(String clz, String key) {
        return getAdditionalStaticField(findClass(clz), key);
    }

    public static <T> T getAdditionalStaticField(String clz, ClassLoader classLoader, String key) {
        return getAdditionalStaticField(findClass(key, classLoader), key);
    }

    public static <T> T removeAdditionalStaticField(String clz, String key) {
        return removeAdditionalStaticField(findClass(clz), key);
    }

    public static <T> T removeAdditionalStaticField(String clz, ClassLoader classLoader, String key) {
        return removeAdditionalStaticField(findClass(clz, classLoader), key);
    }

    private static String tag() {
        String tag = LogExpand.tag();
        if (tag == null) return "CoreTool";
        return tag;
    }
}
