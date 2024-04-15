package com.hchen.hooktool.action;

import static com.hchen.hooktool.log.XposedLog.logE;

import com.hchen.hooktool.hc.HCHook;

import de.robv.android.xposed.XC_MethodHook;

public class Action extends XC_MethodHook {
    private static final String TAG = HCHook.TAG;

    protected void before(MethodHookParam param) {
    }

    protected void after(MethodHookParam param) {
    }

    public Action() {
        super();
    }

    public Action(int priority) {
        super(priority);
    }

    public static Action returnConstant(final Object result) {
        return new Action(PRIORITY_DEFAULT) {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(result);
            }
        };
    }

    public static final Action DO_NOTHING = new Action(PRIORITY_HIGHEST * 2) {
        @Override
        protected void before(MethodHookParam param) {
            param.setResult(null);
        }

    };

    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        try {
            before(param);
        } catch (Throwable e) {
            logE(TAG + ":" + "before", e.toString());
        }
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) {
        try {
            after(param);
        } catch (Throwable e) {
            logE(TAG + ":" + "after", e.toString());
        }
    }
}
