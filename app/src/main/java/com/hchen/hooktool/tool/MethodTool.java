package com.hchen.hooktool.tool;

import android.support.annotation.Nullable;

import com.hchen.hooktool.hc.HCHook;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import de.robv.android.xposed.XposedHelpers;

/**
 * @hidden
 */
public class MethodTool extends FieldTool {

    public MethodTool() {
        // methodTool = this;
    }

    public HCHook getMethod(String method, Class<?>... obj) {
        if (!classSafe()) return hcHook;
        try {
            clear();
            methods.add(findClass.getMethod(method, obj));
            return hcHook;
        } catch (NoSuchMethodException e) {
            logE(useTAG(), "The method to get the claim failed: " + method + " obj: " +
                    Arrays.toString(obj) + " e: " + e);
        }
        return hcHook;
    }

    public HCHook getAnyMethod(String method) {
        if (!classSafe()) return hcHook;
        try {
            clear();
            Method[] methods = findClass.getDeclaredMethods();
            for (Method m : methods) {
                if (method.equals(m.getName())) {
                    this.methods.add(m);
                }
            }
        } catch (Throwable e) {
            logE(useTAG(), "Error getting match method: " + method + " e: " + e);
        }
        return hcHook;
    }

    public HCHook getConstructor(Class<?>... obj) {
        if (!classSafe()) return hcHook;
        try {
            clear();
            constructors.add(findClass.getConstructor(obj));
        } catch (NoSuchMethodException e) {
            logE(useTAG(), "The specified constructor could not be found: " + findClass.getName() +
                    " obj: " + Arrays.toString(obj) + " e: " + e);
        }
        return hcHook;
    }

    public HCHook getAnyConstructor() {
        if (!classSafe()) return hcHook;
        try {
            clear();
            Constructor<?>[] constructors = findClass.getConstructors();
            this.constructors.addAll(Arrays.asList(constructors));
        } catch (Throwable e) {
            logE(useTAG(), "The any constructor could not be found: " + findClass.getName() + " e: " + e);
        }
        return hcHook;
    }

    @Nullable
    public Object callMethod(String method, Object... args) {
        try {
            if (!thisObjectSafe()) return null;
            return XposedHelpers.callMethod(thisObject, method, args);
        } catch (Throwable e) {
            logE(useTAG(), "Error calling method: " + method + " args: " + Arrays.toString(args));
        }
        return null;
    }

    @Nullable
    public Object callStaticMethod(String method, Object... args) {
        if (!classSafe()) return null;
        try {
            return XposedHelpers.callStaticMethod(findClass, method, args);
        } catch (Throwable e) {
            logE(useTAG(), "Error calling method: " + method + " class: " + findClass
                    + " args: " + Arrays.toString(args));
        }
        return null;
    }

    private void clear() {
        if (!methods.isEmpty()) methods.clear();
        if (!constructors.isEmpty()) constructors.clear();
    }
}
