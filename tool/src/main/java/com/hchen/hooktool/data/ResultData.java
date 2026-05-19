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
import androidx.annotation.Nullable;

import com.hchen.hooktool.callback.IDecomposer;
import com.hchen.hooktool.core.CoreTool;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 执行结果数据类。封装 {@link IDecomposer} 的延迟执行结果，支持成功/失败判断、默认值回退、异常处理等操作。
 * 线程安全，结果仅计算一次。
 *
 * @author 焕晨HChen
 */
public final class ResultData<R> {
    private final IDecomposer<R> decomposer;
    private boolean isExecuted;
    private R result;
    private Throwable throwable;

    /**
     * 创建执行结果数据实例。
     *
     * @param decomposer 可抛出异常的供应者
     */
    public ResultData(@NonNull IDecomposer<R> decomposer) {
        this.decomposer = decomposer;
        this.isExecuted = false;
    }

    /**
     * 获取执行结果。如果尚未执行则触发执行。
     *
     * @return 执行结果
     */
    public R get() {
        runIfNeed();
        return result;
    }

    /**
     * 获取执行结果，如果执行失败则返回默认值。
     *
     * @param def 默认值
     * @return 执行结果或默认值
     */
    public R getOrDefault(R def) {
        runIfNeed();
        if (isSuccess()) {
            return result;
        }
        return def;
    }

    /**
     * 获取执行结果，如果执行失败则抛出异常。
     *
     * @return 执行结果
     * @throws Throwable 执行过程中抛出的异常
     */
    public R getOrThrow() {
        runIfNeed();
        if (isSuccess()) {
            return result;
        }
        CoreTool.throwIt(throwable);
        return null; // Not actually executed
    }

    /**
     * 执行失败时的回调处理。
     *
     * @param consumer 异常消费者
     */
    public void onThrow(@NonNull Consumer<Throwable> consumer) {
        runIfNeed();
        if (isSuccess()) return;
        consumer.accept(throwable);
    }

    /**
     * 执行失败时的回调处理，并返回替代结果。
     *
     * @param consumer 异常处理函数
     * @return 执行结果或替代结果
     */
    public R onThrow(@NonNull Function<Throwable, R> consumer) {
        runIfNeed();
        if (isSuccess()) return result;
        return consumer.apply(throwable);
    }

    /**
     * 获取执行过程中产生的异常。
     *
     * @return 异常对象，如果执行成功则返回 null
     */
    @Nullable
    public Throwable getThrowable() {
        runIfNeed();
        return throwable;
    }

    /**
     * 判断执行是否成功。
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        runIfNeed();
        return Objects.isNull(throwable);
    }

    private synchronized void runIfNeed() {
        if (isExecuted) {
            return;
        }

        try {
            this.result = decomposer.get();
            this.throwable = null;
        } catch (Throwable throwable) {
            this.result = null;
            this.throwable = throwable;
        } finally {
            isExecuted = true;
        }
    }
}
