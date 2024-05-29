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

    private Object instance = null;
    private boolean needHoming = false;

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

    public ActAchieve to(Object instance) {
        return to(instance, true);
    }

    /**
     * 让指定的实例进行下面的动作设置。
     *
     * @param instance 实例
     * @param homing   是否归位实例
     * @return this
     */
    public ActAchieve to(Object instance, boolean homing) {
        homing();
        needHoming = homing;
        this.instance = instance;
        return this;
    }

    // --------- 调用方法 --------------

    /**
     * 请使用 new Object[]{} 传入参数。<br/>
     * 如果仅传入一个参数可以不使用 new Object[]{}<br/>
     * 这是为了规避泛型与可变参数的冲突。
     */
    public <T, R> R callMethod(String name, T ts) {
        return iDynamic.callMethod(checkAndReturn(), name, genericToObjectArray(ts));
    }

    public <R> R callMethod(String name) {
        return iDynamic.callMethod(checkAndReturn(), name);
    }

    // ----------- 获取/修改 字段 -------------

    public <T> T getField(String name) {
        return iDynamic.getField(checkAndReturn(), name);
    }

    public boolean setField(String name, Object key) {
        return iDynamic.setField(checkAndReturn(), name, key);
    }

    // ---------- 设置自定义字段 --------------
    public boolean setAdditionalInstanceField(String name, Object key) {
        return iDynamic.setAdditionalInstanceField(checkAndReturn(), name, key);
    }

    public <T> T getAdditionalInstanceField(String name) {
        return iDynamic.getAdditionalInstanceField(checkAndReturn(), name);
    }

    public boolean removeAdditionalInstanceField(String name) {
        return iDynamic.removeAdditionalInstanceField(checkAndReturn(), name);
    }

    private Object checkAndReturn() {
        if (instance == null) return param.thisObject;
        Object i = instance;
        if (needHoming) homing();
        return i;
    }

    /**
     * 手动归位。
     */
    public void homing() {
        instance = null;
    }
}
