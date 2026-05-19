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
 * 基于惰性求值策略的结果包装器，对 {@link IDecomposer} 的执行结果进行延迟计算与缓存。
 * <p>
 * 实际的计算逻辑在首次调用任意读取方法（{@link #get}、{@link #getOrDefault}、
 * {@link #getOrThrow} 等）时才被触发，之后的调用将直接返回已缓存的结果。
 * 整个计算过程通过 {@code synchronized} 机制保证线程安全，确保多线程并发场景下
 * 计算逻辑仅执行一次。
 * <p>
 * 该类提供了丰富的结果操作模式，包括：成功/失败状态判断、默认值回退、
 * 异常对象获取，以及自定义异常处理等。
 *
 * @param <R> 计算结果的类型参数
 * @author 焕晨HChen
 */
public final class ResultData<R> {
    private final IDecomposer<R> decomposer;
    private boolean isExecuted;
    private R result;
    private Throwable throwable;

    /**
     * 创建一个惰性求值的结果包装实例。
     * <p>
     * 构造时不会立即执行传入的 {@link IDecomposer}，计算将延迟到首次读取结果时才被触发。
     *
     * @param decomposer 提供实际计算逻辑的 {@link IDecomposer} 实例，不可为 {@code null}
     */
    public ResultData(@NonNull IDecomposer<R> decomposer) {
        this.decomposer = decomposer;
        this.isExecuted = false;
    }

    /**
     * 获取计算结果。若计算尚未执行则在此时触发。
     * <p>
     * 当计算过程中发生异常时，该方法返回 {@code null}。
     *
     * @return 计算成功时返回结果值；计算失败时返回 {@code null}
     */
    public R get() {
        runIfNeed();
        return result;
    }

    /**
     * 获取计算结果；若计算失败则返回指定的默认值。
     *
     * @param def 计算失败时用作回退的默认值
     * @return 计算成功时返回结果值，否则返回 {@code def} 参数值
     */
    public R getOrDefault(R def) {
        runIfNeed();
        if (isSuccess()) {
            return result;
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
        runIfNeed();
        if (isSuccess()) {
            return result;
        }
        CoreTool.throwIt(throwable);
        return null; // Not actually executed
    }

    /**
     * 注册一个异常消费回调，仅在计算失败时被调用。
     * <p>
     * 若计算已成功完成，则该方法不执行任何操作。
     *
     * @param consumer 接收异常对象的回调函数，不可为 {@code null}
     */
    public void onThrow(@NonNull Consumer<Throwable> consumer) {
        runIfNeed();
        if (isSuccess()) return;
        consumer.accept(throwable);
    }

    /**
     * 注册一个异常转换函数，在计算失败时将异常映射为替代结果。
     * <p>
     * 若计算已成功完成，则直接返回原始结果，转换函数不会被调用。
     *
     * @param consumer 将异常转换为替代结果的映射函数，不可为 {@code null}
     * @return 计算成功时返回原始结果；计算失败时返回转换函数生成的替代结果
     */
    public R onThrow(@NonNull Function<Throwable, R> consumer) {
        runIfNeed();
        if (isSuccess()) return result;
        return consumer.apply(throwable);
    }

    /**
     * 获取计算过程中捕获的异常对象。
     *
     * @return 计算失败时返回捕获的异常；计算成功时返回 {@code null}
     */
    @Nullable
    public Throwable getThrowable() {
        runIfNeed();
        return throwable;
    }

    /**
     * 判断计算是否已成功完成（即执行过程中未抛出任何异常）。
     *
     * @return 计算成功返回 {@code true}；计算失败返回 {@code false}
     */
    public boolean isSuccess() {
        runIfNeed();
        return Objects.isNull(throwable);
    }

    /**
     * 延迟执行计算逻辑的内部方法。若计算已执行则直接返回。
     * <p>
     * 通过 {@code synchronized} 关键字保证线程安全，确保在多线程并发访问时
     * 计算逻辑只被执行一次，结果（或异常）被安全地缓存。
     */
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
