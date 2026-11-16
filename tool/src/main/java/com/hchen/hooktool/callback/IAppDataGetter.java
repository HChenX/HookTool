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

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * 应用包信息查询契约接口。
 *
 * @param <T> 包信息列表中元素的类型
 * @author 焕晨HChen
 */
@FunctionalInterface
public interface IAppDataGetter<T> {
    /**
     * 从 PackageManager 中查询目标应用的包信息列表。
     *
     * @param pm 用于执行包信息查询的 {@link PackageManager} 实例
     * @return 包含目标应用包信息的列表.
     */
    @NonNull
    List<T> getPackages(@NonNull PackageManager pm);
}
