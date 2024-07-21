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
package com.hchen.hooktool.tool.param;

import com.hchen.hooktool.itool.IDynamic;
import com.hchen.hooktool.utils.LogExpand;

import de.robv.android.xposed.XC_MethodHook;

/**
 * 动作
 * <p>
 * action
 */
public class ActAchieve {
    protected XC_MethodHook.MethodHookParam methodHookParam;
    protected String mTag;
    protected IDynamic iDynamic;
    private LogExpand logExpand;
    // protected ToolData data;

    /**
     * 获取方法执行完毕后的返回值。
     * <p>
     * Obtain the return value after the method is executed.
     */
    final public <T> T getResult() {
        return (T) methodHookParam.getResult();
    }

    /**
     * before 中使用拦截方法执行并直接返回设定值。<br/>
     * after 中使用修改返回结果。
     * <p>
     * before, using the interception method and returns the setpoint directly.
     * <p>
     * after, to modify the returned result.
     */
    final public <T> void setResult(T value) {
        methodHookParam.setResult(value);
    }

    /**
     * before 中使用使方法失效。<br/>
     * after 中使用修改返回结果。
     * <p>
     * before, to invalidate the method. after, to modify the returned result.
     */
    final public void returnNull() {
        methodHookParam.setResult(null);
    }

    /**
     * 使方法返回指定的布尔值 true。
     * <p>
     * Causes the method to return the specified boolean value of true.
     */
    final public void returnTure() {
        methodHookParam.setResult(true);
    }

    /**
     * 使方法返回指定布尔值 false。
     * <p>
     * Causes the method to return the specified boolean value of false.
     */
    final public void returnFalse() {
        methodHookParam.setResult(false);
    }

    /**
     * 如果方法引发了异常，则返回 true。
     * <p>
     * If the method throws an exception, it returns true.
     */
    final public boolean hasCrash() {
        return methodHookParam.hasThrowable();
    }

    /**
     * 返回该方法抛出的异常或者返回 null。
     * <p>
     * Returns an exception thrown by the method or returns null.
     */
    final public Throwable getCrash() {
        return methodHookParam.getThrowable();
    }

    /**
     * 引发异常，在 before 中使用可阻止方法执行。
     * <p>
     * An exception is thrown, which is used in before to prevent the method from executing.
     */
    final public void makeCrash(Throwable t) {
        methodHookParam.setThrowable(t);
    }

    /**
     * 返回方法调用的结果，或获取该方法的异常。
     * <p>
     * Returns the result of a method call, or gets an exception for that method.
     */
    final public <T> T getResultOrThrowable() throws Throwable {
        return (T) methodHookParam.getResultOrThrowable();
    }

    // --------- 观察调用 --------------

    /**
     * 观察方法是否被调用，如果被调用则打印一些日志。
     * <p>
     * Observe if the method is called, and if it is called, print some logs.
     */
    final public void observeCall() {
        if (logExpand == null) {
            logExpand = new LogExpand(methodHookParam, mTag);
        }
        logExpand.detailedLogs();
    }

    // --------- 调用方法 --------------
    /**
     * 方法具体介绍请看实现类。<br/>
     * <br/>
     * For more information about methods, see Implementation Classes.
     * {@link com.hchen.hooktool.tool.CoreTool}
     */

    /**
     * 请使用 new Object[]{} 传入参数。<br/>
     * 如果仅传入一个参数可以不使用 new Object[]{}<br/>
     * 这是为了规避泛型与可变参数的冲突。
     * <p>
     * Use new Object[]{} to pass in the parameter. <br>
     * If you pass in only one parameter, you can leave out new Object[]{}<br>
     * to avoid conflicts between generics and variadics.
     */
    final public <T, R> R callThisMethod(String name, T ts) {
        return iDynamic.callMethod(methodHookParam.thisObject, name, ts);
    }

    final public <R> R callThisMethod(String name) {
        return iDynamic.callMethod(methodHookParam.thisObject, name);
    }

    // ----------- 获取/修改 字段 -------------

    final public <T> T getThisField(String name) {
        return iDynamic.getField(methodHookParam.thisObject, name);
    }

    final public boolean setThisField(String name, Object value) {
        return iDynamic.setField(methodHookParam.thisObject, name, value);
    }

    // ---------- 设置自定义字段 --------------

    final public boolean setThisAdditionalInstanceField(String key, Object value) {
        return iDynamic.setAdditionalInstanceField(methodHookParam.thisObject, key, value);
    }

    final public <T> T getThisAdditionalInstanceField(String key) {
        return iDynamic.getAdditionalInstanceField(methodHookParam.thisObject, key);
    }

    final public boolean removeThisAdditionalInstanceField(String key) {
        return iDynamic.removeAdditionalInstanceField(methodHookParam.thisObject, key);
    }
}
