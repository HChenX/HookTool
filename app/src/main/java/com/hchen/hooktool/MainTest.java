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
package com.hchen.hooktool;

import android.content.Context;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.tool.ParamTool;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * 测试和示例类
 */
public class MainTest extends BaseHC {

    @Override
    public void init() {
        new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Context context = (Context) param.thisObject;
                String string = (String) param.args[0];
                param.args[1] = 1;
                String result = (String) XposedHelpers.callMethod(param.thisObject, "call",
                        param.thisObject, param.args[0]);
                XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.demo.Main", ClassLoader.getSystemClassLoader()),
                        "callStatic", param.thisObject, param.args[1]);
                int i = (int) XposedHelpers.getStaticObjectField(XposedHelpers.findClass("com.demo.Main", ClassLoader.getSystemClassLoader()),
                        "field");
            }
        };

        new IAction() {
            @Override
            public void before(ParamTool param) {
                // hook 方法所属的类
                Class<?> c = param.mClass;

                Context context = param.thisObject();
                String string = param.first();
                param.second(1);

                // 非静态的本类内实例可直接使用 param.xx() 进行设置。
                param.setField("demo", 1);
                param.callMethod("method");
                param.getField("test");

                // 静态需要 class
                String result = param.callMethod("call", new Object[]{param.thisObject(), param.first()});
                callStaticMethod(findClass("com.demo.Main"), "callStatic", new Object[]{param.thisObject(), param.second()});
                int i = getStaticField(findClass("com.demo.Main"), "field");
                setStaticField(findClass("com.demo.Main"), "test", true);
            }
        };
    }
}