package com.hchen.hooktool;

import static com.hchen.hooktool.HookLog.logE;

import de.robv.android.xposed.XC_MethodHook;

public class HookAction extends XC_MethodHook {
    private static final String TAG = HookTool.TAG;

    protected void before(MethodHookParam param) {
    }

    protected void after(MethodHookParam param) {
    }

    public HookAction() {
        super();
    }

    public HookAction(int priority) {
        super(priority);
    }

    public static HookAction returnConstant(final Object result) {
        return new HookAction(PRIORITY_DEFAULT) {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(result);
            }
        };
    }

    public static final HookAction DO_NOTHING = new HookAction(PRIORITY_HIGHEST * 2) {
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
