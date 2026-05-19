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
 * 钩子桥接器，负责将 {@link AbsHook} 实现适配并对接到 Xposed 拦截器接口。
 * <p>
 * 本类在 {@link AbsHook} 与 {@link XposedInterface.HookBuilder} 之间起到桥接作用，主要职责包括：
 * <ul>
 *     <li>将钩子优先级传递至底层构建器</li>
 *     <li>在拦截器回调中调度 {@link AbsHook} 的完整生命周期（{@code before → proceed → after}）</li>
 *     <li>在各阶段捕获异常并委派给 {@link AbsHook#onThrow(AbsHook.StageEnum, Throwable)} 处理</li>
 *     <li>管理拦截上下文的进入与退出</li>
 * </ul>
 *
 * @author 焕晨HChen
 * @see AbsHook
 */
public final class HookBridge {
    private final XposedInterface.HookBuilder builder;

    /**
     * 构造一个新的钩子桥接器实例。
     *
     * @param builder 用于配置拦截参数并最终创建钩子的 Xposed API 构建器，不为 {@code null}
     */
    public HookBridge(@NonNull XposedInterface.HookBuilder builder) {
        this.builder = builder;
    }

    /**
     * 将钩子优先级设置到底层构建器中。
     *
     * @param priority 优先级数值，值越小优先级越高
     */
    private void setPriority(int priority) {
        Objects.requireNonNull(builder);
        builder.setPriority(priority);
    }

    /**
     * 注册拦截器，将指定的 {@link AbsHook} 实现绑定到目标方法上。
     * <p>
     * 内部创建匿名 {@link XposedInterface.Hooker} 实现，在拦截回调中按如下顺序调度钩子生命周期：
     * <ol>
     *     <li>进入拦截上下文（{@code enter}）</li>
     *     <li>执行 {@link AbsHook#before()} 前置拦截</li>
     *     <li>检查是否已有返回值替换或累积异常；若是则跳过原方法调用</li>
     *     <li>执行 {@link AbsHook#proceed(XposedInterface.Chain)} 调用原方法</li>
     *     <li>执行 {@link AbsHook#after()} 后置拦截</li>
     *     <li>检查并抛出累积的异常</li>
     *     <li>退出拦截上下文（{@code exit}，在 {@code finally} 块中确保执行）</li>
     * </ol>
     * <p>
     * 各阶段捕获到的异常会先委派给 {@link AbsHook#onThrow(AbsHook.StageEnum, Throwable)}，
     * 若未被消费则根据阶段决定是否立即抛出。
     *
     * @param absHook 自定义钩子实例，不为 {@code null}
     * @return 框架返回的钩子句柄，可用于后续解除钩子等操作，不为 {@code null}
     */
    @NonNull
    public XposedInterface.HookHandle intercept(@NonNull AbsHook absHook) {
        Objects.requireNonNull(builder);
        setPriority(absHook.priority);
        XposedInterface.HookHandle handle = builder.intercept(new XposedInterface.Hooker() {
            @Override
            public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                absHook.enter(chain);

                try {
                    try {
                        absHook.before();
                    } catch (Throwable throwable) {
                        if (!absHook.onThrow(AbsHook.StageEnum.BEFORE, throwable)) {
                            absHook.setThrowable(throwable);
                            throw throwable;
                        }
                    }

                    if (absHook.getThrowable() != null) {
                        throw absHook.getThrowable();
                    }

                    if (absHook.isResultChanged()) {
                        return absHook.getResult();
                    }

                    try {
                        Object result = absHook.proceed(absHook.getChain());
                        absHook.setOriginalResult(result);
                    } catch (Throwable throwable) {
                        if (!absHook.onThrow(AbsHook.StageEnum.PROCEED, throwable)) {
                            absHook.setThrowable(throwable);
                            // 此处不抛出异常，给予 after 拦截的机会
                        }
                    }

                    try {
                        absHook.after();
                    } catch (Throwable throwable) {
                        if (!absHook.onThrow(AbsHook.StageEnum.AFTER, throwable)) {
                            absHook.setThrowable(throwable);
                            throw throwable;
                        }
                    }

                    if (absHook.getThrowable() != null) {
                        throw absHook.getThrowable();
                    }

                    return absHook.getResult();
                } finally {
                    absHook.exit();
                }
            }
        });
        absHook.setHandle(handle);
        return handle;
    }
}
