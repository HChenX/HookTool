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

import static com.hchen.hooktool.log.XposedLog.logD;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logI;
import static com.hchen.hooktool.log.XposedLog.logW;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.data.ToolData;
import com.hchen.hooktool.itool.IDynamic;
import com.hchen.hooktool.itool.IFilter;
import com.hchen.hooktool.itool.IMember;
import com.hchen.hooktool.itool.IStatic;
import com.hchen.hooktool.log.LogExpand;

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

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 核心工具
 * <p>
 * Core tool
 * 
 * @author 焕晨HChen
 */
public class CoreTool implements IDynamic, IStatic, IMember {
    private final ToolData data;

    public CoreTool(ToolData toolData) {
        data = toolData;
    }

    //------------ 检查指定类是否存在 --------------

    /**
     * 查找指定类是否存在。
     * <p>
     * Find if the specified class exists.
     */
    public boolean existsClass(String clazz) {
        if (data.isZygoteState()) return false;
        return existsClass(clazz, ToolData.classLoader);
    }

    public boolean existsClass(String clazz, ClassLoader classLoader) {
        if (classLoader == null) return false;
        return XposedHelpers.findClassIfExists(clazz, classLoader) != null;
    }

    // --------- 查找类 -----------
    public Class<?> findClass(String name) {
        if (data.isZygoteState()) return null;
        return findClass(name, ToolData.classLoader);
    }

