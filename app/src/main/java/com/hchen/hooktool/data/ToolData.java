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
package com.hchen.hooktool.data;

import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.XposedLog.logW;

import com.hchen.hooktool.HCHook;
import com.hchen.hooktool.HCInit;
import com.hchen.hooktool.ToolRestrict;
import com.hchen.hooktool.helper.ConvertHelper;
import com.hchen.hooktool.helper.HookFactory;
import com.hchen.hooktool.tool.ChainTool;
import com.hchen.hooktool.tool.CoreTool;
import com.hchen.hooktool.tool.PrefsTool;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 这是本工具的读写数据类，请不要继承或重写。
 * <p>
 * This is the read and write data class of this tool, please do not extends or override.
 * 
 * @author 焕晨HChen
 */
public class ToolData {
    // HCInit
    public static String mInitTag = "[Unknown]";
    public static String spareTag = "Unknown";
    public static int mInitLogLevel = HCInit.LOG_I;
    public static String modulePackageName = null;
    public static boolean autoReload = true;
    public static XC_LoadPackage.LoadPackageParam lpparam = null;
    public static ClassLoader classLoader = null;
    public static IXposedHookZygoteInit.StartupParam startupParam = null;
    // HCHook
    public String mThisTag = null;
    public boolean isZygote = false;
    public HCHook hc = null;
    public CoreTool core = null;
    public ChainTool chain = null;
    public PrefsTool prefs = null;
    public ConvertHelper convert = null;
    public HookFactory hook = null;

    public ToolData(ToolRestrict helper) {
    }
    
    public String tag() {
        if (mThisTag != null) return mThisTag;
        return spareTag;
    }

    public boolean isZygoteState() {
        if (isZygote) {
            logW(tag(), "in zygote state, call method please set classloader!" + getStackTrace());
            return true;
        }
        return false;
    }
}