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
package com.hchen.hooktool.exception;

/**
 * 非 Xposed 环境异常
 *
 * @author 焕晨HChen
 */
public class NonXposedException extends RuntimeException {
    public NonXposedException() {
    }

    public NonXposedException(String message) {
        super(message);
    }

    public NonXposedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NonXposedException(Throwable cause) {
        super(cause);
    }
}
