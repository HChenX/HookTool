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
 * 异步 Prefs 接口
 *
 * @author 焕晨HChen
 */
public interface IAsyncPrefs {
    /**
     * 获取寄生应用的共享首选项实例，你可以在此处读取/写入值
     */
    void async(@NonNull IPrefsApply sPrefs);
}
