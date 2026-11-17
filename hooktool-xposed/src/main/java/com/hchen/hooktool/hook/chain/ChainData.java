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
package com.hchen.hooktool.hook.chain;

import androidx.annotation.NonNull;

import com.hchen.hooktool.hook.AbsHook;

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.function.Function;

/**
 * 链式 Hook 操作的数据载体，封装了一次链式调用所需的完整上下文信息。
 *
 * @author 焕晨HChen
 */
final class ChainData {
    // -------------------------- Data ------------------------------

    /**
     * 当前链式调用所采用的查找策略类型，运行时据此决定执行哪种方法查找逻辑。
     */
    ChainType chainType;
    /**
     * 查找阶段解析得到的目标可执行对象数组，初始大小为 1。当使用 {@code findAll} 系列查找方式时，该数组将被替换为所有匹配到的可执行对象。
     */
    Executable[] executables = new Executable[1];
    /**
     * 用户提供的 Hook 回调实现，在目标方法被调用时执行自定义拦截逻辑。
     */
    AbsHook absHook;
    /**
     * 查找阶段捕获的异常对象。若查找过程正常完成则为 {@code null}。
     */
    Throwable throwable;
    /**
     * 用户注册的异常处理函数，用于在查找失败时决定是否抑制异常或执行降级逻辑。
     */
    Function<Throwable, Boolean> function;
    /**
     * 标记是否在查找失败时静默忽略异常。默认为 {@code false}（即不忽略，会向上抛出）。
     */
    boolean isIgnoreThrow = false;

    // ---------------------------------------------------------------

    /**
     * 目标方法或构造函数的参数类型列表，用于查找阶段的签名匹配。
     */
    Object[] parameterTypes;
    /**
     * 用户直接提供的可执行对象引用，使用此字段时将跳过方法查找阶段。
     */
    Executable executable;

    // -------------------------- Method ------------------------------

    /**
     * 待查找的目标方法名称。
     */
    String methodName;

    /**
     * 构造用于"按方法名与参数类型精确查找单一方法"场景的数据实例。
     * <p>
     * 查找策略自动设为 {@link ChainType#FIND_METHOD}。
     *
     * @param methodName     目标方法的名称
     * @param parameterTypes 方法的参数类型列表
     */
    ChainData(@NonNull String methodName, @NonNull Object... parameterTypes) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.chainType = ChainType.FIND_METHOD;
    }

    /**
     * 构造用于"仅按方法名查找所有同名方法"场景的数据实例。
     * <p>
     * 查找策略自动设为 {@link ChainType#FIND_ALL_METHOD}。
     *
     * @param methodName 目标方法的名称
     */
    ChainData(@NonNull String methodName) {
        this.methodName = methodName;
        this.chainType = ChainType.FIND_ALL_METHOD;
    }

    // -------------------------- Constructor ------------------------------

    /**
     * 构造用于"按参数类型精确查找单一构造函数"场景的数据实例。
     * <p>
     * 查找策略自动设为 {@link ChainType#FIND_CONSTRUCTOR}。
     *
     * @param parameterTypes 构造函数的参数类型列表
     */
    ChainData(@NonNull Object... parameterTypes) {
        this.parameterTypes = parameterTypes;
        this.chainType = ChainType.FIND_CONSTRUCTOR;
    }

    /**
     * 构造用于"查找目标类中的全部构造函数"场景的数据实例。
     * <p>
     * 查找策略自动设为 {@link ChainType#FIND_ALL_CONSTRUCTOR}。
     */
    ChainData() {
        this.chainType = ChainType.FIND_ALL_CONSTRUCTOR;
    }

    // ----------------------- Executable ----------------------------

    /**
     * 构造用于"直接使用已有可执行对象"场景的数据实例，跳过方法查找阶段。
     * <p>
     * 查找策略自动设为 {@link ChainType#EXECUTABLE}。
     *
     * @param executable 已获取的目标方法或构造函数引用
     */
    ChainData(@NonNull Executable executable) {
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

}