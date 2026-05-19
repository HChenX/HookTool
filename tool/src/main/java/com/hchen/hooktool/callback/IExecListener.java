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

import androidx.annotation.NonNull;

/**
 * Shell 命令执行结果监听器接口。
 * <p>
 * 提供 Shell 命令执行完成后的多种回调方法，覆盖标准输出、标准错误输出、
 * Root 权限检测结果以及管道异常等场景。所有回调方法均提供默认空实现，
 * 实现者可按需选择性覆写。
 *
 * @author 焕晨HChen
 */
public interface IExecListener {
    /**
     * 标准输出（stdout）的回调方法。
     * <p>
     * 当 Shell 命令执行后产生标准输出数据时触发此回调。
     * 输出内容以字符串数组形式传递，每个元素代表一行输出。
     *
     * @param command  已执行的 Shell 命令字符串，不为 {@code null}
     * @param exitCode 命令的退出码字符串，不为 {@code null}
     * @param outputs  标准输出内容数组，每个元素为一行输出，不为 {@code null}
     */
    default void output(@NonNull String command, @NonNull String exitCode, @NonNull String[] outputs) {
    }

    /**
     * 标准错误输出（stderr）的回调方法。
     * <p>
     * 当 Shell 命令执行后产生错误输出数据时触发此回调。
     * 错误信息以字符串数组形式传递，每个元素代表一行错误信息。
     *
     * @param command  已执行的 Shell 命令字符串，不为 {@code null}
     * @param exitCode 命令的退出码字符串，不为 {@code null}
     * @param errors   标准错误输出内容数组，每个元素为一行错误信息，不为 {@code null}
     */
    default void error(@NonNull String command, @NonNull String exitCode, @NonNull String[] errors) {
    }

    /**
     * Root 权限检测结果的回调方法。
     * <p>
     * 当系统完成 Root 权限可用性检测后触发此回调，报告当前设备是否具备 Root 权限。
     *
     * @param hasRoot  {@code true} 表示设备已获取 Root 权限，{@code false} 表示未获取
     * @param exitCode 检测命令的退出码字符串，不为 {@code null}
     */
    default void rootResult(boolean hasRoot, @NonNull String exitCode) {
    }

    /**
     * Shell 管道破裂的回调方法。
     * <p>
     * 当 Shell 进程的输入/输出流非正常终止时触发此回调，
     * 通常表示命令执行过程中发生了底层通信异常。
     *
     * @param reason 管道破裂的原因描述文本，不为 {@code null}
     * @param errors 管道破裂时附带的错误输出内容数组，不为 {@code null}
     */
    default void brokenPip(@NonNull String reason, @NonNull String[] errors) {
    }
}
