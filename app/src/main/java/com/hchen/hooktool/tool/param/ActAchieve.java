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

import de.robv.android.xposed.XC_MethodHook;

/**
 * 动作
 */
public class ActAchieve {
    protected XC_MethodHook.MethodHookParam methodHookParam;
    protected String mTag;
    protected IDynamic iDynamic;
    // protected ToolData data;

    /**
     * 获取方法执行完毕后的返回值。
     *
     * @return 方法返回值。
     */
    final public <T> T getResult() {
        return (T) methodHookParam.getResult();
    }

    /**
     * before 中使用使方法失效。<br/>
     * after 中使用修改返回结果。
     */
    final public void returnNull() {
        methodHookParam.setResult(null);
    }

    /**
     * 使方法返回指定的布尔值 true。
     */
    final public void returnTure() {
        methodHookParam.setResult(true);
    }

    /**
     * 使方法返回指定布尔值 false。
     */
    final public void returnFalse() {
        methodHookParam.setResult(false);
    }

    /**
     * before 中使用拦截方法执行并直接返回设定值。<br/>
     * after 中使用修改返回结果。
     */
    final public <T> void setResult(T value) {
        methodHookParam.setResult(value);
    }

    /**
     * 如果方法引发了异常，则返回 true。
     */
    final public boolean hasCrash() {
        return methodHookParam.hasThrowable();
    }

    /**
     * 返回该方法抛出的 throwable 或者返回 null。
     */
    final public Throwable getCrash() {
        return methodHookParam.getThrowable();
    }

    /**
     * 调用方法将引发异常，在 before 中使用可阻止方法执行。
     */
    final public void makeCrash(Throwable t) {
        methodHookParam.setThrowable(t);
    }

    /**
     * 返回方法调用的结果，或抛出由该方法调用引起的 Throwable。
     */
    final public <T> T getResultOrThrowable() throws Throwable {
        return (T) methodHookParam.getResultOrThrowable();
    }

    // --------- 调用方法 --------------
    /**
     * 方法具体介绍请看实现类。<br/>
     * {@link com.hchen.hooktool.tool.CoreTool}
     */

    /**
     * 请使用 new Object[]{} 传入参数。<br/>
     * 如果仅传入一个参数可以不使用 new Object[]{}<br/>
     * 这是为了规避泛型与可变参数的冲突。
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
