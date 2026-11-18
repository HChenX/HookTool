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

import com.hchen.hooktool.hook.Hook
import com.hchen.hooktool.kt.HookGroup
import com.hchen.hooktool.kt.MissingMember
import com.hchen.hooktool.kt.MissingPolicy
import com.hchen.hooktool.log.AndroidLog
import com.hchen.hooktool.reflect.Reflect
import com.hchen.hooktool.reflect.ReflectException

import java.lang.reflect.Executable
import java.util.concurrent.atomic.AtomicInteger

import kotlin.reflect.KClass

/**
 * 安装阶段：解析记录下来的声明并安装 Hook。
 *
 * @author 焕晨HChen
 */
internal object Installer {
    private const val TAG = "HookDsl"
    private val counter = AtomicInteger()

    /**
     * 安装一个类声明。
     *
     * @param spec 记录下来的声明
     * @return 整个代码块的句柄
     */
    fun install(spec: ClassSpec): HookGroup {
        val group = HookGroup("g${counter.incrementAndGet()}")
        group.setReloadHandlers(spec.onReloading, spec.onReloaded)
        try {
            installMembers(spec, group)
            return group
        } catch (throwable: Throwable) {
            // 安装中途失败时卸载已生效的 Hook：调用方拿不到句柄，不能留下无法解除的拦截。
            group.unhook()
            throw throwable
        }
    }

    /**
     * 解析并安装一个类声明中的全部成员。
     *
     * @param spec 记录下来的声明
     * @param group 本次安装的句柄集合
     */
    private fun installMembers(spec: ClassSpec, group: HookGroup) {
        val clazz = try {
            spec.targetClass ?: Reflect.findClass(spec.targetName!!, spec.loader)
        } catch (error: ReflectException) {
            if (spec.missingPolicy == MissingPolicy.STRICT) throw error
            if (spec.missingPolicy == MissingPolicy.REPORT) {
                group.recordMissing(
                    MissingMember(spec.targetName ?: "?", error.kind(), error.message ?: "")
                )
            }
            return
        }

        val installed = mutableSetOf<Executable>()

        for (member in spec.members) {
            if (member.kind == MemberKind.CLINIT) {
                val hook = DslHook(member, group)
                val handle = Hook.hookClassInitializer(clazz, hook)
                group.add(hook, handle, "${clazz.name}.<clinit>")
                logInstall(member, "${clazz.name}.<clinit>")
                continue
            }

            val executables = try {
                resolve(clazz, member, spec.loader)
            } catch (error: ReflectException) {
                handleMissing(group, member, spec, error)
                continue
            }

            if (executables.isEmpty()) {
                handleMissing(
                    group,
                    member,
                    spec,
                    ReflectException(
                        ReflectException.Kind.METHOD_NOT_FOUND,
                        member.label,
                        "No member matched ${member.label}",
                    ),
                )
                continue
            }

            for (executable in executables) {
                if (!installed.add(executable)) {
                    throw ReflectException(
                        ReflectException.Kind.DUPLICATE,
                        member.label,
                        "Member declared twice in the same hook block: ${executable.toGenericString()}",
                    )
                }
                val hook = DslHook(member, group)
                val handle = Hook.hook(executable, hook)
                group.add(hook, handle, executable.toGenericString())
                logInstall(member, executable.toGenericString())
            }
        }
    }

    /**
     * 按成员级 `optional` 与类级缺失策略处理无法解析的成员。
     *
     * @param group 本次安装的句柄集合
     * @param member 无法解析的成员
     * @param spec 记录下来的声明
     * @param error 解析失败的原因
     */
    private fun handleMissing(
        group: HookGroup,
        member: MemberSpec,
        spec: ClassSpec,
        error: ReflectException,
    ) {
        val policy = if (member.optional) MissingPolicy.IGNORE else spec.missingPolicy
        if (policy == MissingPolicy.STRICT) throw error
        if (policy == MissingPolicy.REPORT) {
            group.recordMissing(MissingMember(member.label, error.kind(), error.message ?: ""))
        }
    }

    private fun logInstall(member: MemberSpec, signature: String) {
        if (member.logInstall) AndroidLog.logI(TAG, "Hooked $signature")
    }

    private fun resolve(
        clazz: Class<*>,
        member: MemberSpec,
        loader: ClassLoader?,
    ): List<Executable> {
        val params = resolveParams(member, loader)
        return when (member.kind) {
            MemberKind.METHOD -> listOf(
                Reflect.lookupMethod(clazz, member.name!!, *params).get()
            )

            MemberKind.CONSTRUCTOR -> listOf(
                Reflect.lookupConstructor(clazz, *params).get()
            )

            MemberKind.ALL_METHODS -> Reflect.findAllMethod(clazz, null).toList()

            MemberKind.PREDICATE_METHODS -> {
                val predicate = member.predicate ?: return emptyList()
                Reflect.findAllMethod(clazz, null).filter(predicate)
            }

            MemberKind.CLINIT -> emptyList()
        }
    }

    private fun resolveParams(member: MemberSpec, loader: ClassLoader?): Array<Class<*>> =
        member.paramTokens.map { token ->
            when (token) {
                is Class<*> -> token
                is KClass<*> -> token.java
                is String -> Reflect.findClass(token, loader)
                else -> throw ReflectException(
                    ReflectException.Kind.PARAM_MISMATCH,
                    member.label,
                    "Parameter tokens must be KClass, Class or a class name String, but were ${token.javaClass.name}",
                )
            }
        }.toTypedArray()
}
