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

import static com.hchen.hooktool.HCHook.initSafe;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import androidx.annotation.NonNull;

import com.hchen.hooktool.data.MemberData;
import com.hchen.hooktool.utils.DataUtils;
import com.hchen.hooktool.utils.MethodOpt;

import de.robv.android.xposed.XposedHelpers;

public class ClassTool extends MethodOpt {
    private final DataUtils utils;

    public ClassTool(DataUtils utils) {
        super(utils);
        this.utils = utils;
        clear();
        utils.classTool = this;
    }

    public MethodTool to(@NonNull Object label) {
        utils.setLabel(label);
        return utils.getMethodTool();
    }

    // -------- 传统索引形式 ----------

    /**
     * 查找指定类，通过索引。<br/>
     * 旧实现。
     */
    /*public ClassTool findClass(String className) {
        return findClass(className, utils.mCustomClassLoader == null ? classLoader : utils.mCustomClassLoader);
    }

    public ClassTool findClass(String className, boolean add) {
        return findClass(className, add, utils.mCustomClassLoader == null ? classLoader : utils.mCustomClassLoader);
    }

    public ClassTool findClass(String className, ClassLoader classLoader) {
        return findClass(className, true, classLoader);
    }

    public ClassTool findClass(String className, boolean add, ClassLoader classLoader) {
        initSafe();
        if (utils.findClass != null) utils.findClass = null;
        try {
            utils.findClass = XposedHelpers.findClass(className,
                    classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(utils.getTAG(), "The specified class could not be found: " + className + " e: " + e);
            utils.findClass = null;
        }
        // utils.classes.add(utils.findClass);
        if (add) utils.classes.put(new MemberData(utils.findClass));
        return utils.getClassTool();
    }*/

    // ---------- 标签形式 ------------

    /**
     * 手动添加类。
     */
    public ClassTool add(@NonNull Object label, Class<?> clazz) {
        if (clazz == null) {
            logW(utils.getTAG(), "the class is null! label: " + label);
        }
        utils.findClass = clazz;
        utils.labelClasses.put(label, new MemberData(clazz));
        utils.setLabel(label);
        return utils.getClassTool();
    }

    /**
     * 查找指定类，并为其设置独有的标签。
     */
    public ClassTool findClass(@NonNull Object label, String className) {
        return findClass(label, className,
                utils.getClassLoader());
    }

    public ClassTool findClass(@NonNull Object label, String className, ClassLoader classLoader) {
        initSafe();
        if (utils.findClass != null) utils.findClass = null;
        try {
            utils.findClass = XposedHelpers.findClass(className,
                    classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(utils.getTAG(), "the class not found!", e);
            utils.findClass = null;
        }
        // utils.classes.add(utils.findClass);
        utils.labelClasses.put(label, new MemberData(utils.findClass));
        utils.setLabel(label);
        return utils.getClassTool();
    }

    /* 获取本次得到的类 */
    public Class<?> getFindClass() {
        return utils.findClass;
    }

    /* 获取指定枚举标签的类。 */
    public Class<?> getClassByLabel(Object label) {
        MemberData data = utils.labelClasses.get(label);
        if (data != null) {
            return data.mClass;
        }
        return null;
    }

    public int size() {
        return utils.labelClasses.size();
    }

    // ---------- 实例方法 -----------

    /**
     * 实例当前索引类。
     */
    public Object newInstance(Object... args) {
        return newInstance(utils.getLabel(), args);
    }

    /**
     * 实例指定索引类，索引从 0 开始。
     */
    public Object newInstance(Object label, Object... args) {
        MemberData memberData = utils.labelClasses.get(label);
        if (memberData != null && memberData.mClass != null) {
            try {
                return XposedHelpers.newInstance(memberData.mClass, args);
            } catch (Throwable e) {
                logE(utils.getTAG(), "new instance class: " + memberData.mClass, e);
            }
        } else logW(utils.getTAG(), "class is null, cant new instance. label: " + label);
        return null;
    }

    /**
     * 实例全部类。
     * 不建议使用。<br/>
     * 已经报废。
     */
    /* public ArrayList<Object> newInstanceAll(MapUtils<Object[]> mapUtils) {
        utils.newInstances.clear();
        if (utils.newInstances.size() != mapUtils.getHashMap().size()) {
            logE(utils.getTAG(), "The length of the instance parameter list is inconsistent!");
            return new ArrayList<>();
        }
        for (int i = 0; i < size(); i++) {
            utils.newInstances.add(newInstance(i, mapUtils.get(i)));
        }
        return utils.newInstances;
    } */

    // 无需再回归此类。
    // public HCHook hcHook() {
    //     return utils.getHCHook();
    // }
    public MethodTool methodTool() {
        return utils.getMethodTool();
    }

    public FieldTool fieldTool() {
        return utils.getFieldTool();
    }

    /* 不建议使用 clear 本工具应该是一次性的。 */
    private void clear() {
        utils.labelClasses.clear();
    }
}
