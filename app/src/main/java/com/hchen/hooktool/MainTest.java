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

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * 测试和示例类
 * <p>
 * Test and sample classes
 * 
 * @author 焕晨HChen
 */
public class MainTest extends BaseHC {
    public static BaseHC sHc;
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        super.initZygote(startupParam);
    }

    @Override
    public void init() {
        sHc = baseHC;
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

        chain("com.hchen.demo", method("test")
                .hook(new IAction() {
                    @Override
                    public void before() {
                        super.before();
                    }
                })

                .method("test_1", String.class)
                .hook(new IAction() {
                    @Override
                    public void after() {
                        super.after();
                    }
                })

                .constructor()
                .returnResult(false)
        );

        hook(findMethod("com.hchen.demo", "test"), new IAction() {
            @Override
            public void before() {
                super.before();
            }
        }).unHook();

        new IAction() {
            @Override
            public void before() {
                // hook 方法所属的类
                Class<?> c = mClass;
                Context context = thisObject();
                String string = first();
                second(1);
                
                // 非静态本类内
                setThisField("demo", 1);
                callThisMethod("method");
                getThisField("test");
                String result = callThisMethod("call", thisObject(), first());
                // 非静态本类外
                Object o = null;
                setField(o, "demo", 1);
                callMethod(o, "method");
                getField(o, "test");

                // 静态需要 class
                callStaticMethod("com.demo.Main", "callStatic", thisObject(), second());
                int i = getStaticField("com.demo.Main", "field");
                setStaticField("com.demo.Main", "test", true);

                // 移除自身
                removeSelf();
                // 观察调用
                observeCall();
                // 获取堆栈
                getStackTrace();
            }
        };
    }
}