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
 * Shell 执行流程失败的分类原因。
 *
 * @author 焕晨HChen
 */
public enum ShellFailureReason {
    /**
     * 未添加待执行命令（调用 {@code exec()/async()} 前未调用 {@code cmd()}）。
     */
    NOT_CONFIGURED,
    /**
     * 命令被 {@code ICommandListener} 监听器拦截（{@code onCommand} 返回 {@code false}）。
     */
    INTERCEPTED,
    /**
     * 命令超过超时上限仍未收敛（同步 10s / 命令兜底 15s）。
     */
    TIMEOUT,
    /**
     * 同步等待结果时当前线程被中断。
     */
    INTERRUPTED,
    /**
     * 流写入/读取失败、进程或流意外终止（含命令重定向关闭流导致的会话失效）。
     */
    IO_ERROR
}
