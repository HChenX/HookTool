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
import static com.hchen.hooktool.log.XposedLog.logW;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.itool.IDynamic;
import com.hchen.hooktool.itool.IMember;
import com.hchen.hooktool.itool.IStatic;
import com.hchen.hooktool.utils.ConvertHelper;
import com.hchen.hooktool.utils.FieldObserver;
import com.hchen.hooktool.utils.ToolData;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 核心工具
 */
public class CoreTool extends ConvertHelper implements IDynamic, IStatic, IMember {
    private final FieldObserver observer;
    private final boolean useFieldObserver = ToolData.useFieldObserver;

    public CoreTool(ToolData toolData) {
        super(toolData);
        observer = new FieldObserver(data);
    }

    //------------ 检查指定类是否存在 --------------

    /**
     * 查找指定类是否存在。
     */
    public boolean existsClass(String clazz) {
        return existsClass(clazz, data.getClassLoader());
    }

    public boolean existsClass(String clazz, ClassLoader classLoader) {
        if (classLoader == null) return false;
        return XposedHelpers.findClassIfExists(clazz, classLoader) != null;
    }

    // --------- 查找类 -----------

    public Class<?> findClass(String name) {
        return findClass(name, data.getClassLoader());
    }

    public Class<?> findClass(String name, ClassLoader classLoader) {
        try {
            if (classLoader == null) {
                logW(data.getTAG(), "classLoader is null! can't find class: " + name);
                return null;
            }
            return XposedHelpers.findClass(name, classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(data.getTAG(), e);
        }
        return null;
    }

    //------------ 检查指定方法是否存在 --------------

    /**
     * 检查指定方法是否存在，不存在则返回 false。
     */
    public boolean existsMethod(String clazz, String name, Object... ojbs) {
        return existsMethod(clazz, data.getClassLoader(), name, ojbs);
    }

    public boolean existsMethod(String clazz, ClassLoader classLoader,
                                String name, Object... ojbs) {
        try {
            Class<?> cl = XposedHelpers.findClassIfExists(clazz, classLoader);
            if (cl == null) return false;
            Class<?>[] classes = arrayToClass(classLoader, ojbs);
            cl.getDeclaredMethod(name, classes);
        } catch (NoSuchMethodException ignored) {
            return false;
        }
        return true;
    }

    /**
     * 检查指定方法名是否存在，不存在则返回 false。
     */
    public boolean existsAnyMethod(String clazz, String name) {
        return existsAnyMethod(clazz, data.getClassLoader(), name);
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
        return findMethod(findClass(clazz), name, objects);
    }

    public Method findMethod(String clazz, ClassLoader classLoader, String name, Object... objects) {
        return findMethod(findClass(clazz, classLoader), name, objects);
    }

    public Method findMethod(Class<?> clazz, String name, Object... objects) {
        try {
            if (clazz == null) {
                logW(data.getTAG(), "class is null! can't find method: " + name);
                return null;
            }
            return clazz.getDeclaredMethod(name, arrayToClass(objects));
        } catch (NoSuchMethodException e) {
            logE(data.getTAG(), e);
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
            logW(data.getTAG(), "class is null! can't find method: " + name);
            return methods;
        }
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name)) methods.add(m);
        }
        return methods;
    }

    // --------- 查找构造函数 -----------

    public Constructor<?> findConstructor(String clazz, Object... objects) {
        return findConstructor(findClass(clazz), objects);
    }

    public Constructor<?> findConstructor(String clazz, ClassLoader classLoader, Object... objects) {
        return findConstructor(findClass(clazz, classLoader), objects);
    }

    public Constructor<?> findConstructor(Class<?> clazz, Object... objects) {
        try {
            if (clazz == null) {
                logW(data.getTAG(), "class is null! can't find constructor!");
                return null;
            }
            return clazz.getDeclaredConstructor(arrayToClass(objects));
        } catch (NoSuchMethodException e) {
            logE(data.getTAG(), e);
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
            logW(data.getTAG(), "class is null! can't find constructor!");
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(clazz.getDeclaredConstructors()));
    }

    //------------ 检查指定字段是否存在 --------------

    /**
     * 查找指定字段是否存在，不存在返回 false
     */
    public boolean existsField(String clazz, String name) {
        return existsField(clazz, data.getClassLoader(), name);
    }

    public boolean existsField(String clazz, ClassLoader classLoader, String name) {
        Class<?> cl = data.getCoreTool().findClass(clazz, classLoader);
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
            logW(data.getTAG(), "class is null! can't find field: " + name);
            return null;
        }
        try {
            return XposedHelpers.findField(clazz, name);
        } catch (Throwable e) {
            logE(data.getTAG(), e);
        }
        return null;
    }

    // --------- 执行 hook -----------

    public XC_MethodHook.Unhook hook(String clazz, String method, Object... params) {
        return hook(clazz, data.getClassLoader(), method, params);
    }

    public XC_MethodHook.Unhook hook(String clazz, ClassLoader classLoader, String method, Object... params) {
        return hook(findClass(clazz, classLoader), method, params);
    }

    public XC_MethodHook.Unhook hook(Class<?> clazz, String method, Object... params) {
        if (params.length == 0 || !(params[params.length - 1] instanceof IAction)) {
            logE(data.getTAG(), "params length == 0 or last param not is IAction! can't hook!!");
            return null;
        }
        return hook(findMethod(clazz, method, params), (IAction) params[params.length - 1]);
    }

    public ArrayList<XC_MethodHook.Unhook> hook(String clazz, IAction iAction) {
        return hook(findAnyConstructor(clazz), iAction);
    }

    public XC_MethodHook.Unhook hook(String clazz, Object... params) {
        if (params.length == 0 || !(params[params.length - 1] instanceof IAction)) {
            logE(data.getTAG(), "params length == 0 or last param not is IAction! can't hook!!");
            return null;
        }
        return hook(findConstructor(clazz, params), (IAction) params[params.length - 1]);
    }

    public XC_MethodHook.Unhook hook(Member member, IAction iAction) {
        if (member == null || iAction == null) {
            logW(data.getTAG(), "member or iAction is null, can't hook!");
            return null;
        }
        try {
            XC_MethodHook.Unhook unhook = XposedBridge.hookMethod(member, data.getActionTool().createHook(member, iAction));
            logD(data.getTAG(), "success hook: " + member);
            return unhook;
        } catch (Throwable e) {
            logE(data.getTAG(), "hook: [" + member + "], failed!", e);
        }
        return null;
    }

    public ArrayList<XC_MethodHook.Unhook> hook(ArrayList<?> members, IAction iAction) {
        ArrayList<XC_MethodHook.Unhook> unhooks = new ArrayList<>();
        for (Object o : members) {
            if (o instanceof Method || o instanceof Constructor<?>) {
                try {
                    unhooks.add(XposedBridge.hookMethod((Member) o,
                            data.getActionTool().createHook((Member) o, iAction)));
                    logD(data.getTAG(), "success hook: " + o);
                } catch (Throwable e) {
                    logE(data.getTAG(), "hook: [" + o + "], failed!", e);
                }
            }
        }
        return unhooks;
    }

    public IAction returnResult(final Object result) {
        return new IAction() {
            @Override
            public void before() throws Throwable {
                setResult(result);
            }
        };
    }

    public IAction doNothing() {
        return new IAction() {
            @Override
            public void before() throws Throwable {
                setResult(null);
            }
        };
    }

    // --------- 解除 hook ---------

    public boolean unHook(XC_MethodHook.Unhook unhook) {
        try {
            unhook.unhook();
            return true;
        } catch (Throwable ignored) {
            return false;
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

    public ArrayList<Method> filterMethod(Class<?> clazz, IFindMethod iFindMethod) {
        ArrayList<Method> methods = new ArrayList<>();
        for (Method m : clazz.getDeclaredMethods()) {
            try {
                if (iFindMethod.test(m)) {
                    methods.add(m);
                }
            } catch (Throwable e) {
                logE(data.getTAG(), "do find method failed!", e);
            }
        }
        return methods;
    }

    public ArrayList<Constructor<?>> filterMethod(Class<?> clazz, IFindConstructor iFindConstructor) {
        ArrayList<Constructor<?>> constructors = new ArrayList<>();
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            try {
                if (iFindConstructor.test(c)) {
                    constructors.add(c);
                }
            } catch (Throwable e) {
                logE(data.getTAG(), "do find constructor failed!", e);
            }
        }
        return constructors;
    }

    public interface IFindMethod {
        boolean test(Method method);
    }

    public interface IFindConstructor {
        boolean test(Constructor<?> constructor);
    }

    // --------- 打印堆栈 ----------
    
    public String getStackTrace() {
        StringBuilder stringBuilder = new StringBuilder();
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        Arrays.stream(stackTraceElements).forEach(new Consumer<StackTraceElement>() {
            @Override
            public void accept(StackTraceElement stackTraceElement) {
                String clazz = stackTraceElement.getClassName();
                String method = stackTraceElement.getMethodName();
                String field = stackTraceElement.getFileName();
                int line = stackTraceElement.getLineNumber();
                stringBuilder.append("\nat ").append(clazz).append(".")
                        .append(method).append("(")
                        .append(field).append(":")
                        .append(line).append(")");
            }
        });
        return stringBuilder.toString();
    }

    // ---------- 非静态 -----------

    /**
     * 请使用 new Object[]{} 传入参数。<br/>
     * 如果仅传入一个参数可以不使用 new Object[]{}<br/>
     * 这是为了规避泛型与可变参数的冲突。
     */
    public <T, R> R callMethod(Object instance, String name, T ts) {
        if (instance == null) {
            logW(data.getTAG(), "instance is null! can't call method: " + name);
            return null;
        }
        try {
            return (R) XposedHelpers.callMethod(instance, name, genericToArray(ts));
        } catch (Throwable e) {
            logE(data.getTAG(), "call method failed!", e);
        }
        return null;
    }

    public <R> R callMethod(Object instance, String name) {
        return callMethod(instance, name, new Object[]{});
    }

    public <T> T getField(Object instance, String name) {
        if (instance == null) {
            logW(data.getTAG(), "instance is null! can't set field: " + name);
            return null;
        }
        try {
            return (T) XposedHelpers.getObjectField(instance, name);
        } catch (Throwable e) {
            logE(data.getTAG(), "get field failed!", e);
        }
        return null;
    }

    public <T> T getField(Object instance, Field field) {
        try {
            if (instance == null) {
                logW(data.getTAG(), "instance is null! can't get field: " + field.getName());
                return null;
            }
            field.setAccessible(true);
            return (T) field.get(instance);
        } catch (Throwable e) {
            logE(data.getTAG(), "get field failed!", e);
        }
        return null;
    }

    public boolean setField(Object instance, String name, Object value) {
        if (instance == null) {
            logW(data.getTAG(), "instance is null! can't set field: " + name);
            return false;
        }
        try {
            XposedHelpers.setObjectField(instance, name, value);
            if (useFieldObserver)
                observer.dynamicObserver(instance, name, value);
            return true;
        } catch (Throwable e) {
            logE(data.getTAG(), "set field failed!", e);
        }
        return false;
    }

    public boolean setField(Object instance, Field field, Object value) {
        try {
            if (instance == null) {
                logW(data.getTAG(), "instance is null! can't set field: " + field.getName());
                return false;
            }
            field.setAccessible(true);
            field.set(instance, value);
            if (useFieldObserver)
                observer.dynamicObserver(field, instance, value);
            return true;
        } catch (Throwable e) {
            logE(data.getTAG(), "set field failed!", e);
        }
        return false;
    }

    public boolean setAdditionalInstanceField(Object instance, String key, Object value) {
        if (instance == null) {
            logW(data.getTAG(), "instance is null! can't remove additional: " + key);
            return false;
        }
        try {
            XposedHelpers.setAdditionalInstanceField(instance, key, value);
            return true;
        } catch (Throwable e) {
            logE(data.getTAG(), "set additional failed!", e);
        }
        return false;
    }

    public <T> T getAdditionalInstanceField(Object instance, String key) {
        if (instance == null) {
            logW(data.getTAG(), "instance is null! can't get additional: " + key);
            return null;
        }
        try {
            return (T) XposedHelpers.getAdditionalInstanceField(instance, key);
        } catch (Throwable e) {
            logE(data.getTAG(), "get additional failed!", e);
        }
        return null;
    }

    public boolean removeAdditionalInstanceField(Object instance, String key) {
        if (instance == null) {
            logW(data.getTAG(), "instance is null! can't remove additional: " + key);
            return false;
        }
        try {
            XposedHelpers.removeAdditionalInstanceField(instance, key);
            return true;
        } catch (Throwable e) {
            logE(data.getTAG(), "remove additional failed!", e);
        }
        return false;
    }

    // ---------- 静态 ------------

    /**
     * 请使用 new Object[]{} 传入参数。<br/>
     * 如果仅传入一个参数可以不使用 new Object[]{}<br/>
     * 这是为了规避泛型与可变参数的冲突。
     */
    public <T, R> R newInstance(Class<?> clz, T objects) {
        if (clz != null) {
            try {
                return (R) XposedHelpers.newInstance(clz, genericToArray(objects));
            } catch (Throwable e) {
                logE(data.getTAG(), "new instance failed!", e);
            }
        } else logW(data.getTAG(), "class is null, can't new instance.");
        return null;
    }

    public <R> R newInstance(Class<?> clz) {
        return newInstance(clz, new Object[]{});
    }
    
    public <T, R> R newInstance(String clz, T objects) {
        return newInstance(findClass(clz), objects);
    }
    
    public <T, R> R newInstance(String clz, ClassLoader classLoader, T objects) {
        return newInstance(findClass(clz, classLoader), objects);
    }
    
    public <R> R newInstance(String clz) {
        return newInstance(findClass(clz));
    }
    
    public <R> R newInstance(String clz, ClassLoader classLoader) {
        return newInstance(findClass(clz, classLoader));
    }

    /**
     * 请使用 new Object[]{} 传入参数。<br/>
     * 如果仅传入一个参数可以不使用 new Object[]{}<br/>
     * 这是为了规避泛型与可变参数的冲突。
     */
    public <T, R> R callStaticMethod(Class<?> clz, String name, T objs) {
        if (clz != null) {
            try {
                return (R) XposedHelpers.callStaticMethod(clz, name, genericToArray(objs));
            } catch (Throwable e) {
                logE(data.getTAG(), "call static method failed!", e);
            }
        } else {
            logW(data.getTAG(), "class is null, can't call static method: " + name);
        }
        return null;
    }

    public <R> R callStaticMethod(Class<?> clz, String name) {
        return callStaticMethod(clz, name, new Object[]{});
    }
    
    public <T, R> R callStaticMethod(String clz, String name, T objs) {
        return callStaticMethod(findClass(clz), name, objs);
    }
    
    public <T, R> R callStaticMethod(String clz, ClassLoader classLoader, String name, T objs) {
        return callStaticMethod(findClass(clz, classLoader), name, objs);
    }
    
    public <R> R callStaticMethod(String clz, String name) {
        return callStaticMethod(findClass(clz), name);
    }
    
    public <R> R callStaticMethod(String clz, ClassLoader classLoader, String name) {
        return callStaticMethod(findClass(clz, classLoader), name);
    }

    public <T> T getStaticField(Class<?> clz, String name) {
        if (clz != null) {
            try {
                return (T) XposedHelpers.getStaticObjectField(clz, name);
            } catch (Throwable e) {
                logE(data.getTAG(), "get static field failed!", e);
            }
        } else logW(data.getTAG(), "class is null, can't get static field: " + name);
        return null;
    }

    public <T> T getStaticField(Field field) {
        try {
            field.setAccessible(true);
            return (T) field.get(null);
        } catch (Throwable e) {
            logE(data.getTAG(), "get static field failed!", e);
        }
        return null;
    }
    
    public <T> T getStaticField(String clz, String name) {
        return getStaticField(findClass(clz), name);
    }
    
    public <T> T getStaticField(String clz, ClassLoader classLoader, String name) {
        return getStaticField(findClass(clz, classLoader), name);
    }

    public boolean setStaticField(Class<?> clz, String name, Object value) {
        if (clz != null) {
            try {
                XposedHelpers.setStaticObjectField(clz, name, value);
                if (useFieldObserver)
                    observer.staticObserver(clz, name, value);
                return true;
            } catch (Throwable e) {
                logE(data.getTAG(), "set static field failed!", e);
            }
        } else logW(data.getTAG(), "class is null, can't set static field: " + name);
        return false;
    }

    public boolean setStaticField(Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(null, value);
            if (useFieldObserver)
                observer.staticObserver(field, value);
            return true;
        } catch (Throwable e) {
            logE(data.getTAG(), "set static field failed!", e);
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
                logE(data.getTAG(), "set additional static field failed!", e);
            }
        } else logW(data.getTAG(), "class is null, can't static additional: " + key);
        return false;
    }

    public <T> T getAdditionalStaticField(Class<?> clz, String key) {
        if (clz != null) {
            try {
                return (T) XposedHelpers.getAdditionalStaticField(clz, key);
            } catch (Throwable e) {
                logE(data.getTAG(), "get additional static field failed!", e);
            }
        } else logW(data.getTAG(), "class is null, can't get static additional: " + key);
        return null;
    }

    public boolean removeAdditionalStaticField(Class<?> clz, String key) {
        if (clz != null) {
            try {
                XposedHelpers.removeAdditionalStaticField(clz, key);
                return true;
            } catch (Throwable e) {
                logE(data.getTAG(), "remove additional static field failed!", e);
            }
        } else
            logW(data.getTAG(), "class is null, can't remove static additional: " + key);
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
