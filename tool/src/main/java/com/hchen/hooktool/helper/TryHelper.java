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
 * Copyright (C) 2023–2025 HChenX
 */
package com.hchen.hooktool.helper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * TryHelper
 *
 * @author 焕晨HChen
 */
public class TryHelper {
    private TryHelper() {
    }

    @NonNull
    public static <R> Result<R> doTry(@NonNull IDecomposer<R> supplier) {
        return new Result<R>(supplier);
    }

    public interface IDecomposer<R> {
        R get() throws Throwable;
    }

    public static final class Result<R> {
        private R result;
        @Nullable
        private Throwable throwable;

        private Result(@NonNull IDecomposer<R> iDecomposer) {
            try {
                this.result = iDecomposer.get();
                this.throwable = null;
            } catch (Throwable throwable) {
                this.result = null;
                this.throwable = throwable;
            }
        }

        public R get() {
            return result;
        }

        public R orElse(R or) {
            if (isSuccess())
                return result;
            return or;
        }

        public void onThrowable(@NonNull Consumer<Throwable> consumer) {
            if (isSuccess()) return;
            consumer.accept(throwable);
        }

        public R onThrowable(@NonNull Function<Throwable, R> consumer) {
            if (isSuccess()) return result;
            return consumer.apply(throwable);
        }

        @Nullable
        public Throwable getThrowable() {
            return throwable;
        }

        public boolean isSuccess() {
            return Objects.isNull(throwable);
        }
    }
}
