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

import static com.hchen.hooktool.data.ToolData.isZygote;
import static com.hchen.hooktool.helper.ConvertHelper.arrayToClass;
import static com.hchen.hooktool.helper.ConvertHelper.toClassAsIAction;
import static com.hchen.hooktool.helper.Try.run;
import static com.hchen.hooktool.hook.HookFactory.createHook;
import static com.hchen.hooktool.log.XposedLog.logD;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logI;
import static com.hchen.hooktool.log.XposedLog.logW;

import com.hchen.hooktool.data.ToolData;
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

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 核心工具
 * <p>
 * Core tool
 *
 * @author 焕晨HChen
 * @noinspection unused
 */
public class CoreTool {

    public static ClassLoader classLoader() {
        return ToolData.classLoader;
    }

    //------------ 检查指定类是否存在 --------------
    public static boolean existsClass(String clazz) {
        if (isZygoteState()) return false;
        return existsClass(clazz, ToolData.classLoader);
    }

    public static boolean existsClass(String clazz, ClassLoader classLoader) {
        if (classLoader == null) return false;
        return XposedHelpers.findClassIfExists(clazz, classLoader) != null;
    }

    // --------- 查找类 -----------
    public static Class<?> findClass(String name) {
        if (isZygoteState()) return null;
        return findClass(name, ToolData.classLoader);
    }

    public static Class<?> findClass(String name, ClassLoader classLoader) {
        return run(() -> XposedHelpers.findClass(name, classLoader))
                .orErr(null, e -> logE(tag(), "Class not found!", e));
    }

    //------------ 检查指定方法是否存在 --------------
    public static boolean existsMethod(String clazz, String name, Object... objs) {
        if (isZygoteState()) return false;
        return existsMethod(clazz, ToolData.classLoader, name, objs);
    }

    public static boolean existsMethod(String clazz, ClassLoader classLoader,
                                       String name, Object... objs) {
        Class<?> cl = XposedHelpers.findClassIfExists(clazz, classLoader);
        if (cl == null) return false;
        return run(() -> cl.getDeclaredMethod(name, arrayToClass(classLoader, objs))).isOk();
    }

    public static boolean existsAnyMethod(String clazz, String name) {
        if (isZygoteState()) return false;
        return existsAnyMethod(clazz, ToolData.classLoader, name);
    }

