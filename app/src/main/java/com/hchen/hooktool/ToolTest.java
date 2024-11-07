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

import com.hchen.hooktool.hook.IHook;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * 测试和示例类
 *
 * @author 焕晨HChen
 */
public class ToolTest extends BaseHC {

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        super.initZygote(startupParam);
    }

    @Override
    public void init() {

        // 原用法
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

        // 链式
        chain("com.hchen.demo", method("test")
                .hook(new IHook() {
                    @Override
                    public void before() {
                        super.before();
                    }
                })

                .method("test1", String.class)
                .hook(new IHook() {
                    @Override
                    public void after() {
                        super.after();
                    }
                })

                .constructor()
                .returnResult(false)
        );

        hookMethod("com.hchen.demo", "test", new IHook() {
            @Override
            public void before() {
                super.before();
            }
        }).unHook();

        // 本工具用法
        new IHook() {
            @Override
            public void before() {
                // hook 方法所属的类
                Class<?> c = mClass;
                Context context = thisObject();
                String string = getArgs(0); // 获取覅一个参数值
                setArgs(1, 1); // 设置第二个参数值

                // 非静态本类内
                setThisField("demo", 1); // 设置本类内 demo 字段值
                callThisMethod("method"); // 调用本类内 method 方法
                getThisField("test");
                String result = callThisMethod("call", thisObject(), getArgs(0));

                // 非静态本类外
                Object o = null;
                setField(o, "demo", 1); // 设置实例 o 的 demo 字段
                callMethod(o, "method");
                getField(o, "test");

                // 静态需要 class
                callStaticMethod("com.demo.Main", "callStatic", thisObject(), getArgs(1)); // 调用静态方法 callStatic
                int i = getStaticField("com.demo.Main", "field");
                setStaticField("com.demo.Main", "test", true); // 设置静态字段 test


                removeSelf(); // 移除自身
                observeCall(); // 观察调用
                getStackTrace(); // 获取堆栈
            }
        };

        prefs().get("test_key", "0"); // 获取 prefs test_key 的值
        prefs().getBoolean("test_key_bool", false); // 获取 prefs test_key_bool 的值

        getFakeResId("test_res"); // 获取 test_res 的虚拟资源 id
        // 设置 pkg 的 string 资源 test_res_str 值为 HC!
        setObjectReplacement("com.hchen.demo", "string", "test_res_str", "HC!");
    }
}
