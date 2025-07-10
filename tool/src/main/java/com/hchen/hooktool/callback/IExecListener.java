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
package com.hchen.hooktool.callback;

import androidx.annotation.NonNull;

/**
 * Shell 执行监听
 *
 * @author 焕晨HChen
 */
public interface IExecListener {
    /**
     * 标准输出
     *
     * @param command  输入的命令
     * @param exitCode 退出码
     * @param outputs  执行后输出的内容
     */
    default void output(@NonNull String command, @NonNull String exitCode, @NonNull String[] outputs) {
    }

    /**
     * 标准错误
     *
     * @param command  输入的命令
     * @param exitCode 退出码
     * @param errors   报错内容
     */
    default void error(@NonNull String command, @NonNull String exitCode, @NonNull String[] errors) {
    }

    /**
     * 返回尝试 Root 的结果
     *
     * @param exitCode 退出码，非零码表尝试 Root 失败
     */
    default void rootResult(boolean hasRoot, @NonNull String exitCode) {
    }

    /**
     * 管道破裂时的回调，代表 Shell 流非正常终止
     *
     * @param reason 崩溃原因
     * @param errors 报错内容
     */
    default void brokenPip(@NonNull String reason, @NonNull String[] errors) {
    }
}
