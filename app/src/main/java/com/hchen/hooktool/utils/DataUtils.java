/*
 * This file is part of HookTool.

 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2024 HookTool Contributions
 */
package com.hchen.hooktool.utils;

import com.hchen.hooktool.HCHook;
import com.hchen.hooktool.HCInit;
import com.hchen.hooktool.data.MemberData;
import com.hchen.hooktool.tool.ActionTool;
import com.hchen.hooktool.tool.ClassTool;
import com.hchen.hooktool.tool.ExpandTool;
import com.hchen.hooktool.tool.FieldTool;
import com.hchen.hooktool.tool.MethodTool;

import java.lang.reflect.Field;
import java.util.HashMap;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 这是本工具的读写数据类，请不要继承重写。
 */
public class DataUtils {
    public static String spareTag = null;
    public String mThisTag = null;
    public HCHook hcHook = null;
    public ClassTool classTool = null;
    public FieldTool fieldTool = null;
    public MethodTool methodTool = null;
    public ActionTool actionTool = null;
    public ExpandTool expandTool = null;
    public static String[] filter = null;
    public static boolean useLogExpand = false;
    public static boolean useFieldObserver = false;
    public static XC_LoadPackage.LoadPackageParam lpparam = null;
    public XC_LoadPackage.LoadPackageParam mCustomLpparam = null;
    public static ClassLoader classLoader = null;
    public ClassLoader mCustomClassLoader = null;
    public Class<?> findClass = null;
    public Field findField = null;
    public Object mLabel = null;
    public final HashMap<Object, MemberData> members = new HashMap<>();

    public ClassLoader getClassLoader() {
        if (mCustomClassLoader != null) return mCustomClassLoader;
        return classLoader;
    }

    public XC_LoadPackage.LoadPackageParam getLpparam() {
        if (mCustomLpparam != null) return mCustomLpparam;
        return lpparam;
    }

    public void setLabel(Object label) {
        this.mLabel = label;
    }

    public Object getLabel() {
        return mLabel;
    }

    public HCHook getHCHook() {
        HCHook hcHook = this.hcHook;
        if (hcHook == null)
            throw new RuntimeException(HCInit.getTAG() + "[" + getTAG() + "][E]: HCHook is null!!");
        return hcHook;
    }

    public ActionTool getActionTool() {
        ActionTool actionTool = this.actionTool;
        if (actionTool == null)
            throw new RuntimeException(HCInit.getTAG() + "[" + getTAG() + "][E]: ActionTool is null!!");
        return actionTool;
    }

    public ClassTool getClassTool() {
        ClassTool classTool = this.classTool;
        if (classTool == null)
            throw new RuntimeException(HCInit.getTAG() + "[" + getTAG() + "][E]: ClassTool is null!!");
        return classTool;
    }

    public MethodTool getMethodTool() {
        MethodTool methodTool = this.methodTool;
        if (methodTool == null)
            throw new RuntimeException(HCInit.getTAG() + "[" + getTAG() + "][E]: MethodTool is null!!");
        return methodTool;
    }

    public FieldTool getFieldTool() {
        FieldTool fieldTool = this.fieldTool;
        if (fieldTool == null)
            throw new RuntimeException(HCInit.getTAG() + "[" + getTAG() + "][E]: FieldTool is null!!");
        return fieldTool;
    }

    public ExpandTool getExpandTool() {
        ExpandTool expandTool = this.expandTool;
        if (expandTool == null)
            throw new RuntimeException(HCInit.getTAG() + "[" + getTAG() + "][E]: ExpandTool is null!!");
        return expandTool;
    }

    public String getTAG() {
        if (mThisTag != null) return mThisTag;
        return spareTag;
    }
}