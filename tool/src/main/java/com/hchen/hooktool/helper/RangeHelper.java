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
package com.hchen.hooktool.helper;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 版本区间比较操作符的常量定义工具类。
 * <p>
 * 本类提供了五种整数比较模式常量（{@code >}、{@code <}、{@code ==}、{@code >=}、{@code <=}），
 * 用于版本号区间的判定逻辑。配合 {@link RangeModeFlag} 注解可在编译期对方法参数进行合法性约束，
 * 确保仅接受本类定义的常量值。
 *
 * @author 焕晨HChen
 */
public final class RangeHelper {
    /**
     * 大于比较操作符，对应 {@code >} 运算。
     */
    public static final int GT = 0;
    /**
     * 小于比较操作符，对应 {@code <} 运算。
     */
    public static final int LT = 1;
    /**
     * 等于比较操作符，对应 {@code ==} 运算。
     */
    public static final int EQ = 2;
    /**
     * 大于等于比较操作符，对应 {@code >=} 运算。
     */
    public static final int GE = 3;
    /**
     * 小于等于比较操作符，对应 {@code <=} 运算。
     */
    public static final int LE = 4;

    /**
     * 范围比较模式的类型安全约束注解。
     * <p>
     * 用于限定方法参数只可接受 {@link #GT}、{@link #LT}、{@link #EQ}、{@link #GE}、{@link #LE}
     * 这五个常量值之一。该注解的保留策略为 {@link RetentionPolicy#SOURCE}，仅在编译期生效。
     */
    @IntDef(value = {
        GT,
        LT,
        EQ,
        GE,
        LE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RangeModeFlag {
    }

    private RangeHelper() {
    }
}
