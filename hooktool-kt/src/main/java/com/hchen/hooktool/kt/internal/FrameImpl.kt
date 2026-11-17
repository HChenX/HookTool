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
import com.hchen.hooktool.kt.ProceedScope
import com.hchen.hooktool.kt.Stage
import com.hchen.hooktool.kt.ThrowScope

import io.github.libxposed.api.XposedInterface

import java.lang.reflect.Executable

/**
 * 调用帧的访问实现。
 *
 * @author 焕晨HChen
 */
internal class FrameImpl(
    private val hook: DslHook,
    override val stage: Stage,
    private val chain: XposedInterface.Chain?,
) : BeforeScope, ProceedScope, AfterScope, ThrowScope {

    private var recovered = false

    /**
     * 是否调用过 {@link recoverWith}。
     */
    val isRecovered: Boolean
        get() = recovered

    override val method: Executable
        get() = hook.executable

    override val thisObject: Any?
        get() = hook.thisObject

    override val argCount: Int
        get() = method.parameterCount

    override fun arg(index: Int): Any? = hook.getArg(index)

    override fun setArg(index: Int, value: Any?) {
        hook.setArg(index, value)
    }

    override var data: Any?
        get() = hook.frameData
        set(value) {
            hook.frameData = value
        }

    override fun skip() {
        hook.setResult(null)
    }

    override fun skipWith(value: Any?) {
        hook.setResult(value)
    }

    override fun throwIt(throwable: Throwable) {
        hook.setThrowable(throwable)
    }

    override fun proceed(): Any? = requireChain().proceed()

    override fun proceedWith(thisObject: Any?, vararg args: Any?): Any? {
        val receiver = requireNotNull(thisObject) {
            "proceedWith() requires a non-null receiver, use proceed() for static calls."
        }
        return requireChain().proceedWith(receiver, arrayOf(*args))
    }

    override var result: Any?
        get() = hook.getResult()
        set(value) {
            hook.setResult(value)
        }

    override val throwable: Throwable?
        get() = hook.getThrowable()

    override fun recoverWith(value: Any?) {
        hook.setResult(value)
        recovered = true
    }

    private fun requireChain(): XposedInterface.Chain = chain
        ?: error("proceed() is only available inside a proceed { } block.")
}