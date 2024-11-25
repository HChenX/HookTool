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

 * Copyright (C) 2023-2024 HookTool Contributions
 */
package com.hchen.hooktool.data;

import android.graphics.Bitmap;

/**
 * App 数据
 *
 * @author 焕晨HChen
 */
public final class AppData {
    public int user = 0; /* user id */
    public int uid = -1; /* uid */
    public Bitmap icon; /* 图标 */
    public String label; /* 应用名 */
    public String packageName; /* 包名 */
    public String versionName; /* 版本名 */
    public String versionCode; /* 版本号 */
    public boolean isSystemApp; /* 是否为系统应用 */
    public boolean enabled; /* 是否启用 */
}
