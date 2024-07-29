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

import static com.hchen.hooktool.log.XposedLog.logW;
import static com.hchen.hooktool.utils.LogExpand.getStackTrace;

import com.hchen.hooktool.HCHook;
import com.hchen.hooktool.HCInit;
import com.hchen.hooktool.ToolRestrict;
import com.hchen.hooktool.tool.ActionTool;
import com.hchen.hooktool.tool.ChainTool;
import com.hchen.hooktool.tool.CoreTool;
import com.hchen.hooktool.tool.PrefsTool;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 这是本工具的读写数据类，请不要继承或重写。
 * <p>
 * This is the read and write data class of this tool, please do not extends or override.
 */
public class ToolData {
    // HCInit
    public static String mInitTag = "[Unknown]";
    public static int mInitLogLevel = HCInit.LOG_I;
    public static String spareTag = "Unknown";
    public static String modulePackageName = null;
    public static boolean autoReload = true;
    public static XC_LoadPackage.LoadPackageParam lpparam = null;
    public static ClassLoader classLoader = null;
    public static IXposedHookZygoteInit.StartupParam startupParam = null;
    // HCHook
    public String mThisTag = null;
    public boolean isZygote = false;
    public HCHook hcHook = null;
    public ActionTool actionTool = null;
    public CoreTool coreTool = null;
    public ChainTool chainTool = null;
    public PrefsTool prefsTool = null;
    public ConvertHelper convertHelper = null;

    private ToolData() {
    }

    public ToolData(ToolRestrict helper) {
    }

    public ClassLoader classLoader() {
        return classLoader;
    }
    
    public HCHook hcHook() {
        HCHook hcHook = this.hcHook;
        if (hcHook == null)
            throw new RuntimeException(mInitTag + "[" + tag() + "][E]: HCHook is null!!" + getStackTrace());
        return hcHook;
    }

    public ActionTool actionTool() {
        ActionTool actionTool = this.actionTool;
        if (actionTool == null)
            throw new RuntimeException(mInitTag + "[" + tag() + "][E]: ActionTool is null!!" + getStackTrace());
        return actionTool;
    }

    public CoreTool coreTool() {
        CoreTool coreTool = this.coreTool;
        if (coreTool == null)
            throw new RuntimeException(mInitTag + "[" + tag() + "][E]: CoreTool is null!!" + getStackTrace());
        return coreTool;
    }

    public ChainTool chainTool() {
        ChainTool chain = this.chainTool;
        if (chain == null)
            throw new RuntimeException(mInitTag + "[" + tag() + "][E]: CreateChain is null!!" + getStackTrace());
        return chain;
    }

    public PrefsTool prefsTool() {
        PrefsTool prefs = this.prefsTool;
        if (prefs == null)
            throw new RuntimeException(mInitTag + "[" + tag() + "][E]: PrefsTool is null!!" + getStackTrace());
        return prefs;
    }

    public ConvertHelper convertHelper() {
        ConvertHelper convertHelper = this.convertHelper;
        if (convertHelper == null)
            throw new RuntimeException(mInitTag + "[" + tag() + "][E]: ConvertHelper is null!!" + getStackTrace());
        return convertHelper;
    }

    public String tag() {
        if (mThisTag != null) return mThisTag;
        return spareTag;
    }

    public boolean isZygoteState() {
        if (isZygote) {
            logW(tag(), "in zygote state, please set classloader!" + getStackTrace());
            return true;
        }
        return false;
    }
}