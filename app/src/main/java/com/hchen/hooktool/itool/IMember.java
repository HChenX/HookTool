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
package com.hchen.hooktool.itool;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.tool.ExpandTool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;

public interface IMember {
    // ---------- 查找类是否存在 -------------
    boolean findClassIfExists(String clazz);

    boolean findClassIfExists(String clazz, ClassLoader classLoader);

    // --------- 查找类 ------
    Class<?> findClass(String name);

    Class<?> findClass(String name, ClassLoader classLoader);

    // --------- 检查指定方法是否存在 ------

    /**
     * 检查指定方法是否存在，不存在则返回 false。
     */
    boolean findMethodIfExists(String clazz, String name, Object... ojbs);

    boolean findMethodIfExists(String clazz, ClassLoader classLoader,
                               String name, Object... ojbs);

    /**
     * 检查指定方法名是否存在，不存在则返回 false。
     */
    boolean findAnyMethodIfExists(String clazz, String name);

    boolean findAnyMethodIfExists(String clazz, ClassLoader classLoader, String name);

    // --------- 查找方法 ------
    Method findMethod(String clazz, String name, Object... objects);

    Method findMethod(String clazz, ClassLoader classLoader, String name, Object... objects);

    Method findMethod(Class<?> clazz, String name, Object... objects);

    ArrayList<Method> findAnyMethod(String clazz, String name);

    ArrayList<Method> findAnyMethod(String clazz, ClassLoader classLoader, String name);

    ArrayList<Method> findAnyMethod(Class<?> clazz, String name);

    // --------- 查找构造函数 -----------
    Constructor<?> findConstructor(String clazz, Object... objects);

    Constructor<?> findConstructor(String clazz, ClassLoader classLoader, Object... objects);

    Constructor<?> findConstructor(Class<?> clazz, Object... objects);

    ArrayList<Constructor<?>> findAnyConstructor(String clazz);

    ArrayList<Constructor<?>> findAnyConstructor(String clazz, ClassLoader classLoader);

    ArrayList<Constructor<?>> findAnyConstructor(Class<?> clazz);

    // --------- 查找字段 -----------

    /**
     * 查找指定字段是否存在，不存在返回 false
     */
    boolean findFieldIfExists(String clazz, String name);

    boolean findFieldIfExists(String clazz, ClassLoader classLoader, String name);

    Field findField(String clazz, String name);

    Field findField(String clazz, ClassLoader classLoader, String name);

    Field findField(Class<?> clazz, String name);

    // --------- 执行 hook -----------
    public void hook(Member member, IAction iAction);

    public void hook(ArrayList<?> members, IAction iAction);

    public IAction returnResult(final Object result);

    public IAction doNothing();

    // --------- 过滤方法 -----------
    public ArrayList<Method> filterMethod(Class<?> clazz, ExpandTool.IFindMethod iFindMethod);

    public ArrayList<Constructor<?>> filterMethod(Class<?> clazz, ExpandTool.IFindConstructor iFindConstructor);

}
