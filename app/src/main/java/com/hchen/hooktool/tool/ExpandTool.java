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

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.itool.IDynamic;
import com.hchen.hooktool.itool.IMember;
import com.hchen.hooktool.itool.IStatic;
import com.hchen.hooktool.utils.ConvertHelper;
import com.hchen.hooktool.utils.DataUtils;
import com.hchen.hooktool.utils.FieldObserver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 杂项类
 */
public class ExpandTool extends ConvertHelper implements IDynamic, IStatic, IMember {
    private final FieldObserver observer;
    private final boolean useFieldObserver = DataUtils.useFieldObserver;

    public ExpandTool(DataUtils dataUtils) {
        super(dataUtils);
        observer = new FieldObserver(utils);
    }

    // --------- 查找类 -----------

    public Class<?> findClass(String name) {
        return findClass(name, utils.getClassLoader());
    }

    public Class<?> findClass(String name, ClassLoader classLoader) {
        try {
            if (classLoader == null) {
                logE(utils.getTAG(), "classLoader is null!");
                return null;
            }
            return XposedHelpers.findClass(name, classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(utils.getTAG(), e);
        }
        return null;
    }

    // --------- 查找方法 -----------

    /**
     * 查找指定类是否存在。
     */
    public boolean findClassIfExists(String clazz) {
        return findClassIfExists(clazz, utils.getClassLoader());
    }

    public boolean findClassIfExists(String clazz, ClassLoader classLoader) {
        try {
            if (classLoader == null) return false;
            return XposedHelpers.findClass(clazz, classLoader) != null;
        } catch (XposedHelpers.ClassNotFoundError _) {
        }
        return false;
    }


    //------------ 检查指定方法是否存在 --------------

    /**
     * 检查指定方法是否存在，不存在则返回 false。
     */
    public boolean findMethodIfExists(String clazz, String name, Object... ojbs) {
        return findMethodIfExists(clazz, utils.getClassLoader(), name, ojbs);
    }

    public boolean findMethodIfExists(String clazz, ClassLoader classLoader,
                                      String name, Object... ojbs) {
        try {
            Class<?> cl = XposedHelpers.findClassIfExists(clazz, classLoader);
            if (cl == null) return false;
            Class<?>[] classes = objectArrayToClassArray(classLoader, ojbs);
            cl.getDeclaredMethod(name, classes);
        } catch (NoSuchMethodException _) {
            return false;
        }
        return true;
    }

    /**
     * 检查指定方法名是否存在，不存在则返回 false。
     */
    public boolean findAnyMethodIfExists(String clazz, String name) {
        return findAnyMethodIfExists(clazz, utils.getClassLoader(), name);
    }

    public boolean findAnyMethodIfExists(String clazz, ClassLoader classLoader, String name) {
        Class<?> cl = XposedHelpers.findClassIfExists(clazz, classLoader);
        if (cl == null) return false;
        for (Method method : cl.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public Method findMethod(String clazz, String name, Object... objects) {
        return findMethod(findClass(clazz), name, objects);
    }

    public Method findMethod(String clazz, ClassLoader classLoader, String name, Object... objects) {
        return findMethod(findClass(clazz, classLoader), name, objects);
    }

    public Method findMethod(Class<?> clazz, String name, Object... objects) {
        try {
            if (clazz == null) {
                logW(utils.getTAG(), "class is null!");
                return null;
            }
            return clazz.getDeclaredMethod(name, objectArrayToClassArray(objects));
        } catch (NoSuchMethodException e) {
            logE(utils.getTAG(), e);
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
            logW(utils.getTAG(), "class is null!");
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
                logW(utils.getTAG(), "class is null!");
                return null;
            }
            return clazz.getConstructor(objectArrayToClassArray(objects));
        } catch (NoSuchMethodException e) {
            logE(utils.getTAG(), e);
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
            logW(utils.getTAG(), "class is null!");
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(clazz.getDeclaredConstructors()));
    }

    // --------- 查找字段 -----------

    /**
     * 查找指定字段是否存在，不存在返回 false
     */
    public boolean findFieldIfExists(String clazz, String name) {
        return findFieldIfExists(clazz, utils.getClassLoader(), name);
    }

    public boolean findFieldIfExists(String clazz, ClassLoader classLoader, String name) {
        Class<?> cl = utils.getExpandTool().findClass(clazz, classLoader);
        try {
            cl.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            logE(utils.getTAG(), e);
            return false;
        }
        return true;
    }

    public Field findField(String clazz, String name) {
        return findField(findClass(clazz), name);
    }

    public Field findField(String clazz, ClassLoader classLoader, String name) {
        return findField(findClass(clazz, classLoader), name);
    }

    public Field findField(Class<?> clazz, String name) {
        if (clazz == null) {
            logW(utils.getTAG(), "class is null!");
            return null;
        }
        try {
            return clazz.getField(name);
        } catch (NoSuchFieldException e) {
            logE(utils.getTAG(), e);
        }
        return null;
    }

    // --------- 执行 hook -----------

    public void hook(Member member, IAction iAction) {
        if (member == null || iAction == null) {
            logW(utils.getTAG(), "member or iAction is null, can't hook!");
            return;
        }
        try {
            XposedBridge.hookMethod(member, utils.getActionTool().hookTool(member, iAction));
        } catch (Throwable e) {
            logE(utils.getTAG(), "hook: [" + member + "], failed!", e);
        }
    }

    public void hook(ArrayList<?> members, IAction iAction) {
        for (Object o : members) {
            if (o instanceof Method || o instanceof Constructor<?>) {
                try {
                    XposedBridge.hookMethod((Member) o,
                            utils.getActionTool().hookTool((Member) o, iAction));
                } catch (Throwable e) {
                    logE(utils.getTAG(), "hook: [" + o + "], failed!", e);
                }
            }
        }
    }

    public IAction returnResult(final Object result) {
        return new IAction() {
            @Override
            public void before(ParamTool param) {
                param.setResult(result);
            }
        };
    }

    public IAction doNothing() {
        return new IAction() {
            @Override
            public void before(ParamTool param) {
                param.setResult(null);
            }
        };
    }

    // --------- 过滤方法 -----------

    public ArrayList<Method> filterMethod(Class<?> clazz, IFindMethod iFindMethod) {
        ArrayList<Method> methods = new ArrayList<>();
        for (Method m : clazz.getDeclaredMethods()) {
            try {
                methods.add(iFindMethod.doFind(m));
            } catch (Throwable e) {
                logE(utils.getTAG(), "do find method failed!", e);
            }
        }
        return methods;
    }

    public ArrayList<Constructor<?>> filterMethod(Class<?> clazz, IFindConstructor iFindConstructor) {
        ArrayList<Constructor<?>> constructors = new ArrayList<>();
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            try {
                constructors.add(iFindConstructor.doFind(c));
            } catch (Throwable e) {
                logE(utils.getTAG(), "do find constructor failed!", e);
            }
        }
        return constructors;
    }

    public interface IFindMethod {
        Method doFind(Method method);
    }

    public interface IFindConstructor {
        Constructor<?> doFind(Constructor<?> constructor);
    }

    // ---------- 非静态 -----------

    /**
     * 请使用 new Object[]{} 传入参数。<br/>
     * 如果仅传入一个参数可以不使用 new Object[]{}<br/>
     * 这是为了规避泛型与可变参数的冲突。
     */
    public <T, R> R callMethod(Object instance, String name, T ts) {
        try {
            return (R) XposedHelpers.callMethod(instance, name, genericToObjectArray(ts));
        } catch (Throwable e) {
            logE(utils.getTAG(), "call method failed!", e);
        }
        return null;
    }

    public <R> R callMethod(Object instance, String name) {
        return callMethod(instance, name, new Object[]{});
    }

    public <T> T getField(Object instance, String name) {
        try {
            return (T) XposedHelpers.getObjectField(instance, name);
        } catch (Throwable e) {
            logE(utils.getTAG(), "get field failed!", e);
        }
        return null;
    }

    public <T> T getField(Object instance, Field field) {
        try {
            field.setAccessible(true);
            return (T) field.get(instance);
        } catch (Throwable e) {
            logE(utils.getTAG(), "get field failed!", e);
        }
        return null;
    }

    public boolean setField(Object instance, String name, Object value) {
        try {
            XposedHelpers.setObjectField(instance, name, value);
            if (useFieldObserver)
                observer.dynamicObserver(instance, name, value);
            return true;
        } catch (Throwable e) {
            logE(utils.getTAG(), "set field failed!", e);
        }
        return false;
    }

    public boolean setField(Object instance, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(instance, value);
            if (useFieldObserver)
                observer.dynamicObserver(field, instance, value);
            return true;
        } catch (Throwable e) {
            logE(utils.getTAG(), "set field failed!", e);
        }
        return false;
    }

