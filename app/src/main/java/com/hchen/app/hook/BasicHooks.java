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
 * Copyright (C) 2024–2026 HChenX
 */
package com.hchen.app.hook;

import com.hchen.app.demo.Target;
import com.hchen.hooktool.hook.AbsHook;
import com.hchen.hooktool.hook.Hook;
import com.hchen.hooktool.log.AndroidLog;

import java.util.ArrayList;
import java.util.List;

import io.github.libxposed.api.XposedInterface;

/**
 * Hook 门面的基础用法。
 * <p>
 * 覆盖方法、静态方法、构造函数、静态初始化块、同名方法批量安装与完全拦截。
 *
 * @author 焕晨HChen
 */
public final class BasicHooks implements DemoHook {
    private static final String TAG = "BasicHooks";
    private final List<XposedInterface.HookHandle> mHandles = new ArrayList<>();

    @Override
    public void install() {
        mHandles.add(Hook.hookMethod(Target.class, "greet", String.class, new AbsHook() {
            @Override
            public void before() {
                AndroidLog.logD(TAG, "greet 参数: " + getArg(0));
                setArg(0, "hooktool");
            }

            @Override
            public void after() {
                AndroidLog.logD(TAG, "greet 返回值: " + getResult());
            }
        }));

        mHandles.add(Hook.hookMethod(Target.class, "add", int.class, int.class,
            Hook.returnResult(100)));

        mHandles.add(Hook.hookConstructor(Target.class, String.class, new AbsHook() {
            @Override
            public void after() {
                AndroidLog.logD(TAG, "构造完成: " + getThisObject());
            }
        }));

        mHandles.add(Hook.hookClassInitializer(Target.class, new AbsHook() {
            @Override
            public void after() {
                AndroidLog.logD(TAG, "静态初始化块执行完成");
            }
        }));

        for (XposedInterface.HookHandle handle : Hook.hookAllMethod(Target.class, "describe",
            new AbsHook() {
                @Override
                public void before() {
                    AndroidLog.logD(TAG, "describe 被调用: " + getExecutable());
                }
            })) {
            mHandles.add(handle);
        }

        mHandles.add(Hook.hookMethod(Target.class, "noise", Hook.doNothing()));

        XposedInterface.HookHandle absent =
            Hook.hookMethodIfExists(Target.class, "absent", Hook.doNothing());
        AndroidLog.logD(TAG, "absent 方法安装结果: " + absent);
    }

    @Override
    public void uninstall() {
        for (XposedInterface.HookHandle handle : mHandles) {
            handle.unhook();
        }
        mHandles.clear();
    }
}
