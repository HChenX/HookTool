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
 */
public class LogExpand {
    private String methodName;
    private String className;
    private XC_MethodHook.MethodHookParam param;
    private String[] filter = null;
    private final String TAG;

    public LogExpand(Member member, String TAG) {
        this.TAG = TAG;
        this.filter = ToolData.filter;
        getName(member);
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
            methodName = constructor.getDeclaringClass().getSimpleName();
            className = constructor.getDeclaringClass().getSimpleName();
        } else {
            logE(TAG, "unknown type! member: " + member);
        }
    }

    public void setParam(XC_MethodHook.MethodHookParam param) {
        this.param = param;
    }

    public void detailedLogs() {
        if (param.args == null || param.args.length == 0) {
            logI(TAG, "class: [" + className + "], method: [" + methodName + "], param: { }");
            return;
        }

        StringBuilder log = new StringBuilder();
        for (int i = 0; i < param.args.length; i++) {
            log.append("(").append(i).append(")->").append("[").append(param.args[i]).append("]");
            if (i < param.args.length - 1) {
                log.append(", ");
            }
        }
        if (!isFilter())
            logI(TAG, "class: [" + className + "], method: [" + methodName + "], param: {" + log + "}");
    }

    private boolean isFilter() {
        if (filter == null) return false;
        for (String s : filter) {
            if (s.equals(TAG)) {
                return true;
            }
        }
        return false;
    }
}
