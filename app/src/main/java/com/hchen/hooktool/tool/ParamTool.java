package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;

import android.support.annotation.Nullable;

import com.hchen.hooktool.utils.ParamUtils;

import de.robv.android.xposed.XC_MethodHook;

public class ParamTool extends ParamUtils {
    private XC_MethodHook.MethodHookParam param;
    private final String TAG;

    public ParamTool(String tag) {
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

    @Nullable
    public <T> T get(int index) {
        if (size() == -1) {
            return null;
        } else if (size() < index + 1) {
            logE(TAG, "param size is " + (index + 1) + " !");
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
            logE(TAG, "param is null!");
            return false;
        }
        return true;
    }
}
