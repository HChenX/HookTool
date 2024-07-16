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
package com.hchen.hooktool.itool;

import com.hchen.hooktool.tool.ChainTool;

/**
 * 链式调用接口
 * 方法具体介绍请看实现类。<br/>
 * {@link com.hchen.hooktool.tool.ChainTool}
 */
public interface IChain {

    void chain(String clazz, ChainTool chain);

    void chain(String clazz, ClassLoader classLoader, ChainTool chain);

    void chain(Class<?> clazz, ChainTool chain);

    ChainTool.ChainHook method(String name, Object... params);

    ChainTool.ChainHook anyMethod(String name);

    ChainTool.ChainHook constructor(Object... params);

    ChainTool.ChainHook anyConstructor();
}
