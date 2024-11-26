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

import com.hchen.hooktool.HCInit;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 核心数据
 *
 * @author 焕晨HChen
 */
public final class ToolData {
    public static String mInitTag = "[Unknown]";
    public static String mSpareTag = "Unknown";
    public static int mInitLogLevel = HCInit.LOG_I;
    public static String modulePackageName = null;
    public static String mPrefsName = null;
    public static String[] mLogExpandPath = null;
    public static boolean isAutoReload = true;
    public static boolean isXposed = false;
    public static ClassLoader mClassLoader = null;
    public static XC_LoadPackage.LoadPackageParam mLpparam = null;
    public static IXposedHookZygoteInit.StartupParam mStartupParam = null;
}