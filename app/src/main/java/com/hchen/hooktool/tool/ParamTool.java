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
package com.hchen.hooktool.tool;

import androidx.annotation.Nullable;

import com.hchen.hooktool.tool.param.Arguments;

import java.lang.reflect.Member;

import de.robv.android.xposed.XC_MethodHook;

public class ParamTool extends Arguments {
    public Class<?> mClass;

    public ParamTool(Member member, String tag) {
        super(member, tag);
        mClass = member.getDeclaringClass();
    }

    @Override
    protected void setParam(XC_MethodHook.MethodHookParam param) {
        this.param = param;
    }

    @Nullable
    public <T> T thisObject() {
        paramSafe();
        return (T) param.thisObject;
    }

    public Member method() {
        if (param == null) {
            return member;
        }
        return param.method;
    }

    /**
     * 获取原 Xposed param 参数。
     */
    public XC_MethodHook.MethodHookParam originalParam() {
        return param;
    }
}
