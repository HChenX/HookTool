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

 * Copyright (C) 2023-2024 HChenX
 */
package com.hchen.hooktool.helper;

import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.XposedLog.logW;
import static com.hchen.hooktool.tool.CoreTool.findClass;

import androidx.annotation.Nullable;

import com.hchen.hooktool.HCData;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.log.LogExpand;

import java.util.ArrayList;
import java.util.List;

/**
 * 快捷转换
 *
 * @author 焕晨HChen
 */
public final class ConvertHelper {
    /**
     * 泛型转换为数组。
     */
    public static <T> Object[] genericToArray(T ts) {
        if (ts instanceof Object[] objects) return objects;
        return new Object[]{ts};
    }

    @Nullable
    public static Class<?>[] arrayToClass(Object... objs) {
        return arrayToClass(HCData.getClassLoader(), objs);
    }

    /**
     * 数组参数转为类。
     */
    @Nullable
    public static Class<?>[] arrayToClass(ClassLoader classLoader, Object... objs) {
        if (classLoader == null || objs == null) return null;
        if (objs.length == 0) return new Class<?>[]{};
        List<Class<?>> classes = new ArrayList<>();
        for (Object o : objs) {
            if (o instanceof Class<?> c) {
                classes.add(c);
            } else if (o instanceof String s) {
                Class<?> ct = findClass(s, classLoader).get();
                if (ct == null) return null;
                classes.add(ct);
            } else if (o instanceof IHook) {
                break; // 一定为最后一个参数
            } else {
                logW(LogExpand.getTag(), "Unknown type: " + o + getStackTrace());
                return null;
            }
        }
        return classes.toArray(new Class<?>[0]);
    }
}
