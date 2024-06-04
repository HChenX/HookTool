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

import androidx.annotation.Nullable;

import com.hchen.hooktool.utils.MapUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.HashMap;

public class MemberData {
    @Nullable
    public Class<?> mClass; // 查找到的类
    public Field mField;
    /* member 存储与读取 */
    public MapUtils<ArrayList<Member>> memberMap = new MapUtils<>();
    /* member 状态记录 */
    public HashMap<ArrayList<Member>, StateEnum> stateMap = new HashMap<>();

    public MemberData(@Nullable Class<?> mClass) {
        this.mClass = mClass;
    }
}
