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
package com.hchen.hooktool.shell;

import androidx.annotation.NonNull;

/**
 * Shell 命令执行结果监听器接口。
 * <p>
 * 提供 Shell 命令执行完成后的多种回调方法，覆盖标准输出、标准错误输出、Root 权限检测结果以及管道异常等场景。所有回调方法均提供默认空实现，实现者可按需选择性覆写。
 *
 * @author 焕晨HChen
 */
public interface IExecListener {
    /**
     * 标准输出（stdout）的回调方法。
     * <p>
     * 当 Shell 命令执行后产生标准输出数据时触发此回调。输出内容以字符串数组形式传递，每个元素代表一行输出。
     * <p>
     * 注意：该回调对同步命令与异步命令均会触发（异步命令结果可通过 {@code Future} 或本回调获取）。
     *
     * @param command  已执行的 Shell 命令字符串
     * @param exitCode 命令的退出码字符串
     * @param outputs  标准输出内容数组，每个元素为一行输出
     */
    default void output(@NonNull String command, @NonNull String exitCode, @NonNull String[] outputs) {
    }

    /**
     * 标准错误输出（stderr）的回调方法。
     * <p>
     * 当 Shell 命令执行后产生错误输出数据时触发此回调。错误信息以字符串数组形式传递，每个元素代表一行错误信息。
     * <p>
     * 注意：该回调对同步命令与异步命令均会触发。
     *
     * @param command  已执行的 Shell 命令字符串
     * @param exitCode 命令的退出码字符串
     * @param errors   标准错误输出内容数组，每个元素为一行错误信息
     */
    default void error(@NonNull String command, @NonNull String exitCode, @NonNull String[] errors) {
    }

    /**
     * Root 权限检测结果的回调方法。
     * <p>
     * 当系统完成 Root 权限可用性检测后触发此回调，报告当前设备是否具备 Root 权限。
     *
     * @param hasRoot  {@code true} 表示设备已获取 Root 权限，{@code false} 表示未获取
     * @param exitCode 检测命令的退出码字符串
     */
    default void onRootResult(boolean hasRoot, @NonNull String exitCode) {
    }

    /**
     * Shell 管道破裂的回调方法。
     * <p>
     * 在以下两种场景触发一次：Shell 进程异常死亡（崩溃、被外部杀死、底层通信异常）；或进程仍存活但标准流已终止。
     *
     * @param reason 管道破裂的原因描述文本
     * @param errors 管道破裂时附带的错误输出内容数组
     */
    default void brokenPipe(@NonNull String reason, @NonNull String[] errors) {
    }
}