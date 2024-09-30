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

import static com.hchen.hooktool.tool.CoreTool.callMethod;
import static com.hchen.hooktool.tool.CoreTool.getAdditionalInstanceField;
import static com.hchen.hooktool.tool.CoreTool.getField;
import static com.hchen.hooktool.tool.CoreTool.removeAdditionalInstanceField;
import static com.hchen.hooktool.tool.CoreTool.setAdditionalInstanceField;
import static com.hchen.hooktool.tool.CoreTool.setField;

import com.hchen.hooktool.log.LogExpand;

import de.robv.android.xposed.XC_MethodHook;

/**
 * 动作
 *
 * @author 焕晨HChen
 */
public class ActAchieve {
    public XC_MethodHook.MethodHookParam MethodHookParam;
    public String mTag;
    private LogExpand logExpand;

    /**
     * 获取方法执行完毕后的返回值。
     */
    final public <T> T getResult() {
        return (T) MethodHookParam.getResult();
    }

    /**
     * before 中使用拦截方法执行并直接返回设定值。<br/>
     * after 中使用修改返回结果。
     */
    final public void setResult(Object value) {
        MethodHookParam.setResult(value);
    }

    /**
     * before 中使用使方法失效。<br/>
     * after 中使用修改返回结果。
     */
    final public void returnNull() {
        MethodHookParam.setResult(null);
    }

    /**
     * 使方法返回指定的布尔值 true。
     */
    final public void returnTure() {
        MethodHookParam.setResult(true);
    }

    /**
     * 使方法返回指定布尔值 false。
     */
    final public void returnFalse() {
        MethodHookParam.setResult(false);
    }

    /**
     * 如果方法引发了异常，则返回 true。
     */
    final public boolean hasThrowable() {
        return MethodHookParam.hasThrowable();
    }

    /**
     * 返回该方法抛出的异常或者返回 null。
     */
    final public Throwable getThrowable() {
        return MethodHookParam.getThrowable();
    }

    /**
     * 引发异常，在 before 中使用可阻止方法执行。
     */
    final public void setThrowable(Throwable t) {
        MethodHookParam.setThrowable(t);
    }

    /**
     * 返回方法调用的结果，或获取该方法的异常。
     */
    final public <T> T getResultOrThrowable() throws Throwable {
        return (T) MethodHookParam.getResultOrThrowable();
    }

    // --------- 观察调用 --------------
    /**
     * 观察方法是否被调用，如果被调用则打印一些日志。
     */
    final public void observeCall() {
        if (logExpand == null) {
            logExpand = new LogExpand(MethodHookParam, mTag);
        }
        logExpand.update(MethodHookParam);
        logExpand.detailedLogs();
    }

    // --------- 调用方法 --------------
    final public <T> T callThisMethod(String name, Object... objs) {
        return callMethod(MethodHookParam.thisObject, name, objs);
    }

    // ----------- 获取/修改 字段 -------------
    final public <T> T getThisField(String name) {
        return getField(MethodHookParam.thisObject, name);
    }

    final public boolean setThisField(String name, Object value) {
        return setField(MethodHookParam.thisObject, name, value);
    }

    // ---------- 设置自定义字段 --------------
    final public <T> T setThisAdditionalInstanceField(String key, Object value) {
        return setAdditionalInstanceField(MethodHookParam.thisObject, key, value);
    }

    final public <T> T getThisAdditionalInstanceField(String key) {
        return getAdditionalInstanceField(MethodHookParam.thisObject, key);
    }

    final public <T> T removeThisAdditionalInstanceField(String key) {
        return removeAdditionalInstanceField(MethodHookParam.thisObject, key);
    }
}
