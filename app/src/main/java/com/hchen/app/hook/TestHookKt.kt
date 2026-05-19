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

import com.hchen.hooktool.AbsModule

/**
 * Hook 功能的 Kotlin 示例模块实现。
 *
 * 继承自 [AbsModule]，以 Kotlin 语法展示了 Hook 模块的基本结构。
 * 开发者应在 [onLoaded] 方法中根据加载阶段注册具体的 Hook 逻辑。
 *
 * @see AbsModule
 */
class TestHookKt : AbsModule() {
    /**
     * 模块加载阶段的回调入口。
     *
     * 由 HookTool 框架在目标模块加载完成时调用。
     * 开发者应根据 [stage] 参数判断当前所处的生命周期阶段，
     * 并在此注册方法 Hook、类拦截等具体操作。
     *
     * @param stage 当前模块所处的加载阶段枚举值
     * @param param 与当前阶段关联的参数对象，具体类型取决于所处阶段
     */
    override fun onLoaded(stage: StageEnum, param: Any) {
    }
}
