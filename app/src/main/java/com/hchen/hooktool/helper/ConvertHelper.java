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
package com.hchen.hooktool.helper;

import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.XposedLog.logW;
import static com.hchen.hooktool.tool.CoreTool.findClass;

import androidx.annotation.Nullable;

import com.hchen.hooktool.data.ToolData;
import com.hchen.hooktool.hook.IAction;
import com.hchen.hooktool.log.LogExpand;

import java.util.ArrayList;

/**
 * 快捷转换
 *
 * @author 焕晨HChen
 */
public class ConvertHelper {
    /**
     * 泛型转换为数组。
     */
    public static <T> Object[] genericToArray(T ts) {
        if (ts instanceof Object[] objects) return objects;
        return new Object[]{ts};
    }

    public static Class<?>[] arrayToClass(Object... objs) {
        return arrayToClass(ToolData.classLoader, objs);
    }

    /**
     * 数组参数转为类。
     */
    @Nullable
    public static Class<?>[] arrayToClass(ClassLoader classLoader, Object... objs) {
        if (objs.length == 0) return new Class<?>[]{};
        ArrayList<Class<?>> classes = new ArrayList<>();
        label:
        for (Object o : objs) {
            switch (o) {
                case Class<?> c:
                    classes.add(c);
                    break;
                case String s:
                    Class<?> ct = findClass(s, classLoader).get();
                    if (ct == null) return null;
                    classes.add(ct);
                    break;
                case IAction iAction:
                    break label; // IAction 必定为最后一个参数 (如果有)
                case null:
                default:
                    logW(LogExpand.tag(), "Unknown type: " + o + getStackTrace());
                    return null;
            }
        }
        return classes.toArray(new Class<?>[0]);
    }
}
