package com.hchen.hooktool;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.hc.HCHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class Main {
    public static void hello() {

    }

    public static void hook(){
        // HookTool.findClass("").getConstructor(null).after();
        XposedBridge.hookMethod(null, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
            }
        });
    }
}
