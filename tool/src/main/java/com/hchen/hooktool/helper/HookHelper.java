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
package com.hchen.hooktool.helper;

import androidx.annotation.NonNull;

import com.hchen.hooktool.core.CoreTool;
import com.hchen.hooktool.hook.IHook;

import java.lang.reflect.Member;

import de.robv.android.xposed.XC_MethodHook;

/**
 * Hook Helper
 *
 * @author 焕晨HChen
 */
public class HookHelper<T extends Member> {
    private final T member;

    protected HookHelper(T member) {
        this.member = member;
    }

    /**
     * Hook 成员
     */
    public XC_MethodHook.Unhook hook(@NonNull IHook iHook) {
        return CoreTool.hook(member, iHook);
    }

    /**
     * 拦截方法执行并返回指定值
     *
     * @param result 要返回的结果
     */
    public XC_MethodHook.Unhook returnResult(final Object result) {
        return CoreTool.hook(member, CoreTool.returnResult(result));
    }

    /**
     * 拦截方法执行
     */
    public XC_MethodHook.Unhook doNothing() {
        return CoreTool.hook(member, CoreTool.doNothing());
    }

    /**
     * 获取成员
     */
    public T get() {
        return member;
    }
}
