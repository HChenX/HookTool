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
package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.LogExpand.getTag;
import static com.hchen.hooktool.log.XposedLog.logE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 储存获取到的成员列表。
 *
 * @author 焕晨HChen
 */
public final class MemberListData<T> extends ArrayList<T> {
    public MemberListData(int initialCapacity) {
        super(initialCapacity);
    }

    public MemberListData() {
        super();
    }

    public MemberListData(@NonNull Collection<? extends T> c) {
        super(c);
    }

    @Override
    @Nullable
    public T get(int index) {
        try {
            return super.get(index);
        } catch (IndexOutOfBoundsException e) {
            logE(getTag(), e);
            return null;
        }
    }

    @Nullable
    public T first() {
        return get(0);
    }

    @Nullable
    public T last() {
        return get(size() - 1);
    }
}
