package com.hchen.hooktool.core;

import static com.hchen.hooktool.core.CoreTool.callMethod;
import static com.hchen.hooktool.core.CoreTool.callMethodIfExists;
import static com.hchen.hooktool.core.CoreTool.getAdditionalInstanceField;
import static com.hchen.hooktool.core.CoreTool.getField;
import static com.hchen.hooktool.core.CoreTool.getFieldIfExists;
import static com.hchen.hooktool.core.CoreTool.removeAdditionalInstanceField;
import static com.hchen.hooktool.core.CoreTool.setAdditionalInstanceField;
import static com.hchen.hooktool.core.CoreTool.setField;
import static com.hchen.hooktool.core.CoreTool.setFieldIfExists;

import androidx.annotation.NonNull;

import com.hchen.hooktool.log.AndroidLog;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import de.robv.android.xposed.XC_MethodHook;

public class ParamTool {
    public String INNER_TAG;
    public volatile XC_MethodHook.MethodHookParam param;

    final public Object thisObject() {
        return param.thisObject;
    }

    final public ClassLoader thisClassLoader() {
        return param.thisObject.getClass().getClassLoader();
    }

    final public Member getMember() {
        return param.method;
    }

    final public Object[] getArgs() {
        return param.args;
    }

    final public Object getArg(int index) {
        return param.args[index];
    }

    @NonNull
    final public Object getArgNonNull(int index, @NonNull Object def) {
        return Optional.ofNullable(param.args[index]).orElse(def);
    }

    final public void setArg(int index, Object value) {
        param.args[index] = value;
    }

    final public int length() {
        return param.args.length;
    }

    final public Object getResult() {
        return param.getResult();
    }

    final public void setResult(Object value) {
        param.setResult(value);
    }

    final public void returnNull() {
        param.setResult(null);
    }

    final public void returnTure() {
        param.setResult(true);
    }

    final public void returnFalse() {
        param.setResult(false);
    }

    final public boolean hasThrowable() {
        return param.hasThrowable();
    }

    final public Throwable getThrowable() {
        return param.getThrowable();
    }

    final public void setThrowable(Throwable t) {
        param.setThrowable(t);
    }

    final public Object getResultOrThrowable() throws Throwable {
        return param.getResultOrThrowable();
    }

    final public Object callThisMethod(String methodName, @NonNull Object... params) {
        return callMethod(param.thisObject, methodName, params);
    }

    final public Object callThisMethodIfExists(String methodName, @NonNull Object... params) {
        return callMethodIfExists(param.thisObject, methodName, params);
    }

    final public Object callThisMethod(@NonNull Method method, @NonNull Object... params) {
        return callMethod(param.thisObject, method, params);
    }

    final public Object getThisField(String fieldName) {
        return getField(param.thisObject, fieldName);
    }

    final public Object getThisFieldIfExists(String fieldName) {
        return getFieldIfExists(param.thisObject, fieldName);
    }

    final public Object getThisField(@NonNull Field field) {
        return getField(param.thisObject, field);
    }

    final public void setThisField(String fieldName, Object value) {
        setField(param.thisObject, fieldName, value);
    }

    final public void setThisFieldIfExists(String fieldName, Object value) {
        setFieldIfExists(param.thisObject, fieldName, value);
    }

    final public void setThisField(@NonNull Field field, Object value) {
        setField(param.thisObject, field, value);
    }

    final public Object setThisAdditionalInstanceField(String key, Object value) {
        return setAdditionalInstanceField(param.thisObject, key, value);
    }

    final public Object getThisAdditionalInstanceField(String key) {
        return getAdditionalInstanceField(param.thisObject, key);
    }

    final public Object removeThisAdditionalInstanceField(String key) {
        return removeAdditionalInstanceField(param.thisObject, key);
    }

    final public void observeCall() {
        if (param.args == null || param.args.length == 0) {
            AndroidLog.logI(INNER_TAG, "→ Called Method\n"
                + "├─ Class:  " + param.method.getDeclaringClass().getName() + "\n"
                + "├─ Method: " + param.method.getName() + "\n"
                + "├─ Params: { }\n"
                + "├─ Return: " + param.getResult() + "\n"
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

        AndroidLog.logI(INNER_TAG, "→ Called Method\n"
            + "├─ Class:  " + param.method.getDeclaringClass().getName() + "\n"
            + "├─ Method: " + param.method.getName() + "\n"
            + "├─ Params: {\n" + log
            + "├─ }\n"
            + "├─ Return: " + param.getResult() + "\n"
            + "└─ Throwable: " + param.getThrowable());
    }

    private String paramToString(Object param) {
        if (param == null) return "null";
        if (param.getClass().isArray())
            return Arrays.toString((Object[]) param);

        return param.toString();
    }
}
