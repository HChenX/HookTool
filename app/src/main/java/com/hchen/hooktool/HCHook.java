package com.hchen.hooktool;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.utils.DataUtils.TAG;
import static de.robv.android.xposed.callbacks.XCallback.PRIORITY_HIGHEST;

import com.hchen.hooktool.action.Action;
import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.tool.ClassTool;
import com.hchen.hooktool.tool.FieldTool;
import com.hchen.hooktool.tool.MethodTool;
import com.hchen.hooktool.tool.ParamTool;
import com.hchen.hooktool.utils.DataUtils;
import com.hchen.hooktool.utils.MapUtils;
import com.hchen.hooktool.utils.SafeUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;

import de.robv.android.xposed.XposedBridge;

public class HCHook {
    private DataUtils utils;

    static {
        if (SAf.initSafe()) {
            try {
                TAG = HookInit.getTAG();
                DataUtils.lpparam = HookInit.getLoadPackageParam();
                DataUtils.classLoader = HookInit.getClassLoader();
            } catch (Throwable e) {
                logE(TAG, e);
            }
        }
    }

    /**
     * 实例化本类开始使用
     */
    public HCHook() {
        utils = new DataUtils();
        utils.hcHook = this;
        utils.safeUtils = new SafeUtils(utils);
        utils.classTool = new ClassTool(utils);
        utils.fieldTool = new FieldTool(utils);
        utils.methodTool = new MethodTool(utils);
    }

    public ClassTool classTool() {
        return utils.getClassTool();
    }

    public MethodTool methodTool() {
        return utils.getMethodTool();
    }

    public FieldTool fieldTool() {
        return utils.getFieldTool();
    }

    public HCHook setClassLoader(ClassLoader classLoader) {
        utils.mClassLoader = classLoader;
        return utils.getHCHook();
    }

    public void after(IAction iAction) {

    }

    public void before(IAction iAction) {
        utils.iAction = iAction;
        for (MapUtils<ArrayList<Method>> m : utils.methods) {
            XposedBridge.hookMethod(m, before);
        }
    }

    public void returnConstant(final Object result) {
        for (MapUtils<ArrayList<Method>> m : utils.methods) {
            XposedBridge.hookMethod(m,
                    new Action(utils.getTAG()) {
                        @Override
                        protected void before(MethodHookParam param) {
                            param.setResult(result);
                        }
                    }
            );
        }
    }

    public void doNothing() {
        for (MapUtils<ArrayList<Method>> m : utils.methods) {
            XposedBridge.hookMethod(m, DO_NOTHING);
        }
    }

    private final Action DO_NOTHING = new Action(utils.getTAG(), PRIORITY_HIGHEST * 2) {
        @Override
        protected void before(MethodHookParam param) {
            param.setResult(null);
        }
    };

    private Action afterTool(Method method, IAction iAction) {
        ParamTool paramTool = new ParamTool(utils.getTAG());
        return new Action(utils.getTAG()) {
            @Override
            protected void after(MethodHookParam param) {
                paramTool.setParam(param);
                iAction.action(paramTool);
            }
        };
    }

    private final Action before = new Action(utils.getTAG()) {
        @Override
        protected void before(MethodHookParam param) {
            if (utils.iAction == null) {
                logE(utils.getTAG(), "The callback is null!");
                return;
            }
        }
    };
}
