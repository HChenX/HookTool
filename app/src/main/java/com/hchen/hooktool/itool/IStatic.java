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

import java.lang.reflect.Field;

/**
 * 静态方法类，
 * 方法具体介绍请看实现类。<br/>
 * {@link com.hchen.hooktool.tool.CoreTool}
 */
public interface IStatic {
    // --------- 实例类 ------------
    <T, R> R newInstance(String clz, T objects);

    <T, R> R newInstance(String clz, ClassLoader classLoader, T objects);

    <T, R> R newInstance(Class<?> clz, T objects);

    <R> R newInstance(String clz);

    <R> R newInstance(String clz, ClassLoader classLoader);

    <R> R newInstance(Class<?> clz);

    // --------- 调用静态方法 ------------
    <T, R> R callStaticMethod(String clz, String name, T objs);

    <T, R> R callStaticMethod(String clz, ClassLoader classLoader, String name, T objs);

    <T, R> R callStaticMethod(Class<?> clz, String name, T objs);

    <R> R callStaticMethod(String clz, String name);

    <R> R callStaticMethod(String clz, ClassLoader classLoader, String name);

    <R> R callStaticMethod(Class<?> clz, String name);

    // --------- 获取静态字段 ------------
    <T> T getStaticField(String clz, String name);

    <T> T getStaticField(String clz, ClassLoader classLoader, String name);

    <T> T getStaticField(Class<?> clz, String name);

    <T> T getStaticField(Field field);

    // --------- 设置静态字段 ------------
    boolean setStaticField(String clz, String name, Object value);

    boolean setStaticField(String clz, ClassLoader classLoader, String name, Object value);

    boolean setStaticField(Class<?> clz, String name, Object value);

    boolean setStaticField(Field field, Object value);

    // --------- 设置获取删除自定义字段 ------------
    boolean setAdditionalStaticField(String clz, String key, Object value);

    boolean setAdditionalStaticField(String clz, ClassLoader classLoader, String key, Object value);

    boolean setAdditionalStaticField(Class<?> clz, String key, Object value);

    <T> T getAdditionalStaticField(String clz, String key);

    <T> T getAdditionalStaticField(String clz, ClassLoader classLoader, String key);

    <T> T getAdditionalStaticField(Class<?> clz, String key);

    boolean removeAdditionalStaticField(String clz, String key);

    boolean removeAdditionalStaticField(String clz, ClassLoader classLoader, String key);

    boolean removeAdditionalStaticField(Class<?> clz, String key);
}
