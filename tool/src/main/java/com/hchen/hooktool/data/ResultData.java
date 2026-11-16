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
package com.hchen.hooktool.data;

import androidx.annotation.NonNull;

import com.hchen.hooktool.callback.IDecomposer;
import com.hchen.hooktool.core.CoreTool;

import java.util.Objects;
import java.util.function.Function;

/**
 * 基于惰性求值策略的结果包装器，对 {@link IDecomposer} 的执行结果进行延迟计算与缓存。
 *
 * @param <R> 计算结果的类型参数
 * @author 焕晨HChen
 */
public final class ResultData<R> {
    private final IDecomposer<R> mDecomposer;
    private volatile boolean mIsExecuted;
    /**
     * 计算结果；{@code volatile} 保证 DCL 快速路径（不持有锁）下对首次计算结果的可见性。
     */
    private volatile R mResult;
    /**
     * 捕获的异常；同 {@link #mResult} 需要跨线程可见。
     */
    private volatile Throwable mThrowable;

    public ResultData(@NonNull IDecomposer<R> decomposer) {
        this.mDecomposer = decomposer;
        this.mIsExecuted = false;
    }

    /**
     * 获取计算结果。若计算尚未执行则在此时触发。
     * <p>
     * 当计算过程中发生异常时，该方法返回 {@code null}。
     *
     * @return 计算成功时返回结果值；计算失败时返回 {@code null}
     */
    public R get() {
        runIfNeeded();
        return mResult;
    }

    /**
     * 获取计算结果；若计算失败则返回指定的默认值。
     * <p>
     * 仅当计算<strong>失败</strong>（执行过程抛异常）时才返回 {@code def}。
     *
     * @param def 计算失败时用作回退的默认值
     * @return 计算成功时返回结果值（可能为 {@code null}），失败时返回 {@code def} 参数值
     */
    public R getOrDefault(R def) {
        runIfNeeded();
        if (isSuccess()) {
            return mResult;
        }
        return def;
    }

    /**
     * 获取计算结果；若计算失败则将捕获的异常重新抛出。
     *
     * @return 计算成功时返回结果值
     * @throws Throwable 计算过程中捕获的异常，经由 {@link CoreTool#throwIt} 抛出
     */
    public R getOrThrow() {
        runIfNeeded();
        if (isSuccess()) {
            return mResult;
        }
        CoreTool.throwIt(mThrowable);
        return null;
    }

    /**
     * 注册一个异常转换函数，在计算失败时将异常映射为替代结果。
     * <p>
     * 若计算已成功完成，则直接返回原始结果，转换函数不会被调用。
     *
     * @param consumer 将异常转换为替代结果的映射函数
     * @return 计算成功时返回原始结果；计算失败时返回转换函数生成的替代结果
     */
    public R onThrow(@NonNull Function<Throwable, R> consumer) {
        runIfNeeded();
        if (isSuccess()) return mResult;
        return consumer.apply(mThrowable);
    }

    /**
     * 获取计算过程中捕获的异常对象。
     *
     * @return 计算失败时返回捕获的异常；计算成功时返回 {@code null}
     */
    public Throwable getThrowable() {
        runIfNeeded();
        return mThrowable;
    }

    /**
     * 判断计算是否已成功完成（即执行过程中未抛出任何异常）。
     *
     * @return 计算成功返回 {@code true}；计算失败返回 {@code false}
     */
    public boolean isSuccess() {
        runIfNeeded();
        return Objects.isNull(mThrowable);
    }

    private void runIfNeeded() {
        if (mIsExecuted) {
            return;
        }
        synchronized (this) {
            if (mIsExecuted) {
                return;
            }
            try {
                this.mResult = mDecomposer.get();
                this.mThrowable = null;
            } catch (Throwable throwable) {
                this.mResult = null;
                this.mThrowable = throwable;
            } finally {
                mIsExecuted = true;
            }
        }
    }
}
