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

import static com.hchen.hooktool.data.ToolData.isZygote;
import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.XposedLog.logW;
import static com.hchen.hooktool.tool.CoreTool.findClass;

import com.hchen.hooktool.data.ToolData;
import com.hchen.hooktool.hook.IAction;
import com.hchen.hooktool.log.LogExpand;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 快捷转换
 * <p>
 * Quick conversion
 *
 * @author 焕晨HChen
 */
public class ConvertHelper {
    /**
     * 泛型转换为数组。
     * <p>
     * Generics are converted to arrays.
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
     * <p>
     * Array parameters are converted to classes.
     */
    public static Class<?>[] arrayToClass(ClassLoader classLoader, Object... objs) {
        if (objs.length == 0) return new Class<?>[]{};
        if (classLoader == null && isZygoteState()) return new Class[]{};
        ArrayList<Class<?>> classes = new ArrayList<>();
        for (Object o : objs) {
            if (o instanceof Class<?> c) {
                classes.add(c);
            } else if (o instanceof String s) {
                Class<?> ct = findClass(s, classLoader);
                if (ct == null) return new Class[]{};
                classes.add(ct);
            } else if (o instanceof IAction) {
                break; // IAction 必定为最后一个参数 (如果有)
            } else {
                logW(tag(), "Unknown type: " + o + getStackTrace());
                return new Class[]{};
            }
        }
        return classes.toArray(new Class<?>[0]);
    }

    // 将数组转成类并保留 IAction 参数
    public static Object[] toClassAsIAction(ClassLoader classLoader, Object... objs) {
        if (objs.length == 0 || !(objs[objs.length - 1] instanceof IAction iAction))
            return new Object[]{};

        Class<?>[] classes = arrayToClass(classLoader, objs);
        ArrayList<Object> arrayList = new ArrayList<>(Arrays.asList(classes));
        arrayList.add(iAction);
        return arrayList.toArray(new Object[0]);
    }

    private static boolean isZygoteState() {
        if (isZygote) {
            logW(tag(), "In zygote state, call method please set classloader!" + getStackTrace());
            return true;
        }
        return false;
    }

    private static String tag() {
        String tag = LogExpand.tag();
        if (tag == null) return "ConvertHelper";
        return tag;
    }
}
