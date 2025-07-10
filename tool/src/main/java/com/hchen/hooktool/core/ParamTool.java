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
package com.hchen.hooktool.core;

import static com.hchen.hooktool.core.CoreTool.callMethod;
import static com.hchen.hooktool.core.CoreTool.callMethodIfExists;
import static com.hchen.hooktool.core.CoreTool.callStaticMethod;
import static com.hchen.hooktool.core.CoreTool.callStaticMethodIfExists;
import static com.hchen.hooktool.core.CoreTool.existsAnyMethod;
import static com.hchen.hooktool.core.CoreTool.existsConstructor;
import static com.hchen.hooktool.core.CoreTool.existsField;
import static com.hchen.hooktool.core.CoreTool.existsMethod;
import static com.hchen.hooktool.core.CoreTool.getAdditionalInstanceField;
import static com.hchen.hooktool.core.CoreTool.getAdditionalStaticField;
import static com.hchen.hooktool.core.CoreTool.getField;
import static com.hchen.hooktool.core.CoreTool.getFieldIfExists;
import static com.hchen.hooktool.core.CoreTool.getStaticField;
import static com.hchen.hooktool.core.CoreTool.getStaticFieldIfExists;
import static com.hchen.hooktool.core.CoreTool.newInstance;
import static com.hchen.hooktool.core.CoreTool.newInstanceIfExists;
import static com.hchen.hooktool.core.CoreTool.removeAdditionalInstanceField;
import static com.hchen.hooktool.core.CoreTool.removeAdditionalStaticField;
import static com.hchen.hooktool.core.CoreTool.setAdditionalInstanceField;
import static com.hchen.hooktool.core.CoreTool.setAdditionalStaticField;
import static com.hchen.hooktool.core.CoreTool.setField;
import static com.hchen.hooktool.core.CoreTool.setFieldIfExists;
import static com.hchen.hooktool.core.CoreTool.setStaticField;
import static com.hchen.hooktool.core.CoreTool.setStaticFieldIfExists;

import androidx.annotation.NonNull;

import com.hchen.hooktool.log.AndroidLog;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * 参数工具
 *
 * @author 焕晨HChen
 */
public class ParamTool {
    public String INNER_TAG; // 内部 TAG，请勿使用
    public volatile XC_MethodHook.MethodHookParam param;

    /**
     * 实例，静态则为 null
     */
    final public Object thisObject() {
        return param.thisObject;
    }

    /**
     * 当前的类加载器
     */
    final public ClassLoader thisClassLoader() {
        return param.thisObject.getClass().getClassLoader();
    }

    /**
     * 获取当前的方法
     */
    final public Member getMember() {
        return param.method;
    }

    /**
     * 获取声明类
     */
    final public Class<?> getDeclaringClass() {
        return param.method.getDeclaringClass();
    }

    /**
     * 获取方法的参数
     */
    final public Object[] getArgs() {
        return param.args;
    }

    /**
     * 获取方法的指定参数
     */
    final public Object getArg(int index) {
        return param.args[index];
    }

    @NonNull
    final public Object getArgNonNull(int index, @NonNull Object def) {
        return Optional.ofNullable(param.args[index]).orElse(def);
    }

    /**
     * 设置指定参数的值
     */
    final public void setArg(int index, Object value) {
        param.args[index] = value;
    }

    /**
     * 方法参数列表长度
     */
    final public int length() {
        return param.args.length;
    }

    /**
     * 获取方法的返回值
     */
    final public Object getResult() {
        return param.getResult();
    }

    /**
     * 设置方法的返回值，before 中设置可拦截方法执行
     */
    final public void setResult(Object value) {
        param.setResult(value);
    }

    final public void returnNull() {
        param.setResult(null);
    }

    final public void returnTrue() {
        param.setResult(true);
    }

    final public void returnFalse() {
        param.setResult(false);
    }

    /**
     * 方法抛出异常则为 true
     */
    final public boolean hasThrowable() {
        return param.hasThrowable();
    }

    /**
     * 获取方法抛出的异常
     */
    final public Throwable getThrowable() {
        return param.getThrowable();
    }

    /**
     * 设置异常
     */
    final public void setThrowable(Throwable t) {
        param.setThrowable(t);
    }

    /**
     * 获取方法返回值或抛出异常
     */
    final public Object getResultOrThrowable() throws Throwable {
        return param.getResultOrThrowable();
    }

    // ---------------------------------------- Non Static -----------------------------------------

