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

public interface IExecListener {
    /**
     * 标准输出。
     *
     * @param command  输入的命令
     * @param outputs  执行后输出的内容
     * @param exitCode 退出码
     */
    default void output(String command, String[] outputs, String exitCode) {
    }

    /**
     * 标准错误。
     *
     * @param command  输入的命令
     * @param errors   报错内容
     * @param exitCode 退出码
     */
    default void error(String command, String[] errors, String exitCode) {
    }

    /**
     * 无 Root 时会执行的回调。
     */
    default void notRoot(String exitCode) {
    }

    /**
     * 管道破裂时的回调，代表 Shell 流非正常终止。
     *
     * @param command 输入的命令
     * @param errors  报错内容
     * @param reason  崩溃原因
     */
    default void brokenPip(String command, String[] errors, String reason) {
    }
}
