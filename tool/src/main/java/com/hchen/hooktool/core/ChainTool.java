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
package com.hchen.hooktool.core;

import static com.hchen.hooktool.core.CoreTool.findClass;

import androidx.annotation.NonNull;

import com.hchen.hooktool.data.ChainData;
import com.hchen.hooktool.exception.UnexpectedException;
import com.hchen.hooktool.hook.AbsHook;
import com.hchen.hooktool.log.LogExpand;
import com.hchen.hooktool.log.XposedLog;

import java.lang.reflect.Executable;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Function;

/**
 * 链式工具
 *
 * @author 焕晨HChen
 */
public class ChainTool {
    /**
     * 目标类
     */
    @NonNull
    private final Class<?> clazz;
    
    /**
     * 链式钩子对象
     */
    @NonNull
    private final ChainHook chainHook;
    
    /**
     * 链式数据对象
     */
    private ChainData chainData;
    
    /**
     * 链式数据集合，用于去重
     */
    private final HashSet<ChainData> chainDataSet = new HashSet<>();

    /**
     * 构造方法
     *
     * @param clazz 目标类
     */
    private ChainTool(@NonNull Class<?> clazz) {
        Objects.requireNonNull(clazz, "Class must not be null.");
        this.clazz = clazz;
        chainHook = new ChainHook();
    }

    /**
     * 构建链式工具实例
     *
     * @param classPath 类路径
     * @return 链式工具实例
     */
    public static ChainTool buildChain(@NonNull String classPath) {
        return new ChainTool(findClass(classPath));
    }

    /**
     * 构建链式工具实例
     *
     * @param classPath 类路径
     * @param classLoader 类加载器
     * @return 链式工具实例
     */
    public static ChainTool buildChain(@NonNull String classPath, ClassLoader classLoader) {
        return new ChainTool(findClass(classPath, classLoader));
    }

    /**
     * 构建链式工具实例
     *
     * @param clazz 目标类
     * @return 链式工具实例
     */
    public static ChainTool buildChain(@NonNull Class<?> clazz) {
        return new ChainTool(clazz);
    }

    /**
     * 查找指定方法
     *
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     * @return 链式钩子对象
     */
    public ChainHook findMethod(@NonNull String methodName, @NonNull Object... parameterTypes) {
        chainData = new ChainData(methodName, parameterTypes);
        return chainHook;
    }

    /**
     * 查找所有指定名称的方法
     *
     * @param methodName 方法名
     * @return 链式钩子对象
     */
    public ChainHook findAllMethod(@NonNull String methodName) {
        chainData = new ChainData(methodName);
        return chainHook;
    }

    /**
     * 查找指定构造函数
     *
     * @param parameterTypes 参数类型
     * @return 链式钩子对象
     */
    public ChainHook findConstructor(@NonNull Object... parameterTypes) {
        chainData = new ChainData(parameterTypes);
        return chainHook;
    }

    /**
     * 查找所有构造函数
     *
     * @return 链式钩子对象
     */
    public ChainHook findAllConstructor() {
        chainData = new ChainData();
        return chainHook;
    }

    /**
     * 使用指定的可执行对象
     *
     * @param executable 可执行对象
     * @return 链式钩子对象
     */
    public ChainHook withExecutable(@NonNull Executable executable) {
        chainData = new ChainData(executable);
        return chainHook;
    }

    /**
     * 执行链式调用
     */
    private void runChain() {
        Objects.requireNonNull(chainData);

        try {
            if (!chainDataSet.contains(chainData)) {
                runFind();
                if (chainData.throwable != null) {
                    if (chainData.isIgnoreThrow) {
                        return;
                    }
                    if (chainData.function != null) {
                        if (Boolean.TRUE.equals(chainData.function.apply(chainData.throwable))) {
                            return;
                        } else {
                            throw new UnexpectedException(chainData.throwable);
                        }
                    } else {
                        throw new UnexpectedException(chainData.throwable);
                    }
                }

                final ChainData tempChainData = chainData;
                chainDataSet.add(tempChainData);
                for (Executable executable : chainData.executables) {
                    CoreTool.hook(executable, chainData.absHook);
                }
            } else {
                XposedLog.logW(LogExpand.getTag(), "Duplicate content will be skipped: " + chainData);
            }
        } finally {
            chainData = null;
        }
    }

    /**
     * 执行查找操作
     */
    private void runFind() {
        try {
            switch (chainData.chainType) {
                case EXECUTABLE -> {
                    chainData.executables[0] = chainData.executable;
                }
                case FIND_METHOD -> {
                    chainData.executables[0] = CoreTool.findMethod(clazz, chainData.methodName, chainData.parameterTypes);
                }
                case FIND_ALL_METHOD -> {
                    chainData.executables = CoreTool.findAllMethod(clazz, chainData.methodName);
                }
                case FIND_CONSTRUCTOR -> {
                    chainData.executables[0] = CoreTool.findConstructor(clazz, chainData.parameterTypes);
                }
                case FIND_ALL_CONSTRUCTOR -> {
                    chainData.executables = CoreTool.findAllConstructor(clazz);
                }
            }
        } catch (Throwable throwable) {
            chainData.throwable = throwable;
        }
    }

    /**
     * 链式钩子类，用于执行钩子操作
     */
    public class ChainHook {
        /**
         * 构造方法
         */
        private ChainHook() {
        }

        /**
         * 执行钩子操作
         *
         * @param absHook 钩子对象
         * @return 链式工具实例
         */
        public ChainTool hook(@NonNull AbsHook absHook) {
            chainData.absHook = absHook;
            runChain();
            return ChainTool.this;
        }

        /**
         * 执行钩子操作并返回指定结果
         *
         * @param result 要返回的结果
         * @return 链式工具实例
         */
        public ChainTool returnResult(final Object result) {
            return hook(CoreTool.returnResult(result));
        }

        /**
         * 执行钩子操作并拦截方法执行
         *
         * @return 链式工具实例
         */
        public ChainTool doNothing() {
            return hook(CoreTool.doNothing());
        }

        /**
         * 执行钩子操作并修改指定参数
         *
         * @param index 参数索引
         * @param value 新的参数值
         * @return 链式工具实例
         */
        public ChainTool setArg(int index, Object value) {
            return hook(CoreTool.setArg(index, value));
        }

        /**
         * 设置异常处理函数
         *
         * @param function 异常处理函数
         * @return 链式钩子对象
         */
        public ChainHook onThrow(@NonNull Function<Throwable, Boolean> function) {
            chainData.function = function;
            return this;
        }

        /**
         * 设置忽略异常
         *
         * @return 链式钩子对象
         */
        public ChainHook ignoreThrow() {
            chainData.isIgnoreThrow = true;
            return this;
        }
    }
}