    public static boolean existsAnyMethod(String clazz, ClassLoader classLoader, String name) {
        Class<?> cl = XposedHelpers.findClassIfExists(clazz, classLoader);
        if (cl == null) return false;
        for (Method method : cl.getDeclaredMethods()) {
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

    public static Method findMethod(Class<?> clazz, String name, Class<?>... objects) {
        return run(() -> clazz.getDeclaredMethod(name, objects))
                .orErr(null, e -> logE(tag(), "Find method failed!", e));
    }

    public static ArrayList<Method> findAnyMethod(String clazz, String name) {
        return findAnyMethod(findClass(clazz), name);
    }

    public static ArrayList<Method> findAnyMethod(String clazz, ClassLoader classLoader, String name) {
        return findAnyMethod(findClass(clazz, classLoader), name);
    }

    public static ArrayList<Method> findAnyMethod(Class<?> clazz, String name) {
        return run(() -> {
            ArrayList<Method> methods = new ArrayList<>();
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(name)) methods.add(m);
            }
            return methods;
        }).orErr(new ArrayList<>(), e -> logE(tag(), "Find any method failed!", e));
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

    public static Constructor<?> findConstructor(Class<?> clazz, Class<?>... objects) {
        return run(() -> clazz.getDeclaredConstructor(objects))
                .orErr(null, e -> logE(tag(), "Find constructor failed!", e));
    }

    public static ArrayList<Constructor<?>> findAnyConstructor(String clazz) {
        return findAnyConstructor(findClass(clazz));
    }

    public static ArrayList<Constructor<?>> findAnyConstructor(String clazz, ClassLoader classLoader) {
        return findAnyConstructor(findClass(clazz, classLoader));
    }

    public static ArrayList<Constructor<?>> findAnyConstructor(Class<?> clazz) {
        return run(() -> new ArrayList<>(Arrays.asList(clazz.getDeclaredConstructors())))
                .orErr(new ArrayList<>(), e -> logE(tag(), "Find any constructor failed!", e));
    }

    //------------ 检查指定字段是否存在 --------------
    public static boolean existsField(String clazz, String name) {
        if (isZygoteState()) return false;
        return existsField(clazz, ToolData.classLoader, name);
    }

    public static boolean existsField(String clazz, ClassLoader classLoader, String name) {
        Class<?> cl = findClass(clazz, classLoader);
        return XposedHelpers.findFieldIfExists(cl, name) != null;
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
                .orErr(null, e -> logE(tag(), e));
    }

    // --------- 执行 hook -----------
    // --------- 普通方法 -------------
    public static UnHook hook(String clazz, String method, Object... params) {
        if (isZygoteState()) return null;
        return hook(clazz, ToolData.classLoader, method, params);
    }

    public static UnHook hook(String clazz, ClassLoader classLoader, String method, Object... params) {
        return hook(findClass(clazz, classLoader), method, toClassAsIAction(classLoader, params));
    }

    public static UnHook hook(Class<?> clazz, ClassLoader classLoader, String method, Object... params) {
        return hook(clazz, method, toClassAsIAction(classLoader, params));
    }

    public static UnHook hook(Class<?> clazz, String method, Object... params) {
        if (params == null) return null;
        if (params.length == 0 || !(params[params.length - 1] instanceof IAction)) {
            logW(tag(), "Params length == 0 or last param not is IAction! can't hook!!" + getStackTrace());
            return null;
        }

        return run(() -> {
            Class<?>[] classes = Arrays.stream(params)
                    .limit(params.length - 1)
                    .map(param -> (Class<?>) param).toArray(Class<?>[]::new);
            return hook(findMethod(clazz, method, classes), (IAction) params[params.length - 1]);
        }).orErr(new UnHook(null), e -> logE(tag(), e));
    }

    public static UnHookList hookAll(String clazz, String method, IAction iAction) {
        return hookAll(findClass(clazz), method, iAction);
    }

    public static UnHookList hookAll(String clazz, ClassLoader classLoader, String method, IAction iAction) {
        return hookAll(findClass(clazz, classLoader), method, iAction);
    }

    public static UnHookList hookAll(Class<?> clazz, String method, IAction iAction) {
        return hookAll(findAnyMethod(clazz, method), iAction);
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
        if (params == null) return null;
        if (params.length == 0 || !(params[params.length - 1] instanceof IAction)) {
            logE(tag(), "Params length == 0 or last param not is IAction! can't hook!!" + getStackTrace());
            return null;
        }

        return run(() -> {
            Class<?>[] classes = Arrays.stream(params)
                    .limit(params.length - 1)
                    .map(param -> (Class<?>) param).toArray(Class<?>[]::new);
            return hook(findConstructor(clazz, classes), (IAction) params[params.length - 1]);
        }).orErr(new UnHook(null), e -> logE(tag(), e));
    }

    public static UnHookList hookAll(String clazz, IAction iAction) {
        return hookAll(findAnyConstructor(clazz), iAction);
    }

    public static UnHookList hookAll(String clazz, ClassLoader classLoader, IAction iAction) {
        return hookAll(findAnyConstructor(clazz, classLoader), iAction);
    }

    public static UnHookList hookAll(Class<?> clazz, IAction iAction) {
        return hookAll(findAnyConstructor(clazz), iAction);
    }

    // ----------- 核心实现 ---------------
    public static UnHook hook(Member member, IAction iAction) {
        return run(() -> {
            UnHook unhook = new UnHook(
                    XposedBridge.hookMethod(member, createHook(tag(), iAction)));
            logD(tag(), "Success hook: " + member);
            return unhook;
        }).orErr(new UnHook(null), e -> logE(tag(), "Hook: [" + member + "], failed!", e));
    }

    public static UnHookList hookAll(ArrayList<?> members, IAction iAction) {
        UnHookList unhooks = new UnHookList();
        for (Object o : members) {
            if (o instanceof Method || o instanceof Constructor<?>) {
                run(() -> {
                    unhooks.add(XposedBridge.hookMethod((Member) o, createHook(tag(), iAction)));
                    logD(tag(), "Success hook: " + o);
                    return null;
                }).orErr(new UnHookList(), e -> logE(tag(), "Hook: [" + o + "], failed!", e));
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

        public void unHookAll() {
            for (XC_MethodHook.Unhook unhook : unhooks) {
                unhook.unhook();
            }
            unhooks.clear();
        }
    }

    // --------- 过滤方法 -----------
    public static ArrayList<Method> filterMethod(String clazz, IFilter iFilter) {
        return filterMethod(findClass(clazz), iFilter);
    }

    public static ArrayList<Method> filterMethod(String clazz, ClassLoader classLoader, IFilter iFilter) {
        return filterMethod(findClass(clazz, classLoader), iFilter);
    }

    public static ArrayList<Method> filterMethod(Class<?> clazz, IFilter iFilter) {
        return run(() -> {
            ArrayList<Method> methods = new ArrayList<>();
            for (Method m : clazz.getDeclaredMethods()) {
                if (iFilter.test(m)) {
                    methods.add(m);
                }
            }
            return methods;
        }).orErr(new ArrayList<>(), e -> logE(tag(), "Filter method failed!", e));
    }

    public static ArrayList<Constructor<?>> filterConstructor(String clazz, IFilter iFilter) {
        return filterConstructor(findClass(clazz), iFilter);
    }

    public static ArrayList<Constructor<?>> filterConstructor(String clazz, ClassLoader classLoader, IFilter iFilter) {
        return filterConstructor(findClass(clazz, classLoader), iFilter);
    }

    public static ArrayList<Constructor<?>> filterConstructor(Class<?> clazz, IFilter iFilter) {
        return run(() -> {
            ArrayList<Constructor<?>> constructors = new ArrayList<>();
            for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                if (iFilter.test(c)) {
                    constructors.add(c);
                }
            }
            return constructors;
        }).orErr(new ArrayList<>(), e -> logE(tag(), "Filter constructor failed!", e));
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
        try {
            Instant start = Instant.now();
            runnable.run();
            Instant end = Instant.now();
            return Duration.between(start, end).toMillis();
        } catch (Throwable e) {
            logE(tag(), "Code time consumption check failed!", e);
            return -1L;
        }
    }

    // ---------- 非静态 -----------
    public static <T> T callMethod(Object instance, String name, Object... objs) {
        return run(() -> (T) XposedHelpers.callMethod(instance, name, objs))
                .orErr(null, e -> logE(tag(), "Call method failed!", e));
    }

    public static <T> T getField(Object instance, String name) {
        return run(() -> (T) XposedHelpers.getObjectField(instance, name))
                .orErr(null, e -> logE(tag(), "Get field failed!", e));
    }

    public static <T> T getField(Object instance, Field field) {
        return run(() -> {
            field.setAccessible(true);
            return (T) field.get(instance);
        }).orErr(null, e -> logE(tag(), "Get field failed!", e));
    }

    public static boolean setField(Object instance, String name, Object value) {
        return run(() -> {
            XposedHelpers.setObjectField(instance, name, value);
            return true;
        }).orErr(false, e -> logE(tag(), "Set field failed!", e));
    }

    public static boolean setField(Object instance, Field field, Object value) {
        return run(() -> {
            field.setAccessible(true);
            field.set(instance, value);
            return true;
        }).orErr(false, e -> logE(tag(), "Set field failed!", e));
    }

    public static boolean setAdditionalInstanceField(Object instance, String key, Object value) {
        return run(() -> {
            XposedHelpers.setAdditionalInstanceField(instance, key, value);
            return true;
        }).orErr(false, e -> logE(tag(), "Set additional failed!", e));
    }

    public static <T> T getAdditionalInstanceField(Object instance, String key) {
        return run(() -> (T) XposedHelpers.getAdditionalInstanceField(instance, key))
                .orErr(null, e -> logE(tag(), "Get additional failed!", e));
    }

    public static boolean removeAdditionalInstanceField(Object instance, String key) {
        return run(() -> {
            XposedHelpers.removeAdditionalInstanceField(instance, key);
            return true;
        }).orErr(false, e -> logE(tag(), "Remove additional failed!", e));
    }

    // ---------- 静态 ------------
    public static <T> T newInstance(Class<?> clz, Object... objects) {
        return run(() -> (T) XposedHelpers.newInstance(clz, objects))
                .orErr(null, e -> logE(tag(), "New instance failed!", e));
    }

    public static <T> T newInstance(String clz, Object... objects) {
        return newInstance(findClass(clz), objects);
    }

    public static <T> T newInstance(String clz, ClassLoader classLoader, Object... objects) {
        return newInstance(findClass(clz, classLoader), objects);
    }

    public static <T> T callStaticMethod(Class<?> clz, String name, Object... objs) {
        return run(() -> (T) XposedHelpers.callStaticMethod(clz, name, objs))
                .orErr(null, e -> logE(tag(), "Call static method failed!", e));
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
        }).orErr(null, e -> logE(tag(), "Call static method failed!", e));
    }

