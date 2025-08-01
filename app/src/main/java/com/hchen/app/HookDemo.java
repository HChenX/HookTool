/*
 * This file is part of HookTool.
 *
 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * HookTool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with HookTool. If not, see <https://www.gnu.org/licenses/lgpl-2.1>.
 *
 * Copyright (C) 2023–2025 HChenX
 */
package com.hchen.app;

import android.content.Context;

import androidx.annotation.NonNull;

import com.hchen.hooktool.HCBase;
import com.hchen.hooktool.hook.IHook;

import de.robv.android.xposed.IXposedHookZygoteInit;

/**
 * Hook 示例
 *
 * @author 焕晨HChen
 */
public class HookDemo extends HCBase /* 建议继承 HCBase 使用 */ {
    @Override
    protected boolean isEnabled() {
        // 是否启用本 Hook
        return super.isEnabled();
    }

    @Override
    protected void init() { // loadPackage 阶段
        boolean isExists = existsClass("com.hchen.demo.Demo"); // 是否存在类
        Class<?> clazz = findClass("com.hchen.demo.Demo"); // 查找类

        hookMethod("com.hchen.demo.Demo", "demo", boolean.class, new IHook() {
            @Override
            public void before() {
                // 在 demo 方法调用前执行
                // 可以拦截方法执行，或者修改方法参数值
                setResult(true); // 拦截并返回 true
                setArg(0, false); // 设置方法第一个参数为 false
            }

            @Override
            public void after() {
                // 在 demo 方法执行后调用
                // 可以用于修改方法返回结果
                setResult(true);
            }

            @Override
            public boolean onThrow(int flag, Throwable e) {
                // before 或者 after 内代码抛错时会调用
                // 返回 true 代表已处理异常，工具将不会自动处理
                return super.onThrow(flag, e);
            }
        });
    }

    @Override
    protected void init(@NonNull ClassLoader classLoader) { // loadPackage 阶段
        // 区别是可以指定自定义的 classloader
        findClass("com.hchen.demo.Demo", classLoader);
    }

    @Override
    protected void initZygote(@NonNull IXposedHookZygoteInit.StartupParam startupParam) { // zygote 阶段
        findClass("com.hchen.demo.Demo", startupParam.getClass().getClassLoader()); // 可以这样写
    }

    @Override
    protected void initApplicationBefore(@NonNull Context context) {
        // 目标应用创建 Context 前回调
    }

    @Override
    protected void initApplicationAfter(@NonNull Context context) {
        // 目标应用创建 Context 后回调
    }

    @Override
    protected void onThrowable(int flag, @NonNull Throwable e) {
        // 上述方法发生抛错时调用，你可以在此处执行清理操作，不建议继续执行 Hook 逻辑
    }
}
