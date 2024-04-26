package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;

import android.support.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import de.robv.android.xposed.XposedHelpers;

/**
 * @hidden
 */
public class MethodTool {
    private final UtilsTool utils;
    private final SafeTool safe;

    public MethodTool(UtilsTool utils) {
        this.utils = utils;
        this.safe = utils.safeTool;
        // methodTool = this;
    }

    public MethodTool getMethod(String method, Class<?>... obj) {
        if (!safe.classSafe()) return getMethodTool();
        try {
            clear();
            utils.methods.add(utils.findClass.getMethod(method, obj));
            return getMethodTool();
        } catch (NoSuchMethodException e) {
            logE(getTAG(), "The method to get the claim failed: " + method + " obj: " +
                    Arrays.toString(obj) + " e: " + e);
        }
        return getMethodTool();
    }

    public MethodTool getAnyMethod(String method) {
        if (!safe.classSafe()) return getMethodTool();
        try {
            clear();
            Method[] methods = utils.findClass.getDeclaredMethods();
            for (Method m : methods) {
                if (method.equals(m.getName())) {
                    utils.methods.add(m);
                }
            }
        } catch (Throwable e) {
            logE(getTAG(), "Error getting match method: " + method + " e: " + e);
        }
        return getMethodTool();
    }

    public MethodTool getConstructor(Class<?>... obj) {
        if (!safe.classSafe()) return getMethodTool();
        try {
            clear();
            utils.constructors.add(utils.findClass.getConstructor(obj));
        } catch (NoSuchMethodException e) {
            logE(getTAG(), "The specified constructor could not be found: " + utils.findClass.getName() +
                    " obj: " + Arrays.toString(obj) + " e: " + e);
        }
        return getMethodTool();
    }

    public MethodTool getAnyConstructor() {
        if (!safe.classSafe()) return getMethodTool();
        try {
            clear();
            Constructor<?>[] constructors = utils.findClass.getConstructors();
            utils.constructors.addAll(Arrays.asList(constructors));
        } catch (Throwable e) {
            logE(getTAG(), "The any constructor could not be found: " + utils.findClass.getName() + " e: " + e);
        }
        return getMethodTool();
    }

    @Nullable
    public Object callMethod(String method, Object... args) {
        try {
            if (!safe.thisObjectSafe()) return null;
            return XposedHelpers.callMethod(utils.thisObject, method, args);
        } catch (Throwable e) {
            logE(getTAG(), "Error calling method: " + method + " args: " + Arrays.toString(args));
        }
        return null;
    }

    @Nullable
    public Object callStaticMethod(String method, Object... args) {
        if (!safe.classSafe()) return null;
        try {
            return XposedHelpers.callStaticMethod(utils.findClass, method, args);
        } catch (Throwable e) {
            logE(getTAG(), "Error calling method: " + method + " class: " + utils.findClass
                    + " args: " + Arrays.toString(args));
        }
        return null;
    }

    private void clear() {
        if (!utils.methods.isEmpty()) utils.methods.clear();
        if (!utils.constructors.isEmpty()) utils.constructors.clear();
    }

    private String getTAG() {
        return utils.useTAG();
    }

    private MethodTool getMethodTool() {
        MethodTool methodTool = utils.methodTool;
        if (methodTool == null)
            throw new RuntimeException(getTAG() + ": MethodTool is null!!");
        return methodTool;
    }
}
