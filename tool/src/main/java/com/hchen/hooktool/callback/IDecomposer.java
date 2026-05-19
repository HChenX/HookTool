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
package com.hchen.hooktool.callback;

/**
 * 可抛出异常的供应者接口。用于封装可能产生异常的计算逻辑。
 *
 * @param <R> 返回值类型
 * @author 焕晨HChen
 */
public interface IDecomposer<R> {
    /**
     * 执行计算并返回结果，可能抛出异常。
     *
     * @return 计算结果
     * @throws Throwable 执行过程中可能抛出的异常
     */
    R get() throws Throwable;
}
