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

import android.content.pm.PackageManager;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * 包信息获取器
 *
 * @author 焕晨HChen
 */
public interface IPackageInfoGetter {
    /**
     * 需要获取信息的包列表
     *
     * @param pm 包管理器
     * @return 包列表
     * @throws PackageManager.NameNotFoundException 未找到指定包
     */
    @NonNull
    Parcelable[] packageInfoGetter(@NonNull PackageManager pm) throws PackageManager.NameNotFoundException;
}
