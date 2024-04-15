package com.hchen.hooktool.hc;

import static com.hchen.hooktool.log.XposedLog.logE;

import com.hchen.hooktool.HookInit;

/**
 * @hidden
 */
public class Safe {
    private static final String TAG = HookInit.TAG;
    private Class<?> findClass = null;

    public static boolean initSafe() {
        boolean init = HookInit.isInitDone();
        if (init) return true;
        logE(TAG, "HookInit not initialized!");
        return false;
    }

    public void setFindClass(Class<?> findClass) {
        this.findClass = findClass;
    }

    public boolean classSafe() {
        if (findClass != null) return true;
        logE(TAG, "Class is null!");
        return false;
    }
}
