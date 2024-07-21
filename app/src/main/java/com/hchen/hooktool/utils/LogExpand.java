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
package com.hchen.hooktool.utils;

import static com.hchen.hooktool.log.AndroidLog.logI;
import static com.hchen.hooktool.log.XposedLog.logE;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;

/**
 * 日志增强
 * <p>
 * Logging enhancements
 */
public class LogExpand {
    private final XC_MethodHook.MethodHookParam param;
    private final String TAG;
    private String methodName;
    private String className;

    public LogExpand(XC_MethodHook.MethodHookParam param, String TAG) {
        this.TAG = TAG;
        this.param = param;
        getName(param.method);
    }

    public static String printStackTrace(Throwable t) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        t.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    private void getName(Member member) {
        if (member instanceof Method method) {
            methodName = method.getName();
            className = method.getDeclaringClass().getSimpleName();
        } else if (member instanceof Constructor<?> constructor) {
            className = constructor.getDeclaringClass().getSimpleName();
            methodName = "Constructor";
        } else {
            logE(TAG, "unknown type! member: " + member);
        }
    }

    public void detailedLogs() {
        if (param.args == null || param.args.length == 0) {
            logI(TAG, "class: [" + className + "], method: [" + methodName + "], param: { }");
            return;
        }

        StringBuilder log = new StringBuilder();
        for (int i = 0; i < param.args.length; i++) {
            log.append("(").append(param.args[i].getClass().getSimpleName())
                    .append(")->").append("[").append(param.args[i]).append("]");
            if (i < param.args.length - 1) {
                log.append(", ");
            }
        }
        logI(TAG, "class: [" + className + "], method: [" + methodName + "], param: {" + log + "}");
    }
}
