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

/**
 * 异常抛出工具类。
 *
 * @author 焕晨HChen
 */
public final class Throwables {
    private Throwables() {
    }

    /**
     * 原样抛出指定异常，不做包装。
     *
     * @param throwable 待抛出的异常
     * @param <T> 返回值占位类型
     * @param <E> 抛出异常的类型
     * @return 不会返回
     * @throws E 始终抛出
     */
    @SuppressWarnings("unchecked")
    public static <T, E extends Throwable> T throwIt(@NonNull Throwable throwable) throws E {
        throw (E) throwable;
    }
}