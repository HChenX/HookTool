package com.hchen.hooktool.tool;

import com.hchen.hooktool.callback.IAction;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 这是字段的读写类，请不要继承重写。
 */
public class UtilsTool {
    protected static String TAG = null;
    protected String mTAG = null;
    protected HCHook hcHook = null;
    protected ClassTool classTool = null;
    protected FieldTool fieldTool = null;
    protected MethodTool methodTool = null;
    protected SafeTool safeTool = null;
    protected static XC_LoadPackage.LoadPackageParam lpparam = null;
    protected static ClassLoader classLoader = null;
    protected ClassLoader mClassLoader = null;
    protected XC_MethodHook.MethodHookParam param = null;
    protected Object thisObject = null;
    protected Class<?> findClass = null;
    protected Field findField = null;
    // private Method method = null;
    protected IAction iAction = null;
    protected final ArrayList<Method> methods = new ArrayList<>();
    protected final ArrayList<Constructor<?>> constructors = new ArrayList<>();

    protected void setMyTAG(String tag) {
        mTAG = tag;
    }

    protected String useTAG() {
        if (mTAG != null) return mTAG;
        return TAG;
    }
}
