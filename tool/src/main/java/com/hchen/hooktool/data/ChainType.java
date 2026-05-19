/*
 * This file is part of HookTool.
 *
 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * HookTool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with HookTool. If not, see <https://www.gnu.org/licenses/lgpl-2.1>.
 *
 * Copyright (C) 2024–2026 HChenX
 */
package com.hchen.hooktool.data;

/**
 * 链式调用类型枚举。
 *
 * @author 焕晨HChen
 */
public enum ChainType {
    /**
     * 使用指定的可执行对象。
     */
    EXECUTABLE,
    /**
     * 查找指定方法。
     */
    FIND_METHOD,
    /**
     * 查找所有指定名称的方法。
     */
    FIND_ALL_METHOD,
    /**
     * 查找指定构造函数。
     */
    FIND_CONSTRUCTOR,
    /**
     * 查找所有构造函数。
     */
    FIND_ALL_CONSTRUCTOR
}
