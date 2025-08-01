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
package com.hchen.hooktool.callback;

import androidx.annotation.NonNull;

/**
 * Shell 执行监听
 *
 * @author 焕晨HChen
 */
public interface IExecListener {
    /**
     * 标准输出
     */
    default void output(@NonNull String command, @NonNull String exitCode, @NonNull String[] outputs) {
    }

    /**
     * 标准错误
     */
    default void error(@NonNull String command, @NonNull String exitCode, @NonNull String[] errors) {
    }

    /**
     * 返回 Root 状态
     */
    default void rootResult(boolean hasRoot, @NonNull String exitCode) {
    }

    /**
     * 管道破裂时的回调，代表 Shell 流非正常终止
     */
    default void brokenPip(@NonNull String reason, @NonNull String[] errors) {
    }
}
