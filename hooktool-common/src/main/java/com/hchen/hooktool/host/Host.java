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
package com.hchen.hooktool.host;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * 宿主环境持有者。
 *
 * @author 焕晨HChen
 */
public final class Host {
    private static volatile HostEnv sEnv = NoopHostEnv.INSTANCE;

    private Host() {
    }

    /**
     * 设置宿主环境。
     *
     * @param env 宿主环境实现
     */
    public static void set(@NonNull HostEnv env) {
        sEnv = Objects.requireNonNull(env, "host environment must not be null");
    }

    /**
     * 恢复为默认宿主环境。
     */
    public static void reset() {
        sEnv = NoopHostEnv.INSTANCE;
    }

    /**
     * 获取当前宿主环境。
     *
     * @return 宿主环境
     */
    @NonNull
    public static HostEnv get() {
        return sEnv;
    }
}
