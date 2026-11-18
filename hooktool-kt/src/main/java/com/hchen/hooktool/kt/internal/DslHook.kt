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

import android.os.Bundle
import com.hchen.hooktool.hook.AbsHook
import com.hchen.hooktool.kt.HookGroup
import io.github.libxposed.api.XposedInterface
import com.hchen.hooktool.kt.Stage as DslStage

/**
 * DSL 使用的 Hook 实现。
 *
 * @author 焕晨HChen
 */
internal class DslHook(
    private val spec: MemberSpec,
    private val group: HookGroup,
) : AbsHook(spec.priority ?: XposedInterface.PRIORITY_DEFAULT, spec.id, null) {

    override fun before() {
        val block = spec.before ?: return
        block(FrameImpl(this, DslStage.BEFORE, null))
    }

    override fun proceed(chain: XposedInterface.Chain): Any? {
        val block = spec.proceed ?: return super.proceed(chain)
        return block(FrameImpl(this, DslStage.PROCEED, chain))
    }

    override fun after() {
        val block = spec.after ?: return
        block(FrameImpl(this, DslStage.AFTER, null))
    }

    override fun onThrow(stage: AbsHook.Stage, e: Throwable): Boolean {
        val block = spec.onThrow ?: return false
        val mapped = when (stage) {
            AbsHook.Stage.BEFORE -> DslStage.BEFORE
            AbsHook.Stage.PROCEED -> DslStage.PROCEED
            AbsHook.Stage.AFTER -> DslStage.AFTER
        }
        val frame = FrameImpl(this, mapped, null)
        val consumed = block(frame, e)
        return frame.isRecovered || consumed
    }

    override fun onHotReloading(extras: Bundle?, state: MutableMap<String, Any?>) {
        group.saveState(state)
    }

    override fun onHotReloaded(thisObject: Any?, inState: MutableMap<String, Any?>) {
        group.restoreState(inState)
    }
}