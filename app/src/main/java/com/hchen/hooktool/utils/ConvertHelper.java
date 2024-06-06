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

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class ConvertHelper {
    private final DataUtils utils;

    public ConvertHelper(DataUtils utils) {
        this.utils = utils;
    }

    protected Class<?> findClass(String name) {
        try {
            return XposedHelpers.findClass(name,
                    utils.getClassLoader());
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(utils.getTAG(), "the specified class could not be found!", e);
        }
        return null;
    }

    protected <T> Object[] genericToObjectArray(T ts) {
        if (ts instanceof Object[] objects) {
            return objects;
        }
        return new Object[]{ts};
    }

    protected Class<?>[] objectArrayToClassArray(Object... objs) {
        ArrayList<Class<?>> classes = new ArrayList<>();
        for (Object o : objs) {
            if (o instanceof Class<?> c) {
                classes.add(c);
            } else if (o instanceof String s) {
                Class<?> ct = findClass(s);
                if (ct == null) {
                    return null;
                }
                classes.add(ct);
            } else {
                logW(utils.getTAG(), "unknown type: " + o);
                return null;
            }
        }
        return classes.toArray(new Class<?>[classes.size()]);
    }
}
