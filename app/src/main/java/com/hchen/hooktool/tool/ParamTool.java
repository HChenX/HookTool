package com.hchen.hooktool.tool;

import android.support.annotation.Nullable;

import com.hchen.hooktool.tool.param.Arguments;

import java.lang.reflect.Member;

import de.robv.android.xposed.XC_MethodHook;

public class ParamTool<T> extends Arguments<T> {
    public Class<?> mClass;

    public ParamTool(Member member, String tag) {
        super(member, tag);
        mClass = member.getDeclaringClass();
    }

    @Override
    protected void setParam(XC_MethodHook.MethodHookParam param) {
        this.param = param;
    }

    @Nullable
    public T thisObject() {
        paramSafe();
        return (T) param.thisObject;

    }

    public Member method() {
        if (param == null) {
            return member;
        }
        return param.method;
    }

    /**
     * 获取原 Xposed param 参数。
     */
    public XC_MethodHook.MethodHookParam originalParam() {
        return param;
    }
}
