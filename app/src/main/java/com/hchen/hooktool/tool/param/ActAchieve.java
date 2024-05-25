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

import static com.hchen.hooktool.log.XposedLog.logE;

import androidx.annotation.Nullable;

import java.lang.reflect.Member;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class ActAchieve {
    protected XC_MethodHook.MethodHookParam param;
    protected final Member member;
    protected final String TAG;

    public ActAchieve(Member member, String tag) {
        this.member = member;
        TAG = tag;
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
        try {
            return (R) XposedHelpers.callMethod(param.thisObject, name, tToObject(ts));
        } catch (Throwable e) {
            logE(TAG, "call method failed!", e);
        }
        return null;
    }

    @Nullable
    public <R> R callMethod(String name) {
        return callMethod(name, new Object[]{});
    }

    @Nullable
    public <T> T getField(String name) {
        paramSafe();
        try {
            return (T) XposedHelpers.getObjectField(param.thisObject, name);
        } catch (Throwable e) {
            logE(TAG, "get field failed!", e);
        }
        return null;
    }

    public boolean setField(String name, Object key) {
        paramSafe();
        try {
            XposedHelpers.setObjectField(param.thisObject, name, key);
            return true;
        } catch (Throwable e) {
            logE(TAG, "set field failed!", e);
        }
        return false;
    }

    public boolean setAdditionalInstanceField(String name, Object key) {
        paramSafe();
        try {
            XposedHelpers.setAdditionalInstanceField(param.thisObject, name, key);
            return true;
        } catch (Throwable e) {
            logE(TAG, "set additional failed!", e);
        }
        return false;
    }

    @Nullable
    public <T> T getAdditionalInstanceField(String name) {
        paramSafe();
        try {
            return (T) XposedHelpers.getAdditionalInstanceField(param.thisObject, name);
        } catch (Throwable e) {
            logE(TAG, "get additional failed!", e);
        }
        return null;
    }

    public boolean removeAdditionalInstanceField(String name) {
        paramSafe();
        try {
            XposedHelpers.removeAdditionalInstanceField(param.thisObject, name);
            return true;
        } catch (Throwable e) {
            logE(TAG, "remove additional failed!", e);
        }
        return false;
    }

    protected void paramSafe() {
        if (param == null) {
            throw new RuntimeException(TAG + " param is null! member: " + member.getName());
        }
    }

    private <T> Object[] tToObject(T ts) {
        if (ts instanceof Object[] objects) {
            return objects;
        }
        return new Object[]{ts};
    }
}
