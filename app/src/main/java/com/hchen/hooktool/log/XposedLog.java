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

import com.hchen.hooktool.HCInit;

import de.robv.android.xposed.XposedBridge;

/**
 * 本工具的日志类。
 */
public class XposedLog {
    private static final String rootTag = HCInit.getTAG();
    private static final int level = HCInit.getLogLevel();

    public static void logE(String tag, String log) {
        if (level < 1) return;
        XposedBridge.log(rootTag + "[" + tag + "]" + "[E]: " + log);
    }

    public static void logE(String tag, Throwable e) {
        if (level < 1) return;
        XposedBridge.log(rootTag + "[" + tag + "]" + "[E]: " + e);
    }

    public static void logE(String tag, String log, Throwable e) {
        if (level < 1) return;
        XposedBridge.log(rootTag + "[" + tag + "]" + "[E]: " + log + " \nError Msg: " + e);
    }

    public static void logW(String tag, String log) {
        if (level < 2) return;
        XposedBridge.log(rootTag + "[" + tag + "]" + "[W]: " + log);
    }

    public static void logW(String tag, Throwable e) {
        if (level < 2) return;
        XposedBridge.log(rootTag + "[" + tag + "]" + "[W]: " + e);
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
        XposedBridge.log(rootTag + "[" + tag + "]" + "[D]: " + e);
    }

    public static void logD(String tag, String e) {
        if (level < 4) return;
        XposedBridge.log(rootTag + "[" + tag + "]" + "[D]: " + e);
    }
}
