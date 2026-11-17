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
import com.hchen.hooktool.hook.HookInterop;
import com.hchen.hooktool.log.AndroidLog;
import com.hchen.hooktool.reflect.Reflect;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;

/**
 * 调用器与去优化示例。
 *
 * @author 焕晨HChen
 */
public final class InteropHooks {
    private static final String TAG = "InteropHooks";

    private InteropHooks() {
    }

    /**
     * 绕过 Hook 调用原方法，并对方法执行去优化。
     */
    public static void run() {
        try {
            Method method = Reflect.findMethod(Target.class, "greet", String.class);
            XposedInterface.Invoker<?, Method> invoker = HookInterop.getMethodInvoker(method);
            invoker.setType(XposedInterface.Invoker.Type.ORIGIN);
            Object result = invoker.invoke(new Target(), "origin");
            AndroidLog.logD(TAG, "原方法返回值: " + result);

            HookInterop.deoptimize(method);
            AndroidLog.logD(TAG, "去优化完成");
        } catch (Throwable throwable) {
            AndroidLog.logE(TAG, "调用器示例执行失败", throwable);
        }
    }
}
