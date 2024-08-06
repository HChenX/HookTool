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

import static com.hchen.hooktool.log.AndroidLog.logE;
import static com.hchen.hooktool.log.AndroidLog.logI;

import android.annotation.SuppressLint;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;

import de.robv.android.xposed.XC_MethodHook;

/**
 * 日志增强
 * <p>
 * Logging enhancements
 *
 * @author 焕晨HChen
 */
public class LogExpand {
    private XC_MethodHook.MethodHookParam param;
    private final String TAG;
    private String methodName;
    private String className;

    public LogExpand(XC_MethodHook.MethodHookParam param, String TAG) {
        this.TAG = TAG;
        this.param = param;
        getName(param.method);
    }

    /**
     * 打印抛错信息的堆栈。
     */
    public static String printStackTrace(Throwable t) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        t.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    /**
     * 打印方法调用的堆栈。
     */
    public static String getStackTrace() {
        StringBuilder stringBuilder = new StringBuilder();
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        Arrays.stream(stackTraceElements).forEach(new Consumer<StackTraceElement>() {
            @Override
            public void accept(StackTraceElement stackTraceElement) {
                String clazz = stackTraceElement.getClassName();
                String method = stackTraceElement.getMethodName();
                String field = stackTraceElement.getFileName();
                int line = stackTraceElement.getLineNumber();
                stringBuilder.append("\nat ").append(clazz).append(".")
                        .append(method).append("(")
                        .append(field).append(":")
                        .append(line).append(")");
            }
        });
        return stringBuilder.toString();
    }

    private static StackWalker stackWalker;
    private static String tag = null;
    private static boolean found = false;

    @SuppressLint("NewApi")
    public static String tag() {
        if (stackWalker == null) {
            stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        }
        tag = null;
        found = false;
        stackWalker.forEach(stackFrame -> {
            if (found) {
                if (tag == null) tag = stackFrame.getClassName();
                return;
            }
            String name = stackFrame.getClassName();
            if (name.contains("BaseHC")) {
                found = true;
            }
        });
        return tag;
    }

    private void getName(Member member) {
        if (member instanceof Method method) {
            methodName = method.getName();
            className = method.getDeclaringClass().getSimpleName();
        } else if (member instanceof Constructor<?> constructor) {
            className = constructor.getDeclaringClass().getSimpleName();
            methodName = "Constructor";
        } else {
            logE(TAG, "LogExpand: unknown type! member: " + member + getStackTrace());
        }
    }

    public void update(XC_MethodHook.MethodHookParam param) {
        this.param = param;
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
