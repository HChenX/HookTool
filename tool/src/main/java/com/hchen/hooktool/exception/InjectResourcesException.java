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
package com.hchen.hooktool.exception;

/**
 * 资源注入异常。当模块资源注入失败时抛出。
 *
 * @author 焕晨HChen
 */
public final class InjectResourcesException extends RuntimeException {
    /**
     * 创建空的资源注入异常。
     */
    public InjectResourcesException() {
    }

    /**
     * 创建带有指定消息的资源注入异常。
     *
     * @param message 异常消息
     */
    public InjectResourcesException(String message) {
        super(message);
    }

    /**
     * 创建带有指定消息和原因的资源注入异常。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public InjectResourcesException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建带有指定原因的资源注入异常。
     *
     * @param cause 异常原因
     */
    public InjectResourcesException(Throwable cause) {
        super(cause);
    }
}