    public Class<?> findClass(String name, ClassLoader classLoader) {
        try {
            if (classLoader == null) {
                logW(data.tag(), "CoreTool: classloader is null, " +
                        "can't find class: [" + name + "]" + getStackTrace());
                return null;
            }
            return XposedHelpers.findClass(name, classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(data.tag(), e);
        }
        return null;
    }

    //------------ 检查指定方法是否存在 --------------

    /**
     * 检查指定方法是否存在，不存在则返回 false。
     * <p>
     * Check whether the specified method exists, and returns false if it does not.
     */
    public boolean existsMethod(String clazz, String name, Object... objs) {
        if (data.isZygoteState()) return false;
        return existsMethod(clazz, ToolData.classLoader, name, objs);
    }

    public boolean existsMethod(String clazz, ClassLoader classLoader,
                                String name, Object... objs) {
        try {
            Class<?> cl = XposedHelpers.findClassIfExists(clazz, classLoader);
            if (cl == null) return false;
            cl.getDeclaredMethod(name, data.convertHelper.arrayToClass(classLoader, objs));
        } catch (NoSuchMethodException ignored) {
            return false;
        }
        return true;
    }

    /**
     * 检查指定方法名是否存在，不存在则返回 false。
     * <p>
     * Check whether the specified method name exists, and return false if it does not.
     */
    public boolean existsAnyMethod(String clazz, String name) {
        if (data.isZygoteState()) return false;
        return existsAnyMethod(clazz, ToolData.classLoader, name);
    }

    public boolean existsAnyMethod(String clazz, ClassLoader classLoader, String name) {
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
    public Method findMethod(String clazz, String name, Object... objects) {
        return findMethod(findClass(clazz), name, data.convertHelper.arrayToClass(objects));
    }

    public Method findMethod(String clazz, ClassLoader classLoader, String name, Object... objects) {
        return findMethod(findClass(clazz, classLoader), name, data.convertHelper.arrayToClass(classLoader, objects));
    }

    public Method findMethod(Class<?> clazz, ClassLoader classLoader, String name, Object... objects) {
        return findMethod(clazz, name, data.convertHelper.arrayToClass(classLoader, objects));
    }

    public Method findMethod(Class<?> clazz, String name, Class<?>... objects) {
        try {
            if (clazz == null) {
                logW(data.tag(), "CoreTool: class is null, can't find method: [" + name + "]" + getStackTrace());
                return null;
            }
            return clazz.getDeclaredMethod(name, objects);
        } catch (NoSuchMethodException e) {
            logE(data.tag(), e);
        }
        return null;
    }

    public ArrayList<Method> findAnyMethod(String clazz, String name) {
        return findAnyMethod(findClass(clazz), name);
    }

    public ArrayList<Method> findAnyMethod(String clazz, ClassLoader classLoader, String name) {
        return findAnyMethod(findClass(clazz, classLoader), name);
    }

    public ArrayList<Method> findAnyMethod(Class<?> clazz, String name) {
        ArrayList<Method> methods = new ArrayList<>();
        if (clazz == null) {
            logW(data.tag(), "CoreTool: class is null, can't find any method: [" + name + "]" + getStackTrace());
            return methods;
        }
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name)) methods.add(m);
        }
        return methods;
    }

    // --------- 查找构造函数 -----------
    public Constructor<?> findConstructor(String clazz, Object... objects) {
        return findConstructor(findClass(clazz), data.convertHelper.arrayToClass(objects));
    }

    public Constructor<?> findConstructor(String clazz, ClassLoader classLoader, Object... objects) {
        return findConstructor(findClass(clazz, classLoader), data.convertHelper.arrayToClass(classLoader, objects));
    }

    public Constructor<?> findConstructor(Class<?> clazz, ClassLoader classLoader, Object... objects) {
        return findConstructor(clazz, data.convertHelper.arrayToClass(classLoader, objects));
    }

    public Constructor<?> findConstructor(Class<?> clazz, Class<?>... objects) {
        try {
            if (clazz == null) {
                logW(data.tag(), "CoreTool: class is null, can't find constructor!" + getStackTrace());
                return null;
            }
            return clazz.getDeclaredConstructor(objects);
        } catch (NoSuchMethodException e) {
            logE(data.tag(), e);
        }
        return null;
    }

    public ArrayList<Constructor<?>> findAnyConstructor(String clazz) {
        return findAnyConstructor(findClass(clazz));
    }

    public ArrayList<Constructor<?>> findAnyConstructor(String clazz, ClassLoader classLoader) {
        return findAnyConstructor(findClass(clazz, classLoader));
    }

    public ArrayList<Constructor<?>> findAnyConstructor(Class<?> clazz) {
        if (clazz == null) {
            logW(data.tag(), "CoreTool: class is null, can't find any constructor!" + getStackTrace());
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(clazz.getDeclaredConstructors()));
    }

    //------------ 检查指定字段是否存在 --------------

    /**
     * 查找指定字段是否存在，不存在返回 false。
     * <p>
     * If the specified field exists, it returns false if it does not.
     */
    public boolean existsField(String clazz, String name) {
        if (data.isZygoteState()) return false;
        return existsField(clazz, ToolData.classLoader, name);
    }

    public boolean existsField(String clazz, ClassLoader classLoader, String name) {
        Class<?> cl = data.coreTool.findClass(clazz, classLoader);
        return XposedHelpers.findFieldIfExists(cl, name) != null;
    }

    // --------- 查找字段 -----------
    public Field findField(String clazz, String name) {
        return findField(findClass(clazz), name);
    }

    public Field findField(String clazz, ClassLoader classLoader, String name) {
        return findField(findClass(clazz, classLoader), name);
    }

    public Field findField(Class<?> clazz, String name) {
        if (clazz == null) {
            logW(data.tag(), "CoreTool: class is null, can't find field: [" + name + "]" + getStackTrace());
            return null;
        }
        try {
            return XposedHelpers.findField(clazz, name);
        } catch (Throwable e) {
            logE(data.tag(), e);
        }
        return null;
    }

    // --------- 执行 hook -----------
    // --------- 普通方法 -------------
    public UnHook hook(String clazz, String method, Object... params) {
        if (data.isZygoteState()) return null;
        return hook(clazz, ToolData.classLoader, method, params);
    }

    public UnHook hook(String clazz, ClassLoader classLoader, String method, Object... params) {
        return hook(findClass(clazz, classLoader), method, data.convertHelper.toClassAsIAction(classLoader, params));
    }

    public UnHook hook(Class<?> clazz, ClassLoader classLoader, String method, Object... params) {
        return hook(clazz, method, data.convertHelper.toClassAsIAction(classLoader, params));
    }

    public UnHook hook(Class<?> clazz, String method, Object... params) {
        if (params == null) return null;
        if (params.length == 0 || !(params[params.length - 1] instanceof IAction)) {
            logW(data.tag(), "CoreTool: params length == 0 or last param not is IAction! can't hook!!" + getStackTrace());
            return null;
        }

        try {
            Class<?>[] classes = Arrays.stream(params)
                    .limit(params.length - 1)
                    .map(param -> (Class<?>) param).toArray(Class<?>[]::new);
            return hook(findMethod(clazz, method, classes), (IAction) params[params.length - 1]);
        } catch (Throwable e) {
            logE(data.tag(), e);
        }
        return null;
    }

    public UnHookList hookAll(String clazz, String method, IAction iAction) {
        return hookAll(findClass(clazz), method, iAction);
    }

    public UnHookList hookAll(String clazz, ClassLoader classLoader, String method, IAction iAction) {
        return hookAll(findClass(clazz, classLoader), method, iAction);
    }

    public UnHookList hookAll(Class<?> clazz, String method, IAction iAction) {
        return hookAll(findAnyMethod(clazz, method), iAction);
    }

    // --------- 构造函数 ------------
    public UnHook hook(String clazz, Object... params) {
        return hook(findClass(clazz), params);
    }

    public UnHook hook(String clazz, ClassLoader classLoader, Object... params) {
        return hook(findClass(clazz, classLoader), data.convertHelper.toClassAsIAction(classLoader, params));
    }

    public UnHook hook(Class<?> clazz, ClassLoader classLoader, Object... params) {
        return hook(clazz, data.convertHelper.toClassAsIAction(classLoader, params));
    }

    public UnHook hook(Class<?> clazz, Object... params) {
        if (params == null) return null;
        if (params.length == 0 || !(params[params.length - 1] instanceof IAction)) {
            logE(data.tag(), "CoreTool: params length == 0 or last param not is IAction! can't hook!!" + getStackTrace());
            return null;
        }

        try {
            Class<?>[] classes = Arrays.stream(params)
                    .limit(params.length - 1)
                    .map(param -> (Class<?>) param).toArray(Class<?>[]::new);
            return hook(findConstructor(clazz, classes), (IAction) params[params.length - 1]);
        } catch (Throwable e) {
            logE(data.tag(), e);
        }
        return null;
    }

    public UnHookList hookAll(String clazz, IAction iAction) {
        return hookAll(findAnyConstructor(clazz), iAction);
    }

    public UnHookList hookAll(String clazz, ClassLoader classLoader, IAction iAction) {
        return hookAll(findAnyConstructor(clazz, classLoader), iAction);
    }

    public UnHookList hookAll(Class<?> clazz, IAction iAction) {
        return hookAll(findAnyConstructor(clazz), iAction);
    }

    // ----------- 核心实现 ---------------
    public UnHook hook(Member member, IAction iAction) {
        try {
            UnHook unhook = new UnHook(
                    XposedBridge.hookMethod(member, data.hookFactory.createHook(iAction)));
            logD(data.tag(), "CoreTool: Success Hook: " + member);
            return unhook;
        } catch (Throwable e) {
            logE(data.tag(), "CoreTool: hook: [" + member + "], failed!", e);
        }
        return null;
    }

    public UnHookList hookAll(ArrayList<?> members, IAction iAction) {
        UnHookList unhooks = new UnHookList();
        for (Object o : members) {
            if (o instanceof Method || o instanceof Constructor<?>) {
                try {
                    unhooks.add(XposedBridge.hookMethod((Member) o,
                            data.hookFactory.createHook(iAction)));
                    logD(data.tag(), "CoreTool: Success Hook: " + o);
                } catch (Throwable e) {
                    logE(data.tag(), "CoreTool: hook: [" + o + "], failed!", e);
                }
            }
        }
        return unhooks;
    }

    // --------- 快捷方法 -----------
    public IAction returnResult(final Object result) {
        return new IAction() {
            @Override
            public void before() {
                setResult(result);
            }
        };
    }

    public IAction doNothing() {
        return new IAction() {
            @Override
            public void before() {
                setResult(null);
            }
        };
    }

    // --------- 解除 hook ---------
    public static class UnHook {
        private final XC_MethodHook.Unhook unhook;

        private UnHook(XC_MethodHook.Unhook unhook) {
            this.unhook = unhook;
        }

        public void unHook() {
            unhook.unhook();
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

    public boolean unHook(Member hookMember, XC_MethodHook xcMethodHook) {
        try {
            XposedBridge.unhookMethod(hookMember, xcMethodHook);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // --------- 过滤方法 -----------
    public ArrayList<Method> filterMethod(String clazz, IFilter iFilter) {
        return filterMethod(findClass(clazz), iFilter);
    }

    public ArrayList<Method> filterMethod(String clazz, ClassLoader classLoader, IFilter iFilter) {
        return filterMethod(findClass(clazz, classLoader), iFilter);
    }

    public ArrayList<Method> filterMethod(Class<?> clazz, IFilter iFilter) {
        ArrayList<Method> methods = new ArrayList<>();
        for (Method m : clazz.getDeclaredMethods()) {
            try {
                if (iFilter.test(m)) {
                    methods.add(m);
                }
            } catch (Throwable e) {
                logE(data.tag(), "CoreTool: filter method failed!", e);
            }
        }
        return methods;
    }

    public ArrayList<Constructor<?>> filterConstructor(String clazz, IFilter iFilter) {
        return filterConstructor(findClass(clazz), iFilter);
    }

    public ArrayList<Constructor<?>> filterConstructor(String clazz, ClassLoader classLoader, IFilter iFilter) {
        return filterConstructor(findClass(clazz, classLoader), iFilter);
    }

    public ArrayList<Constructor<?>> filterConstructor(Class<?> clazz, IFilter iFilter) {
        ArrayList<Constructor<?>> constructors = new ArrayList<>();
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            try {
                if (iFilter.test(c)) {
                    constructors.add(c);
                }
            } catch (Throwable e) {
                logE(data.tag(), "CoreTool: filter constructor failed!", e);
            }
        }
        return constructors;
    }

    // --------- 打印堆栈 ----------
    public String getStackTrace(boolean autoLog) {
        String task = getStackTrace();
        if (autoLog) {
            logI(data.tag(), task);
            return "";
        }
        return task;
    }

    public String getStackTrace() {
        return LogExpand.getStackTrace();
    }

    // --------- 耗时检查 -----------

    /**
     * 返回指定代码逻辑的耗时，单位是 ms。
     * <p>
     * Return the time consumption of the specified code logic, in milliseconds.
     */
    public long timeConsumption(Runnable runnable) {
        try {
            Instant start = Instant.now();
            runnable.run();
            Instant end = Instant.now();
            return Duration.between(start, end).toMillis();
        } catch (Throwable e) {
            logE(data.tag(), "CoreTool: code time consumption check failed!", e);
            return -1L;
        }
    }

    // ---------- 非静态 -----------
    public <T> T callMethod(Object instance, String name, Object... objs) {
        if (instance == null) {
            logW(data.tag(), "CoreTool: instance is null, can't call method: " + name + getStackTrace());
            return null;
        }
        try {
            return (T) XposedHelpers.callMethod(instance, name, objs);
        } catch (Throwable e) {
            logE(data.tag(), "CoreTool: call method failed!", e);
        }
        return null;
    }

    public <T> T getField(Object instance, String name) {
        if (instance == null) {
            logW(data.tag(), "CoreTool: instance is null, can't set field: " + name + getStackTrace());
            return null;
        }
        try {
            return (T) XposedHelpers.getObjectField(instance, name);
        } catch (Throwable e) {
            logE(data.tag(), "CoreTool: get field failed!", e);
        }
        return null;
    }

    public <T> T getField(Object instance, Field field) {
        try {
            if (instance == null) {
                logW(data.tag(), "CoreTool: instance is null, can't get field: " + field.getName() + getStackTrace());
                return null;
            }
            field.setAccessible(true);
            return (T) field.get(instance);
        } catch (Throwable e) {
            logE(data.tag(), "CoreTool: get field failed!", e);
        }
        return null;
    }

    public boolean setField(Object instance, String name, Object value) {
        if (instance == null) {
            logW(data.tag(), "CoreTool: instance is null, can't set field: " + name + getStackTrace());
            return false;
        }
        try {
            XposedHelpers.setObjectField(instance, name, value);
            return true;
        } catch (Throwable e) {
            logE(data.tag(), "CoreTool: set field failed!", e);
        }
        return false;
    }

    public boolean setField(Object instance, Field field, Object value) {
        try {
            if (instance == null) {
                logW(data.tag(), "CoreTool: instance is null, can't set field: " + field.getName() + getStackTrace());
                return false;
            }
            field.setAccessible(true);
            field.set(instance, value);
            return true;
        } catch (Throwable e) {
            logE(data.tag(), "CoreTool: set field failed!", e);
        }
        return false;
    }

    public boolean setAdditionalInstanceField(Object instance, String key, Object value) {
        if (instance == null) {
            logW(data.tag(), "CoreTool: instance is null, can't remove additional: " + key + getStackTrace());
            return false;
        }
        try {
            XposedHelpers.setAdditionalInstanceField(instance, key, value);
            return true;
        } catch (Throwable e) {
            logE(data.tag(), "CoreTool: set additional failed!", e);
        }
        return false;
    }

    public <T> T getAdditionalInstanceField(Object instance, String key) {
        if (instance == null) {
            logW(data.tag(), "CoreTool: instance is null, can't get additional: " + key + getStackTrace());
            return null;
        }
        try {
            return (T) XposedHelpers.getAdditionalInstanceField(instance, key);
        } catch (Throwable e) {
            logE(data.tag(), "CoreTool: get additional failed!", e);
        }
        return null;
    }

    public boolean removeAdditionalInstanceField(Object instance, String key) {
        if (instance == null) {
            logW(data.tag(), "CoreTool: instance is null, can't remove additional: " + key + getStackTrace());
            return false;
        }
        try {
            XposedHelpers.removeAdditionalInstanceField(instance, key);
            return true;
        } catch (Throwable e) {
            logE(data.tag(), "CoreTool: remove additional failed!", e);
        }
        return false;
    }

    // ---------- 静态 ------------
    public <T> T newInstance(Class<?> clz, Object... objects) {
        if (clz != null) {
            try {
                return (T) XposedHelpers.newInstance(clz, objects);
            } catch (Throwable e) {
                logE(data.tag(), "CoreTool: new instance failed!", e);
            }
        } else logW(data.tag(), "CoreTool: class is null, can't new instance." + getStackTrace());
        return null;
    }

    public <T> T newInstance(String clz, Object... objects) {
        return newInstance(findClass(clz), objects);
    }

    public <T> T newInstance(String clz, ClassLoader classLoader, Object... objects) {
        return newInstance(findClass(clz, classLoader), objects);
    }

    public <T> T callStaticMethod(Class<?> clz, String name, Object... objs) {
        if (clz != null) {
            try {
                return (T) XposedHelpers.callStaticMethod(clz, name, objs);
            } catch (Throwable e) {
                logE(data.tag(), "CoreTool: call static method failed!", e);
            }
        } else {
            logW(data.tag(), "CoreTool: class is null, can't call static method: " + name + getStackTrace());
        }
        return null;
    }

    public <T> T callStaticMethod(String clz, String name, Object... objs) {
        return callStaticMethod(findClass(clz), name, objs);
    }

    public <T> T callStaticMethod(String clz, ClassLoader classLoader, String name, Object... objs) {
        return callStaticMethod(findClass(clz, classLoader), name, objs);
    }

    public <T> T callStaticMethod(Method method, Object... objs) {
        try {
            method.setAccessible(true);
            return (T) method.invoke(null, objs);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logE(data.tag(), "CoreTool: call static method failed!", e);
        }
        return null;
    }

    public <T> T getStaticField(Class<?> clz, String name) {
        if (clz != null) {
            try {
                return (T) XposedHelpers.getStaticObjectField(clz, name);
            } catch (Throwable e) {
                logE(data.tag(), "CoreTool: get static field failed!", e);
            }
        } else
            logW(data.tag(), "CoreTool: class is null, can't get static field: " + name + getStackTrace());
        return null;
    }

    public <T> T getStaticField(String clz, String name) {
        return getStaticField(findClass(clz), name);
    }

    public <T> T getStaticField(String clz, ClassLoader classLoader, String name) {
        return getStaticField(findClass(clz, classLoader), name);
    }

    public <T> T getStaticField(Field field) {
        try {
            field.setAccessible(true);
            return (T) field.get(null);
        } catch (Throwable e) {
            logE(data.tag(), "CoreTool: get static field failed!", e);
        }
        return null;
    }

    public boolean setStaticField(Class<?> clz, String name, Object value) {
        if (clz != null) {
            try {
                XposedHelpers.setStaticObjectField(clz, name, value);
                return true;
            } catch (Throwable e) {
                logE(data.tag(), "CoreTool: set static field failed!", e);
            }
        } else
            logW(data.tag(), "CoreTool: class is null, can't set static field: " + name + getStackTrace());
        return false;
    }

    public boolean setStaticField(Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(null, value);
            return true;
        } catch (Throwable e) {
            logE(data.tag(), "CoreTool: set static field failed!", e);
        }
        return false;
    }

    public boolean setStaticField(String clz, String name, Object value) {
        return setStaticField(findClass(clz), name, value);
    }

    public boolean setStaticField(String clz, ClassLoader classLoader, String name, Object value) {
        return setStaticField(findClass(clz, classLoader), name, value);
    }

    public boolean setAdditionalStaticField(Class<?> clz, String key, Object value) {
        if (clz != null) {
            try {
                XposedHelpers.setAdditionalStaticField(clz, key, value);
                return true;
            } catch (Throwable e) {
                logE(data.tag(), "CoreTool: set additional static field failed!", e);
            }
        } else
            logW(data.tag(), "CoreTool: class is null, can't static additional: " + key + getStackTrace());
        return false;
    }

    public <T> T getAdditionalStaticField(Class<?> clz, String key) {
        if (clz != null) {
            try {
                return (T) XposedHelpers.getAdditionalStaticField(clz, key);
            } catch (Throwable e) {
                logE(data.tag(), "CoreTool: get additional static field failed!", e);
            }
        } else
            logW(data.tag(), "CoreTool: class is null, can't get static additional: " + key + getStackTrace());
        return null;
    }

    public boolean removeAdditionalStaticField(Class<?> clz, String key) {
        if (clz != null) {
            try {
                XposedHelpers.removeAdditionalStaticField(clz, key);
                return true;
            } catch (Throwable e) {
                logE(data.tag(), "CoreTool: remove additional static field failed!", e);
            }
        } else
            logW(data.tag(), "CoreTool: class is null, can't remove static additional: " + key + getStackTrace());
        return false;
    }

    public boolean setAdditionalStaticField(String clz, String key, Object value) {
        return setAdditionalStaticField(findClass(clz), key, value);
    }

    public boolean setAdditionalStaticField(String clz, ClassLoader classLoader, String key, Object value) {
        return setAdditionalStaticField(findClass(clz, classLoader), key, value);
    }

    public <T> T getAdditionalStaticField(String clz, String key) {
        return getAdditionalStaticField(findClass(clz), key);
    }

    public <T> T getAdditionalStaticField(String clz, ClassLoader classLoader, String key) {
        return getAdditionalStaticField(findClass(key, classLoader), key);
    }

    public boolean removeAdditionalStaticField(String clz, String key) {
        return removeAdditionalStaticField(findClass(clz), key);
    }

    public boolean removeAdditionalStaticField(String clz, ClassLoader classLoader, String key) {
        return removeAdditionalStaticField(findClass(clz, classLoader), key);
    }
}
