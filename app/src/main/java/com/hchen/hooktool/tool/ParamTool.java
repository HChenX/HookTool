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
import com.hchen.hooktool.utils.ToolData;

import java.lang.reflect.Member;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * 参数工具
 * <p>
 * Parameter tool
 */
public class ParamTool extends Arguments {
    // hook 的方法的所在类
    public Class<?> mClass;
    // hook 的方法
    public Member mMember;
    // hook 的方法的参数
    public Object[] mArgs;
    // 自身
    public ParamTool paramTool = this;

    private XC_MethodHook xcMethodHook;

    final protected void putMethodHookParam(XC_MethodHook.MethodHookParam param) {
        if (param == null)
            throw new RuntimeException(ToolData.mInitTag + "[" + mTag + "][E]: param is null!!");
        this.methodHookParam = param;
        mClass = param.method.getDeclaringClass();
        mMember = param.method;
        mArgs = param.args;
    }

    final protected void putXCMethodHook(XC_MethodHook xcMethodHook) {
        this.xcMethodHook = xcMethodHook;
    }

    final protected void putUtils(ToolData data) {
        mTag = data.tag();
        iDynamic = data.coreTool();
    }

    /**
     * 被 hook 类的实例。
     * <p>
     * An instance of the hook class.
     */
    final public <T> T thisObject() {
        return (T) methodHookParam.thisObject;
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
    final public ClassLoader classLoader() {
        return methodHookParam.thisObject.getClass().getClassLoader();
    }

    /**
     * 获取原参数。
     * <p>
     * Get the original parameters.
     */
    final public XC_MethodHook.MethodHookParam originalParam() {
        return methodHookParam;
    }
}
