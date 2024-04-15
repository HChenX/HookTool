package com.hchen.hooktool.hc;

import static com.hchen.hooktool.hc.Safe.initSafe;

import com.hchen.hooktool.HookInit;
import com.hchen.hooktool.action.Action;
import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.tool.MethodTool;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class HCHook extends MethodTool {
    static {
        if (initSafe()) {
            try {
                TAG = HookInit.TAG;
                lpparam = HookInit.getLoadPackageParam();
                classLoader = HookInit.getClassLoader();
            } catch (Throwable e) {
                logE(TAG, e);

            }
        }
    }

    public HCHook() {
        safe = new Safe();
        hcHook = this;
    }

    public HCHook findClass(String className) {
        if (!initSafe()) return this;
        try {
            findClass = XposedHelpers.findClass(className, classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(TAG, "The specified class could not be found: " + className + " e: " + e);
            findClass = null;
        }
        safe.setFindClass(findClass);
        return this;
    }

    public HCHook findClassIfExists(String className) {
        if (!initSafe()) return this;
        findClass = XposedHelpers.findClassIfExists(className, classLoader);
        safe.setFindClass(findClass);
        return this;
    }

    public void after(IAction iAction) {
        this.iAction = iAction;
        for (Method m : methods) {
            XposedBridge.hookMethod(m, after);
            // XposedHelpers.findAndHookConstructor()
        }
    }

    public void before(IAction iAction) {
        this.iAction = iAction;
        for (Method m : methods) {
            XposedBridge.hookMethod(m, before);
        }
    }

    private final Action after = new Action() {
        @Override
        protected void after(MethodHookParam param) {
            if (iAction == null) {
                logE(TAG, "The callback is null!");
                return;
            }
            iAction.action(param);
        }
    };

    private final Action before = new Action() {
        @Override
        protected void before(MethodHookParam param) {
            if (iAction == null) {
                logE(TAG, "The callback is null!");
                return;
            }
            iAction.action(param);
        }
    };
}
