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

import com.hchen.hooktool.kt.internal.MemberSpec

/**
 * 单个成员的 Hook 配置。
 *
 * @author 焕晨HChen
 */
@HookDsl
class MemberScope internal constructor(private val member: MemberSpec) {
    /**
     * 设置原方法执行前的代码块。
     *
     * @param block 拦截逻辑
     */
    fun before(block: BeforeScope.() -> Unit) {
        member.before = block
    }

    /**
     * 设置执行原方法的代码块。
     * <p>
     * 返回值作为方法结果，`after` 仍会执行。
     *
     * @param block 拦截逻辑
     */
    fun proceed(block: ProceedScope.() -> Any?) {
        member.proceed = block
    }

    /**
     * 设置原方法返回后的代码块。
     *
     * @param block 拦截逻辑
     */
    fun after(block: AfterScope.() -> Unit) {
        member.after = block
    }

    /**
     * 设置异常处理代码块。
     *
     * @param block 返回 true 表示异常已消费
     */
    fun onThrow(block: ThrowScope.(Throwable) -> Boolean) {
        member.onThrow = block
    }

    /**
     * 设置 Hook 标识，用于原子替换。
     *
     * @param id Hook 标识
     */
    fun id(id: String) {
        member.id = id
    }

    /**
     * 设置 Hook 优先级，数值越大越先执行，与框架默认值 50 比较。
     *
     * @param priority 优先级
     */
    fun priority(priority: Int) {
        member.priority = priority
    }

    /**
     * 安装成功时输出一行日志。
     */
    fun logInstall() {
        member.logInstall = true
    }

    /**
     * 该成员无法解析时静默跳过，不受类级策略影响。
     */
    fun optional() {
        member.optional = true
    }
}
