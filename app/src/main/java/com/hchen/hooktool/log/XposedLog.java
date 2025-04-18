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

 * Copyright (C) 2023-2025 HChenX
 */
package com.hchen.hooktool.log;

import com.hchen.hooktool.HCData;

import de.robv.android.xposed.XposedBridge;

/**
 * LSP 框架日志
 *
 * @author 焕晨HChen
 */
public class XposedLog {
    // -------- logE -------------
    public static void logE(String tag, String log) {
        if (HCData.getInitLogLevel() < 1) return;
        XposedBridge.log(HCData.getInitTag() + "[" + tag + "]" + "[E]: " + log);
    }

    public static void logE(String tag, String log, String stackTrace) {
        if (HCData.getInitLogLevel() < 1) return;
        XposedBridge.log(HCData.getInitTag() + "[" + tag + "]" + "[E]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    public static void logE(String tag, Throwable e) {
        if (HCData.getInitLogLevel() < 1) return;
        XposedBridge.log(HCData.getInitTag() + "[" + tag + "]" + "[E]:\n" + LogExpand.printStackTrace(e));
    }

    public static void logE(String tag, String log, Throwable e) {
        if (HCData.getInitLogLevel() < 1) return;
        XposedBridge.log(HCData.getInitTag() + "[" + tag + "]" + "[E]: " + log + "\n[Stack Info]: " + LogExpand.printStackTrace(e));
    }

    // ----------- logW --------------
    public static void logW(String tag, String log) {
        if (HCData.getInitLogLevel() < 2) return;
        XposedBridge.log(HCData.getInitTag() + "[" + tag + "]" + "[W]: " + log);
    }

    public static void logW(String tag, String log, String stackTrace) {
        if (HCData.getInitLogLevel() < 2) return;
        XposedBridge.log(HCData.getInitTag() + "[" + tag + "]" + "[W]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    public static void logW(String tag, Throwable e) {
        if (HCData.getInitLogLevel() < 2) return;
        XposedBridge.log(HCData.getInitTag() + "[" + tag + "]" + "[W]:\n" + LogExpand.printStackTrace(e));
    }

    public static void logW(String tag, String log, Throwable e) {
        if (HCData.getInitLogLevel() < 2) return;
        XposedBridge.log(HCData.getInitTag() + "[" + tag + "]" + "[W]: " + log + "\n[Stack Info]: " + LogExpand.printStackTrace(e));
    }

    // ----------- logI --------------
    public static void logI(String log) {
        if (HCData.getInitLogLevel() < 3) return;
        XposedBridge.log(HCData.getInitTag() + "[I]: " + log);
    }

    public static void logI(String tag, String log) {
        if (HCData.getInitLogLevel() < 3) return;
        XposedBridge.log(HCData.getInitTag() + "[" + tag + "]" + "[I]: " + log);
    }

    public static void logI(String tag, String pkg, String log) {
        if (HCData.getInitLogLevel() < 3) return;
        XposedBridge.log(HCData.getInitTag() + "[" + tag + "]" + "[" + pkg + "][I]: " + log);
    }

    public static void logI(String tag, String log, Throwable e) {
        if (HCData.getInitLogLevel() < 3) return;
        XposedBridge.log(HCData.getInitTag() + "[" + tag + "]" + "[I]: " + log + "\n[Stack Info]: " + LogExpand.printStackTrace(e));
    }

    // ------------ logD --------------
    public static void logD(String tag, String log) {
        if (HCData.getInitLogLevel() < 4) return;
        XposedBridge.log(HCData.getInitTag() + "[" + tag + "]" + "[D]: " + log);
    }

    public static void logD(String tag, String log, String stackTrace) {
        if (HCData.getInitLogLevel() < 4) return;
        XposedBridge.log(HCData.getInitTag() + "[" + tag + "]" + "[D]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    public static void logD(String tag, Throwable e) {
        if (HCData.getInitLogLevel() < 4) return;
        XposedBridge.log(HCData.getInitTag() + "[" + tag + "]" + "[D]:\n" + LogExpand.printStackTrace(e));
    }

    public static void logD(String tag, String log, Throwable e) {
        if (HCData.getInitLogLevel() < 4) return;
        XposedBridge.log(HCData.getInitTag() + "[" + tag + "]" + "[D]: " + log + "\n[Stack Info]: " + LogExpand.printStackTrace(e));
    }
}
