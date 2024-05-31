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

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import androidx.annotation.NonNull;

import com.hchen.hooktool.data.MemberData;
import com.hchen.hooktool.utils.DataUtils;

import java.lang.reflect.Field;

import de.robv.android.xposed.XposedHelpers;

public class FieldTool {
    private final DataUtils utils;

    public FieldTool(DataUtils utils) {
        this.utils = utils;
    }

    public FieldTool to(@NonNull Object label) {
        utils.getClassTool().to(label);
        return utils.getFieldTool();
    }

    /**
     * 按标签类获取指定字段。
     */
    public FieldTool findField(String name) {
        utils.findField = null;
        if (utils.labelClasses.isEmpty()) {
            logW(utils.getTAG(), "The class list is empty!");
            return utils.getFieldTool();
        }
        Object label = utils.getLabel();
        MemberData data = utils.labelClasses.get(label);
        if (data == null) {
            logW(utils.getTAG(), "data is null, cant find field: [" + name + "] label: " + label);
            return utils.getFieldTool();
        }
        Class<?> c = data.mClass;
        if (c == null) {
            logW(utils.getTAG(), "find field but class is null!");
            return utils.getFieldTool();
        }
        try {
            utils.findField = XposedHelpers.findField(c, name);
            data.mField = utils.findField;
        } catch (NoSuchFieldError e) {
            logE(utils.getTAG(), "failed to get claim field: [" + name + "] class: " + utils.findClass, e);
        }
        return utils.getFieldTool();
    }

    /**
     * 获取查找到的字段，需要在下次查找前调用，否则被覆盖。
     */
    public Field getField() {
        return utils.findField;
    }

    /**
     * 设置指定字段。
     */
    public FieldTool set(Object value) {
        if (utils.findField != null) {
            utils.findField.setAccessible(true);
            try {
                utils.findField.set(null, value);
            } catch (IllegalAccessException e) {
                logE(utils.getTAG(), "set: " + utils.findField, e);
            }
        } else logW(utils.getTAG(), "find field is null!");
        return utils.getFieldTool();
    }

    /**
     * 获取指定字段。
     */
    public <T> T get() {
        if (utils.findField != null) {
            utils.findField.setAccessible(true);
            try {
                return (T) utils.findField.get(null);
            } catch (IllegalAccessException e) {
                logE(utils.getTAG(), "get: " + utils.findField, e);
            }
        } else logW(utils.getTAG(), "findField is null!");
        return null;
    }

    /* 不需要再回到此类 */
    // public HCHook hcHook() {
    //     return utils.getHCHook();
    // }

    public ClassTool classTool() {
        return utils.getClassTool();
    }

    // 更棒的无缝衔接
    public ClassTool findClass(Object label, String className) {
        return utils.getClassTool().findClass(label, className);
    }

    public ClassTool findClass(Object label, String className, ClassLoader classLoader) {
        return utils.getClassTool().findClass(label, className, classLoader);
    }

    public MethodTool methodTool() {
        return utils.getMethodTool();
    }
}
