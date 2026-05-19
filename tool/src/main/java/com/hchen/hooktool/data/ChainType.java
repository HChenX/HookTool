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
 * 链式 Hook 调用中可用的方法查找策略枚举。
 * <p>
 * 每个枚举常量对应 {@link ChainData} 的一种构造场景，运行时由框架据此
 * 决定采用何种方式在目标类中定位可执行对象。
 *
 * @author 焕晨HChen
 */
public enum ChainType {
    /**
     * 跳过查找流程，直接使用用户已提供的 {@link java.lang.reflect.Executable} 实例。
     */
    EXECUTABLE,
    /**
     * 根据方法名和参数类型进行精确匹配，定位到唯一的目标方法。
     */
    FIND_METHOD,
    /**
     * 仅依据方法名进行模糊匹配，返回所有同名方法（不区分参数类型）。
     */
    FIND_ALL_METHOD,
    /**
     * 根据构造函数的参数类型进行精确匹配，定位到唯一的目标构造函数。
     */
    FIND_CONSTRUCTOR,
    /**
     * 查找目标类中声明的全部构造函数。
     */
    FIND_ALL_CONSTRUCTOR
}
