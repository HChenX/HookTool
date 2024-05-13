package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import org.jetbrains.annotations.Nullable;

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

    public StaticTool setClass(Class<?> clzz) {
        findClass = clzz;
        return this;
    }

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
    public <T> Object newInstance(T objects) {
        if (findClass != null) {
            try {
                return XposedHelpers.newInstance(findClass, tToObject(objects));
            } catch (Throwable e) {
                logE(TAG, "new instance: " + e);
            }
        } else logW(TAG, "class is null, cant new instance.");
        return null;
    }

    /**
     * 请使用 new Object[]{} 传入参数。<br/>
     * 如果仅传入一个参数可以不使用 new Object[]{}<br/>
     * 这是为了规避泛型与可变参数的冲突。
     */
    @Nullable
    public <T, R> R callStaticMethod(String name, T objs) {
        if (findClass != null) {
            try {
                return (R) XposedHelpers.callStaticMethod(findClass, name, tToObject(objs));
            } catch (Throwable e) {
                logE(TAG, "call static method: " + e);
            }
        } else {
            logW(TAG, "class is null, cant call: " + name);
        }
        return null;
    }

    @Nullable
    public <T> T getStaticField(String name) {
        if (findClass != null) {
            try {
                return (T) XposedHelpers.getStaticObjectField(findClass, name);
            } catch (Throwable e) {
                logE(TAG, "get static field: " + e);
            }
        } else logW(TAG, "class is null, cant get field: " + name);
        return null;
    }

    public boolean setStaticField(String name, Object value) {
        if (findClass != null) {
            try {
                XposedHelpers.setStaticObjectField(findClass, name, value);
                return true;
            } catch (Throwable e) {
                logE(TAG, "set static field: " + e);
            }
        } else logW(TAG, "class is null, cant set field: " + name);
        return false;
    }

    public boolean setAdditionalStaticField(String key, Object value) {
        if (findClass != null) {
            try {
                XposedHelpers.setAdditionalStaticField(findClass, key, value);
                return true;
            } catch (Throwable e) {
                logE(TAG, "set additional static field: " + e);
            }
        } else logW(TAG, "class is null, cant additional: " + key);
        return false;
    }

    @Nullable
    public <T> T getAdditionalStaticField(String key) {
        if (findClass != null) {
            try {
                return (T) XposedHelpers.getAdditionalStaticField(findClass, key);
            } catch (Throwable e) {
                logE(TAG, "get additional static field: " + e);
            }
        } else logW(TAG, "class is null, cant get additional: " + key);
        return null;
    }

    public boolean removeAdditionalStaticField(String key) {
        if (findClass != null) {
            try {
                XposedHelpers.removeAdditionalStaticField(findClass, key);
                return true;
            } catch (Throwable e) {
                logE(TAG, "remove additional static field: " + e);
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
