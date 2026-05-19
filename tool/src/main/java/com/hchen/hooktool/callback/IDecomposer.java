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
 * 支持抛出异常的函数式供应者接口。
 * <p>
 * 功能类似于 {@link java.util.function.Supplier}，但其 {@code get()} 方法
 * 允许声明并抛出受检异常。适用于封装可能产生异常的延迟计算或资源获取场景。
 *
 * @param <R> 供应者返回结果的类型
 * @author 焕晨HChen
 */
public interface IDecomposer<R> {
    /**
     * 执行业务计算并返回结果。
     * <p>
     * 实现者应在此方法中封装实际的业务计算逻辑或资源获取逻辑。
     * 调用方需自行处理可能抛出的异常。
     *
     * @return 计算或获取到的结果值
     * @throws Throwable 执行过程中可能抛出的任意异常
     */
    R get() throws Throwable;
}
