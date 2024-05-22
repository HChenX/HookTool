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
package com.hchen.hooktool.utils;

import com.hchen.hooktool.tool.ActionTool;

// 优化调用，只提供基本用法，详细用法请获取工具类对象
public class MethodOpt {
    private final DataUtils utils;

    public MethodOpt(DataUtils utils) {
        this.utils = utils;
    }

    public ActionTool getMethod(String name, Object... objs) {
        return utils.getMethodTool().getMethod(name, objs);
    }

    public ActionTool getAnyMethod(String name) {
        return utils.getMethodTool().getAnyMethod(name);
    }

    public ActionTool getConstructor(Object... objs) {
        return utils.getMethodTool().getConstructor(objs);
    }

    public ActionTool getAnyConstructor() {
        return utils.getMethodTool().getAnyConstructor();
    }
}
