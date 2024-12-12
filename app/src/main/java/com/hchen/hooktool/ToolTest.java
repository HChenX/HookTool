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
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 测试和示例类
 *
 * @author 焕晨HChen
 */
@Deprecated
final class ToolTest extends BaseHC {
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        super.initZygote(startupParam);
    }

    @Override
    public void init() {
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
        }).unhook();

        // 本工具用法
        new IHook() {
            @Override
            public void before() {
                // hook 方法所属的类
                Class<?> c = mClass;
                Context context = (Context) thisObject();
                String string = (String) getArgs(0); // 获取第一个参数值
                setArgs(1, 1); // 设置第二个参数值

                // 非静态本类内
                setThisField("demo", 1); // 设置本类内 demo 字段值
                callThisMethod("method"); // 调用本类内 method 方法
                getThisField("test");
                String result = (String) callThisMethod("call", thisObject(), getArgs(0));

                // 非静态本类外
                Object o = null;
                setField(o, "demo", 1); // 设置实例 o 的 demo 字段
                callMethod(o, "method");
                getField(o, "test");

                // 静态需要 class
                callStaticMethod("com.demo.Main", "callStatic", thisObject(), getArgs(1)); // 调用静态方法 callStatic
                int i = (int) getStaticField("com.demo.Main", "field");
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

    private static class InitHook extends HCEntrance {
        @Override
        public HCInit.BasicData initHC(HCInit.BasicData basicData) {
            return basicData.setTag("HookTool")
                    .setLogLevel(HCInit.LOG_D)
                    .setModulePackageName("com.hchen.demo")
                    .setPrefsName("myprefs") // 可选
                    .xPrefsAutoReload(true) // 可选
                    .initLogExpand(new String[]{
                            "com.hchen.demo.hook"
                    });
        }

        @Override
        public void onLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
            new ToolTest().onLoadPackage();
        }
    }
}
