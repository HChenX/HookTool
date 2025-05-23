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

import java.lang.reflect.Member;

/**
 * Member 过滤
 *
 * @author 焕晨HChen
 */
public interface IMemberFilter<T extends Member> {
    /**
     * 过滤成员，返回 true 则保留，false 则摈弃
     */
    boolean test(@NonNull T member);
}
