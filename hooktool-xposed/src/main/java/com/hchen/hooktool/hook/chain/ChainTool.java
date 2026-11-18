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

import static com.hchen.hooktool.reflect.Reflect.findClass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.hook.AbsHook;
import com.hchen.hooktool.hook.Hook;
import com.hchen.hooktool.reflect.Reflect;
import com.hchen.hooktool.reflect.Throwables;

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Function;

/**
 * 提供链式调用风格的 Hook 操作工具类。
 *
 * @author 焕晨HChen
 */
public final class ChainTool {
    /**
     * 本次 Hook 操作所指向的目标类。
     */
    @NonNull
    private final Class<?> mClazz;

    /**
     * 负责执行各类 Hook 策略的内部链式钩子对象。
     */
    @NonNull
    private final ChainHook mChainHook;

    /**
     * 当前正在组装的链式数据实例。
     */
    private ChainData mChainData;

    /**
     * 已成功执行的链式数据标识集合，用于运行时去重，防止对相同目标重复 Hook。
     */
    private final HashSet<String> mExecutedKeys = new HashSet<>();

    private ChainTool(@NonNull Class<?> clazz) {
        Objects.requireNonNull(clazz, "Class must not be null.");

        this.mClazz = clazz;
        this.mChainHook = new ChainHook();
    }

    /**
     * 根据类的全限定名创建 {@link ChainTool} 实例。
     * <p>
     * 内部使用默认类加载器按全限定名查找并加载目标类。
     *
     * @param classPath 目标类的全限定名，例如 {@code "com.example.MyClass"}
     * @return 已绑定目标类的 {@link ChainTool} 实例
     */
    @NonNull
    public static ChainTool buildChain(@NonNull String classPath) {
        return new ChainTool(findClass(classPath));
    }

    /**
     * 根据类的全限定名及指定的类加载器创建 {@link ChainTool} 实例。
     *
     * @param classPath   目标类的全限定名
     * @param classLoader 用于加载目标类的类加载器，传入 {@code null} 时将使用默认行为
     * @return 已绑定目标类的 {@link ChainTool} 实例
     */
    @NonNull
    public static ChainTool buildChain(@NonNull String classPath, @Nullable ClassLoader classLoader) {
        return new ChainTool(findClass(classPath, classLoader));
    }

    /**
     * 直接传入 {@link Class} 对象创建 {@link ChainTool} 实例。
     *
     * @param clazz 目标类的 {@link Class} 对象
     * @return 已绑定目标类的 {@link ChainTool} 实例
     */
    @NonNull
    public static ChainTool buildChain(@NonNull Class<?> clazz) {
        return new ChainTool(clazz);
    }

    /**
     * 在目标类中按方法名和参数类型查找唯一匹配的方法。
     *
     * @param methodName     待查找的方法名称
     * @param parameterTypes 方法的形参类型列表，每个元素可以是 {@link Class} 对象或表示基本类型名称的字符串
     * @return {@link ChainHook} 实例，用于配置具体的 Hook 行为
     */
    @NonNull
    public ChainHook findMethod(@NonNull String methodName, @NonNull Object... parameterTypes) {
        this.mChainData = new ChainData(methodName, parameterTypes);
        return mChainHook;
    }

    /**
     * 在目标类中按方法名查找所有同名方法（不区分参数类型）。
     * <p>
     * 所有匹配到的方法将统一执行相同的 Hook 操作。
     *
     * @param methodName 待查找的方法名称
     * @return {@link ChainHook} 实例，用于配置具体的 Hook 行为
     */
    @NonNull
    public ChainHook findAllMethod(@NonNull String methodName) {
        this.mChainData = new ChainData(methodName);
        return mChainHook;
    }

    /**
     * 在目标类中按参数类型查找匹配的构造函数。
     *
     * @param parameterTypes 构造函数的形参类型列表，每个元素可以是 {@link Class} 对象或表示基本类型名称的字符串
     * @return {@link ChainHook} 实例，用于配置具体的 Hook 行为
     */
    @NonNull
    public ChainHook findConstructor(@NonNull Object... parameterTypes) {
        this.mChainData = new ChainData(parameterTypes);
        return mChainHook;
    }

    /**
     * 在目标类中查找全部构造函数。
     * <p>
     * 所有被查找到的构造函数将统一执行相同的 Hook 操作。
     *
     * @return {@link ChainHook} 实例，用于配置具体的 Hook 行为
     */
    @NonNull
    public ChainHook findAllConstructor() {
        this.mChainData = new ChainData();
        return mChainHook;
    }

    /**
     * 直接以一个已有的 {@link Executable} 对象作为 Hook 目标。
     * <p>
     * 此方法适用于调用方已通过反射等手段持有目标方法或构造函数引用的场景，无需再通过方法名或参数类型进行查找。
     *
     * @param executable 待 Hook 的可执行对象（{@link java.lang.reflect.Method} 或 {@link java.lang.reflect.Constructor}）
     * @return {@link ChainHook} 实例，用于配置具体的 Hook 行为
     */
    @NonNull
    public ChainHook withExecutable(@NonNull Executable executable) {
        Objects.requireNonNull(executable, "Executable must not be null.");
        this.mChainData = new ChainData(executable);
        return mChainHook;
    }

    /**
     * 获取当前的链式数据，拒绝复用已执行过的 {@link ChainHook}。
     *
     * @return 当前链式数据
     * @throws IllegalStateException 当链式数据已被执行并清空时抛出
     */
    @NonNull
    private ChainData requireChainData() {
        if (mChainData == null) {
            throw new IllegalStateException("chainData is null; do not reuse ChainHook after hook().");
        }
        return mChainData;
    }

