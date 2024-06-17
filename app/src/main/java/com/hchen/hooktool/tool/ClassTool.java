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
import com.hchen.hooktool.utils.MethodOpt;

import de.robv.android.xposed.XposedHelpers;

/**
 * 类工具
 */
public class ClassTool extends MethodOpt {
    private final DataUtils utils;

    public ClassTool(DataUtils utils) {
        super(utils);
        this.utils = utils;
        clear();
    }

    public MethodTool to(@NonNull Object label) {
        utils.setLabel(label);
        return utils.getMethodTool();
    }

    // ---------- 标签形式 ------------

    /**
     * 手动添加类。
     */
    public ClassTool add(@NonNull Object label, Class<?> clazz) {
        if (clazz == null) {
            logW(utils.getTAG(), "the class is null! label: " + label);
        }
        utils.findClass = clazz;
        utils.members.put(label, new MemberData(clazz));
        utils.setLabel(label);
        return utils.getClassTool();
    }

    /**
     * 查找指定类，并为其设置独有的标签。
     */
    public ClassTool findClass(@NonNull Object label, String className) {
        return findClass(label, className, utils.getClassLoader());
    }

    public ClassTool findClass(@NonNull Object label, String className, ClassLoader classLoader) {
        if (utils.findClass != null) utils.findClass = null;
        utils.findClass = utils.getExpandTool().findClass(className, classLoader);
        MemberData data = utils.members.get(label);
        if (data != null) {
            Class<?> old = data.mClass;
            if (old != null && old.equals(utils.findClass)) {
                utils.setLabel(label);
                logW(utils.getTAG(), "malicious coverage! label: [" + label + "], class: " + className);
                return utils.getClassTool();
            }
        }
        utils.members.put(label, new MemberData(utils.findClass));
        utils.setLabel(label);
        return utils.getClassTool();
    }

    /* 获取本次得到的类 */
    public Class<?> getFindClass() {
        return utils.findClass;
    }

    /* 获取指定枚举标签的类。 */
    public Class<?> getClassByLabel(Object label) {
        MemberData data = utils.members.get(label);
        if (data != null) {
            return data.mClass;
        }
        return null;
    }

    public int size() {
        return utils.members.size();
    }

    // ---------- 实例方法 -----------

    /**
     * 实例指定标签类
     */
    public Object newInstance(Object... args) {
        return newInstance(utils.getLabel(), args);
    }

    public Object newInstance(Object label, Object[] objects) {
        MemberData memberData = utils.members.get(label);
        if (memberData != null && memberData.mClass != null) {
            try {
                return XposedHelpers.newInstance(memberData.mClass, objects);
            } catch (Throwable e) {
                logE(utils.getTAG(), "new instance class: " + memberData.mClass, e);
            }
        } else logW(utils.getTAG(), "class is null, cant new instance. label: " + label);
        return null;
    }

    public MethodTool methodTool() {
        return utils.getMethodTool();
    }

    public FieldTool fieldTool() {
        return utils.getFieldTool();
    }

    private void clear() {
        utils.members.clear();
    }
}
