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
 * 执行结果的数据
 *
 * @author 焕晨HChen
 */
public final class ResultData<R> {
    private final IDecomposer<R> decomposer;
    private boolean isExecuted;
    private R result;
    private Throwable throwable;

    public ResultData(@NonNull IDecomposer<R> decomposer) {
        this.decomposer = decomposer;
        this.isExecuted = false;
    }

    public R get() {
        runIfNeed();
        return result;
    }

    public R orElse(R or) {
        runIfNeed();
        if (isSuccess()) {
            return result;
        }
        return or;
    }

    public R getOrThrow() {
        runIfNeed();
        if (isSuccess()) {
            return result;
        }
        CoreTool.throwIt(throwable);
        return null; // Not actually executed
    }

    public void onThrow(@NonNull Consumer<Throwable> consumer) {
        runIfNeed();
        if (isSuccess()) return;
        consumer.accept(throwable);
    }

    public R onThrow(@NonNull Function<Throwable, R> consumer) {
        runIfNeed();
        if (isSuccess()) return result;
        return consumer.apply(throwable);
    }

    @Nullable
    public Throwable getThrowable() {
        runIfNeed();
        return throwable;
    }

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
