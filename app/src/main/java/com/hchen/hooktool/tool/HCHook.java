package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.tool.UtilsTool.TAG;
import static de.robv.android.xposed.callbacks.XCallback.PRIORITY_HIGHEST;

import com.hchen.hooktool.HookInit;
import com.hchen.hooktool.action.Action;
import com.hchen.hooktool.callback.IAction;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;

public class HCHook {
    private IAction iAction;
    private final UtilsTool utils;

    static {
        if (SafeTool.initSafe()) {
            try {
                TAG = HookInit.getTAG();
                UtilsTool.lpparam = HookInit.getLoadPackageParam();
                UtilsTool.classLoader = HookInit.getClassLoader();
            } catch (Throwable e) {
                logE(TAG, e);
            }
        }
    }

    public HCHook() {
        utils = new UtilsTool();
        utils.hcHook = this;
        utils.safeTool = new SafeTool(utils);
        utils.classTool = new ClassTool(utils);
        utils.fieldTool = new FieldTool(utils);
        utils.methodTool = new MethodTool(utils);
    }

    public HCHook setClassLoader(ClassLoader classLoader) {
        utils.mClassLoader = classLoader;
        return getHCHook();
    }

    public void after(IAction iAction) {
        this.iAction = iAction;
        for (Method m : utils.methods) {
            XposedBridge.hookMethod(m, after);
            // XposedHelpers.findAndHookConstructor()
        }
    }

    public void before(IAction iAction) {
        this.iAction = iAction;
        for (Method m : utils.methods) {
            XposedBridge.hookMethod(m, before);
        }
    }

    public void returnConstant(final Object result) {
        for (Method m : utils.methods) {
            XposedBridge.hookMethod(m,
                    new Action(getTAG()) {
                        @Override
                        protected void before(MethodHookParam param) {
                            param.setResult(result);
                        }
                    }
            );
        }
    }

    public void doNothing() {
        for (Method m : utils.methods) {
            XposedBridge.hookMethod(m, DO_NOTHING);
        }
    }

    private final Action DO_NOTHING = new Action(getTAG(), PRIORITY_HIGHEST * 2) {
        @Override
        protected void before(MethodHookParam param) {
            param.setResult(null);
        }
    };

    private final Action after = new Action(getTAG()) {
        @Override
        protected void after(MethodHookParam param) {
            if (iAction == null) {
                logE(getTAG(), "The callback is null!");
                return;
            }
            utils.param = param;
            utils.thisObject = param.thisObject;
            iAction.action(param, getHCHook());
        }
    };

    private final Action before = new Action(getTAG()) {
        @Override
        protected void before(MethodHookParam param) {
            if (iAction == null) {
                logE(getTAG(), "The callback is null!");
                return;
            }
            utils.param = param;
            utils.thisObject = param.thisObject;
            iAction.action(param, getHCHook());
        }
    };

    private String getTAG() {
        return utils.useTAG();
    }

    private HCHook getHCHook() {
        HCHook hcHook = utils.hcHook;
        if (hcHook == null)
            throw new RuntimeException(getTAG() + ": HCHook is null!!");
        return hcHook;
    }
}