    public static <T> T getStaticField(Class<?> clz, String name) {
        return run(() -> (T) XposedHelpers.getStaticObjectField(clz, name))
                .orErr(null, e -> logE(tag(), "Get static field failed!", e));
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
        }).orErr(null, e -> logE(tag(), "Get static field failed!", e));
    }

    public static boolean setStaticField(Class<?> clz, String name, Object value) {
        return run(() -> {
            XposedHelpers.setStaticObjectField(clz, name, value);
            return true;
        }).orErr(false, e -> logE(tag(), "Set static field failed!", e));
    }

    public static boolean setStaticField(Field field, Object value) {
        return run(() -> {
            field.setAccessible(true);
            field.set(null, value);
            return true;
        }).orErr(false, e -> logE(tag(), "Set static field failed!", e));
    }

    public static boolean setStaticField(String clz, String name, Object value) {
        return setStaticField(findClass(clz), name, value);
    }

    public static boolean setStaticField(String clz, ClassLoader classLoader, String name, Object value) {
        return setStaticField(findClass(clz, classLoader), name, value);
    }

    public static boolean setAdditionalStaticField(Class<?> clz, String key, Object value) {
        return run(() -> {
            XposedHelpers.setAdditionalStaticField(clz, key, value);
            return true;
        }).orErr(false, e -> logE(tag(), "Set additional static field failed!", e));
    }

    public static <T> T getAdditionalStaticField(Class<?> clz, String key) {
        return run(() -> (T) XposedHelpers.getAdditionalStaticField(clz, key))
                .orErr(null, e -> logE(tag(), "Get additional static field failed!", e));
    }

    public static boolean removeAdditionalStaticField(Class<?> clz, String key) {
        return run(() -> {
            XposedHelpers.removeAdditionalStaticField(clz, key);
            return true;
        }).orErr(false, e -> logE(tag(), "Remove additional static field failed!", e));
    }

    public static boolean setAdditionalStaticField(String clz, String key, Object value) {
        return setAdditionalStaticField(findClass(clz), key, value);
    }

    public static boolean setAdditionalStaticField(String clz, ClassLoader classLoader, String key, Object value) {
        return setAdditionalStaticField(findClass(clz, classLoader), key, value);
    }

    public static <T> T getAdditionalStaticField(String clz, String key) {
        return getAdditionalStaticField(findClass(clz), key);
    }

    public static <T> T getAdditionalStaticField(String clz, ClassLoader classLoader, String key) {
        return getAdditionalStaticField(findClass(key, classLoader), key);
    }

    public static boolean removeAdditionalStaticField(String clz, String key) {
        return removeAdditionalStaticField(findClass(clz), key);
    }

    public static boolean removeAdditionalStaticField(String clz, ClassLoader classLoader, String key) {
        return removeAdditionalStaticField(findClass(clz, classLoader), key);
    }

    private static boolean isZygoteState() {
        if (isZygote) {
            logW(tag(), "in zygote state, call method please set classloader!" + getStackTrace());
            return true;
        }
        return false;
    }

    private static String tag() {
        String tag = LogExpand.tag();
        if (tag == null) return "CoreTool";
        return tag;
    }
}
