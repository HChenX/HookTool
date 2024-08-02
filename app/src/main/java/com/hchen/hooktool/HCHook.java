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

import com.hchen.hooktool.helper.ConvertHelper;
import com.hchen.hooktool.helper.HookFactory;
import com.hchen.hooktool.tool.ChainTool;
import com.hchen.hooktool.tool.CoreTool;
import com.hchen.hooktool.tool.PrefsTool;
import com.hchen.hooktool.utils.ToolData;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 工具入口
 * <p>
 * Tool entry
 * 
 * @author 焕晨HChen
 */
public class HCHook {
    private final ToolData data;

    /**
     * 实例化本类开始使用
     * <p>
     * Instantiate this class to get started
     */
    public HCHook() {
        data = new ToolData(new ToolRestrict());
        data.hcHook = this;
        data.coreTool = new CoreTool(data);
        data.chainTool = new ChainTool(data);
        data.convertHelper = new ConvertHelper(data);
        data.hookFactory = new HookFactory(data);
        if (PrefsTool.xposedPrefs() == null) {
            data.prefsTool = new PrefsTool(data);
        } else data.prefsTool = PrefsTool.xposedPrefs();
    }

    public HCHook setThisTag(String tag) {
        data.mThisTag = tag;
        return data.hcHook;
    }

    public CoreTool core() {
        return data.coreTool;
    }

    public ChainTool chain() {
        return data.chainTool;
    }

    public PrefsTool prefs() {
        return data.prefsTool;
    }

    public XC_LoadPackage.LoadPackageParam lpparam() {
        return ToolData.lpparam;
    }

    public ClassLoader classLoader() {
        return ToolData.classLoader;
    }

    protected void setStateChange(boolean isZygote) {
        data.isZygote = isZygote;
    }
}
