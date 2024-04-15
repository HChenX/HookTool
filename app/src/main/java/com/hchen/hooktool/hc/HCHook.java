package com.hchen.hooktool.hc;

import static com.hchen.hooktool.safe.Safe.initSafe;

import com.hchen.hooktool.HookInit;
import com.hchen.hooktool.action.Action;
import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.safe.Safe;
import com.hchen.hooktool.tool.ClassTool;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;

public class HCHook extends ClassTool {
    static {
        if (initSafe()) {
            try {
                TAG = HookInit.getTAG();
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

    private final Action after = new Action(useTAG()) {
        @Override
        protected void after(MethodHookParam param) {
            if (iAction == null) {
                logE(useTAG(), "The callback is null!");
                return;
            }
            hcHook.param = param;
            hcHook.thisObject = param.thisObject;
            iAction.action(param, hcHook);
        }
    };

    private final Action before = new Action(useTAG()) {
        @Override
        protected void before(MethodHookParam param) {
            if (iAction == null) {
                logE(useTAG(), "The callback is null!");
                return;
            }
            hcHook.param = param;
            hcHook.thisObject = param.thisObject;
            iAction.action(param, hcHook);
        }
    };
}
