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
 * <p>
 * Parameter tool
 *
 * @author 焕晨HChen
 */
public class ParamTool extends Arguments {
    // hook 的方法的所在类
    public Class<?> mClass;
    // hook 的方法
    public Member mMember;
    // hook 的方法的参数
    public Object[] mArgs;

    private XC_MethodHook xcMethodHook;

    /**
     * 被 hook 类的实例。
     * <p>
     * An instance of the hook class.
     */
    final public <T> T thisObject() {
        return (T) MethodHookParam.thisObject;
    }

    /**
     * 移除 hook 自身。
     * <p>
     * Remove the hook itself.
     */
    final public void removeSelf() {
        XposedBridge.unhookMethod(mMember, xcMethodHook);
    }

    /**
     * 返回被 hook 实例的类加载器。
     * <p>
     * Returns the classloader of the hooked instance.
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
