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
 * Copyright (C) 2023–2025 HChenX
 */
package com.hchen.hooktool.data;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * Shell 返回数据
 *
 * @author 焕晨HChen
 * @noinspection DeconstructionCanBeUsed
 */
public record ShellResult(@NonNull String command, @NonNull String exitCode,
                          @NonNull String[] outputs, @NonNull String[] errors) {
    /**
     * 是否成功执行
     */
    public boolean isSuccess() {
        return "0".equals(exitCode);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ShellResult that)) return false;
        return Objects.equals(command, that.command) &&
            Objects.equals(exitCode, that.exitCode) &&
            Objects.deepEquals(errors, that.errors) &&
            Objects.deepEquals(outputs, that.outputs);
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
