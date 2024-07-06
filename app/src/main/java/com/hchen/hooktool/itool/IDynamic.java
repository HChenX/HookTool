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
 * 动态方法接口，
 * 方法具体介绍请看实现类。<br/>
 * {@link com.hchen.hooktool.tool.CoreTool}
 */
public interface IDynamic {

    <T, R> R callMethod(Object instance, String name, T ts);

    <R> R callMethod(Object instance, String name);

    <T> T getField(Object instance, String name);

    <T> T getField(Object instance, Field field);

    boolean setField(Object instance, String name, Object value);

    boolean setField(Object instance, Field field, Object value);

    boolean setAdditionalInstanceField(Object instance, String key, Object value);

    <T> T getAdditionalInstanceField(Object instance, String key);

    boolean removeAdditionalInstanceField(Object instance, String key);
}
