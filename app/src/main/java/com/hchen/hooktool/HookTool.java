package com.hchen.hooktool;

import com.hchen.hooktool.callback.IAction;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookTool extends HookLog {
    public static XC_LoadPackage.LoadPackageParam lpparam = null;
    public static ClassLoader classLoader = null;
    public static HookTool hookTool = null;
    public static String TAG = null;
    private Class<?> findClass = null;
    // private Method method = null;
    private IAction iAction = null;
    private final ArrayList<Method> methods = new ArrayList<>();
    private final ArrayList<Constructor<?>> constructors = new ArrayList<>();

    static {
        if (initSafe()) {
            try {
                TAG = HookInit.TAG;
                hookTool = new HookTool();
                lpparam = HookInit.getLoadPackageParam();
                classLoader = HookInit.getClassLoader();
            } catch (Throwable e) {
                logE(TAG, e);
            }
        }
    }

    public static HookTool findClass(String className) {
        if (!initSafe()) return hookTool;
        try {
            hookTool.findClass = XposedHelpers.findClass(className, classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(TAG, "The specified class could not be found: " + className + " e: " + e);
            hookTool.findClass = null;
        }
        return hookTool;
    }

    public static HookTool findClassIfExists(String className) {
        if (!initSafe()) return hookTool;
        hookTool.findClass = XposedHelpers.findClassIfExists(className, classLoader);
        return hookTool;
    }

    public HookTool getMethod(String method, Class<?>... obj) {
        if (!classSafe()) return this;
        try {
            this.methods.add(findClass.getMethod(method, obj));
            return this;
        } catch (NoSuchMethodException e) {
            logE(TAG, "The method to get the claim failed: " + method + " obj: " + Arrays.toString(obj) + " e: " + e);
        }
        return this;
    }

    public HookTool getAnyMethod(String method) {
        if (!classSafe()) return this;
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
        return this;
    }

    public HookTool getConstructor(Class<?>... obj) {
        if (!classSafe()) return this;
        try {
            constructors.add(findClass.getConstructor(obj));
        } catch (NoSuchMethodException e) {
            logE(TAG, "The specified constructor could not be found: " + findClass.getName() + " obj: " + Arrays.toString(obj) + " e: " + e);
        }
        return this;
    }

    public void after(IAction iAction) {
        this.iAction = iAction;
        for (Method m : methods) {
            XposedBridge.hookMethod(m, after);
            // XposedHelpers.findAndHookConstructor()
        }
        clear();
    }

    public void before(IAction iAction) {
        this.iAction = iAction;
        for (Method m : methods) {
            XposedBridge.hookMethod(m, before);
        }
        clear();
    }

    private final HookAction after = new HookAction() {
        @Override
        protected void after(MethodHookParam param) {
            if (iAction == null) {
                logE(TAG, "The callback is null!");
                return;
            }
            iAction.action(param);
        }
    };

    private final HookAction before = new HookAction() {
        @Override
        protected void before(MethodHookParam param) {
            if (iAction == null) {
                logE(TAG, "The callback is null!");
                return;
            }
            iAction.action(param);
        }
    };

    private static boolean initSafe() {
        boolean init = HookInit.isInitDone();
        if (init) return true;
        logE(TAG, "HookInit not initialized!");
        return false;
    }

    private static boolean classSafe() {
        if (hookTool.findClass != null) return true;
        logE(TAG, "Class is null!");
        return false;
    }

    private void clear() {
        findClass = null;
        methods.clear();
    }
}
