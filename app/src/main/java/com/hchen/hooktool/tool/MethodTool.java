package com.hchen.hooktool.tool;

import com.hchen.hooktool.hc.HCHook;
import com.hchen.hooktool.utils.Utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @hidden
 */
public class MethodTool extends Utils {

    public MethodTool() {
        // methodTool = this;
    }

    public HCHook getMethod(String method, Class<?>... obj) {
        if (!safe.classSafe()) return hcHook;
        try {
            methods.add(findClass.getMethod(method, obj));
            return hcHook;
        } catch (NoSuchMethodException e) {
            logE(TAG, "The method to get the claim failed: " + method + " obj: " +
                    Arrays.toString(obj) + " e: " + e);
        }
        return hcHook;
    }

    public HCHook getAnyMethod(String method) {
        if (!safe.classSafe()) return hcHook;
        try {
            Method[] methods = findClass.getDeclaredMethods();
            for (Method m : methods) {
                if (method.equals(m.getName())) {
                    this.methods.add(m);
                }
            }
        } catch (Throwable e) {
            logE(TAG, "Error getting match method: " + method + " e: " + e);
        }
        return hcHook;
    }

    public HCHook getConstructor(Class<?>... obj) {
        if (!safe.classSafe()) return hcHook;
        try {
            constructors.add(findClass.getConstructor(obj));
        } catch (NoSuchMethodException e) {
            logE(TAG, "The specified constructor could not be found: " + findClass.getName() +
                    " obj: " + Arrays.toString(obj) + " e: " + e);
        }
        return hcHook;
    }

    public HCHook getAnyConstructor() {
        if (!safe.classSafe()) return hcHook;
        try {
            Constructor<?>[] constructors = findClass.getConstructors();
            this.constructors.addAll(Arrays.asList(constructors));
        } catch (Throwable e) {
            logE(TAG, "The any constructor could not be found: " + findClass.getName() + " e: " + e);
        }
        return hcHook;
    }

    public HCHook clear() {
        if (!methods.isEmpty()) methods.clear();
        if (!constructors.isEmpty()) constructors.clear();
        return hcHook;
    }
}
