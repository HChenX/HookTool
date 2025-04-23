package com.hchen.hooktool.hook;

import androidx.annotation.IntDef;

import com.hchen.hooktool.core.ParamTool;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public abstract class IHook extends ParamTool {
    protected XC_MethodHook xcMethodHook;
    public final int PRIORITY;
    public static final int BEFORE = 1;
    public static final int AFTER = 2;

    @IntDef(value = {
        BEFORE,
        AFTER,
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface ActionFlag {
    }

    public IHook() {
        this.PRIORITY = Priority.DEFAULT;
    }

    public IHook(int priority) {
        this.PRIORITY = priority;
    }

    public void before() {
    }

    public void after() {
    }

    public boolean onThrow(@ActionFlag int flag, Throwable e) {
        return false;
    }

    final public void unHookSelf() {
        XposedBridge.unhookMethod(param.method, xcMethodHook);
    }
}
