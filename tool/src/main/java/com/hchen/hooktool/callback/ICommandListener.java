/*
 * This file is part of HookTool.

 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HChenX
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
     *
     * @param cmd 命令
     * @return 是否拦截
     */
    boolean onCommand(@NonNull String cmd);
}
