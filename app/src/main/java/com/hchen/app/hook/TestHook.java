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
package com.hchen.app.hook;

import androidx.annotation.NonNull;

import com.hchen.hooktool.AbsModule;

/**
 * 示例 Hook 模块类。继承 {@link AbsModule}，用于演示 Hook 模块的基本结构。
 */
public class TestHook extends AbsModule {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onLoaded(@NonNull StageEnum stage, @NonNull Object param) {
    }
}
