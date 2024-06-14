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

import static com.hchen.hooktool.log.AndroidLog.logE;
import static com.hchen.hooktool.log.AndroidLog.logI;
import static com.hchen.hooktool.log.AndroidLog.logW;

import java.lang.reflect.Field;

import de.robv.android.xposed.XposedHelpers;

/**
 * 字段设置结果观察
 */
public class FieldObserver {
    private final DataUtils utils;

    public FieldObserver(DataUtils utils) {
        this.utils = utils;
    }

    public void dynamicObserver(Field field, Object instance, Object value) {
        Object o = null;
        field.setAccessible(true);
        try {
            o = field.get(instance);
        } catch (IllegalAccessException e) {
            logE(utils.getTAG(), e);
            return;
        }
        doInspect("", field.getName(), value, o);
    }

    public void dynamicObserver(Object instance, String name, Object value) {
        Object o = null;
        try {
            o = XposedHelpers.getObjectField(instance, name);
        } catch (Throwable e) {
            logE(utils.getTAG(), e);
            return;
        }
        doInspect("", name, value, o);
    }

    public void staticObserver(Field field, Object value) {
        Object o = null;
        field.setAccessible(true);
        try {
            o = field.get(null);
        } catch (IllegalAccessException e) {
            logE(utils.getTAG(), e);
            return;
        }
        doInspect("static", field.getName(), value, o);
    }

    public void staticObserver(Class<?> clazz, String name, Object value) {
        Object o = null;
        try {
            o = XposedHelpers.getStaticObjectField(clazz, name);
        } catch (Throwable e) {
            logE(utils.getTAG(), e);
            return;
        }
        doInspect("static", name, value, o);
    }

    private void doInspect(String call, String name, Object value, Object o) {
        if (o == null && value == null) {
            logI(utils.getTAG(), "put field: [" + name + "], value to null");
            return;
        }
        if ((o == null && value != null) || (o != null && value == null)) {
            logW(utils.getTAG(), "failed put " + call + " field: [" + name + "]," +
                    " value to: [" + value + "], now is: " + o);
            return;
        }
        if (o == value || o.equals(value)) {
            logI(utils.getTAG(), "success put " + call + " field: [" + name + "]," +
                    " value to: [" + value + "], now is: " + o);
        } else {
            logW(utils.getTAG(), "failed put " + call + " field: [" + name + "]," +
                    " value to: [" + value + "], now is: " + o);
        }
    }
}
