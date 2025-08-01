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

import android.content.pm.PackageManager;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.hchen.hooktool.data.AppData;

/**
 * 包信息获取器
 *
 * @author 焕晨HChen
 */
public interface IAppDataGetter {
    /**
     * 需要获取 AppData 信息的包列表
     */
    @NonNull
    Parcelable[] getPackages(@NonNull PackageManager pm) throws PackageManager.NameNotFoundException;

    /**
     * 异步获取到的 AppData 信息
     */
    default void getAsyncAppData(@NonNull AppData[] appData) {
    }
}
