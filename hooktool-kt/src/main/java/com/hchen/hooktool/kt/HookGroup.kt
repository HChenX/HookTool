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

import com.hchen.hooktool.hook.AbsHook
import com.hchen.hooktool.hook.HookRegistry
import com.hchen.hooktool.log.AndroidLog
import com.hchen.hooktool.reflect.ReflectException

import io.github.libxposed.api.XposedInterface

/**
 * 未能解析的成员。
 *
 * @property target 成员的签名描述
 * @property kind 反射层给出的失败分类
 * @property message 详细信息
 * @author 焕晨HChen
 */
data class MissingMember(
    val target: String,
    val kind: ReflectException.Kind,
    val message: String,
)

/**
 * Hook 代码块的安装结果。
 *
 * @property installed 已安装的成员签名
 * @property missing 未能解析的成员
 * @author 焕晨HChen
 */
data class InstallReport(
    val installed: List<String>,
    val missing: List<MissingMember>,
)

/**
 * Hook 代码块的句柄。
 *
 * @property id 分组标识
 * @author 焕晨HChen
 */
class HookGroup internal constructor(
    /**
     * 分组标识。
     */
    val id: String,
) {
    private val hooks = ArrayList<AbsHook>()
    private val handles = ArrayList<XposedInterface.HookHandle>()
    private val installed = ArrayList<String>()
    private val missing = ArrayList<MissingMember>()

    private var onReloadingBlock: ((MutableMap<String, Any?>) -> Unit)? = null
    private var onReloadedBlock: ((Map<String, Any?>) -> Unit)? = null
    private var saved = false
    private var restored = false

    /**
     * 获取安装结果。
     *
     * @return 安装结果快照
     */
    fun report(): InstallReport = InstallReport(installed.toList(), missing.toList())

    /**
     * 获取该分组安装的全部 Hook 句柄。
     *
     * @return 句柄快照
     */
    fun handles(): List<XposedInterface.HookHandle> = handles.toList()

    /**
     * 卸载该分组安装的全部 Hook。
     */
    fun unhook() {
        for (hook in hooks) {
            try {
                hook.unhookSelf()
            } catch (_: IllegalStateException) {
                // 该成员尚未安装
            } catch (throwable: Throwable) {
                AndroidLog.logW(TAG, "Failed to unhook a member of group $id.", throwable)
            }
        }
        for (hook in hooks) {
            HookRegistry.unregister(hook)
        }
        hooks.clear()
        handles.clear()
    }

    internal fun add(hook: AbsHook, handle: XposedInterface.HookHandle, signature: String) {
        hooks.add(hook)
        handles.add(handle)
        installed.add(signature)
    }

    internal fun recordMissing(member: MissingMember) {
        missing.add(member)
    }

    internal fun setReloadHandlers(
        onReloading: ((MutableMap<String, Any?>) -> Unit)?,
        onReloaded: ((Map<String, Any?>) -> Unit)?,
    ) {
        onReloadingBlock = onReloading
        onReloadedBlock = onReloaded
    }

    /**
     * 执行一次状态保存，同一分组内只执行一次。
     *
     * @param state 当前 Hook 的状态集合
     */
    internal fun saveState(state: MutableMap<String, Any?>) {
        val block = onReloadingBlock ?: return
        if (saved) return
        saved = true
        restored = false
        val local = HashMap<String, Any?>()
        block(local)
        for ((key, value) in local) {
            state[STATE_PREFIX + key] = value
        }
    }

    /**
     * 执行一次状态恢复，同一分组内只执行一次。
     *
     * @param state 框架合并后的状态集合
     */
    internal fun restoreState(state: Map<String, Any?>) {
        val block = onReloadedBlock ?: return
        if (restored) return
        restored = true
        saved = false
        val local = HashMap<String, Any?>()
        for ((key, value) in state) {
            if (key.startsWith(STATE_PREFIX)) {
                local[key.substring(STATE_PREFIX.length)] = value
            }
        }
        block(local)
    }

    internal companion object {
        /**
         * 状态键前缀。
         */
        private const val STATE_PREFIX = "dsl:"

        private const val TAG = "HookGroup"
    }
}