    /**
     * 调用本实例的方法
     */
    final public Object callThisMethod(@NonNull String methodName, @NonNull Object... params) {
        return callMethod(param.thisObject, methodName, params);
    }

    /**
     * 调用本实例的方法，如果存在
     */
    final public Object callThisMethodIfExists(@NonNull String methodName, @NonNull Object... params) {
        return callMethodIfExists(param.thisObject, methodName, params);
    }

    /**
     * 调用本实例的方法
     */
    final public Object callThisMethod(@NonNull String methodName, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return callMethod(param.thisObject, methodName, paramTypes, params);
    }

    /**
     * 调用本实例的方法，如果存在
     */
    final public Object callThisMethodIfExists(@NonNull String methodName, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return callMethodIfExists(param.thisObject, methodName, paramTypes, params);
    }

    /**
     * 调用本实例的方法
     */
    final public Object callThisMethod(@NonNull Method method, @NonNull Object... params) {
        return callMethod(param.thisObject, method, params);
    }

    /**
     * 获取本实例的字段
     */
    final public Object getThisField(@NonNull String fieldName) {
        return getField(param.thisObject, fieldName);
    }

    /**
     * 获取本实例的字段，如果存在
     */
    final public Object getThisFieldIfExists(@NonNull String fieldName) {
        return getFieldIfExists(param.thisObject, fieldName);
    }

    /**
     * 获取本实例的字段
     */
    final public Object getThisField(@NonNull Field field) {
        return getField(param.thisObject, field);
    }

    /**
     * 设置本实例的字段
     */
    final public void setThisField(@NonNull String fieldName, Object value) {
        setField(param.thisObject, fieldName, value);
    }

    /**
     * 设置本实例的字段，如果存在
     */
    final public void setThisFieldIfExists(@NonNull String fieldName, Object value) {
        setFieldIfExists(param.thisObject, fieldName, value);
    }

    /**
     * 设置本实例的字段
     */
    final public void setThisField(@NonNull Field field, Object value) {
        setField(param.thisObject, field, value);
    }

    /**
     * 将字段附加到本实例
     */
    final public Object setThisAdditionalInstanceField(@NonNull String key, Object value) {
        return setAdditionalInstanceField(param.thisObject, key, value);
    }

    /**
     * 获取附加的字段值
     */
    final public Object getThisAdditionalInstanceField(@NonNull String key) {
        return getAdditionalInstanceField(param.thisObject, key);
    }

    /**
     * 删除附加的字段
     */
    final public Object removeThisAdditionalInstanceField(@NonNull String key) {
        return removeAdditionalInstanceField(param.thisObject, key);
    }

    // ---------------------------------------- Static -------------------------------------------

    /**
     * 为本实例创建新实例
     */
    final public Object newThisInstance(@NonNull Object... params) {
        return newInstance(param.method.getDeclaringClass(), params);
    }

    /**
     * 为本实例创建新实例
     */
    final public Object newThisInstanceIfExists(@NonNull Object... params) {
        return newInstanceIfExists(param.method.getDeclaringClass(), params);
    }

    /**
     * 为本实例创建新实例
     */
    final public Object newThisInstance(@NonNull Object[] paramTypes, @NonNull Object... params) {
        return newInstance(param.method.getDeclaringClass(), paramTypes, params);
    }

    /**
     * 为本实例创建新实例
     */
    final public Object newThisInstanceIfExists(@NonNull Object[] paramTypes, @NonNull Object... params) {
        return newInstanceIfExists(param.method.getDeclaringClass(), paramTypes, params);
    }

    /**
     * 调用本实例的静态方法
     */
    final public Object callThisStaticMethod(@NonNull String methodName, @NonNull Object... params) {
        return callStaticMethod(param.method.getDeclaringClass(), methodName, params);
    }

    /**
     * 调用本实例的静态方法，如果存在
     */
    final public Object callThisStaticMethodIfExists(@NonNull String methodName, @NonNull Object... params) {
        return callStaticMethodIfExists(param.method.getDeclaringClass(), methodName, params);
    }

    /**
     * 调用本实例的静态方法
     */
    final public Object callThisStaticMethod(@NonNull String methodName, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return callStaticMethod(param.method.getDeclaringClass(), methodName, paramTypes, params);
    }

    /**
     * 调用本实例的静态方法，如果存在
     */
    final public Object callThisStaticMethodIfExists(@NonNull String methodName, @NonNull Object[] paramTypes, @NonNull Object... params) {
        return callStaticMethodIfExists(param.method.getDeclaringClass(), methodName, paramTypes, params);
    }

