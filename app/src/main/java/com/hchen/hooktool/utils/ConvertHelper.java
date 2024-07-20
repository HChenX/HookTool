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
package com.hchen.hooktool.utils;

import static com.hchen.hooktool.log.XposedLog.logW;

import com.hchen.hooktool.callback.IAction;

import java.util.ArrayList;

/**
 * 本工具的快捷转换
 */
public class ConvertHelper {
    protected ToolData data;

    public ConvertHelper(ToolData data) {
        this.data = data;
    }

    /**
     * 泛型转换为数组。
     *
     * @param ts 泛型
     * @return 数组
     */
    final public <T> Object[] genericToArray(T ts) {
        if (ts instanceof Object[] objects) return objects;
        return new Object[]{ts};
    }

    final public Class<?>[] arrayToClass(Object... objs) {
        return arrayToClass(data.getClassLoader(), objs);
    }

    /**
     * 数组参数转为类。
     */
    final public Class<?>[] arrayToClass(ClassLoader classLoader, Object... objs) {
        ArrayList<Class<?>> classes = new ArrayList<>();
        for (Object o : objs) {
            if (o instanceof Class<?> c) {
                classes.add(c);
            } else if (o instanceof String s) {
                Class<?> ct = data.getCoreTool().findClass(s, classLoader);
                if (ct == null) {
                    return new Class[]{};
                }
                classes.add(ct);
            } else if (o instanceof IAction) {
                continue;
            } else {
                logW(data.getTAG(), "unknown type: " + o);
                return new Class[]{};
            }
        }
        return classes.toArray(new Class<?>[0]);
    }
}
