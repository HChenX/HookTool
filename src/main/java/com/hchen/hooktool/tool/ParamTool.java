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

import com.hchen.hooktool.tool.param.Arguments;

import java.lang.reflect.Member;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * 参数工具
 *
 * @author 焕晨HChen
 */
public class ParamTool extends Arguments {
    public Class<?> mClass;
    public Member mMember;
    public Object[] mArgs;

    private XC_MethodHook xcMethodHook;

    /**
     * 被 hook 类的实例。
     */
    final public <T> T thisObject() {
        return (T) MethodHookParam.thisObject;
    }

    /**
     * 移除 hook 自身。
     */
    final public void removeSelf() {
        XposedBridge.unhookMethod(mMember, xcMethodHook);
    }

    /**
     * 返回被 hook 实例的类加载器。
     */
    final public ClassLoader thisClassLoader() {
        return MethodHookParam.thisObject.getClass().getClassLoader();
    }

    final public void MethodHookParam(XC_MethodHook.MethodHookParam param) {
        this.MethodHookParam = param;
        mClass = param.method.getDeclaringClass();
        mMember = param.method;
        mArgs = param.args;
    }

    final public void XCMethodHook(XC_MethodHook xcMethodHook) {
        this.xcMethodHook = xcMethodHook;
    }
}
