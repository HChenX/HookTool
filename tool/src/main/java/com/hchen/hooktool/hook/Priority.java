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

 * Copyright (C) 2023-2025 HChenX
 */
package com.hchen.hooktool.hook;

/**
 * Hook 优先级
 * <p>
 * 范围 [ -10000, 10000 ]
 *
 * @author 焕晨HChen
 */
public class Priority {
    public static final int DEFAULT = 50;
    public static final int LOWEST = -10000;
    public static final int HIGHEST = 10000;

    private Priority() {
    }
}
