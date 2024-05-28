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

import androidx.annotation.Nullable;

import com.hchen.hooktool.utils.DataUtils;

import de.robv.android.xposed.XC_MethodHook;

public class ActAchieve extends StaticAct {
    protected XC_MethodHook.MethodHookParam param;

    public ActAchieve(DataUtils utils) {
        super(utils);
    }

    protected void setParam(XC_MethodHook.MethodHookParam param) {
    }

    @Nullable
    public <T> T getResult() {
        paramSafe();
        return (T) param.getResult();
    }

    public void returnNull() {
        paramSafe();
        param.setResult(null);
    }

    public <T> void setResult(T value) {
        paramSafe();
        param.setResult(value);
    }

    public boolean hasThrowable() {
        paramSafe();
        return param.hasThrowable();
    }

    @Nullable
    public Throwable getThrowable() {
        paramSafe();
        return param.getThrowable();
    }

    public void setThrowable(Throwable t) {
        paramSafe();
        param.setThrowable(t);
    }

    @Nullable
    public <T> T getResultOrThrowable() throws Throwable {
        paramSafe();
        return (T) param.getResultOrThrowable();
    }

    /**
     * 请使用 new Object[]{} 传入参数。<br/>
     * 如果仅传入一个参数可以不使用 new Object[]{}<br/>
     * 这是为了规避泛型与可变参数的冲突。
     */
    @Nullable
    public <T, R> R callMethod(String name, T ts) {
        paramSafe();
        return iDynamic.callMethod(param.thisObject, name, genericToObjectArray(ts));
    }

    @Nullable
    public <R> R callMethod(String name) {
        return iDynamic.callMethod(param.thisObject, name);
    }

    @Nullable
    public <T> T getField(String name) {
        paramSafe();
        return iDynamic.getField(param.thisObject, name);
    }

    public boolean setField(String name, Object key) {
        paramSafe();
        return iDynamic.setField(param.thisObject, name, key);
    }

    public boolean setAdditionalInstanceField(String name, Object key) {
        paramSafe();
        return iDynamic.setAdditionalInstanceField(param.thisObject, name, key);
    }

    @Nullable
    public <T> T getAdditionalInstanceField(String name) {
        paramSafe();
        return iDynamic.getAdditionalInstanceField(param.thisObject, name);
    }

    public boolean removeAdditionalInstanceField(String name) {
        paramSafe();
        return iDynamic.removeAdditionalInstanceField(param.thisObject, name);
    }

    protected void paramSafe() {
        if (param == null) {
            throw new RuntimeException(utils.getTAG() + " param is null! member: " + param.method.getName());
        }
    }
}
