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
package com.hchen.hooktool.hook;

import androidx.annotation.NonNull;

import java.util.Objects;

import io.github.libxposed.api.XposedInterface;

/**
 * Hook 适配桥
 *
 * @author 焕晨HChen
 */
public class HookBridge {
    private final XposedInterface.HookBuilder builder;

    public HookBridge(@NonNull XposedInterface.HookBuilder builder) {
        this.builder = builder;
    }

    /**
     * 设置钩子优先级
     *
     * @param priority 优先级值
     */
    public HookBridge setPriority(int priority) {
        Objects.requireNonNull(builder);
        builder.setPriority(priority);
        return this;
    }

    /**
     * 设置钩子拦截器
     * <p>
     * 将自定义的 AbsHook 实现与 Xposed API 连接起来
     *
     * @param absHook 自定义的钩子实现
     * @return 钩子句柄，可用于后续操作（如解除钩子）
     */
    public XposedInterface.HookHandle intercept(@NonNull AbsHook absHook) {
        Objects.requireNonNull(builder);
        XposedInterface.HookHandle handle = builder.intercept(new XposedInterface.Hooker() {
            @Override
            public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                absHook.setChain(chain);
                absHook.reset();

                try {
                    absHook.setCurrentStage(AbsHook.StageEnum.BEFORE);
                    absHook.before();
                } catch (Throwable throwable) {
                    if (!absHook.onThrow(AbsHook.StageEnum.BEFORE, throwable)) {
                        throw throwable;
                    }
                }

                try {
                    absHook.setCurrentStage(AbsHook.StageEnum.PROCEED);
                    absHook.callProceed();
                } catch (Throwable throwable) {
                    if (!absHook.onThrow(AbsHook.StageEnum.PROCEED, throwable)) {
                        throw throwable;
                    }
                }

                try {
                    absHook.setCurrentStage(AbsHook.StageEnum.AFTER);
                    absHook.after();
                } catch (Throwable throwable) {
                    if (!absHook.onThrow(AbsHook.StageEnum.AFTER, throwable)) {
                        throw throwable;
                    }
                }

                return absHook.getResult();
            }
        });
        absHook.setHandle(handle);
        return handle;
    }
}
