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
package com.hchen.hooktool.hook;

import androidx.annotation.IntDef;

import com.hchen.hooktool.core.ParamTool;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * IHook
 *
 * @author 焕晨HChen
 */
public abstract class IHook extends ParamTool {
    protected XC_MethodHook xcMethodHook;
    public final int PRIORITY;
    public static final int BEFORE = 1;
    public static final int AFTER = 2;

    @IntDef(value = {
        BEFORE,
        AFTER,
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface ActionFlag {
    }

    public IHook() {
        this.PRIORITY = Priority.DEFAULT;
    }

    /**
     * IHook
     *
     * @param priority 优先级
     */
    public IHook(int priority) {
        this.PRIORITY = priority;
    }

    /**
     * 方法执行之前
     */
    public void before() {
    }

    /**
     * 方法执行后
     */
    public void after() {
    }

    /**
     * Hook 代码抛出异常时调用
     *
     * @param flag 抛出异常的时机
     * @param e    异常
     * @return 是否被处理
     */
    public boolean onThrow(@ActionFlag int flag, Throwable e) {
        return false;
    }

    /**
     * 解除 Hook
     */
    final public void unHookSelf() {
        XposedBridge.unhookMethod(param.method, xcMethodHook);
    }
}
