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

import com.hchen.hooktool.itool.IDynamic;
import com.hchen.hooktool.itool.IStatic;
import com.hchen.hooktool.utils.ConvertHelper;
import com.hchen.hooktool.utils.DataUtils;
import com.hchen.hooktool.utils.FieldObserver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class ExpandTool extends ConvertHelper implements IDynamic, IStatic {
    private final DataUtils utils;
    private final FieldObserver observer;
    private final boolean useFieldObserver = DataUtils.useFieldObserver;

    public ExpandTool(DataUtils dataUtils) {
        super(dataUtils);
        utils = dataUtils;
        observer = new FieldObserver(utils);
    }

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

    public ArrayList<Method> getMethod(Class<?> clazz, IFindMethod iFindMethod) {
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

    public ArrayList<Constructor<?>> getMethod(Class<?> clazz, IFindConstructor iFindConstructor) {
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
