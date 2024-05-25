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

import androidx.annotation.Nullable;

import de.robv.android.xposed.XposedHelpers;

/**
 * 静态操作专用工具。
 */
public class StaticTool {
    private final String TAG;
    private ClassLoader classLoader;
    private Class<?> findClass = null;

    public StaticTool(String TAG) {
        this.TAG = TAG;
    }

    public StaticTool(ClassLoader classLoader, String tag) {
        TAG = tag;
        this.classLoader = classLoader;
    }

    public StaticTool setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * 直接设置类
     */
    public StaticTool setClass(Class<?> clzz) {
        findClass = clzz;
        return this;
    }

    /**
     * 查找指定的类
     */
    public StaticTool findClass(String name) {
        try {
            if (classLoader == null) {
                logE(TAG, "classLoader is null!");
                return this;
            }
            findClass = XposedHelpers.findClass(name, classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(TAG, e);
            findClass = null;
        }
        return this;
    }

    @Nullable
    public Class<?> getFindClass() {
        return findClass;
    }

    /**
     * 请使用 new Object[]{} 传入参数。<br/>
     * 如果仅传入一个参数可以不使用 new Object[]{}<br/>
     * 这是为了规避泛型与可变参数的冲突。
     */
    @Nullable
    public <T, R> R newInstance(T objects) {
        return newInstance(findClass, objects);
    }

    @Nullable
    public <T, R> R newInstance(Class<?> claz, T objs) {
        if (claz != null) {
            try {
                return (R) XposedHelpers.newInstance(claz, tToObject(objs));
            } catch (Throwable e) {
                logE(TAG, "new instance failed!", e);
            }
        } else logW(TAG, "class is null, cant new instance.");
        return null;
    }

    @Nullable
    public <R> R newInstance() {
        return newInstance(new Object[]{});
    }

    @Nullable
    public <R> R newInstance(Class<?> clz) {
        return newInstance(clz, new Object[]{});
    }

    /**
     * 请使用 new Object[]{} 传入参数。<br/>
     * 如果仅传入一个参数可以不使用 new Object[]{}<br/>
     * 这是为了规避泛型与可变参数的冲突。
     */
    @Nullable
    public <T, R> R callStaticMethod(String name, T objs) {
        return callStaticMethod(findClass, name, objs);
    }

    @Nullable
    public <T, R> R callStaticMethod(Class<?> clz, String name, T objs) {
        if (clz != null) {
            try {
                return (R) XposedHelpers.callStaticMethod(clz, name, tToObject(objs));
            } catch (Throwable e) {
                logE(TAG, "call static method failed!", e);
            }
        } else {
            logW(TAG, "class is null, cant call: " + name);
        }
        return null;
    }

    @Nullable
    public <R> R callStaticMethod(String name) {
        return callStaticMethod(name, new Object[]{});
    }

    @Nullable
    public <R> R callStaticMethod(Class<?> clz, String name) {
        return callStaticMethod(clz, name, new Object[]{});
    }

    @Nullable
    public <T> T getStaticField(String name) {
        return getStaticField(findClass, name);
    }

    @Nullable
    public <T> T getStaticField(Class<?> clz, String name) {
        if (clz != null) {
            try {
                return (T) XposedHelpers.getStaticObjectField(clz, name);
            } catch (Throwable e) {
                logE(TAG, "get static field failed!", e);
            }
        } else logW(TAG, "class is null, cant get field: " + name);
        return null;
    }

    public boolean setStaticField(String name, Object value) {
        return setStaticField(findClass, name, value);
    }

    public boolean setStaticField(Class<?> clz, String name, Object value) {
        if (clz != null) {
            try {
                XposedHelpers.setStaticObjectField(clz, name, value);
                return true;
            } catch (Throwable e) {
                logE(TAG, "set static field failed!", e);
            }
        } else logW(TAG, "class is null, cant set field: " + name);
        return false;
    }

    public boolean setAdditionalStaticField(String key, Object value) {
        return setAdditionalStaticField(findClass, key, value);
    }

    public boolean setAdditionalStaticField(Class<?> clz, String key, Object value) {
        if (clz != null) {
            try {
                XposedHelpers.setAdditionalStaticField(clz, key, value);
                return true;
            } catch (Throwable e) {
                logE(TAG, "set additional static field failed!", e);
            }
        } else logW(TAG, "class is null, cant additional: " + key);
        return false;
    }

    @Nullable
    public <T> T getAdditionalStaticField(String key) {
        return getAdditionalStaticField(findClass, key);
    }

    @Nullable
    public <T> T getAdditionalStaticField(Class<?> clz, String key) {
        if (clz != null) {
            try {
                return (T) XposedHelpers.getAdditionalStaticField(clz, key);
            } catch (Throwable e) {
                logE(TAG, "get additional static field failed!", e);
            }
        } else logW(TAG, "class is null, cant get additional: " + key);
        return null;
    }

    public boolean removeAdditionalStaticField(String key) {
        return removeAdditionalStaticField(findClass, key);
    }

    public boolean removeAdditionalStaticField(Class<?> clz, String key) {
        if (clz != null) {
            try {
                XposedHelpers.removeAdditionalStaticField(clz, key);
                return true;
            } catch (Throwable e) {
                logE(TAG, "remove additional static field failed!", e);
            }
        } else
            logW(TAG, "class is null, cant remove additional: " + key);
        return false;
    }

    private <T> Object[] tToObject(T ts) {
        if (ts instanceof Object[] objects) {
            return objects;
        }
        return new Object[]{ts};
    }
}
