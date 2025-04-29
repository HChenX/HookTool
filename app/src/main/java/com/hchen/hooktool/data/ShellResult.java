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
package com.hchen.hooktool.data;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * Shell 返回数据
 *
 * @author 焕晨HChen
 */
public record ShellResult(String command, String[] outputs, String exitCode) {
    /**
     * 是否成功执行
     */
    public boolean isSuccess() {
        return "0".equals(exitCode);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ShellResult(String command1, String[] outputs1, String code)))
            return false;

        return Objects.equals(command, command1) &&
            Objects.equals(exitCode, code) &&
            Objects.deepEquals(outputs, outputs1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command, Arrays.hashCode(outputs), exitCode);
    }

    @Override
    @NonNull
    public String toString() {
        return "ShellResult{" +
            "command='" + command + '\'' +
            ", outputs=" + Arrays.toString(outputs) +
            ", exitCode='" + exitCode + '\'' +
            '}';
    }
}
