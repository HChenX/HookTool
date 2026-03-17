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
import com.hchen.hooktool.ModuleData;

/**
 * Xposed 日志
 *
 * @author 焕晨HChen
 */
public class XposedLog {
    // -------- logE -------------
    public static void logE(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        ModuleData.getWrapper().log(getLogLevel(), "[" + tag + "]", "[E]: " + log);
    }

    public static void logE(String tag, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        ModuleData.getWrapper().log(getLogLevel(), "[" + tag + "]", "[E]: ", e);
    }

    public static void logE(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        ModuleData.getWrapper().log(getLogLevel(), "[" + tag + "]", "[E]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    public static void logE(String tag, String log, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_E) return;
        ModuleData.getWrapper().log(getLogLevel(), "[" + tag + "]", "[E]: " + log, e);
    }

    // ----------- logW --------------
    public static void logW(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        ModuleData.getWrapper().log(getLogLevel(), "[" + tag + "]", "[W]: " + log);
    }

    public static void logW(String tag, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        ModuleData.getWrapper().log(getLogLevel(), "[" + tag + "]", "[W]: ", e);
    }

    public static void logW(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        ModuleData.getWrapper().log(getLogLevel(), "[" + tag + "]", "[W]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    public static void logW(String tag, String log, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_W) return;
        ModuleData.getWrapper().log(getLogLevel(), "[" + tag + "]", "[W]: " + log, e);
    }

    // ----------- logI --------------

    public static void logI(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        ModuleData.getWrapper().log(getLogLevel(), "[" + tag + "]", "[I]: " + log);
    }

    public static void logI(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        ModuleData.getWrapper().log(getLogLevel(), "[" + tag + "]", "[I]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    public static void logI(String tag, String log, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_I) return;
        ModuleData.getWrapper().log(getLogLevel(), "[" + tag + "]", "[I]: " + log, e);
    }

    // ------------ logD --------------
    public static void logD(String tag, String log) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        ModuleData.getWrapper().log(getLogLevel(), "[" + tag + "]", "[D]: " + log);
    }

    public static void logD(String tag, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        ModuleData.getWrapper().log(getLogLevel(), "[" + tag + "]", "[D]: ", e);
    }

    public static void logD(String tag, String log, String stackTrace) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        ModuleData.getWrapper().log(getLogLevel(), "[" + tag + "]", "[D]: " + log + "\n[Stack Info]: " + stackTrace);
    }

    public static void logD(String tag, String log, Throwable e) {
        if (ModuleConfig.getLogLevel() < ModuleConfig.LOG_D) return;
        ModuleData.getWrapper().log(getLogLevel(), "[" + tag + "]", "[D]: " + log, e);
    }

    private static int getLogLevel() {
        return switch (ModuleConfig.getLogLevel()) {
            case ModuleConfig.LOG_I -> Log.INFO;
            case ModuleConfig.LOG_W -> Log.WARN;
            case ModuleConfig.LOG_E -> Log.ERROR;
            case ModuleConfig.LOG_D -> Log.DEBUG;
            default ->
                throw new IllegalStateException("Unexpected value: " + ModuleConfig.getLogLevel());
        };
    }
}
