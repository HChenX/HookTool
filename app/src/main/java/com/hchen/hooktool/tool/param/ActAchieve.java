package com.hchen.hooktool.tool.param;

import static com.hchen.hooktool.log.XposedLog.logE;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class ActAchieve<T> {
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
    public T getResult() {
        paramSafe();
        return (T) param.getResult();
    }

    public void setResult(T value) {
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
    public T getResultOrThrowable() throws Throwable {
        paramSafe();
        return (T) param.getResultOrThrowable();
    }

    @Nullable
    public T callMethod(String name, T... ts) {
        paramSafe();
        try {
            return (T) XposedHelpers.callMethod(param.thisObject, name, tToObject(ts));
        } catch (Throwable e) {
            logE(TAG, "call method: " + e);
        }
        return null;
    }

    @Nullable
    public T getField(String name) {
        paramSafe();
        try {
            return (T) XposedHelpers.getObjectField(param.thisObject, name);
        } catch (Throwable e) {
            logE(TAG, "get field: " + name);
        }
        return null;
    }

    public boolean setField(String name, T key) {
        paramSafe();
        try {
            XposedHelpers.setObjectField(param.thisObject, name, key);
            return true;
        } catch (Throwable e) {
            logE(TAG, "set field: " + e);
        }
        return false;
    }

    public boolean setAdditionalInstanceField(String name, T key) {
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
    public T setAdditionalInstanceField(String name) {
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

    private Object[] tToObject(T... ts) {
        ArrayList<Object> list = new ArrayList<>(Arrays.asList(ts));
        return list.toArray(new Object[ts.length]);
    }

    protected void paramSafe() {
        if (param == null) {
            throw new RuntimeException(TAG + " param is null! member: " + member.getName());
        }
    }

}
