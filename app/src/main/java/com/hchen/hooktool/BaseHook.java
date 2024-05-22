package com.hchen.hooktool;

import com.hchen.hooktool.tool.ClassTool;
import com.hchen.hooktool.tool.DexkitTool;
import com.hchen.hooktool.tool.ExpandTool;
import com.hchen.hooktool.tool.FieldTool;
import com.hchen.hooktool.tool.MethodTool;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 对需要使用工具的类继承本类，课快速使用工具。
 */
public abstract class BaseHook {
    public static XC_LoadPackage.LoadPackageParam lpparam;
    public static HCHook hcHook;
    public static ClassTool classTool;
    public static MethodTool methodTool;
    public static FieldTool fieldTool;
    public static DexkitTool dexkitTool;
    public static ExpandTool expandTool;

    public BaseHook() {
    }

    public abstract void init();

    public void onCreate(HCHook hcHook, XC_LoadPackage.LoadPackageParam lpparam) {
        BaseHook.hcHook = hcHook;
        BaseHook.classTool = hcHook.classTool();
        BaseHook.methodTool = hcHook.methodTool();
        BaseHook.fieldTool = hcHook.fieldTool();
        BaseHook.dexkitTool = hcHook.dexkitTool();
        BaseHook.expandTool = hcHook.expandTool();
        BaseHook.lpparam = lpparam;
        init();
    }
}
