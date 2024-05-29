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
    protected final DataUtils utils;

    public StaticAct(DataUtils utils) {
        super(utils);
        this.utils = utils;
        ExpandTool expandTool = new ExpandTool(utils);
        iStatic = expandTool;
        iDynamic = expandTool;
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
    public <T, R> R newInstance(Class<?> claz, T objs) {
        return iStatic.newInstance(claz, objs);
    }

    public <R> R newInstance(Class<?> clz) {
        return iStatic.newInstance(clz);
    }

    /**
     * 请使用 new Object[]{} 传入参数。<br/>
     * 如果仅传入一个参数可以不使用 new Object[]{}<br/>
     * 这是为了规避泛型与可变参数的冲突。
     */
    public <T, R> R callStaticMethod(Class<?> clz, String name, T objs) {
        return iStatic.callStaticMethod(clz, name, objs);
    }

    public <R> R callStaticMethod(Class<?> clz, String name) {
        return iStatic.callStaticMethod(clz, name);
    }

    public <T> T getStaticField(Class<?> clz, String name) {
        return iStatic.getStaticField(clz, name);
    }

    public boolean setStaticField(Class<?> clz, String name, Object value) {
        return iStatic.setStaticField(clz, name, value);
    }

    public boolean setAdditionalStaticField(Class<?> clz, String key, Object value) {
        return iStatic.setAdditionalStaticField(clz, key, value);
    }

    public <T> T getAdditionalStaticField(Class<?> clz, String key) {
        return iStatic.getAdditionalStaticField(clz, key);
    }

    public boolean removeAdditionalStaticField(Class<?> clz, String key) {
        return iStatic.removeAdditionalStaticField(clz, key);
    }
}
