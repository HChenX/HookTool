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

import com.hchen.hooktool.tool.ActionTool;
import com.hchen.hooktool.tool.ChainTool;
import com.hchen.hooktool.tool.CoreTool;
import com.hchen.hooktool.tool.PrefsTool;
import com.hchen.hooktool.utils.ToolData;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 工具入口
 */
public class HCHook {
    private final ToolData data;

    static {
        ToolData.lpparam = HCInit.getLoadPackageParam();
        ToolData.classLoader = HCInit.getClassLoader();
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
        if (PrefsTool.getXposedPrefs() == null) {
            data.prefsTool = new PrefsTool(data);
        } else data.prefsTool = PrefsTool.getXposedPrefs();
    }

    public HCHook setThisTag(String tag) {
        data.mThisTag = tag;
        return data.getHCHook();
    }

    public CoreTool core() {
        return data.getCoreTool();
    }

    public ChainTool chain() {
        return data.getChainTool();
    }

    public PrefsTool prefs() {
        return data.getPrefsTool();
    }

    public XC_LoadPackage.LoadPackageParam lpparam() {
        return data.getLpparam();
    }

    public ClassLoader classLoader() {
        return data.getClassLoader();
    }
}
