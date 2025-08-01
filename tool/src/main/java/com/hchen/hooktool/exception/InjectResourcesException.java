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
package com.hchen.hooktool.exception;

/**
 * 资源注入异常
 *
 * @author 焕晨HChen
 */
public class InjectResourcesException extends RuntimeException {
    public InjectResourcesException() {
    }

    public InjectResourcesException(String message) {
        super(message);
    }

    public InjectResourcesException(String message, Throwable cause) {
        super(message, cause);
    }

    public InjectResourcesException(Throwable cause) {
        super(cause);
    }
}
