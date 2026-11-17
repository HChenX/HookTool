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
package com.hchen.hooktool.kt.internal

import com.hchen.hooktool.kt.AfterScope
import com.hchen.hooktool.kt.BeforeScope
import com.hchen.hooktool.kt.MissingPolicy
import com.hchen.hooktool.kt.ProceedScope
import com.hchen.hooktool.kt.ThrowScope

import java.lang.reflect.Method

/**
 * 声明指向的成员类型。
 *
 * @author 焕晨HChen
 */
internal enum class MemberKind {
    METHOD,
    CONSTRUCTOR,
    CLINIT,
    ALL_METHODS,
    PREDICATE_METHODS,
}

/**
 * 记录下来的成员声明。
 *
 * @author 焕晨HChen
 */
internal class MemberSpec(
    val kind: MemberKind,
    val name: String?,
    val paramTokens: Array<out Any>,
    val predicate: ((Method) -> Boolean)?,
    val label: String,
) {
    var optional: Boolean = false
    var before: (BeforeScope.() -> Unit)? = null
    var proceed: (ProceedScope.() -> Any?)? = null
    var after: (AfterScope.() -> Unit)? = null
    var onThrow: (ThrowScope.(Throwable) -> Boolean)? = null
    var id: String? = null
    var priority: Int? = null
    var observe: Boolean = false
}

/**
 * 记录下来的类声明。
 *
 * @author 焕晨HChen
 */
internal class ClassSpec(
    val targetName: String?,
    val targetClass: Class<*>?,
    val loader: ClassLoader?,
) {
    var missingPolicy: MissingPolicy = MissingPolicy.STRICT
    val members = ArrayList<MemberSpec>()
    var onReloading: ((MutableMap<String, Any?>) -> Unit)? = null
    var onReloaded: ((Map<String, Any?>) -> Unit)? = null
}
