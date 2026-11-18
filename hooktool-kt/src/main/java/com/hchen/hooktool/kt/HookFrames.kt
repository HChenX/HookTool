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

import java.lang.reflect.Executable

/**
 * 拦截阶段共享的状态接口。
 *
 * @author 焕晨HChen
 */
@HookDsl
interface HookFrame {
    /**
     * 当前拦截的方法或构造函数。
     */
    val method: Executable

    /**
     * 调用目标对象，静态方法为 null。
     */
    val thisObject: Any?

    /**
     * 参数个数。
     */
    val argCount: Int

    /**
     * 获取指定下标的参数。
     *
     * @param index 参数下标
     * @return 当前参数值
     */
    fun arg(index: Int): Any?

    /**
     * 修改指定下标的参数。
     *
     * @param index 参数下标
     * @param value 新的参数值
     */
    fun setArg(index: Int, value: Any?)

    /**
     * 单次调用的数据槽。
     */
    var data: Any?
}

/**
 * `before` 代码块的作用域。
 *
 * @author 焕晨HChen
 */
@HookDsl
interface BeforeScope : HookFrame {
    /**
     * 跳过原方法并返回 null，同时不执行 `after`。
     */
    fun skip()

    /**
     * 跳过原方法并返回指定值。
     *
     * @param value 返回给调用方的值
     */
    fun skipWith(value: Any?)

    /**
     * 抛出指定异常，不再调用原方法。
     *
     * @param throwable 待抛出的异常
     */
    fun throwIt(throwable: Throwable)
}

/**
 * `proceed` 代码块的作用域。
 *
 * @author 焕晨HChen
 */
@HookDsl
interface ProceedScope : HookFrame {
    /**
     * 使用当前参数调用原方法。
     *
     * @return 原方法返回值
     */
    fun proceed(): Any?

    /**
     * 使用指定目标对象与参数调用原方法。
     *
     * @param thisObject 调用目标对象
     * @param args 调用参数
     * @return 原方法返回值
     */
    fun proceedWith(thisObject: Any?, vararg args: Any?): Any?
}

/**
 * `after` 代码块的作用域。
 *
 * @author 焕晨HChen
 */
@HookDsl
interface AfterScope : HookFrame {
    /**
     * 调用返回值，赋值可替换。
     */
    var result: Any?

    /**
     * `proceed` 阶段未被消费的异常。
     */
    val throwable: Throwable?
}

/**
 * `onThrow` 代码块的作用域。
 *
 * @author 焕晨HChen
 */
@HookDsl
interface ThrowScope : HookFrame {
    /**
     * 发生异常的阶段。
     */
    val stage: Stage

    /**
     * 设置替代结果并将异常标记为已消费。
     *
     * @param value 返回给调用方的值
     */
    fun recoverWith(value: Any?)
}
