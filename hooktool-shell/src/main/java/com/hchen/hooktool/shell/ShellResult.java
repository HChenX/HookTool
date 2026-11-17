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
 * Shell 命令执行流程的最终结果类型。
 * <p>
 * 命令执行完毕为 {@link Completed}，流程未能收敛为 {@link Failed}。
 *
 * @author 焕晨HChen
 */
public sealed interface ShellResult {
    /**
     * 命令已正常执行完毕并产出数据的变体。
     *
     * @param result 单条命令的完整执行数据记录
     */
    record Completed(@NonNull CommandResult result) implements ShellResult {
        /**
         * 判断该命令自身是否执行成功。
         *
         * @return 命令退出码为 {@code "0"} 时返回 {@code true}，否则返回 {@code false}
         */
        public boolean isSuccess() {
            return result.isSuccess();
        }
    }

    /**
     * shell 执行流程未能正常完成的变体。
     *
     * @param reason 流程失败原因分类
     */
    record Failed(@NonNull ShellFailureReason reason) implements ShellResult {
    }
}