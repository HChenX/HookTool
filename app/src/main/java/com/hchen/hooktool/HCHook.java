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

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.utils.ToolData.spareTag;

import com.hchen.hooktool.tool.ActionTool;
import com.hchen.hooktool.tool.ChainTool;
import com.hchen.hooktool.tool.CoreTool;
import com.hchen.hooktool.utils.ToolData;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 工具入口
 */
public class HCHook {
    private final ToolData data;

    static {
        initSafe();
        try {
            spareTag = HCInit.spareTag;
            ToolData.lpparam = HCInit.getLoadPackageParam();
            ToolData.useLogExpand = HCInit.getUseLogExpand();
            ToolData.useFieldObserver = HCInit.getUseFieldObserver();
            ToolData.filter = HCInit.getFilter();
            ToolData.classLoader = HCInit.getClassLoader();
        } catch (Throwable e) {
            logE(spareTag, e);
        }
    }

    /**
     * 实例化本类开始使用
     */
    public HCHook() {
        data = new ToolData();
        data.hcHook = this;
        data.actionTool = new ActionTool(data);
        data.coreTool = new CoreTool(data);
        data.chainTool = new ChainTool(data);
    }

    public HCHook setThisTag(String tag) {
        data.mThisTag = tag;
        return data.getHCHook();
    }

    public CoreTool coreTool() {
        return data.getCoreTool();
    }
    
    public ActionTool actionTool() {
        return data.getActionTool();
    }

    public ChainTool chainTool() {
        return data.getChainTool();
    }

    /* 设置自定义 class loader */
    public HCHook setClassLoader(ClassLoader classLoader) {
        data.mCustomClassLoader = classLoader;
        return data.getHCHook();
    }

    /* 设置自定义 lpparam */
    public HCHook setLpparam(XC_LoadPackage.LoadPackageParam lpparam) {
        data.mCustomLpparam = lpparam;
        setClassLoader(lpparam.classLoader);
        return data.getHCHook();
    }

    public XC_LoadPackage.LoadPackageParam getLpparam() {
        return data.getLpparam();
    }

    public ClassLoader getClassLoader() {
        return data.getClassLoader();
    }

    public static void initSafe() {
        if (!HCInit.isInitDone())
            throw new RuntimeException(HCInit.getTAG() + "[E]: HookInit not initialized!");
    }
}
