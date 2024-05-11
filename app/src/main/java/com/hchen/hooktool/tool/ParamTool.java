package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;

import android.support.annotation.Nullable;

import com.hchen.hooktool.utils.ParamUtils;

import java.lang.reflect.Member;

import de.robv.android.xposed.XC_MethodHook;

public class ParamTool extends ParamUtils {
    private XC_MethodHook.MethodHookParam param;
    private final Member member;
    private final String TAG;

    public ParamTool(Member member, String tag) {
        this.member = member;
        TAG = tag;
    }

    protected void setParam(XC_MethodHook.MethodHookParam param) {
        this.param = param;
    }

    @Nullable
    public <T> T thisObject() {
        if (paramSafe()) {
            return (T) param.thisObject;
        }
        return null;
    }

    public XC_MethodHook.MethodHookParam originalParam() {
        return param;
    }

    public Member method() {
        if (param == null) {
            return member;
        }
        return param.method;
    }

    public Class<?> thisClass() {
        return member.getDeclaringClass();
    }

    @Nullable
    public <T> T get(int index) {
        if (size() == -1) {
            return null;
        } else if (size() < index + 1) {
            logE(TAG, "method: " + member.getName() +
                    " param size is " + (index + 1) + " !");
            return null;
        }
        return (T) param.args[index];
    }

    @Nullable
    public <T> T one() {
        return get(0);
    }

    @Nullable
    public <T> T two() {
        return get(1);
    }

    @Nullable
    public <T> T three() {
        return get(2);
    }

    @Nullable
    public <T> T four() {
        return get(3);
    }

    @Nullable
    public <T> T five() {
        return get(4);
    }

    public int size() {
        if (paramSafe()) {
            return param.args.length;
        }
        return -1;
    }

    private boolean paramSafe() {
        if (param == null) {
            logE(TAG, "param is null! member: " + member.getName());
            return false;
        }
        return true;
    }
}
