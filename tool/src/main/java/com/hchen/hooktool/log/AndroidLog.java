/*
 * This file is part of HookTool.
 *
 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * HookTool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with HookTool. If not, see <https://www.gnu.org/licenses/lgpl-2.1>.
 *
 * Copyright (C) 2024–2026 HChenX
 */
package com.hchen.hooktool.log;

import android.util.Log;

import com.hchen.hooktool.ModuleConfig;

/**
 * 安卓日志
 *
 * @author 焕晨HChen
 */
public class AndroidLog {
    private AndroidLog() {
    }

    // ----------- logE ----------
    public static void logE(String tag, String log) {
        if (ModuleConfig.getLogLevel() < HCInit.LOG_E) return;
        Log.e(ModuleConfig.getTag(), "[" + tag + "]" + "[E]: " + log);
    }

    public static void logE(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < HCInit.LOG_E) return;
        Log.e(ModuleConfig.getTag(), "[" + tag + "]" + "[E]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    public static void logE(String tag, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < HCInit.LOG_E) return;
        Log.e(ModuleConfig.getTag(), "[" + tag + "]" + "[E]: ", throwable);
    }

    public static void logE(String tag, String log, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < HCInit.LOG_E) return;
        Log.e(ModuleConfig.getTag(), "[" + tag + "]" + "[E]: " + log, throwable);
    }

    // -------- logW --------------
    public static void logW(String tag, String log) {
        if (ModuleConfig.getLogLevel() < HCInit.LOG_W) return;
        Log.w(ModuleConfig.getTag(), "[" + tag + "]" + "[W]: " + log);
    }

    public static void logW(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < HCInit.LOG_W) return;
        Log.w(ModuleConfig.getTag(), "[" + tag + "]" + "[W]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    public static void logW(String tag, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < HCInit.LOG_W) return;
        Log.w(ModuleConfig.getTag(), "[" + tag + "]" + "[W]: ", throwable);
    }

    public static void logW(String tag, String log, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < HCInit.LOG_W) return;
        Log.w(ModuleConfig.getTag(), "[" + tag + "]" + "[W]: " + log, throwable);
    }

    // ------------ logI -------------
    public static void logI(String tag, String pkg, String log) {
        if (ModuleConfig.getLogLevel() < HCInit.LOG_I) return;
        Log.i(ModuleConfig.getTag(), "[" + tag + "]" + "[" + pkg + "][I]: " + log);
    }

    public static void logI(String tag, String log) {
        if (ModuleConfig.getLogLevel() < HCInit.LOG_I) return;
        Log.i(ModuleConfig.getTag(), "[" + tag + "]" + "[I]: " + log);
    }

    public static void logI(String tag, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < HCInit.LOG_I) return;
        Log.i(ModuleConfig.getTag(), "[" + tag + "]" + "[I]: ", throwable);
    }

    public static void logI(String tag, String log, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < HCInit.LOG_I) return;
        Log.i(ModuleConfig.getTag(), "[" + tag + "]" + "[I]: " + log, throwable);
    }

    // ---------- logD ---------------
    public static void logD(String tag, String log) {
        if (ModuleConfig.getLogLevel() < HCInit.LOG_D) return;
        Log.d(ModuleConfig.getTag(), "[" + tag + "]" + "[D]: " + log);
    }

    public static void logD(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < HCInit.LOG_D) return;
        Log.d(ModuleConfig.getTag(), "[" + tag + "]" + "[D]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    public static void logD(String tag, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < HCInit.LOG_D) return;
        Log.d(ModuleConfig.getTag(), "[" + tag + "]" + "[D]: ", throwable);
    }

    public static void logD(String tag, String log, Throwable throwable) {
        if (ModuleConfig.getLogLevel() < HCInit.LOG_D) return;
        Log.d(ModuleConfig.getTag(), "[" + tag + "]" + "[D]: " + log, throwable);
    }
}
