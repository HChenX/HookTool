package com.hchen.hooktool.tool.param;

import static com.hchen.hooktool.log.XposedLog.logE;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Member;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class ActAchieve {
    protected XC_MethodHook.MethodHookParam param;
    protected final Member member;
    protected final String TAG;

    public ActAchieve(Member member, String tag) {
        this.member = member;
        TAG = tag;
    }

    protected void setParam(XC_MethodHook.MethodHookParam param) {
    }

    @Nullable
    public <T> T getResult() {
        paramSafe();
        return (T) param.getResult();
    }

    public void returnNull() {
        paramSafe();
        param.setResult(null);
    }

    public <T> void setResult(T value) {
        paramSafe();
        param.setResult(value);
    }

    public boolean hasThrowable() {
        paramSafe();
        return param.hasThrowable();
    }

    @Nullable
    public Throwable getThrowable() {
        paramSafe();
        return param.getThrowable();
    }

    public void setThrowable(Throwable t) {
        paramSafe();
        param.setThrowable(t);
    }

    @Nullable
    public <T> T getResultOrThrowable() throws Throwable {
        paramSafe();
        return (T) param.getResultOrThrowable();
    }

    /**
     * 请使用 new Object[]{} 传入参数。<br/>
     * 如果仅传入一个参数可以不使用 new Object[]{}<br/>
     * 这是为了规避泛型与可变参数的冲突。
     */
    @Nullable
    public <T, R> R callMethod(String name, T ts) {
        paramSafe();
        try {
            return (R) XposedHelpers.callMethod(param.thisObject, name, tToObject(ts));
        } catch (Throwable e) {
            logE(TAG, "call method: " + e);
        }
        return null;
    }

    @Nullable
    public <T> T getField(String name) {
        paramSafe();
        try {
            return (T) XposedHelpers.getObjectField(param.thisObject, name);
        } catch (Throwable e) {
            logE(TAG, "get field: " + name);
        }
        return null;
    }

    public <T> boolean setField(String name, T key) {
        paramSafe();
        try {
            XposedHelpers.setObjectField(param.thisObject, name, key);
            return true;
        } catch (Throwable e) {
            logE(TAG, "set field: " + e);
        }
        return false;
    }

    public <T> boolean setAdditionalInstanceField(String name, T key) {
        paramSafe();
        try {
            XposedHelpers.setAdditionalInstanceField(param.thisObject, name, key);
            return true;
        } catch (Throwable e) {
            logE(TAG, "set additional: " + name + " e: " + e);
        }
        return false;
    }

    @Nullable
    public <T> T setAdditionalInstanceField(String name) {
        paramSafe();
        try {
            return (T) XposedHelpers.getAdditionalInstanceField(param.thisObject, name);
        } catch (Throwable e) {
            logE(TAG, "get additional: " + name + " e: " + e);
        }
        return null;
    }

    public boolean removeAdditionalInstanceField(String name) {
        paramSafe();
        try {
            XposedHelpers.removeAdditionalInstanceField(param.thisObject, name);
            return true;
        } catch (Throwable e) {
            logE(TAG, "remove additional: " + name + " e: " + e);
        }
        return false;
    }

    protected void paramSafe() {
        if (param == null) {
            throw new RuntimeException(TAG + " param is null! member: " + member.getName());
        }
    }

    private <T> Object[] tToObject(T ts) {
        if (ts instanceof Object[] objects) {
            return objects;
        }
        return new Object[]{ts};
    }
}
