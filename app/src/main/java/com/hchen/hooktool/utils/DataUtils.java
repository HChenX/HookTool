package com.hchen.hooktool.utils;

import com.hchen.hooktool.HCHook;
import com.hchen.hooktool.data.MemberData;
import com.hchen.hooktool.tool.ActionTool;
import com.hchen.hooktool.tool.ClassTool;
import com.hchen.hooktool.tool.FieldTool;
import com.hchen.hooktool.tool.MethodTool;

import java.lang.reflect.Field;
import java.util.ArrayList;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 这是字段的读写类，请不要继承重写。
 */
public class DataUtils {
    public static String TAG = null;
    public String mThisTag = null;
    public HCHook hcHook = null;
    public ClassTool classTool = null;
    public FieldTool fieldTool = null;
    public MethodTool methodTool = null;
    public ActionTool actionTool = null;
    public static XC_LoadPackage.LoadPackageParam lpparam = null;
    public static ClassLoader classLoader = null;
    public ClassLoader mCustomClassLoader = null;
    public Class<?> findClass = null;
    public MapUtils<MemberData> classes = new MapUtils<>();
    // public ArrayList<Class<?>> classes = new ArrayList<>();
    public ArrayList<Object> newInstances = new ArrayList<>();
    public Field findField = null;
    public int next = 0;
    // private Method method = null;
    public final MapUtils<MemberData> members = new MapUtils<>();
    // public final MapUtils<MemberData> constructors = new MapUtils<>();

    public ClassLoader getClassLoader() {
        if (mCustomClassLoader != null) return mCustomClassLoader;
        return classLoader;
    }

    public int getCount() {
        return next;
    }

    public void reset() {
        next = 0;
    }

    public void next() {
        next = next + 1;
    }

    public void back() {
        next = next - 1;
    }

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

    public String getTAG() {
        if (mThisTag != null) return mThisTag;
        return TAG;
    }
}