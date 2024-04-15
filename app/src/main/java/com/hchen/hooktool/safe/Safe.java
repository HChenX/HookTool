package com.hchen.hooktool.safe;

import com.hchen.hooktool.HookInit;
import com.hchen.hooktool.utils.Utils;

/**
 * @noinspection BooleanMethodIsAlwaysInverted
 */
public class Safe extends Utils {
    public static boolean initSafe() {
        boolean init = HookInit.isInitDone();
        if (init) return true;
        logE(TAG, "HookInit not initialized!");
        return false;
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

    public boolean paramSafe() {
        if (param != null) return true;
        logE(useTAG(), "Param is null!");
        return false;
    }

    public boolean thisObjectSafe() {
        if (thisObject != null) return true;
        logE(useTAG(), "ThisObject is null!");
        return false;
    }
}
