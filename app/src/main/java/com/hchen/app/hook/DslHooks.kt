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

import com.hchen.app.demo.Target
import com.hchen.hooktool.kt.HookGroup
import com.hchen.hooktool.kt.MissingPolicy
import com.hchen.hooktool.kt.argAs
import com.hchen.hooktool.kt.hook
import com.hchen.hooktool.kt.resultAs
import com.hchen.hooktool.log.AndroidLog

/**
 * Kotlin DSL 用法示例。
 * <p>
 * 覆盖成员声明、阶段回调、参数改写、批量安装、缺失策略与热更新状态。
 *
 * @author 焕晨HChen
 */
class DslHooks : DemoHook {
    private val groups = mutableListOf<HookGroup>()
    private var callCount = 0

    override fun install() {
        groups += hook(Target::class.java) {
            method("greet", String::class) {
                id("dsl-greet")
                priority(60)
                observe()
                before {
                    callCount++
                    AndroidLog.logD(TAG, "greet 参数: " + argAs<String>(0))
                    setArg(0, "dsl")
                }
                after {
                    AndroidLog.logD(TAG, "greet 返回值: " + resultAs<String>())
                }
            }

            constructor(String::class) {
                after { AndroidLog.logD(TAG, "构造完成") }
            }

            clinit {
                after { AndroidLog.logD(TAG, "静态初始化块执行完成") }
            }

            method("noise") {
                optional()
                before { skip() }
            }

            onReloading { state -> state["count"] = callCount }
            onReloaded { state -> callCount = (state["count"] as? Int) ?: 0 }
        }

        groups += hook("com.hchen.app.demo.Target") {
            onMissing(MissingPolicy.REPORT)

            method("getSecret") {
                after { result = "dsl-secret" }
            }

            method("absent") {
                before { skip() }
            }
        }

        groups += hook(Target::class.java) {
            methods({ method -> method.name.startsWith("describe") }) {
                before { AndroidLog.logD(TAG, "批量拦截: " + method.name) }
            }
        }

        groups += hook(Target::class.java) {
            method("add", Int::class, Int::class) {
                proceed {
                    val sum = proceed() as? Int ?: 0
                    sum * 2
                }
                onThrow {
                    recoverWith(-1)
                    true
                }
            }
        }

        for (group in groups) {
            for (missing in group.report().missing) {
                AndroidLog.logW(TAG, "未安装: " + missing.target)
            }
        }
    }

    override fun uninstall() {
        for (group in groups) {
            group.unhook()
        }
        groups.clear()
    }

    private companion object {
        private const val TAG = "DslHooks"
    }
}