    /**
     * 调用本实例的静态方法
     */
    final public Object callThisStaticMethod(@NonNull Method method, @NonNull Object... params) {
        return callStaticMethod(method, params);
    }

    /**
     * 获取本实例的静态字段
     */
    final public Object getThisStaticField(@NonNull String fieldName) {
        return getStaticField(param.method.getDeclaringClass(), fieldName);
    }

    /**
     * 获取本实例的静态字段，如果存在
     */
    final public Object getThisStaticFieldIfExists(@NonNull String fieldName) {
        return getStaticFieldIfExists(param.method.getDeclaringClass(), fieldName);
    }

    /**
     * 获取本实例的静态字段
     */
    final public Object getThisStaticField(@NonNull Field field) {
        return getStaticField(field);
    }

    /**
     * 设置本实例的静态字段
     */
    final public void setThisStaticField(@NonNull String fieldName, Object value) {
        setStaticField(param.method.getDeclaringClass(), fieldName, value);
    }

    /**
     * 设置本实例的静态字段，如果存在
     */
    final public void setThisStaticFieldIfExists(@NonNull String fieldName, Object value) {
        setStaticFieldIfExists(param.method.getDeclaringClass(), fieldName, value);
    }

    /**
     * 设置本实例的静态字段
     */
    final public void setThisStaticField(@NonNull Field field, Object value) {
        setStaticField(field, value);
    }

    /**
     * 将静态字段附加到本实例
     */
    final public Object setThisStaticAdditionalInstanceField(@NonNull String key, Object value) {
        return setAdditionalStaticField(param.method.getDeclaringClass(), key, value);
    }

    /**
     * 获取附加的静态字段值
     */
    final public Object getThisStaticAdditionalInstanceField(@NonNull String key) {
        return getAdditionalStaticField(param.method.getDeclaringClass(), key);
    }

    /**
     * 删除附加的静态字段
     */
    final public Object removeThisStaticAdditionalInstanceField(@NonNull String key) {
        return removeAdditionalStaticField(param.method.getDeclaringClass(), key);
    }

    // -------------------------------------- Exists --------------------------------------------

    /**
     * 是否存在指定方法
     */
    final public boolean existsThisMethod(@NonNull String methodName, @NonNull Object... params) {
        return existsMethod(param.method.getDeclaringClass(), methodName, params);
    }

    /**
     * 是否存在指定方法名的方法
     */
    final public boolean existsThisAnyMethod(@NonNull String methodName) {
        return existsAnyMethod(param.method.getDeclaringClass(), methodName);
    }

    /**
     * 是否存在指定构造函数
     */
    final public boolean existsThisConstructor(@NonNull Object... params) {
        return existsConstructor(param.method.getDeclaringClass(), params);
    }

    /**
     * 是否存在指定字段
     */
    final public boolean existsThisField(@NonNull String fieldName) {
        return existsField(param.method.getDeclaringClass(), fieldName);
    }

    // ---------------------------------------- Original -------------------------------------------

    final public Object invokeOriginalMethod(@NonNull Object... params)
        throws InvocationTargetException, IllegalAccessException {
        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, params);
    }

    // ---------------------------------------- Other -------------------------------------------

    /**
     * 观察方法调用
     */
    final public void observeCall() {
        if (param == null) return;
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
        Class<?> clazz = param.getClass();
        if (!clazz.isArray())
            return param.toString();

        class Frame {
            final Object array;
            final int length;
            int index;

            Frame(Object array) {
                this.array = array;
                this.length = Array.getLength(array);
                this.index = 0;
            }
        }

        StringBuilder sb = new StringBuilder();
        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(param));
        sb.append("[");

        while (!stack.isEmpty()) {
            Frame top = stack.peek();
            assert top != null;
            if (top.index >= top.length) {
                stack.pop();
                sb.append("]");
                if (!stack.isEmpty()) {
                    Frame parent = stack.peek();
                    assert parent != null;
                    if (parent.index < parent.length) {
                        sb.append(", ");
                    }
                }
                continue;
            }

            Object element = Array.get(top.array, top.index);
            top.index++;

            if (element != null && element.getClass().isArray()) {
                sb.append("[");
                stack.push(new Frame(element));
            } else {
                sb.append(element == null ? "null" : element.toString());
                if (top.index < top.length) {
                    sb.append(", ");
                }
            }
        }

        return sb.toString();
    }
}
