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
package com.hchen.hooktool.helper;

import androidx.annotation.NonNull;

import com.hchen.hooktool.callback.IDecomposer;
import com.hchen.hooktool.data.ResultData;

/**
 * 异常安全执行的辅助工具类。
 * <p>
 * 提供 {@link #doTry(IDecomposer)} 方法，将可能抛出异常的操作委托给
 * {@link IDecomposer} 回调执行，并将执行结果或捕获的异常统一封装为
 * {@link ResultData} 对象返回，从而简化调用方的异常处理逻辑。
 *
 * @author 焕晨HChen
 */
public final class TryHelper {
    private TryHelper() {
    }

    /**
     * 以异常安全的方式执行给定的操作，并将结果封装为 {@link ResultData} 返回。
     * <p>
     * 该方法通过 {@link ResultData} 的构造过程捕获 {@link IDecomposer}
     * 执行期间抛出的任何异常（包括受检和非受检异常），调用方无需自行处理。
     *
     * @param supplier 封装了可能抛出异常的操作的回调对象，不为 {@code null}
     * @param <R>      操作返回值的类型
     * @return 包含执行结果或异常信息的 {@link ResultData} 实例，不为 {@code null}
     */
    @NonNull
    public static <R> ResultData<R> doTry(@NonNull IDecomposer<R> supplier) {
        return new ResultData<R>(supplier);
    }
}
