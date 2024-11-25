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

import android.util.Log;

import com.hchen.hooktool.data.ToolData;

/**
 * 安卓日志
 * 
 * @author 焕晨HChen
 */
public final class AndroidLog {
    // ----------- logE ----------
    public static void logE(String tag, String log) {
        if (ToolData.mInitLogLevel < 1) return;
        Log.e(ToolData.mSpareTag, "[" + tag + "]" + "[E]: " + log);
    }

    public static void logE(String tag, Throwable throwable) {
        if (ToolData.mInitLogLevel < 1) return;
        Log.e(ToolData.mSpareTag, "[" + tag + "]" + "[E]: ", throwable);
    }

    public static void logE(String tag, String log, Throwable throwable) {
        if (ToolData.mInitLogLevel < 1) return;
        Log.e(ToolData.mSpareTag, "[" + tag + "]" + "[E]: " + log, throwable);
    }

    // -------- logW --------------
    public static void logW(String tag, String log) {
        if (ToolData.mInitLogLevel < 2) return;
        Log.w(ToolData.mSpareTag, "[" + tag + "]" + "[W]: " + log);
    }

    public static void logW(String tag, Throwable throwable) {
        if (ToolData.mInitLogLevel < 2) return;
        Log.w(ToolData.mSpareTag, "[" + tag + "]" + "[W]: ", throwable);
    }

    public static void logW(String tag, String log, Throwable throwable) {
        if (ToolData.mInitLogLevel < 2) return;
        Log.w(ToolData.mSpareTag, "[" + tag + "]" + "[W]: " + log, throwable);
    }

    // ------------ logI -------------
    public static void logI(String tag, String pkg, String log) {
        if (ToolData.mInitLogLevel < 3) return;
        Log.i(ToolData.mSpareTag, "[" + tag + "]" + "[" + pkg + "][I]: " + log);
    }

    public static void logI(String tag, String log) {
        if (ToolData.mInitLogLevel < 3) return;
        Log.i(ToolData.mSpareTag, "[" + tag + "]" + "[I]: " + log);
    }

    public static void logI(String tag, Throwable throwable) {
        if (ToolData.mInitLogLevel < 3) return;
        Log.i(ToolData.mSpareTag, "[" + tag + "]" + "[I]: ", throwable);
    }

    public static void logI(String tag, String log, Throwable throwable) {
        if (ToolData.mInitLogLevel < 3) return;
        Log.i(ToolData.mSpareTag, "[" + tag + "]" + "[I]: " + log, throwable);
    }

    // ---------- logD ---------------
    public static void logD(String tag, String log) {
        if (ToolData.mInitLogLevel < 4) return;
        Log.d(ToolData.mSpareTag, "[" + tag + "]" + "[D]: " + log);
    }

    public static void logD(String tag, Throwable throwable) {
        if (ToolData.mInitLogLevel < 4) return;
        Log.d(ToolData.mSpareTag, "[" + tag + "]" + "[D]: ", throwable);
    }

    public static void logD(String tag, String log, Throwable throwable) {
        if (ToolData.mInitLogLevel < 4) return;
        Log.d(ToolData.mSpareTag, "[" + tag + "]" + "[D]: " + log, throwable);
    }
}
