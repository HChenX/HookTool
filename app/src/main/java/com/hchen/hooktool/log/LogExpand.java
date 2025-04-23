package com.hchen.hooktool.log;

import com.hchen.hooktool.HCData;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.function.Consumer;

public class LogExpand {
    public static String printStackTrace(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        return stringWriter.toString();
    }

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

    public static String getTag() {
        String[] logExpandPath = HCData.getLogExpandPath();
        if (logExpandPath == null || logExpandPath.length == 0) return "HookTool";
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
}
