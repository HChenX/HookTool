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

import androidx.annotation.NonNull;

import com.hchen.hooktool.hook.AbsHook;

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

/**
 * 链式数据
 *
 * @author 焕晨HChen
 */
public final class ChainData {
    // -------------------------- Data ------------------------------

    /**
     * 链式调用类型
     */
    public ChainType chainType;
    
    /**
     * 可执行对象数组
     */
    public Executable[] executables = new Executable[1];
    
    /**
     * 钩子对象
     */
    public AbsHook absHook;
    
    /**
     * 异常对象
     */
    public Throwable throwable;
    
    /**
     * 异常处理函数
     */
    public Function<Throwable, Boolean> function;
    
    /**
     * 是否忽略异常
     */
    public boolean isIgnoreThrow = false;

    // ---------------------------------------------------------------
    
    /**
     * 参数类型数组
     */
    public Object[] parameterTypes;
    
    /**
     * 单个可执行对象
     */
    public Executable executable;

    // -------------------------- Method ------------------------------

    /**
     * 方法名
     */
    public String methodName;

    /**
     * 构造方法，用于查找指定方法
     *
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     */
    public ChainData(@NonNull String methodName, @NonNull Object... parameterTypes) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.chainType = ChainType.FIND_METHOD;
    }

    /**
     * 构造方法，用于查找所有指定名称的方法
     *
     * @param methodName 方法名
     */
    public ChainData(@NonNull String methodName) {
        this.methodName = methodName;
        this.chainType = ChainType.FIND_ALL_METHOD;
    }

    // -------------------------- Constructor ------------------------------

    /**
     * 构造方法，用于查找指定构造函数
     *
     * @param parameterTypes 参数类型
     */
    public ChainData(@NonNull Object... parameterTypes) {
        this.parameterTypes = parameterTypes;
        this.chainType = ChainType.FIND_CONSTRUCTOR;
    }

    /**
     * 构造方法，用于查找所有构造函数
     */
    public ChainData() {
        this.chainType = ChainType.FIND_ALL_CONSTRUCTOR;
    }

    // ----------------------- Executable ----------------------------
    
    /**
     * 构造方法，用于指定可执行对象
     *
     * @param executable 可执行对象
     */
    public ChainData(@NonNull Executable executable) {
        this.executable = executable;
        this.chainType = ChainType.EXECUTABLE;
    }

    // ---------------------------------------------------------------

    @NonNull
    @Override
    public String toString() {
        return "ChainData{" +
            "chainType=" + chainType +
            ", executables=" + Arrays.toString(executables) +
            ", absHook=" + absHook +
            ", throwable=" + throwable +
            ", function=" + function +
            ", isIgnoreThrow=" + isIgnoreThrow +
            ", parameterTypes=" + Arrays.toString(parameterTypes) +
            ", executable=" + executable +
            ", methodName='" + methodName + '\'' +
            '}';
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof ChainData chainData)) return false;

        return chainType == chainData.chainType &&
            // isIgnoreThrow == chainData.isIgnoreThrow &&
            // Arrays.equals(executables, chainData.executables) &&
            // Objects.equals(absHook, chainData.absHook) &&
            // Objects.equals(throwable, chainData.throwable) &&
            // Objects.equals(function, chainData.function) &&
            Arrays.equals(parameterTypes, chainData.parameterTypes) &&
            Objects.equals(executable, chainData.executable) &&
            Objects.equals(methodName, chainData.methodName);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(chainType);
        // result = 31 * result + Arrays.hashCode(executables);
        // result = 31 * result + Objects.hashCode(absHook);
        // result = 31 * result + Objects.hashCode(throwable);
        // result = 31 * result + Objects.hashCode(function);
        // result = 31 * result + Boolean.hashCode(isIgnoreThrow);
        result = 31 * result + Arrays.hashCode(parameterTypes);
        result = 31 * result + Objects.hashCode(executable);
        result = 31 * result + Objects.hashCode(methodName);
        return result;
    }
}
