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
package com.hchen.hooktool.util;

import androidx.annotation.NonNull;

import java.time.Duration;
import java.time.Instant;

/**
 * 耗时统计工具类。
 *
 * @author 焕晨HChen
 */
public final class Timing {
    private Timing() {
    }

    /**
     * 统计指定代码块的执行耗时。
     *
     * @param action 待执行的代码块
     * @return 耗时毫秒数
     */
    public static long measure(@NonNull Runnable action) {
        Instant start = Instant.now();
        action.run();
        return Duration.between(start, Instant.now()).toMillis();
    }
}