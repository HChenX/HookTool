package com.hchen.hooktool.action;

import static com.hchen.hooktool.log.XposedLog.logE;

import de.robv.android.xposed.XC_MethodHook;

public class Action extends XC_MethodHook {
    private static final String TAG = null;

    protected void before(MethodHookParam param) throws Throwable {
    }

    protected void after(MethodHookParam param) throws Throwable {
    }

    public Action(String tag) {
        super();
    }

    public Action(String tag, int priority) {
        super(priority);
    }

    public static Action returnConstant(final Object result) {
        return new Action(TAG, PRIORITY_DEFAULT) {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(result);
            }
        };
    }

    public static final Action DO_NOTHING = new Action(TAG, PRIORITY_HIGHEST * 2) {
        @Override
        protected void before(MethodHookParam param) throws Throwable {
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
