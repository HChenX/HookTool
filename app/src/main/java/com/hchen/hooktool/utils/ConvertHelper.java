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
import java.util.Arrays;

/**
 * 快捷转换
 * <p>
 * Quick conversion
 */
public class ConvertHelper {
    private final ToolData data;

    public ConvertHelper(ToolData data) {
        this.data = data;
    }

    /**
     * 泛型转换为数组。
     * <p>
     * Generics are converted to arrays.
     *
     * @param ts 泛型
     * @return 数组
     */
    public <T> Object[] genericToArray(T ts) {
        if (ts instanceof Object[] objects) return objects;
        return new Object[]{ts};
    }

    public Class<?>[] arrayToClass(Object... objs) {
        return arrayToClass(data.getClassLoader(), objs);
    }

    /**
     * 数组参数转为类。
     * <p>
     * Array parameters are converted to classes.
     */
    public Class<?>[] arrayToClass(ClassLoader classLoader, Object... objs) {
        ArrayList<Class<?>> classes = new ArrayList<>();
        if (objs.length == 0) {
            logW(data.getTag(), "array length is 0!!");
            return new Class<?>[]{};
        }
        if (objs.length == 1 && objs[0] instanceof Class<?>[] clazzArray) {
            return clazzArray;
        }
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
                break;
            } else {
                logW(data.getTag(), "unknown type: " + o);
                return new Class[]{};
            }
        }
        return classes.toArray(new Class<?>[0]);
    }

    public Object[] toClassAsIAction(ClassLoader classLoader, Object... objs) {
        if (objs.length == 0 || !(objs[objs.length - 1] instanceof IAction iAction)) {
            logW(data.getTag(), "params length == 0 or last param not is IAction! can't convert!!");
            return new Object[]{};
        }
        Class<?>[] classes = arrayToClass(classLoader, objs);
        ArrayList<Object> arrayList = new ArrayList<>(Arrays.asList(classes));
        arrayList.add(iAction);
        return arrayList.toArray(new Object[0]);
    }
    
}
