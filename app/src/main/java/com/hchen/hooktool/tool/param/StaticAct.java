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
package com.hchen.hooktool.tool.param;

import com.hchen.hooktool.itool.IDynamic;
import com.hchen.hooktool.itool.IStatic;
import com.hchen.hooktool.tool.ExpandTool;
import com.hchen.hooktool.utils.ConvertHelper;
import com.hchen.hooktool.utils.DataUtils;

/**
 * 静态操作专用工具。
 */
public class StaticAct extends ConvertHelper {
    private final IStatic iStatic;
    protected final IDynamic iDynamic;
    private Class<?> mClass;

    public StaticAct(DataUtils utils) {
        super(utils);
        ExpandTool expandTool = new ExpandTool(utils);
        iStatic = expandTool;
        iDynamic = expandTool;
    }

    public StaticAct to(Class<?> mClass) {
        this.mClass = mClass;
        return this;
    }

    public StaticAct to(String clazz) {
        this.mClass = utils.getExpandTool().findClass(clazz);
        return this;
    }

    /**
     * 指定静态对象，若不重新指定则一直使用上次静态对象。
     */
    public StaticAct to(String clazz, ClassLoader classLoader) {
        this.mClass = findClass(clazz, classLoader);
        return this;
    }

    /**
     * 查找指定的类
     */
    public Class<?> findClass(String name) {
        return iStatic.findClass(name);
    }

    public Class<?> findClass(String name, ClassLoader classLoader) {
        return iStatic.findClass(name, classLoader);
    }

    /**
     * 请使用 new Object[]{} 传入参数。<br/>
     * 如果仅传入一个参数可以不使用 new Object[]{}<br/>
     * 这是为了规避泛型与可变参数的冲突。
     */
    public <T, R> R newInstance(T objs) {
        return iStatic.newInstance(mClass, objs);
    }

    public <R> R newInstance() {
        return iStatic.newInstance(mClass);
    }

    /**
     * 请使用 new Object[]{} 传入参数。<br/>
     * 如果仅传入一个参数可以不使用 new Object[]{}<br/>
     * 这是为了规避泛型与可变参数的冲突。
     */
    public <T, R> R callStaticMethod(String name, T objs) {
        return iStatic.callStaticMethod(mClass, name, objs);
    }

    public <R> R callStaticMethod(String name) {
        return iStatic.callStaticMethod(mClass, name);
    }

    public <T> T getStaticField(String name) {
        return iStatic.getStaticField(mClass, name);
    }

    public boolean setStaticField(String name, Object value) {
        return iStatic.setStaticField(mClass, name, value);
    }

    public boolean setAdditionalStaticField(String key, Object value) {
        return iStatic.setAdditionalStaticField(mClass, key, value);
    }

    public <T> T getAdditionalStaticField(String key) {
        return iStatic.getAdditionalStaticField(mClass, key);
    }

    public boolean removeAdditionalStaticField(String key) {
        return iStatic.removeAdditionalStaticField(mClass, key);
    }
}
