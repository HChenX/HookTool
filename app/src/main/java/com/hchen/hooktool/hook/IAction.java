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
package com.hchen.hooktool.hook;

import com.hchen.hooktool.data.Priority;
import com.hchen.hooktool.tool.ParamTool;

/**
 * Hook 动作接口
 * 
 * @author 焕晨HChen
 */
public abstract class IAction extends ParamTool {
    public final Priority priority;

    public IAction() {
        this.priority = Priority.DEFAULT;
    }

    public IAction(Priority priority) {
        this.priority = priority;
    }

    /**
     * 在目标方法调用前回调。
     */
    public void before() {
    }

    /**
     * 在目标方法调用后回调。
     */
    public void after() {
    }
}
