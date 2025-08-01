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
 * Copyright (C) 2023–2025 HChenX
 */
package com.hchen.hooktool.helper;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 范围模式
 *
 * @author 焕晨HChen
 */
public class RangeHelper {
    /**
     * 大于
     */
    public static final int GT = 0;
    /**
     * 小于
     */
    public static final int LT = 1;
    /**
     * 等于
     */
    public static final int EQ = 2;
    /**
     * 大于等于
     */
    public static final int GE = 3;
    /**
     * 小于等于
     */
    public static final int LE = 4;

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
