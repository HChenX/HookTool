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
import com.hchen.hooktool.hook.chain.ChainTool;
import com.hchen.hooktool.log.AndroidLog;
import com.hchen.hooktool.reflect.Reflect;

import java.util.ArrayList;
import java.util.List;

/**
 * 链式调用示例。
 *
 * @author 焕晨HChen
 */
public final class ChainHooks implements DemoHook {
    private static final String TAG = "ChainHooks";
    private final List<AbsHook> mHooks = new ArrayList<>();

    @Override
    public void install() {
        AbsHook secretHook = new AbsHook() {
            @Override
            public void after() {
                setResult("hooked-secret");
            }
        };
        mHooks.add(secretHook);
        ChainTool.buildChain(Target.class)
            .findMethod("getSecret")
            .hook(secretHook);

        AbsHook constructorHook = new AbsHook() {
            @Override
            public void before() {
                AndroidLog.logD(TAG, "构造参数: " + getArg(0));
            }
        };
        mHooks.add(constructorHook);
        ChainTool.buildChain(Target.class)
            .findConstructor(String.class)
            .ignoreThrow()
            .hook(constructorHook);

        AbsHook executableHook = new AbsHook() {
            @Override
            public void before() {
                AndroidLog.logD(TAG, "静态方法被调用: " + getExecutable());
            }
        };
        mHooks.add(executableHook);
        ChainTool.buildChain(Target.class)
            .withExecutable(Reflect.findMethodExact(Target.class, "add", int.class, int.class))
            .hook(executableHook);
    }

    @Override
    public void uninstall() {
        for (AbsHook hook : mHooks) {
            try {
                hook.unhook();
            } catch (IllegalStateException ignored) {
                // 该 Hook 未生效
            }
        }
        mHooks.clear();
    }
}
