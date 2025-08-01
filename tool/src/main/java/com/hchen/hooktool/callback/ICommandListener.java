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
 * 命令监听器
 *
 * @author 焕晨HChen
 */
public interface ICommandListener {
    /**
     * 输入命令时回调本方法
     * <p>
     * 返回 true 放行命令
     * <p>
     * 返回 false 拦截命令
     */
    boolean onCommand(@NonNull String cmd);
}
