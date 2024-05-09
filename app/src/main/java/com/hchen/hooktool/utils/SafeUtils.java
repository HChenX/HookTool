package com.hchen.hooktool.utils;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.utils.DataUtils.TAG;

import com.hchen.hooktool.HookInit;

/**
 * @noinspection BooleanMethodIsAlwaysInverted
 */
public class SafeUtils {
    private final DataUtils utils;

    public SafeUtils(DataUtils utils) {
        this.utils = utils;
    }

    /**
     * 工具是否已经初始化。
     *
     * @return 布尔
     */
    public static boolean initSafe() {
        boolean init = HookInit.isInitDone();
        if (init) return true;
        logE(TAG, "HookInit not initialized!");
        return false;
    }

    public boolean classSafe() {
        // if (utils.findClass != null) return true;
        if (!utils.classes.isEmpty()) return true;
        logE(utils.getTAG(), "Class is null!");
        return false;
    }

    public boolean fieldSafe() {
        if (utils.findField != null) return true;
        logE(utils.getTAG(), "Field is null!");
        return false;
    }
}
