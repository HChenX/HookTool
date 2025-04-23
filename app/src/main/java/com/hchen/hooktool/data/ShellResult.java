package com.hchen.hooktool.data;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Objects;

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
