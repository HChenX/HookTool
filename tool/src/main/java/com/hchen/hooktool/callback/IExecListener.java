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
package com.hchen.hooktool.callback;

import androidx.annotation.NonNull;

/**
 * Shell 命令执行监听器接口。提供命令执行结果的回调方法。
 *
 * @author 焕晨HChen
 */
public interface IExecListener {
    /**
     * 标准输出回调。
     *
     * @param command  执行的命令
     * @param exitCode 退出码
     * @param outputs  标准输出内容
     */
    default void output(@NonNull String command, @NonNull String exitCode, @NonNull String[] outputs) {
    }

    /**
     * 标准错误输出回调。
     *
     * @param command  执行的命令
     * @param exitCode 退出码
     * @param errors   标准错误输出内容
     */
    default void error(@NonNull String command, @NonNull String exitCode, @NonNull String[] errors) {
    }

    /**
     * Root 权限检测结果回调。
     *
     * @param hasRoot  是否拥有 Root 权限
     * @param exitCode 退出码
     */
    default void rootResult(boolean hasRoot, @NonNull String exitCode) {
    }

    /**
     * 管道破裂回调，当 Shell 流非正常终止时触发。
     *
     * @param reason 原因描述
     * @param errors 错误输出内容
     */
    default void brokenPip(@NonNull String reason, @NonNull String[] errors) {
    }
}
