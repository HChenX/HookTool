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
import androidx.annotation.Nullable;

import com.hchen.hooktool.data.AppData;

import java.util.List;

/**
 * 应用数据获取器接口。定义了获取应用包信息列表和异步回调的契约。
 *
 * @param <T> 包信息的类型参数
 * @author 焕晨HChen
 */
public interface IAppDataGetter<T> {
    /**
     * 获取需要查询的应用包列表。
     *
     * @param pm PackageManager 实例
     * @return 包信息列表
     * @throws PackageManager.NameNotFoundException 包未找到时抛出
     */
    @NonNull
    List<T> getPackages(@NonNull PackageManager pm) throws PackageManager.NameNotFoundException;

    /**
     * 异步获取应用数据的回调方法。
     *
     * @param appData 应用数据数组
     * @param e       异常信息，如果为 null 表示成功
     */
    default void getAsyncAppData(@NonNull AppData[] appData, @Nullable PackageManager.NameNotFoundException e) {
    }
}
