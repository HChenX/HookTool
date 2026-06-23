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
import androidx.annotation.Nullable;

import com.hchen.hooktool.ModuleData;

import java.util.Objects;

import io.github.libxposed.api.XposedInterface;

/**
 * 钩子桥接器，负责将 {@link AbsHook} 实现适配并对接到 Xposed 拦截器接口。
 * <p>
 * 本类在 {@link AbsHook} 与 {@link XposedInterface.HookBuilder} 之间起到桥接作用，主要职责包括：
 * <ul>
 *     <li>将钩子优先级传递至底层构建器</li>
 *     <li>设置钩子唯一标识符（API 102）</li>
 *     <li>设置异常处理模式</li>
 *     <li>在拦截器回调中调度 {@link AbsHook} 的完整生命周期（{@code before → proceed → after}）</li>
 *     <li>在各阶段捕获异常并委派给 {@link AbsHook#onThrow(AbsHook.StageEnum, Throwable)} 处理</li>
 *     <li>管理拦截上下文的进入与退出</li>
 *     <li>将调用方预先计算的声明类名（{@link #key}）和静态性标志（{@link #isStatic}）
 *         注入到 {@link AbsHook} 中，供热重载时按类去重存储 {@code thisObject}</li>
 * </ul>
 *
 * @author 焕晨HChen
 * @see AbsHook
 */
public final class HookBridge {
    private final XposedInterface.HookBuilder builder;
    private final String key;
    private final boolean isStatic;

    /**
     * 构造一个新的钩子桥接器实例。
     *
     * @param builder 用于配置拦截参数并最终创建钩子的 Xposed API 构建器，不为 {@code null}
     */
    public HookBridge(@NonNull XposedInterface.HookBuilder builder) {
        this.builder = builder;
        this.key = null;
        this.isStatic = false;
    }

    /**
     * 构造一个新的钩子桥接器实例，并指定类级标识键与静态性。
     * <p>
     * 键与静态性由调用方（通常是 {@code CoreTool}）根据可执行对象预先计算：
     * <ul>
     *   <li><b>key</b>：声明类的全限定类名，同一类的所有方法/构造函数共享相同 key</li>
     *   <li><b>isStatic</b>：{@code true} 表示静态方法或 {@code <clinit>}，
     *       {@code false} 表示实例方法或构造函数</li>
     * </ul>
     *
     * @param builder  用于配置拦截参数并最终创建钩子的 Xposed API 构建器，不为 {@code null}
     * @param key      声明类的全限定类名，用于热重载时按类去重存储 {@code thisObject}；
     *                 为 {@code null} 表示不参与热重载的自动状态管理
     * @param isStatic 是否为静态上下文；静态钩子在热重载时不会读取/写入 {@code thisObject}
     */
    public HookBridge(@NonNull XposedInterface.HookBuilder builder,
                      @Nullable String key, boolean isStatic) {
        this.builder = builder;
        this.key = key;
        this.isStatic = isStatic;
    }

    /**
     * 设置钩子的唯一标识符。
     * <p>
     * 标识符用于在同一个模块中对同一可执行对象上的 Hook 进行唯一标识。
     * 具有相同 ID 的新 Hook 会原子性地替换旧 Hook。Hook ID 在模块之间隔离。
     * <p>
     * 此 API 从 libxposed API 102 开始可用。
     *
     * @param id Hook 的唯一标识符；为 {@code null} 则表示不关心后续替换
     */
    private void setId(@Nullable String id) {
        if (ModuleData.getApiVersion() >= 102) {
            builder.setId(id);
        }
    }

    /**
     * 设置钩子的异常处理模式。
     * <p>
     * 通过此方法可以配置当钩子拦截器抛出异常时框架的处理策略。
     *
     * @param mode 异常处理模式
     */
    private void setExceptionMode(@NonNull XposedInterface.ExceptionMode mode) {
        builder.setExceptionMode(mode);
    }

    /**
     * 将钩子优先级设置到底层构建器中。
     *
     * @param priority 优先级数值，值越小优先级越高
     */
    private void setPriority(int priority) {
        builder.setPriority(priority);
    }

    /**
     * 注册拦截器，将指定的 {@link AbsHook} 实现绑定到目标方法上。
     * <p>
     * 内部创建匿名 {@link XposedInterface.Hooker} 实现，在拦截回调中按如下顺序调度钩子生命周期：
     * <ol>
     *     <li>将钩子优先级、标识符及异常处理模式传递至底层构建器</li>
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
     * <p>
     * 注意：{@code id} 和 {@code mode} 字段仅在非 {@code null} 时才会传递至底层；
     * 当 {@code id} 不为 {@code null} 时，相同可执行对象 + 相同 id 的旧 Hook 会被原子替换。
     *
     * @param absHook 自定义钩子实例，不为 {@code null}
     * @return 框架返回的钩子句柄，可用于后续解除钩子等操作，不为 {@code null}
     */
    @NonNull
    public XposedInterface.HookHandle intercept(@NonNull AbsHook absHook) {
        Objects.requireNonNull(builder);
        setPriority(absHook.priority);
        if (absHook.id != null) {
            setId(absHook.id);
        }
        if (absHook.mode != null) {
            setExceptionMode(absHook.mode);
        }
        if (key != null) {
            absHook.setKey(key);
        }
        absHook.isStatic = isStatic;
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
