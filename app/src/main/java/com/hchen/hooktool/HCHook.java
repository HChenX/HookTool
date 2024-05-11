package com.hchen.hooktool;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.utils.DataUtils.TAG;

import com.hchen.hooktool.tool.ActionTool;
import com.hchen.hooktool.tool.ClassTool;
import com.hchen.hooktool.tool.FieldTool;
import com.hchen.hooktool.tool.MethodTool;
import com.hchen.hooktool.utils.DataUtils;

public class HCHook {
    private final DataUtils utils;

    static {
        initSafe();
        try {
            TAG = HookInit.getTAG();
            DataUtils.lpparam = HookInit.getLoadPackageParam();
            DataUtils.classLoader = HookInit.getClassLoader();
        } catch (Throwable e) {
            logE(TAG, e);
        }
    }

    /**
     * 实例化本类开始使用
     */
    public HCHook() {
        utils = new DataUtils();
        utils.hcHook = this;
        utils.actionTool = new ActionTool(utils);
        utils.classTool = new ClassTool(utils);
        utils.fieldTool = new FieldTool(utils);
        utils.methodTool = new MethodTool(utils);
    }

    public ClassTool classTool() {
        return utils.getClassTool();
    }

    public MethodTool methodTool() {
        return utils.getMethodTool();
    }

    public FieldTool fieldTool() {
        return utils.getFieldTool();
    }

    /* 设置自定义 ClassLoader */
    public HCHook setClassLoader(ClassLoader classLoader) {
        utils.mCustomClassLoader = classLoader;
        return utils.getHCHook();
    }

    public static void initSafe() {
        if (!HookInit.isInitDone())
            throw new RuntimeException(HookInit.getTAG() + " HookInit not initialized!");
    }
}
