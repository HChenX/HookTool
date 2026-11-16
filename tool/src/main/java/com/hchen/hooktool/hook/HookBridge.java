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
 *
 * @author 焕晨HChen
 */
public final class HookBridge {
    private final XposedInterface.HookBuilder mBuilder;
    private final String mKey;
    private final boolean mIsStatic;

    /**
     * 构造一个新的钩子桥接器实例。
     *
     * @param builder 用于配置拦截参数并最终创建钩子的 Xposed API 构建器
     */
    public HookBridge(@NonNull XposedInterface.HookBuilder builder) {
        this.mBuilder = builder;
        this.mKey = null;
        this.mIsStatic = false;
    }

    /**
     * 构造一个新的钩子桥接器实例，并指定类级标识键与静态性。
     *
     * @param builder  用于配置拦截参数并最终创建钩子的 Xposed API 构建器
     * @param key      声明类的全限定类名，用于热重载时按类去重存储 {@code thisObject}；
     *                 为 {@code null} 表示不参与热重载的自动状态管理
     * @param isStatic 是否为静态上下文；静态钩子在热重载时不会读取/写入 {@code thisObject}
     */
    public HookBridge(@NonNull XposedInterface.HookBuilder builder,
                      @Nullable String key, boolean isStatic) {
        this.mBuilder = builder;
        this.mKey = key;
        this.mIsStatic = isStatic;
    }

    /**
     * 设置钩子的唯一标识符。
     * <p>
     * 具有相同 ID 的新 Hook 会原子性地替换旧 Hook。
     *
     * @param id Hook 的唯一标识符；为 {@code null} 则表示不关心后续替换
     */
    private void setId(@Nullable String id) {
        if (ModuleData.getApiVersion() >= 102) {
            mBuilder.setId(id);
        }
    }

    /**
     * 设置钩子的异常处理模式。
     *
     * @param mode 异常处理模式
     */
    private void setExceptionMode(@NonNull XposedInterface.ExceptionMode mode) {
        mBuilder.setExceptionMode(mode);
    }

    /**
     * 将钩子优先级设置到底层构建器中。
     *
     * @param priority 优先级数值，值越小优先级越高
     */
    private void setPriority(int priority) {
        mBuilder.setPriority(priority);
    }

    /**
     * 注册拦截器，将指定的 {@link AbsHook} 实现绑定到目标方法上。
     *
     * @param absHook 自定义钩子实例
     * @return 框架返回的钩子句柄，可用于后续解除钩子等操作
     */
    @NonNull
    public XposedInterface.HookHandle intercept(@NonNull AbsHook absHook) {
        Objects.requireNonNull(mBuilder);
        setPriority(absHook.mPriority);
        if (absHook.mId != null) {
            setId(absHook.mId);
        }
        if (absHook.mMode != null) {
            setExceptionMode(absHook.mMode);
        }
        if (mKey != null) {
            absHook.setKey(mKey);
        }
        absHook.mIsStatic = mIsStatic;
        XposedInterface.HookHandle handle = mBuilder.intercept(new XposedInterface.Hooker() {
            @Override
            public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                absHook.enter(chain);

                try {
                    try {
                        absHook.before();
                    } catch (Throwable throwable) {
                        if (!absHook.onThrow(AbsHook.StageEnum.BEFORE, throwable)) {
                            absHook.setNonActiveThrowable(throwable);
                        }
                    }

                    Throwable beforeThrowable = absHook.getThrowable();
                    if (beforeThrowable != null) {
                        throw beforeThrowable;
                    }

                    if (absHook.isResultChanged()) {
                        return absHook.getResult();
                    }

                    try {
                        Object result = absHook.proceed(absHook.getChain());
                        absHook.setOriginalResult(result);
                    } catch (Throwable throwable) {
                        if (!absHook.onThrow(AbsHook.StageEnum.PROCEED, throwable)) {
                            absHook.setNonActiveThrowable(throwable);
                        }
                    }

                    try {
                        absHook.after();
                    } catch (Throwable throwable) {
                        if (!absHook.onThrow(AbsHook.StageEnum.AFTER, throwable)) {
                            absHook.setNonActiveThrowable(throwable);
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
