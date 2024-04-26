package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.tool.SafeTool.initSafe;
import static com.hchen.hooktool.tool.UtilsTool.classLoader;

import android.support.annotation.Nullable;

import java.util.Arrays;

import de.robv.android.xposed.XposedHelpers;

public class ClassTool {
    private final UtilsTool utils;
    private final SafeTool safe;

    public ClassTool(UtilsTool utils) {
        this.utils = utils;
        this.safe = utils.safeTool;
        utils.classTool = this;
    }

    public ClassTool findClass(String className) {
        if (!initSafe()) return getClassTool();
        try {
            utils.findClass = XposedHelpers.findClass(className,
                    utils.mClassLoader == null ? classLoader : utils.mClassLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(getTAG(), "The specified class could not be found: " + className + " e: " + e);
            utils.findClass = null;
        }
        return getClassTool();
    }

    public ClassTool findClassIfExists(String className) {
        if (!initSafe()) return getClassTool();
        utils.findClass = XposedHelpers.findClassIfExists(className,
                utils.mClassLoader == null ? classLoader : utils.mClassLoader);
        return getClassTool();
    }

    @Nullable
    public Object newInstance(Object... args) {
        if (!safe.classSafe()) return null;
        try {
            return XposedHelpers.newInstance(utils.findClass, args);
        } catch (Throwable e) {
            logE(getTAG(), "Error creating instance: " + utils.findClass + " args: " + Arrays.toString(args));
        }
        return null;
    }

    private String getTAG() {
        return utils.useTAG();
    }

    private ClassTool getClassTool() {
        ClassTool classTool = utils.classTool;
        if (classTool == null)
            throw new RuntimeException(getTAG() + ": ClassTool is null!!");
        return classTool;
    }
}
