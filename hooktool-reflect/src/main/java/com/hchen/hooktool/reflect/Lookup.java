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
package com.hchen.hooktool.reflect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * 不抛异常的查找结果，保留失败原因。
 *
 * @param <T> 解析结果类型
 * @author 焕晨HChen
 */
public final class Lookup<T> {
    @Nullable
    private final T value;
    @Nullable
    private final ReflectException error;

    private Lookup(@Nullable T value, @Nullable ReflectException error) {
        this.value = value;
        this.error = error;
    }

    /**
     * 创建成功结果。
     *
     * @param value 解析结果
     * @param <T> 结果类型
     * @return 成功结果
     */
    @NonNull
    public static <T> Lookup<T> of(@NonNull T value) {
        return new Lookup<>(Objects.requireNonNull(value, "value"), null);
    }

    /**
     * 创建失败结果。
     *
     * @param error 失败原因
     * @param <T> 结果类型
     * @return 失败结果
     */
    @NonNull
    public static <T> Lookup<T> failed(@NonNull ReflectException error) {
        return new Lookup<>(null, Objects.requireNonNull(error, "error"));
    }

    /**
     * 判断是否解析成功。
     *
     * @return 成功时返回 true
     */
    public boolean isPresent() {
        return value != null;
    }

    /**
     * 获取解析结果。
     *
     * @return 解析结果
     * @throws ReflectException 查找失败时抛出
     */
    @NonNull
    public T get() {
        if (value == null) throw Objects.requireNonNull(error);
        return value;
    }

    /**
     * 获取解析结果，失败时返回 null。
     *
     * @return 解析结果或 null
     */
    @Nullable
    public T getOrNull() {
        return value;
    }

    /**
     * 获取失败原因。
     *
     * @return 失败原因，成功时为 null
     */
    @Nullable
    public ReflectException error() {
        return error;
    }
}