    /**
     * 执行完整的链式 Hook 流程：先查找目标方法或构造函数，随后对其应用 Hook。
     *
     * @throws IllegalStateException 当检测到重复的链式数据时抛出
     */
    private void runChain() {
        try {
            String chainKey = chainKey(mChainData);
            if (mExecutedKeys.contains(chainKey)) {
                throw new IllegalStateException("Duplicate chain data: " + mChainData);
            }
            runFind();
            if (mChainData.throwable != null) {
                if (mChainData.isIgnoreThrow) {
                    return;
                }
                if (mChainData.function != null) {
                    if (Boolean.TRUE.equals(mChainData.function.apply(mChainData.throwable))) {
                        return;
                    } else {
                        Throwables.throwIt(mChainData.throwable);
                    }
                } else {
                    Throwables.throwIt(mChainData.throwable);
                }
            }

            if (mChainData.executables == null || mChainData.executables.length == 0) {
                return;
            }
            for (Executable executable : mChainData.executables) {
                Hook.hook(executable, mChainData.absHook);
            }
            mExecutedKeys.add(chainKey);
        } finally {
            mChainData = null;
        }
    }

    /**
     * 生成链式数据的稳定去重键。
     *
     * @param data 待处理的链式数据
     * @return 稳定的去重键字符串
     */
    @NonNull
    private static String chainKey(@NonNull ChainData data) {
        if (data.chainType == ChainType.EXECUTABLE && data.executable != null) {
            return data.chainType + "#" + data.executable.toGenericString();
        }
        String params = Arrays.toString(data.parameterTypes);
        return data.chainType + "#" + data.methodName + "#" + params;
    }

    /**
     * 根据当前 {@link ChainData} 中记录的 {@link ChainType} 类型执行相应的查找操作，并将查找到的结果填充到 {@code mChainData.executables} 数组中。
     */
    private void runFind() {
        try {
            switch (mChainData.chainType) {
                case EXECUTABLE -> {
                    mChainData.executables[0] = mChainData.executable;
                }
                case FIND_METHOD -> {
                    mChainData.executables[0] = Reflect.findMethodExact(mClazz, mChainData.methodName, mChainData.parameterTypes);
                }
                case FIND_ALL_METHOD -> {
                    mChainData.executables = Reflect.findAllMethod(mClazz, mChainData.methodName);
                }
                case FIND_CONSTRUCTOR -> {
                    mChainData.executables[0] = Reflect.findConstructorExact(mClazz, mChainData.parameterTypes);
                }
                case FIND_ALL_CONSTRUCTOR -> {
                    mChainData.executables = Reflect.findAllConstructor(mClazz);
                }
            }
        } catch (Throwable throwable) {
            mChainData.throwable = throwable;
        }
    }

    /**
     * 链式钩子内部类，为已定位的方法或构造函数提供多种 Hook 策略配置。
     *
     * @author 焕晨HChen
     */
    public final class ChainHook {
        private ChainHook() {
        }

        /**
         * 使用指定的 {@link AbsHook} 回调对目标方法或构造函数执行 Hook。
         *
         * @param absHook 自定义的 Hook 回调实现
         * @return 外部 {@link ChainTool} 实例，便于继续链式调用
         */
        @NonNull
        public ChainTool hook(@NonNull AbsHook absHook) {
            requireChainData().absHook = absHook;
            runChain();
            return ChainTool.this;
        }

        /**
         * Hook 目标方法并直接返回指定结果。
         * <p>
         * 原方法体与 {@code after} 阶段都不会执行。
         *
         * @param result 要返回给调用方的结果对象，允许为 {@code null}
         * @return 外部 {@link ChainTool} 实例，便于继续链式调用
         */
        @NonNull
        public ChainTool returnResult(@Nullable final Object result) {
            return hook(Hook.returnResult(result));
        }

        /**
         * Hook 目标方法并完全拦截其执行（方法体将不会被执行）。
         * <p>
         * 对于返回类型为 {@code void} 的方法，调用方将正常返回；对于具有返回值的方法，调用方将收到 {@code null}。
         *
         * @return 外部 {@link ChainTool} 实例，便于继续链式调用
         */
        @NonNull
        public ChainTool doNothing() {
            return hook(Hook.doNothing());
        }

        /**
         * Hook 目标方法并修改其指定位置的实参值。
         *
         * @param index 参数的索引位置（从 0 开始计数）
         * @param value 要替换成的新参数值，允许为 {@code null}
         * @return 外部 {@link ChainTool} 实例，便于继续链式调用
         */
        @NonNull
        public ChainTool setArg(int index, @Nullable Object value) {
            return hook(Hook.setArg(index, value));
        }

        /**
         * 注册一个异常处理函数，在目标方法或构造函数查找失败时被调用。
         * <p>
         * 该函数接收捕获到的 {@link Throwable} 对象：返回 {@code true} 表示异常已被妥善处理，后续流程将正常退出；返回 {@code false} 则表示异常未被处理，将继续向上抛出。
         *
         * @param function 异常处理函数
         * @return 当前 {@link ChainHook} 实例，便于继续链式配置
         */
        @NonNull
        public ChainHook onThrow(@NonNull Function<Throwable, Boolean> function) {
            requireChainData().function = function;
            return this;
        }

        /**
         * 指定在查找阶段发生异常时静默忽略，不做任何额外处理。
         *
         * @return 当前 {@link ChainHook} 实例，便于继续链式配置
         */
        @NonNull
        public ChainHook ignoreThrow() {
            requireChainData().isIgnoreThrow = true;
            return this;
        }
    }
}