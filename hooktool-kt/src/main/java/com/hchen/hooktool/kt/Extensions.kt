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
@file:JvmName("HookExtensionsKt")

package com.hchen.hooktool.kt

/**
 * 带类型检查地读取参数。
 *
 * @param index 参数下标
 * @return 类型匹配时返回参数值，否则返回 null
 */
@JvmSynthetic
inline fun <reified T> HookFrame.argAs(index: Int): T? = arg(index) as? T

/**
 * 带类型检查地读取返回值。
 *
 * @return 类型匹配时返回结果，否则返回 null
 */
@JvmSynthetic
inline fun <reified T> AfterScope.resultAs(): T? = result as? T

/**
 * 带类型检查地读取调用目标对象。
 *
 * @return 类型匹配时返回目标对象，静态方法返回 null
 */
@JvmSynthetic
inline fun <reified T> HookFrame.thisAs(): T? = thisObject as? T