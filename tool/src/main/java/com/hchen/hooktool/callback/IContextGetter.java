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

import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Context 获取器
 *
 * @author 焕晨HChen
 */
public interface IContextGetter {
    /**
     * 获取 context，未获取到则返回 null
     *
     * @param context 上下文
     */
    void onContext(@Nullable Context context);
}
