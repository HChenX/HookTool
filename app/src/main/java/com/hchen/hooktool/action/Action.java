package com.hchen.hooktool.action;

import static com.hchen.hooktool.log.XposedLog.logE;

import de.robv.android.xposed.XC_MethodHook;

public class Action extends XC_MethodHook {
    private String TAG = null;

    protected void before(MethodHookParam param) throws Throwable {
    }

    protected void after(MethodHookParam param) throws Throwable {
    }

    public Action(String tag) {
        super();
        TAG = tag;
    }

    public Action(String tag, int priority) {
        super(priority);
        TAG = tag;
    }

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
