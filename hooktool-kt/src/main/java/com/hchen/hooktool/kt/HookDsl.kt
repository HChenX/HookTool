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

/**
 * DSL 作用域标记。
 * <p>
 * 所有作用域共用该标记，内层作用域不可隐式访问外层成员。
 *
 * @author 焕晨HChen
 */
@DslMarker
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPE,
    AnnotationTarget.PROPERTY,
)
annotation class HookDsl

/**
 * 拦截阶段。
 *
 * @author 焕晨HChen
 */
enum class Stage {
    /**
     * 原方法执行之前。
     */
    BEFORE,

    /**
     * 原方法执行期间。
     */
    PROCEED,

    /**
     * 原方法返回之后。
     */
    AFTER,
}

/**
 * 成员无法解析时的处理策略。
 *
 * @author 焕晨HChen
 */
enum class MissingPolicy {
    /**
     * 抛出异常终止安装，默认值。
     */
    STRICT,

    /**
     * 静默跳过该成员。
     */
    IGNORE,

    /**
     * 跳过该成员并记录到 [HookGroup.report]。
     */
    REPORT,
}
