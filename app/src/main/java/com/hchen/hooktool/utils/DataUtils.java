package com.hchen.hooktool.utils;

import com.hchen.hooktool.HCHook;
import com.hchen.hooktool.tool.ActionTool;
import com.hchen.hooktool.tool.ClassTool;
import com.hchen.hooktool.tool.FieldTool;
import com.hchen.hooktool.tool.MethodTool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 这是字段的读写类，请不要继承重写。
 */
public class DataUtils {
    public static String TAG = null;
    public String mTAG = null;
    public HCHook hcHook = null;
    public ClassTool classTool = null;
    public FieldTool fieldTool = null;
    public MethodTool methodTool = null;
    public ActionTool actionTool = null;
    public SafeUtils safeUtils = null;
    public static XC_LoadPackage.LoadPackageParam lpparam = null;
    public static ClassLoader classLoader = null;
    public ClassLoader mClassLoader = null;
    public Class<?> findClass = null;
    public MapUtils<Class<?>> classes = new MapUtils<>();
    // public ArrayList<Class<?>> classes = new ArrayList<>();
    public ArrayList<Object> newInstances = new ArrayList<>();
    public Field findField = null;
    // private Method method = null;
    public final MapUtils<ArrayList<Method>> methods = new MapUtils<>();
    public final MapUtils<Constructor<?>[]> constructors = new MapUtils<>();

    public HCHook getHCHook() {
        HCHook hcHook = this.hcHook;
        if (hcHook == null)
            throw new RuntimeException(getTAG() + ": HCHook is null!!");
        return hcHook;
    }

    public ActionTool getActionTool() {
        ActionTool actionTool = this.actionTool;
        if (actionTool == null)
            throw new RuntimeException(getTAG() + ": ActionTool is null!!");
        return actionTool;
    }

    public ClassTool getClassTool() {
        ClassTool classTool = this.classTool;
        if (classTool == null)
            throw new RuntimeException(getTAG() + ": ClassTool is null!!");
        return classTool;
    }

    public MethodTool getMethodTool() {
        MethodTool methodTool = this.methodTool;
        if (methodTool == null)
            throw new RuntimeException(getTAG() + ": MethodTool is null!!");
        return methodTool;
    }

    public FieldTool getFieldTool() {
        FieldTool fieldTool = this.fieldTool;
        if (fieldTool == null)
            throw new RuntimeException(getTAG() + ": FieldTool is null!!");
        return fieldTool;
    }

    public void setMyTAG(String tag) {
        mTAG = tag;
    }

    public String getTAG() {
        if (mTAG != null) return mTAG;
        return TAG;
    }
}