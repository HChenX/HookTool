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
public abstract class BaseHC {
    public String TAG = getClass().getSimpleName();
    // 直接调用是 null，必须 onCreate 初始化一次。
    public static XC_LoadPackage.LoadPackageParam lpparam = getLpparam();
    public static HCHook hcHook;
    public static ClassTool classTool;
    public static MethodTool methodTool;
    public static FieldTool fieldTool;
    public static DexkitTool dexkitTool;
    public static ExpandTool expandTool;

    public abstract void init();

    public void onCreate() {
        BaseHC.hcHook = new HCHook();
        BaseHC.classTool = hcHook.classTool();
        BaseHC.methodTool = hcHook.methodTool();
        BaseHC.fieldTool = hcHook.fieldTool();
        BaseHC.dexkitTool = hcHook.dexkitTool();
        BaseHC.expandTool = hcHook.expandTool();
        BaseHC.hcHook.setThisTag(TAG);
        init();
    }

    private static XC_LoadPackage.LoadPackageParam getLpparam() {
        if (hcHook != null) {
            return hcHook.getLpparam();
        }
        return null;
    }
}