    public boolean setAdditionalInstanceField(Object instance, String key, Object value) {
        try {
            XposedHelpers.setAdditionalInstanceField(instance, key, value);
            return true;
        } catch (Throwable e) {
            logE(utils.getTAG(), "set additional failed!", e);
        }
        return false;
    }

    public <T> T getAdditionalInstanceField(Object instance, String key) {
        try {
            return (T) XposedHelpers.getAdditionalInstanceField(instance, key);
        } catch (Throwable e) {
            logE(utils.getTAG(), "get additional failed!", e);
        }
        return null;
    }

    public boolean removeAdditionalInstanceField(Object instance, String key) {
        try {
            XposedHelpers.removeAdditionalInstanceField(instance, key);
            return true;
        } catch (Throwable e) {
            logE(utils.getTAG(), "remove additional failed!", e);
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
                return (R) XposedHelpers.newInstance(clz, genericToObjectArray(objects));
            } catch (Throwable e) {
                logE(utils.getTAG(), "new instance failed!", e);
            }
        } else logW(utils.getTAG(), "class is null, can't new instance.");
        return null;
    }

    public <R> R newInstance(Class<?> clz) {
        return newInstance(clz, new Object[]{});
    }

    /**
     * 请使用 new Object[]{} 传入参数。<br/>
     * 如果仅传入一个参数可以不使用 new Object[]{}<br/>
     * 这是为了规避泛型与可变参数的冲突。
     */
    public <T, R> R callStaticMethod(Class<?> clz, String name, T objs) {
        if (clz != null) {
            try {
                return (R) XposedHelpers.callStaticMethod(clz, name, genericToObjectArray(objs));
            } catch (Throwable e) {
                logE(utils.getTAG(), "call static method failed!", e);
            }
        } else {
            logW(utils.getTAG(), "class is null, can't call: " + name);
        }
        return null;
    }

    public <R> R callStaticMethod(Class<?> clz, String name) {
        return callStaticMethod(clz, name, new Object[]{});
    }

    public <T> T getStaticField(Class<?> clz, String name) {
        if (clz != null) {
            try {
                return (T) XposedHelpers.getStaticObjectField(clz, name);
            } catch (Throwable e) {
                logE(utils.getTAG(), "get static field failed!", e);
            }
        } else logW(utils.getTAG(), "class is null, can't get field: " + name);
        return null;
    }

    public <T> T getStaticField(Field field) {
        try {
            field.setAccessible(true);
            return (T) field.get(null);
        } catch (Throwable e) {
            logE(utils.getTAG(), "get static field failed!", e);
        }
        return null;
    }

    public boolean setStaticField(Class<?> clz, String name, Object value) {
        if (clz != null) {
            try {
                XposedHelpers.setStaticObjectField(clz, name, value);
                if (useFieldObserver)
                    observer.staticObserver(clz, name, value);
                return true;
            } catch (Throwable e) {
                logE(utils.getTAG(), "set static field failed!", e);
            }
        } else logW(utils.getTAG(), "class is null, can't set field: " + name);
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
            logE(utils.getTAG(), "set static field failed!", e);
        }
        return false;
    }

    public boolean setAdditionalStaticField(Class<?> clz, String key, Object value) {
        if (clz != null) {
            try {
                XposedHelpers.setAdditionalStaticField(clz, key, value);
                return true;
            } catch (Throwable e) {
                logE(utils.getTAG(), "set additional static field failed!", e);
            }
        } else logW(utils.getTAG(), "class is null, can't additional: " + key);
        return false;
    }

    public <T> T getAdditionalStaticField(Class<?> clz, String key) {
        if (clz != null) {
            try {
                return (T) XposedHelpers.getAdditionalStaticField(clz, key);
            } catch (Throwable e) {
                logE(utils.getTAG(), "get additional static field failed!", e);
            }
        } else logW(utils.getTAG(), "class is null, can't get additional: " + key);
        return null;
    }

    public boolean removeAdditionalStaticField(Class<?> clz, String key) {
        if (clz != null) {
            try {
                XposedHelpers.removeAdditionalStaticField(clz, key);
                return true;
            } catch (Throwable e) {
                logE(utils.getTAG(), "remove additional static field failed!", e);
            }
        } else
            logW(utils.getTAG(), "class is null, can't remove additional: " + key);
        return false;
    }
}
