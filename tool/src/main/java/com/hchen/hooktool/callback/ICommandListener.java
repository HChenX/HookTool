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
 * Shell 命令执行拦截监听器。
 * <p>
 * 该接口在 Shell 命令即将被提交执行前提供拦截与过滤能力，
 * 实现者可通过返回值决定是否允许该命令继续执行。
 *
 * @author 焕晨HChen
 */
public interface ICommandListener {
    /**
     * Shell 命令执行前的拦截回调。
     * <p>
     * 当一条 Shell 命令即将被执行时，框架将调用此方法。
     * 返回 {@code true} 表示放行该命令，返回 {@code false} 则拦截并取消执行。
     *
     * @param cmd 即将被执行的 Shell 命令字符串，不为 {@code null}
     * @return {@code true} 允许命令继续执行，{@code false} 拦截命令使其不被执行
     */
    boolean onCommand(@NonNull String cmd);
}
