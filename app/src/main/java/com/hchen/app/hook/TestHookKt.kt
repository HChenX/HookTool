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
package com.hchen.app.hook

import android.annotation.SuppressLint
import android.os.Bundle
import com.hchen.hooktool.AbsModule
import com.hchen.hooktool.hook.AbsHook
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

/**
 * Hook 功能的 Kotlin 示例模块实现。
 *
 * @author 焕晨HChen
 */
class TestHookKt : AbsModule() {
    /**
     * 包就绪回调：注册示例 Hook。
     */
    @SuppressLint("XposedNewApi")
    override fun onPackageReady(param: PackageReadyParam) {
        registerHooks()
    }

    /**
     * 热更新完成回调：重新注册 Hook（旧句柄已由框架解除）。
     */
    @SuppressLint("XposedNewApi")
    override fun onHotReloaded(param: HotReloadedParam) {
        registerHooks()
    }

    @SuppressLint("XposedNewApi")
    private fun registerHooks() {
        "com.hchen.test.Test".hookMethod(
            "test",
            String::class.java,
            object : AbsHook() {
                /**
                 * 前置拦截回调，在目标方法执行之前调用。
                 */
                override fun before() {
                    super.before()
                }

                /**
                 * 原方法调用回调，调用被拦截的目标方法。
                 *
                 * @param chain 当前调用链对象
                 * @return 原方法的执行结果
                 */
                @Throws(Throwable::class)
                override fun proceed(chain: XposedInterface.Chain): Any? {
                    return super.proceed(chain)
                }

                /**
                 * 后置拦截回调，在目标方法执行完成后调用。
                 */
                override fun after() {
                    super.after()
                    thisObject?.setField("test", true)
                }

                /**
                 * 异常回调，当钩子生命周期中发生异常时触发。
                 *
                 * @param stage 异常发生的生命周期阶段
                 * @param e     被捕获的异常对象
                 * @return 返回 `true` 表示异常已被消费
                 */
                override fun onThrow(stage: StageEnum, e: Throwable): Boolean {
                    return super.onThrow(stage, e)
                }

                /**
                 * 热重载完成回调，恢复之前保存的状态（本示例仅恢复 thisObject 字段）。
                 *
                 * @param thisObject 该实例最新的宿主对象实例，
                 *                   可能为 `null`（静态方法或 key 未设置时）
                 * @param inState    合并后的全局状态快照
                 */
                override fun onHotReloaded(thisObject: Any?, inState: MutableMap<String, Any?>) {
                    super.onHotReloaded(thisObject, inState)
                    thisObject?.setField("test", true)
                }
            }
        )
    }

    /**
     * 模块级热重载准备回调。
     *
     * @param extras 热重载附加数据，可能为 `null`
     * @return 模块级状态键值对
     */
    override fun onHotReloading(extras: Bundle?): MutableMap<String, Any?> {
        return mutableMapOf()
    }
}
