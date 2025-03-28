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
 * Shell 工具返回的数据
 *
 * @author 焕晨HChen
 */
public record ShellResult(String command, String[] outputs, String exitCode) {
    @NonNull
    @Override
    public String toString() {
        return "ShellResult[command=" + command + ", outputs=" + Arrays.toString(outputs) + ", exitCode=" + exitCode + "]";
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ShellResult that)) return false;
        return Objects.equals(command, that.command) && Objects.equals(exitCode, that.exitCode) && Objects.deepEquals(outputs, that.outputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command, Arrays.hashCode(outputs), exitCode);
    }

    /**
     * 是否成功执行。
     */
    public boolean isSuccess() {
        return "0".equals(exitCode);
    }
}
