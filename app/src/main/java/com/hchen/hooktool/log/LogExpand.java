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

import static com.hchen.hooktool.log.AndroidLog.logI;
import static com.hchen.hooktool.log.AndroidLog.logW;

import com.hchen.hooktool.HCData;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;

import de.robv.android.xposed.XC_MethodHook;

/**
 * 日志增强
 *
 * @author 焕晨HChen
 */
public final class LogExpand {
    private volatile XC_MethodHook.MethodHookParam param;
    private final String TAG;
    private String methodName;
    private String className;

    // 不要自己实例化
    public LogExpand(String tag) {
        this.TAG = tag;
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

    public static String createRuntimeExceptionMsg(String msg) {
        return HCData.getInitTag() + "[" + getTag() + "][E]: " + msg + "\n[Stack Info]: " + getStackTrace();
    }

    public static String getTag() {
        String[] logExpandPath = HCData.getLogExpandPath();
        if (logExpandPath == null) return "HookTool";
        if (logExpandPath.length == 0) {
            if (HCData.getModulePackageName() == null || HCData.getModulePackageName().isEmpty())
                return "HookTool";

            logExpandPath = new String[]{HCData.getModulePackageName()};
        }
        String tag = null;
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            if (tag != null) break;
            String className = stackTraceElement.getClassName();
            if (Arrays.stream(logExpandPath).anyMatch(className::contains)) {
                int index = className.lastIndexOf(".");
                int index2 = className.lastIndexOf("$");
                if (index == -1) break;
                if (index2 == -1) {
                    tag = className.substring(index + 1);
                    break;
                }
                tag = className.substring(index + 1, index2);
                while (tag.lastIndexOf("$") != -1) {
                    index = tag.lastIndexOf("$");
                    tag = tag.substring(0, index);
                }
            }
        }
        if (tag == null)
            return "HookTool";
        return tag;
    }

    public void update(XC_MethodHook.MethodHookParam param) {
        this.param = param;

        if (param.method instanceof Method method) {
            methodName = method.getName();
            className = method.getDeclaringClass().getName();
        } else if (param.method instanceof Constructor<?> constructor) {
            methodName = "<init>";
            className = constructor.getDeclaringClass().getName();
        } else {
            logW(TAG, "Unknown type! member: " + param.method, getStackTrace());
        }
    }

    public void observeCall() {
        if (param.args == null || param.args.length == 0) {
            logI(TAG, "→ Called Method\n"
                + "├─ Class:  " + className + "\n"
                + "├─ Method: " + methodName + "\n"
                + "├─ Params: { }\n"
                + "├─ Return: " + param.getResult()
                + "└─ Throwable: " + param.getThrowable());
            return;
        }

        StringBuilder log = new StringBuilder();
        for (int i = 0; i < param.args.length; i++) {
            Object arg = param.args[i];
            log.append("    [").append(i).append("] ");
            log.append(arg == null ? "(null)" : arg.getClass().getSimpleName());
            log.append(" = ").append(paramToString(arg)).append("\n");
        }

        logI(TAG, "→ Called Method\n"
            + "├─ Class:  " + className + "\n"
            + "├─ Method: " + methodName + "\n"
            + "├─ Params: {\n" + log
            + "├─ }\n"
            + "├─ Return: " + param.getResult()
            + "└─ Throwable: " + param.getThrowable());
    }

    private String paramToString(Object param) {
        if (param == null) return "null";
        if (param.getClass().isArray())
            return Arrays.toString((Object[]) param);

        return param.toString();
    }
}
