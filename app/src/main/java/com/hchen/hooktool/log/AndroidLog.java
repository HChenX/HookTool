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

import android.util.Log;

import com.hchen.hooktool.HCData;
import com.hchen.hooktool.HCInit;

public class AndroidLog {
    // ----------- logE ----------
    public static void logE(String tag, String log) {
        if (HCData.getLogLevel() < HCInit.LOG_E) return;
        Log.e(HCData.getTag(), "[" + tag + "]" + "[E]: " + log);
    }

    public static void logE(String tag, String log, String stackTrace) {
        if (HCData.getLogLevel() < HCInit.LOG_E) return;
        Log.e(HCData.getTag(), "[" + tag + "]" + "[E]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    public static void logE(String tag, Throwable throwable) {
        if (HCData.getLogLevel() < HCInit.LOG_E) return;
        Log.e(HCData.getTag(), "[" + tag + "]" + "[E]: ", throwable);
    }

    public static void logE(String tag, String log, Throwable throwable) {
        if (HCData.getLogLevel() < HCInit.LOG_E) return;
        Log.e(HCData.getTag(), "[" + tag + "]" + "[E]: " + log, throwable);
    }

    // -------- logW --------------
    public static void logW(String tag, String log) {
        if (HCData.getLogLevel() < HCInit.LOG_W) return;
        Log.w(HCData.getTag(), "[" + tag + "]" + "[W]: " + log);
    }

    public static void logW(String tag, String log, String stackTrace) {
        if (HCData.getLogLevel() < HCInit.LOG_W) return;
        Log.w(HCData.getTag(), "[" + tag + "]" + "[W]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    public static void logW(String tag, Throwable throwable) {
        if (HCData.getLogLevel() < HCInit.LOG_W) return;
        Log.w(HCData.getTag(), "[" + tag + "]" + "[W]: ", throwable);
    }

    public static void logW(String tag, String log, Throwable throwable) {
        if (HCData.getLogLevel() < HCInit.LOG_W) return;
        Log.w(HCData.getTag(), "[" + tag + "]" + "[W]: " + log, throwable);
    }

    // ------------ logI -------------
    public static void logI(String tag, String pkg, String log) {
        if (HCData.getLogLevel() < HCInit.LOG_I) return;
        Log.i(HCData.getTag(), "[" + tag + "]" + "[" + pkg + "][I]: " + log);
    }

    public static void logI(String tag, String log) {
        if (HCData.getLogLevel() < HCInit.LOG_I) return;
        Log.i(HCData.getTag(), "[" + tag + "]" + "[I]: " + log);
    }

    public static void logI(String tag, Throwable throwable) {
        if (HCData.getLogLevel() < HCInit.LOG_I) return;
        Log.i(HCData.getTag(), "[" + tag + "]" + "[I]: ", throwable);
    }

    public static void logI(String tag, String log, Throwable throwable) {
        if (HCData.getLogLevel() < HCInit.LOG_I) return;
        Log.i(HCData.getTag(), "[" + tag + "]" + "[I]: " + log, throwable);
    }

    // ---------- logD ---------------
    public static void logD(String tag, String log) {
        if (HCData.getLogLevel() < HCInit.LOG_D) return;
        Log.d(HCData.getTag(), "[" + tag + "]" + "[D]: " + log);
    }

    public static void logD(String tag, String log, String stackTrace) {
        if (HCData.getLogLevel() < HCInit.LOG_D) return;
        Log.d(HCData.getTag(), "[" + tag + "]" + "[D]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    public static void logD(String tag, Throwable throwable) {
        if (HCData.getLogLevel() < HCInit.LOG_D) return;
        Log.d(HCData.getTag(), "[" + tag + "]" + "[D]: ", throwable);
    }

    public static void logD(String tag, String log, Throwable throwable) {
        if (HCData.getLogLevel() < HCInit.LOG_D) return;
        Log.d(HCData.getTag(), "[" + tag + "]" + "[D]: " + log, throwable);
    }
}
