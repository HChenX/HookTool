package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.tool.UtilsTool.TAG;

import com.hchen.hooktool.HookInit;

/**
 * @noinspection BooleanMethodIsAlwaysInverted
 */
public class SafeTool {
    private final UtilsTool utils;

    public SafeTool(UtilsTool utils) {
        this.utils = utils;
    }

    protected static boolean initSafe() {
        boolean init = HookInit.isInitDone();
        if (init) return true;
        logE(TAG, "HookInit not initialized!");
        return false;
    }

    protected boolean classSafe() {
        if (utils.findClass != null) return true;
        logE(utils.useTAG(), "Class is null!");
        return false;
    }

    protected boolean fieldSafe() {
        if (utils.findField != null) return true;
        logE(utils.useTAG(), "Field is null!");
        return false;
    }

    protected boolean paramSafe() {
        if (utils.param != null) return true;
        logE(utils.useTAG(), "Param is null!");
        return false;
    }

    protected boolean thisObjectSafe() {
        if (utils.thisObject != null) return true;
        logE(utils.useTAG(), "ThisObject is null!");
        return false;
    }
}
