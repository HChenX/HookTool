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
package com.hchen.hooktool.kt

import com.hchen.hooktool.kt.internal.ClassSpec
import com.hchen.hooktool.kt.internal.MemberKind
import com.hchen.hooktool.kt.internal.MemberSpec

import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * 目标类的声明作用域。
 * <p>
 * 代码块内只记录声明，解析与安装在其结束后统一进行。
 *
 * @author 焕晨HChen
 */
@HookDsl
class ClassScope internal constructor(internal val spec: ClassSpec) {
    /**
     * 声明一个精确匹配的方法。
     *
     * @param name 方法名
     * @param params 参数类型，取值为 KClass、Class 或类名字符串
     * @param block Hook 配置
     * @return 该声明的句柄
     */
    fun method(name: String, vararg params: Any, block: MemberScope.() -> Unit): MemberRef =
        declare(MemberKind.METHOD, name, params, null, block)

    /**
     * 声明一个构造函数。
     *
     * @param params 参数类型
     * @param block Hook 配置
     * @return 该声明的句柄
     */
    fun constructor(vararg params: Any, block: MemberScope.() -> Unit): MemberRef =
        declare(MemberKind.CONSTRUCTOR, null, params, null, block)

    /**
     * 声明类的静态初始化器。
     *
     * @param block Hook 配置
     * @return 该声明的句柄
     */
    fun classInitializer(block: MemberScope.() -> Unit): MemberRef =
        declare(MemberKind.CLINIT, null, emptyArray(), null, block)

    /**
     * 声明类中全部已声明方法。
     *
     * @param block 所有方法共用的 Hook 配置
     * @return 该声明的句柄
     */
    fun allMethods(block: MemberScope.() -> Unit): MemberRef =
        declare(MemberKind.ALL_METHODS, null, emptyArray(), null, block)

    /**
     * 声明匹配条件的所有方法。
     *
     * @param predicate 方法过滤条件
     * @param block 所有匹配方法共用的 Hook 配置
     * @return 该声明的句柄
     */
    fun methods(predicate: (Method) -> Boolean, block: MemberScope.() -> Unit): MemberRef =
        declare(MemberKind.PREDICATE_METHODS, null, emptyArray(), predicate, block)

    /**
     * 设置成员无法解析时的处理策略。
     *
     * @param policy 处理策略
     */
    fun onMissing(policy: MissingPolicy) {
        spec.missingPolicy = policy
    }

    /**
     * 设置热更新前的状态保存逻辑。
     *
     * @param block 将状态写入传入的集合
     */
    fun onReloading(block: (MutableMap<String, Any?>) -> Unit) {
        spec.onReloading = block
    }

    /**
     * 设置热更新后的状态恢复逻辑。
     *
     * @param block 读取 [onReloading] 保存的状态
     */
    fun onReloaded(block: (Map<String, Any?>) -> Unit) {
        spec.onReloaded = block
    }

    private fun declare(
        kind: MemberKind,
        name: String?,
        params: Array<out Any>,
        predicate: ((Method) -> Boolean)?,
        block: MemberScope.() -> Unit,
    ): MemberRef {
        val label = buildLabel(kind, name, params)
        val member = MemberSpec(kind, name, params, predicate, label)
        MemberScope(member).block()
        spec.members.add(member)
        return MemberRef(label)
    }

    private fun buildLabel(kind: MemberKind, name: String?, params: Array<out Any>): String {
        val owner = spec.targetName ?: spec.targetClass?.name ?: "?"
        val paramsText = params.joinToString(",") { tokenToString(it) }
        return when (kind) {
            MemberKind.CLINIT -> "<clinit>"
            MemberKind.ALL_METHODS -> "*"
            MemberKind.PREDICATE_METHODS -> "methods{predicate}"
            MemberKind.CONSTRUCTOR -> "$owner.<init>($paramsText)"
            MemberKind.METHOD -> "$owner#$name($paramsText)"
        }
    }

    private fun tokenToString(token: Any): String = when (token) {
        is Class<*> -> token.name
        is KClass<*> -> token.java.name
        is String -> token
        else -> token.toString()
    }
}

/**
 * 成员声明的句柄。
 *
 * @property label 声明的签名描述
 * @author 焕晨HChen
 */
class MemberRef internal constructor(
    /**
     * 声明的签名描述。
     */
    val label: String,
)