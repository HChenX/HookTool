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

import com.hchen.hooktool.HCInit;
import com.hchen.hooktool.tool.param.Arguments;
import com.hchen.hooktool.utils.DataUtils;

import java.lang.reflect.Member;

import de.robv.android.xposed.XC_MethodHook;

/**
 * 参数工具
 */
public class ParamTool extends Arguments {
    public Class<?> mClass;
    public Member mMember;
    public Object[] mParam;

    public ParamTool(DataUtils utils) {
        super(utils);
    }

    @Override
    protected void setParam(XC_MethodHook.MethodHookParam param) {
        if (param == null)
            throw new RuntimeException(HCInit.getTAG() + "[" + utils.getTAG() + "][E]: param is null!!");
        this.param = param;
        mClass = param.method.getDeclaringClass();
        mMember = param.method;
        mParam = param.args;
    }

    public <T> T thisObject() {
        return (T) param.thisObject;
    }

    /**
     * 获取原 Xposed param 参数。
     */
    public XC_MethodHook.MethodHookParam originalParam() {
        return param;
    }
}
