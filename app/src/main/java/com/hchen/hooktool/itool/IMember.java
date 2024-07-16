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
import com.hchen.hooktool.tool.CoreTool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;

/**
 * 成员操作接口，
 * 方法具体介绍请看实现类。<br/>
 * {@link com.hchen.hooktool.tool.CoreTool}
 */
public interface IMember {
    // ---------- 查找类是否存在 -------------
    boolean existsClass(String clazz);

    boolean existsClass(String clazz, ClassLoader classLoader);

    // --------- 查找类 ------
    Class<?> findClass(String name);

    Class<?> findClass(String name, ClassLoader classLoader);

    // --------- 检查指定方法是否存在 ------

    /**
     * 检查指定方法是否存在，不存在则返回 false。
     */
    boolean existsMethod(String clazz, String name, Object... ojbs);

    boolean existsMethod(String clazz, ClassLoader classLoader,
                         String name, Object... ojbs);

    /**
     * 检查指定方法名是否存在，不存在则返回 false。
     */
    boolean existsAnyMethod(String clazz, String name);

    boolean existsAnyMethod(String clazz, ClassLoader classLoader, String name);

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
    boolean existsField(String clazz, String name);

    boolean existsField(String clazz, ClassLoader classLoader, String name);

    Field findField(String clazz, String name);

    Field findField(String clazz, ClassLoader classLoader, String name);

    Field findField(Class<?> clazz, String name);

    // --------- 执行 hook -----------
    XC_MethodHook.Unhook hook(String clazz, String method, Object... params);

    XC_MethodHook.Unhook hook(String clazz, ClassLoader classLoader, String method, Object... params);

    ArrayList<XC_MethodHook.Unhook> hook(String clazz, IAction iAction);

    XC_MethodHook.Unhook hook(String clazz, Object... params);

    XC_MethodHook.Unhook hook(Class<?> clazz, String method, Object... params);

    XC_MethodHook.Unhook hook(Member member, IAction iAction);

    ArrayList<XC_MethodHook.Unhook> hook(ArrayList<?> members, IAction iAction);

    IAction returnResult(final Object result);

    IAction doNothing();

    // --------- 解除 hook ---------

    boolean unHook(XC_MethodHook.Unhook unhook);

    boolean unHook(Member hookMember, XC_MethodHook xcMethodHook);

    // --------- 过滤方法 -----------
    
    ArrayList<Method> filterMethod(Class<?> clazz, CoreTool.IFindMethod iFindMethod);

    ArrayList<Constructor<?>> filterMethod(Class<?> clazz, CoreTool.IFindConstructor iFindConstructor);

    // ------- 打印堆栈 --------------
    
    String getStackTrace();
}
