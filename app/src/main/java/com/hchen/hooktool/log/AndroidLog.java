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

import com.hchen.hooktool.HCInit;

/**
 * 没啥用的安卓日志类
 */
public class AndroidLog {
    private static final String rootTag = HCInit.getTAG();

    public static void logI(String tag, String pkg, String log) {
        Log.i(rootTag, "[" + tag + "]" + "[" + pkg + "][I]: " + log);
    }

    public static void logI(String tag, String log) {
        Log.i(rootTag, "[" + tag + "]" + "[I]: " + log);
    }

    public static void logW(String tag, String log) {
        Log.w(rootTag, "[" + tag + "]" + "[W]: " + log);
    }

    public static void logE(String tag, String log) {
        Log.e(rootTag, "[" + tag + "]" + "[E]: " + log);
    }

    public static void logE(String tag, String log, Throwable throwable) {
        Log.e(rootTag, "[" + tag + "]" + "[E]: " + log, throwable);
    }
}
