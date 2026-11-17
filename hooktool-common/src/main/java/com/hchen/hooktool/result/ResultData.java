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
package com.hchen.hooktool.result;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.function.Function;

/**
 * 惰性求值的结果包装类。
 * <p>
 * 首次访问时执行计算并缓存结果。
 *
 * @param <R> 结果类型
 * @author 焕晨HChen
 */
public final class ResultData<R> {
    private final IDecomposer<R> mDecomposer;
    private volatile boolean mIsExecuted;
    private volatile R mResult;
    private volatile Throwable mThrowable;

    /**
     * 创建结果包装类。
     *
     * @param decomposer 计算逻辑
     */
    public ResultData(@NonNull IDecomposer<R> decomposer) {
        this.mDecomposer = decomposer;
        this.mIsExecuted = false;
    }

    /**
     * 获取结果，必要时先执行计算。
     *
     * @return 计算结果，失败时为 null
     */
    @Nullable
    public R get() {
        runIfNeeded();
        return mResult;
    }

    /**
     * 获取结果，计算失败时返回默认值。
     *
     * @param def 默认值
     * @return 计算结果或默认值
     */
    @Nullable
    public R getOrDefault(@Nullable R def) {
        runIfNeeded();
        return isSuccess() ? mResult : def;
    }

    /**
     * 获取结果，计算失败时重新抛出异常。
     *
     * @return 计算结果
     */
    @Nullable
    public R getOrThrow() {
        runIfNeeded();
        if (isSuccess()) return mResult;
        return throwIt(mThrowable);
    }

    /**
     * 将失败原因映射为替代结果。
     *
     * @param consumer 失败映射逻辑
     * @return 计算结果或映射结果
     */
    @Nullable
    public R onThrow(@NonNull Function<Throwable, R> consumer) {
        runIfNeeded();
        if (isSuccess()) return mResult;
        return consumer.apply(mThrowable);
    }

    /**
     * 获取计算过程中捕获的异常。
     *
     * @return 异常或 null
     */
    @Nullable
    public Throwable getThrowable() {
        runIfNeeded();
        return mThrowable;
    }

    /**
     * 判断计算是否成功。
     *
     * @return 未抛出异常时返回 true
     */
    public boolean isSuccess() {
        runIfNeeded();
        return Objects.isNull(mThrowable);
    }

    private void runIfNeeded() {
        if (mIsExecuted) return;
        synchronized (this) {
            if (mIsExecuted) return;
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

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T, E extends Throwable> T throwIt(@Nullable Throwable throwable) throws E {
        throw (E) throwable;
    }
}
