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
@file:JvmName("HookKt")

package com.hchen.hooktool.kt

import com.hchen.hooktool.kt.internal.ClassSpec
import com.hchen.hooktool.kt.internal.Installer

/**
 * 为一个类声明并安装 Hook。
 * <p>
 * 代码块只记录声明，解析与安装在其结束后进行。
 *
 * @param target 目标类名
 * @param loader 解析类使用的类加载器，为 null 时使用默认值
 * @param block 声明内容
 * @return 整个代码块的句柄
 */
@JvmSynthetic
fun hook(
    target: String,
    loader: ClassLoader? = null,
    block: ClassScope.() -> Unit,
): HookGroup {
    val spec = ClassSpec(target, null, loader)
    ClassScope(spec).block()
    return Installer.install(spec)
}

/**
 * 为一个类声明并安装 Hook。
 *
 * @param target 目标类
 * @param block 声明内容
 * @return 整个代码块的句柄
 */
@JvmSynthetic
fun hook(
    target: Class<*>,
    block: ClassScope.() -> Unit,
): HookGroup {
    val spec = ClassSpec(null, target, target.classLoader)
    ClassScope(spec).block()
    return Installer.install(spec)
}
