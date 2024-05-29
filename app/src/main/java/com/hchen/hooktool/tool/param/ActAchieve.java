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

import com.hchen.hooktool.utils.DataUtils;

import de.robv.android.xposed.XC_MethodHook;

public class ActAchieve extends StaticAct {
    protected XC_MethodHook.MethodHookParam param;

    public ActAchieve(DataUtils utils) {
        super(utils);
    }

    protected void setParam(XC_MethodHook.MethodHookParam param) {
    }

    public <T> T getResult() {
        return (T) param.getResult();
    }

    public void returnNull() {
        param.setResult(null);
    }

    public <T> void setResult(T value) {
        param.setResult(value);
    }

    public boolean hasThrowable() {
        return param.hasThrowable();
    }

    public Throwable getThrowable() {
        return param.getThrowable();
    }

    public void setThrowable(Throwable t) {
        param.setThrowable(t);
    }

    public <T> T getResultOrThrowable() throws Throwable {
        return (T) param.getResultOrThrowable();
    }

    // --------- 调用方法 --------------
    /**
     * 请使用 new Object[]{} 传入参数。<br/>
     * 如果仅传入一个参数可以不使用 new Object[]{}<br/>
     * 这是为了规避泛型与可变参数的冲突。
     */
    public <T, R> R callMethod(String name, T ts) {
        return iDynamic.callMethod(param.thisObject, name, genericToObjectArray(ts));
    }

    public <R> R callMethod(String name) {
        return iDynamic.callMethod(param.thisObject, name);
    }

    public <T, R> R callMethod(Object instance, String name, T ts) {
        return iDynamic.callMethod(instance, name, genericToObjectArray(ts));
    }

    public <R> R callMethod(Object instance, String name) {
        return iDynamic.callMethod(instance, name);
    }

    // ----------- 获取/修改 字段 -------------

    public <T> T getField(String name) {
        return iDynamic.getField(param.thisObject, name);
    }

    public boolean setField(String name, Object key) {
        return iDynamic.setField(param.thisObject, name, key);
    }

    public <T> T getField(Object instance, String name) {
        return iDynamic.getField(instance, name);
    }

    public boolean setField(Object instance, String name, Object key) {
        return iDynamic.setField(instance, name, key);
    }

    // ---------- 设置自定义字段 --------------
    public boolean setAdditionalInstanceField(String name, Object key) {
        return iDynamic.setAdditionalInstanceField(param.thisObject, name, key);
    }

    public <T> T getAdditionalInstanceField(String name) {
        return iDynamic.getAdditionalInstanceField(param.thisObject, name);
    }

    public boolean removeAdditionalInstanceField(String name) {
        return iDynamic.removeAdditionalInstanceField(param.thisObject, name);
    }

    public boolean setAdditionalInstanceField(Object instance, String name, Object key) {
        return iDynamic.setAdditionalInstanceField(instance, name, key);
    }

    public <T> T getAdditionalInstanceField(Object instance, String name) {
        return iDynamic.getAdditionalInstanceField(instance, name);
    }

    public boolean removeAdditionalInstanceField(Object instance, String name) {
        return iDynamic.removeAdditionalInstanceField(instance, name);
    }
}
