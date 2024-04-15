package com.hchen.hooktool.safe;

import static com.hchen.hooktool.log.XposedLog.logE;

import com.hchen.hooktool.HookInit;

import java.lang.reflect.Field;

/**
 * @noinspection BooleanMethodIsAlwaysInverted
 */
public class Safe {
    private static final String TAG = HookInit.getTAG();
    private String mTAG = null;
    private Class<?> findClass = null;
    private Field findField = null;

    public static boolean initSafe() {
        boolean init = HookInit.isInitDone();
        if (init) return true;
        logE(TAG, "HookInit not initialized!");
        return false;
    }

    public void setFindClass(Class<?> findClass) {
        this.findClass = findClass;
    }

    public void setFindField(Field field) {
        this.findField = field;
    }

    public boolean classSafe() {
        if (findClass != null) return true;
        logE(useTAG(), "Class is null!");
        return false;
    }

    public boolean fieldSafe() {
        if (findField != null) return true;
        logE(useTAG(), "Field is null!");
        return false;
    }

    public void setMyTAG(String tag) {
        mTAG = tag;
    }

    private String useTAG() {
        if (mTAG != null) return mTAG;
        return TAG;
    }
}
