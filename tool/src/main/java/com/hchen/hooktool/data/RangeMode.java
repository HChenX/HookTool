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
package com.hchen.hooktool.data;

/**
 * 数值区间比较操作符枚举。
 *
 * @author 焕晨HChen
 */
public enum RangeMode {
    /**
     * 大于比较，对应 {@code >} 运算。
     */
    GT,
    /**
     * 小于比较，对应 {@code <} 运算。
     */
    LT,
    /**
     * 等于比较，对应 {@code ==} 运算。
     */
    EQ,
    /**
     * 大于等于比较，对应 {@code >=} 运算。
     */
    GE,
    /**
     * 小于等于比较，对应 {@code <=} 运算。
     */
    LE;

    /**
     * 判断实际值是否满足本枚举对应的比较关系。
     *
     * @param actual 实际测量值
     * @param target 目标比较值
     * @return 满足比较关系时返回 {@code true}
     */
    public boolean matches(float actual, float target) {
        return switch (this) {
            case EQ -> actual == target;
            case GT -> actual > target;
            case LT -> actual < target;
            case GE -> actual >= target;
            case LE -> actual <= target;
        };
    }

    public boolean matches(int actual, int target) {
        return matches((float) actual, (float) target);
    }
}
