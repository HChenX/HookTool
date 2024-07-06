/*
 * This file is part of HookTool.

 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2024 HookTool Contributions
 */
package com.hchen.hooktool.callback;

import com.hchen.hooktool.tool.ParamTool;

/**
 * Hook 动作接口
 */
public abstract class IAction extends ParamTool {
    /**
     * 在目标方法调用前回调。
     */
    public void before() throws Throwable {
    }

    /**
     * 在目标方法调用后回调。
     */
    public void after() throws Throwable {
    }
}
