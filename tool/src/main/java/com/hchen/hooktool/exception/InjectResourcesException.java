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
 * 资源注入异常类，属于非受检异常（{@link RuntimeException} 的子类）。
 * <p>
 * 当 HookTool 在运行时向目标应用注入模块资源（如布局、字符串、drawable 等）失败时抛出。
 * 常见触发场景包括资源 ID 冲突、资源表损坏或目标应用上下文不可用等。
 *
 * @author 焕晨HChen
 */
public final class InjectResourcesException extends RuntimeException {
    /**
     * 构造一个不携带详细消息和原因的资源注入异常实例。
     */
    public InjectResourcesException() {
    }

    /**
     * 构造一个携带指定描述消息的资源注入异常实例。
     *
     * @param message 异常的描述信息
     */
    public InjectResourcesException(String message) {
        super(message);
    }

    /**
     * 构造一个携带指定描述消息和根本原因的资源注入异常实例。
     *
     * @param message 异常的描述信息
     * @param cause   导致此异常的根本原因（即原始异常）
     */
    public InjectResourcesException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造一个仅携带根本原因的资源注入异常实例。
     *
     * @param cause 导致此异常的根本原因（即原始异常）
     */
    public InjectResourcesException(Throwable cause) {
        super(cause);
    }
}
