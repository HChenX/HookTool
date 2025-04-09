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
package com.hchen.hooktool.tool;

import static com.hchen.hooktool.tool.CoreTool.callMethod;
import static com.hchen.hooktool.tool.CoreTool.getAdditionalInstanceField;
import static com.hchen.hooktool.tool.CoreTool.getField;
import static com.hchen.hooktool.tool.CoreTool.removeAdditionalInstanceField;
import static com.hchen.hooktool.tool.CoreTool.setAdditionalInstanceField;
import static com.hchen.hooktool.tool.CoreTool.setField;

import androidx.annotation.NonNull;

import com.hchen.hooktool.log.LogExpand;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * 钩子方法内动作工具
 *
 * @author 焕晨HChen
 */
public class ParamTool {
    protected String TAG;
    private XC_MethodHook xcMethodHook;
    private LogExpand logExpand;
    public volatile XC_MethodHook.MethodHookParam param;

    /**
     * 被 hook 类的实例。
     */
    final public Object thisObject() {
        return param.thisObject;
    }

    /**
     * 移除 hook 自身。
     */
    final public void unHookSelf() {
        XposedBridge.unhookMethod(param.method, xcMethodHook);
    }

    /**
     * 返回被 hook 实例的类加载器。
     */
    final public ClassLoader thisClassLoader() {
        return param.thisObject.getClass().getClassLoader();
    }

    final public Member getMember() {
        return param.method;
    }

    final protected void setMethodHookParam(XC_MethodHook.MethodHookParam param) {
        this.param = param;
    }

    final protected void setXCMethodHook(XC_MethodHook xcMethodHook) {
        this.xcMethodHook = xcMethodHook;
    }

    // ---------------- 设置和获取参数 --------------------

    final public Object[] getArgs() {
        return param.args;
    }

    /**
     * 获取方法的指定参数。
     */
    final public Object getArgs(int index) {
        return param.args[index];
    }

    /**
     * 获取不为空的参数。
     */
    final public Object getArgsNonNull(int index, @NonNull Object def) {
        return param.args[index] != null ? param.args[index] : def;
    }

    /**
     * 设置方法的指定参数。
     */
    final public void setArgs(int index, Object value) {
        param.args[index] = value;
    }

    /**
     * 获取方法参数列表长度。
     */
    final public int length() {
        return param.args.length;
    }

    // ------------ 各种动作 --------------------

    /**
     * 获取方法执行完毕后的返回值。
     */
    final public Object getResult() {
        return param.getResult();
    }

    /**
     * before 中使用拦截方法执行并直接返回设定值。<br/>
     * after 中使用修改返回结果。
     */
    final public void setResult(Object value) {
        param.setResult(value);
    }

    /**
     * before 中使用使方法失效。<br/>
     * after 中使用修改返回结果为 null。
     */
    final public void returnNull() {
        param.setResult(null);
    }

    /**
     * 使方法返回指定的布尔值 true。
     */
    final public void returnTure() {
        param.setResult(true);
    }

    /**
     * 使方法返回指定布尔值 false。
     */
    final public void returnFalse() {
        param.setResult(false);
    }

    /**
     * 如果方法引发了异常，则返回 true。
     */
    final public boolean hasThrowable() {
        return param.hasThrowable();
    }

    /**
     * 返回该方法抛出的异常，若没有则返回 null。
     */
    final public Throwable getThrowable() {
        return param.getThrowable();
    }

    /**
     * 引发异常，在 before 中使用可阻止方法执行。
     */
    final public void setThrowable(Throwable t) {
        param.setThrowable(t);
    }

    /**
     * 返回方法调用的结果，或获取该方法的异常。
     */
    final public Object getResultOrThrowable() throws Throwable {
        return param.getResultOrThrowable();
    }

    // --------- 观察调用 --------------

    /**
     * 观察方法是否被调用，如果被调用则打印一些日志。
     */
    final public void observeCall() {
        if (logExpand == null)
            logExpand = new LogExpand(TAG);
        logExpand.update(param);
        logExpand.observeCall();
    }

    // --------- 调用方法 --------------
    final public Object callThisMethod(String name, Object... objs) {
        return callMethod(param.thisObject, name, objs);
    }

    final public Object callThisMethod(Method method, Object... objs) {
        return callMethod(param.thisObject, method, objs);
    }

    // ----------- 获取/修改 字段 -------------
    final public Object getThisField(String name) {
        return getField(param.thisObject, name);
    }

    final public Object getThisField(Field field) {
        return getField(param.thisObject, field);
    }

    final public boolean setThisField(String name, Object value) {
        return setField(param.thisObject, name, value);
    }

    final public boolean setThisField(Field field, Object value) {
        return setField(param.thisObject, field, value);
    }

    // ---------- 设置自定义字段 --------------
    final public Object setThisAdditionalInstanceField(String key, Object value) {
        return setAdditionalInstanceField(param.thisObject, key, value);
    }

    final public Object getThisAdditionalInstanceField(String key) {
        return getAdditionalInstanceField(param.thisObject, key);
    }

    final public Object removeThisAdditionalInstanceField(String key) {
        return removeAdditionalInstanceField(param.thisObject, key);
    }
}
