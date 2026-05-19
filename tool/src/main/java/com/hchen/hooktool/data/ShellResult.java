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

import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * Shell 命令执行结果的不可变数据记录。
 * <p>
 * 该记录封装了一次 Shell 命令执行的完整输出信息，包括所执行的命令字符串、
 * 进程退出码、标准输出（stdout）内容以及标准错误（stderr）内容，
 * 方便调用方对命令执行结果进行全方位的检查与分析。
 *
 * @param command  实际执行的完整命令字符串
 * @param exitCode 命令进程的退出码字符串（{@code "0"} 通常表示执行成功）
 * @param outputs  标准输出内容按行分割后所得的字符串数组
 * @param errors   标准错误输出内容按行分割后所得的字符串数组
 * @author 焕晨HChen
 * @noinspection DeconstructionCanBeUsed
 */
public record ShellResult(@NonNull String command, @NonNull String exitCode,
                          @NonNull String[] outputs, @NonNull String[] errors) {
    /**
     * 判断该命令是否执行成功。
     * <p>
     * 通过检查退出码是否等于 {@code "0"} 来确定执行是否成功。
     *
     * @return 当退出码为 {@code "0"} 时返回 {@code true}，否则返回 {@code false}
     */
    public boolean isSuccess() {
        return TextUtils.equals("0", exitCode);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ShellResult that)) return false;
        return Objects.equals(command, that.command) &&
            Objects.equals(exitCode, that.exitCode) &&
            Arrays.deepEquals(errors, that.errors) &&
            Arrays.deepEquals(outputs, that.outputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command, exitCode, Arrays.hashCode(outputs), Arrays.hashCode(errors));
    }

    @NonNull
    @Override
    public String toString() {
        return "ShellResult{" +
            "command='" + command + '\'' +
            ", exitCode='" + exitCode + '\'' +
            ", outputs=" + Arrays.toString(outputs) +
            ", errors=" + Arrays.toString(errors) +
            '}';
    }
}
