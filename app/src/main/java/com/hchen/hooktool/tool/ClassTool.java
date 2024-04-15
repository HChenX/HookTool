package com.hchen.hooktool.tool;

import static com.hchen.hooktool.safe.Safe.initSafe;

import android.support.annotation.Nullable;

import com.hchen.hooktool.hc.HCHook;

import java.util.Arrays;

import de.robv.android.xposed.XposedHelpers;

public class ClassTool extends MethodTool {
    public HCHook findClass(String className) {
        if (!initSafe()) return hcHook;
        try {
            findClass = XposedHelpers.findClass(className, classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(useTAG(), "The specified class could not be found: " + className + " e: " + e);
            findClass = null;
        }
        safe.setFindClass(findClass);
        return hcHook;
    }

    public HCHook findClassIfExists(String className) {
        if (!initSafe()) return hcHook;
        findClass = XposedHelpers.findClassIfExists(className, classLoader);
        safe.setFindClass(findClass);
        return hcHook;
    }

    @Nullable
    public Object newInstance(Object... args) {
        if (!safe.classSafe()) return null;
        try {
            return XposedHelpers.newInstance(findClass, args);
        } catch (Throwable e) {
            logE(TAG, "Error creating instance: " + findClass + " args: " + Arrays.toString(args));
        }
        return null;
    }
}
