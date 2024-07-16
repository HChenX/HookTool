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
package com.hchen.hooktool.log;

import com.hchen.hooktool.utils.LogExpand;
import com.hchen.hooktool.utils.ToolData;

import de.robv.android.xposed.XposedBridge;

/**
 * LSP 框架日志类
 */
public class XposedLog {
    private static final String rootTag = ToolData.mInitTag; /*根 TAG*/
    private static final int level = ToolData.mInitLogLevel; /*日志等级*/

    public static void logE(String tag, String log) {
        if (level < 1) return;
        XposedBridge.log(rootTag + "[" + tag + "]" + "[E]: " + log);
    }

    public static void logE(String tag, Throwable e) {
        if (level < 1) return;
        XposedBridge.log(rootTag + "[" + tag + "]" + "[E]: " + LogExpand.printStackTrace(e));
    }

    public static void logE(String tag, String log, Throwable e) {
        if (level < 1) return;
        XposedBridge.log(rootTag + "[" + tag + "]" + "[E]: " + log + " \nError Msg: " +
                LogExpand.printStackTrace(e));
    }

    public static void logW(String tag, String log) {
        if (level < 2) return;
        XposedBridge.log(rootTag + "[" + tag + "]" + "[W]: " + log);
    }

    public static void logW(String tag, Throwable e) {
        if (level < 2) return;
        XposedBridge.log(rootTag + "[" + tag + "]" + "[W]: " + LogExpand.printStackTrace(e));
    }

    public static void logI(String tag, String log) {
        if (level < 3) return;
        XposedBridge.log(rootTag + "[" + tag + "]" + "[I]: " + log);
    }

    public static void logI(String tag, String pkg, String log) {
        if (level < 3) return;
        XposedBridge.log(rootTag + "[" + tag + "]" + "[" + pkg + "][I]: " + log);
    }

    public static void logD(String tag, Throwable e) {
        if (level < 4) return;
        XposedBridge.log(rootTag + "[" + tag + "]" + "[D]: " + LogExpand.printStackTrace(e));
    }

    public static void logD(String tag, String log) {
        if (level < 4) return;
        XposedBridge.log(rootTag + "[" + tag + "]" + "[D]: " + log);
    }
}
