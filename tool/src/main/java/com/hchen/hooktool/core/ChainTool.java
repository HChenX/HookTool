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
import com.hchen.hooktool.data.ChainType;
import com.hchen.hooktool.exception.UnexpectedException;
import com.hchen.hooktool.hook.AbsHook;

import java.lang.reflect.Executable;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Function;

/**
 * 提供链式调用风格的 Hook 操作工具类。
 * <p>
 * 本类是整个链式 Hook 流程的入口。使用者首先通过 {@link #buildChain(String)}、
 * {@link #buildChain(String, ClassLoader)} 或 {@link #buildChain(Class)} 等静态工厂方法
 * 创建实例，然后借助 {@link #findMethod}、{@link #findAllMethod}、{@link #findConstructor}、
 * {@link #findAllConstructor} 或 {@link #withExecutable} 定位待 Hook 的方法或构造函数，
 * 最终通过返回的 {@link ChainHook} 对象指定具体的 Hook 策略（例如替换返回值、拦截调用、
 * 修改参数等）。
 * <p>
 * 每个 {@code ChainTool} 实例内部维护一个哈希集合，用于在运行阶段对已执行过的链式数据
 * 进行去重校验，从而避免对同一目标进行重复 Hook。
 *
 * @author 焕晨HChen
 */
public final class ChainTool {
    /**
     * 本次 Hook 操作所指向的目标类。
     */
    @NonNull
    private final Class<?> clazz;

    /**
     * 负责执行各类 Hook 策略的内部链式钩子对象。
     */
    @NonNull
    private final ChainHook chainHook;

    /**
     * 当前正在组装的链式数据实例，用于暂存本次链式调用过程中的全部配置信息。
     */
    private ChainData chainData;

    /**
     * 已成功执行的链式数据哈希集合，用于运行时去重，防止对相同目标重复 Hook。
     */
    private final HashSet<Integer> dataHashSet = new HashSet<>();

    /**
     * 以指定的目标类初始化链式工具实例。
     *
     * @param clazz 待 Hook 的目标类，不得为 {@code null}
     */
    private ChainTool(@NonNull Class<?> clazz) {
        Objects.requireNonNull(clazz, "Class must not be null.");

        this.clazz = clazz;
        this.chainHook = new ChainHook();
    }

    /**
     * 根据类的全限定名创建 {@link ChainTool} 实例。
     * <p>
     * 内部使用默认类加载器按全限定名查找并加载目标类。
     *
     * @param classPath 目标类的全限定名，例如 {@code "com.example.MyClass"}
     * @return 已绑定目标类的 {@link ChainTool} 实例
     */
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
    public static ChainTool buildChain(@NonNull String classPath, ClassLoader classLoader) {
        return new ChainTool(findClass(classPath, classLoader));
    }

    /**
     * 直接传入 {@link Class} 对象创建 {@link ChainTool} 实例。
     *
     * @param clazz 目标类的 {@link Class} 对象，不得为 {@code null}
     * @return 已绑定目标类的 {@link ChainTool} 实例
     */
    public static ChainTool buildChain(@NonNull Class<?> clazz) {
        return new ChainTool(clazz);
    }

    /**
     * 在目标类中按方法名和参数类型查找唯一匹配的方法。
     * <p>
     * 成功定位后会创建一条 {@link ChainData} 记录，并返回 {@link ChainHook} 对象供调用方
     * 进一步指定 Hook 策略。
     *
     * @param methodName     待查找的方法名称
     * @param parameterTypes 方法的形参类型列表，每个元素可以是 {@link Class} 对象或表示基本类型名称的字符串
     * @return {@link ChainHook} 实例，用于配置具体的 Hook 行为
     */
    public ChainHook findMethod(@NonNull String methodName, @NonNull Object... parameterTypes) {
        this.chainData = new ChainData(methodName, parameterTypes);
        return chainHook;
    }

    /**
     * 在目标类中按方法名查找所有同名方法（不区分参数类型）。
     * <p>
     * 所有匹配到的方法将统一执行相同的 Hook 操作。
     *
     * @param methodName 待查找的方法名称
     * @return {@link ChainHook} 实例，用于配置具体的 Hook 行为
     */
    public ChainHook findAllMethod(@NonNull String methodName) {
        this.chainData = new ChainData(methodName);
        return chainHook;
    }

    /**
     * 在目标类中按参数类型查找匹配的构造函数。
     *
     * @param parameterTypes 构造函数的形参类型列表，每个元素可以是 {@link Class} 对象或表示基本类型名称的字符串
     * @return {@link ChainHook} 实例，用于配置具体的 Hook 行为
     */
    public ChainHook findConstructor(@NonNull Object... parameterTypes) {
        this.chainData = new ChainData(parameterTypes);
        return chainHook;
    }

    /**
     * 在目标类中查找全部构造函数。
     * <p>
     * 所有被查找到的构造函数将统一执行相同的 Hook 操作。
     *
     * @return {@link ChainHook} 实例，用于配置具体的 Hook 行为
     */
    public ChainHook findAllConstructor() {
        this.chainData = new ChainData();
        return chainHook;
    }

    /**
     * 直接以一个已有的 {@link Executable} 对象作为 Hook 目标。
     * <p>
     * 此方法适用于调用方已通过反射等手段持有目标方法或构造函数引用的场景，
     * 无需再通过方法名或参数类型进行查找。
     *
     * @param executable 待 Hook 的可执行对象（{@link java.lang.reflect.Method} 或
     *                   {@link java.lang.reflect.Constructor}），不得为 {@code null}
     * @return {@link ChainHook} 实例，用于配置具体的 Hook 行为
     */
    public ChainHook withExecutable(@NonNull Executable executable) {
        this.chainData = new ChainData(executable);
        return chainHook;
    }

    /**
     * 执行完整的链式 Hook 流程：先查找目标方法或构造函数，随后对其应用 Hook。
     * <p>
     * 本方法内置去重机制——若检测到当前链式数据已在先前被处理过，将抛出
     * {@link UnexpectedException}。执行完毕后会自动将 {@code chainData} 置为 {@code null}，
     * 以避免被意外复用。
     *
     * @throws UnexpectedException 当检测到重复的链式数据时抛出
     */
    private void runChain() {
        Objects.requireNonNull(chainData);

        try {
            if (!dataHashSet.contains(chainData.hashCode())) {
                runFind();
                if (chainData.throwable != null) {
                    if (chainData.isIgnoreThrow) {
                        return;
                    }
                    if (chainData.function != null) {
                        if (Boolean.TRUE.equals(chainData.function.apply(chainData.throwable))) {
                            return;
                        } else {
                            CoreTool.throwIt(chainData.throwable);
                        }
                    } else {
                        CoreTool.throwIt(chainData.throwable);
                    }
                }

                dataHashSet.add(chainData.hashCode());
                for (Executable executable : chainData.executables) {
                    CoreTool.hook(executable, chainData.absHook);
                }
            } else {
                throw new UnexpectedException("Duplicate chain data: " + chainData);
            }
        } finally {
            chainData = null;
        }
    }

    /**
     * 根据当前 {@link ChainData} 中记录的 {@link ChainType} 类型执行相应的查找操作，
     * 并将查找到的结果填充到 {@code chainData.executables} 数组中。
     * <p>
     * 若查找过程中发生异常，异常对象会被暂存到 {@code chainData.throwable} 字段中，
     * 供后续 {@link #runChain()} 统一处理。
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
     * 链式钩子内部类，为已定位的方法或构造函数提供多种 Hook 策略配置。
     * <p>
     * 支持的 Hook 策略包括：
     * <ul>
     *   <li>{@link #hook(AbsHook)} — 使用自定义回调执行 Hook</li>
     *   <li>{@link #returnResult(Object)} — 替换方法的返回值</li>
     *   <li>{@link #doNothing()} — 完全拦截原方法使其不执行</li>
     *   <li>{@link #setArg(int, Object)} — 修改方法调用时的实参</li>
     * </ul>
     * 此外还提供异常处理相关的配置方法：
     * <ul>
     *   <li>{@link #onThrow(Function)} — 注册自定义异常处理函数</li>
     *   <li>{@link #ignoreThrow()} — 静默忽略查找阶段的异常</li>
     * </ul>
     */
    public final class ChainHook {
        /**
         * 构造方法，仅供外部类 {@link ChainTool} 创建实例。
         */
        private ChainHook() {
        }

        /**
         * 使用指定的 {@link AbsHook} 回调对目标方法或构造函数执行 Hook。
         * <p>
         * 本方法将触发完整的链式执行流程（查找 + Hook），随后返回外部 {@link ChainTool} 实例，
         * 以便调用方继续对其他方法发起新的链式调用。
         *
         * @param absHook 自定义的 Hook 回调实现，不得为 {@code null}
         * @return 外部 {@link ChainTool} 实例，便于继续链式调用
         */
        public ChainTool hook(@NonNull AbsHook absHook) {
            chainData.absHook = absHook;
            runChain();
            return ChainTool.this;
        }

        /**
         * Hook 目标方法并将其返回值替换为指定的结果。
         * <p>
         * 原方法体会照常执行，但调用方最终收到的返回值会被替换为 {@code result}。
         *
         * @param result 要返回给调用方的结果对象，允许为 {@code null}
         * @return 外部 {@link ChainTool} 实例，便于继续链式调用
         */
        public ChainTool returnResult(final Object result) {
            return hook(CoreTool.returnResult(result));
        }

        /**
         * Hook 目标方法并完全拦截其执行（方法体将不会被执行）。
         * <p>
         * 对于返回类型为 {@code void} 的方法，调用方将正常返回；
         * 对于具有返回值的方法，调用方将收到 {@code null}。
         *
         * @return 外部 {@link ChainTool} 实例，便于继续链式调用
         */
        public ChainTool doNothing() {
            return hook(CoreTool.doNothing());
        }

        /**
         * Hook 目标方法并修改其指定位置的实参值。
         *
         * @param index 参数的索引位置（从 0 开始计数）
         * @param value 要替换成的新参数值，允许为 {@code null}
         * @return 外部 {@link ChainTool} 实例，便于继续链式调用
         */
        public ChainTool setArg(int index, Object value) {
            return hook(CoreTool.setArg(index, value));
        }

        /**
         * 注册一个异常处理函数，在目标方法或构造函数查找失败时被调用。
         * <p>
         * 该函数接收捕获到的 {@link Throwable} 对象：返回 {@code true} 表示异常已被妥善处理，
         * 后续流程将正常退出；返回 {@code false} 则表示异常未被处理，将继续向上抛出。
         *
         * @param function 异常处理函数，不得为 {@code null}
         * @return 当前 {@link ChainHook} 实例，便于继续链式配置
         */
        public ChainHook onThrow(@NonNull Function<Throwable, Boolean> function) {
            chainData.function = function;
            return this;
        }

        /**
         * 指定在查找阶段发生异常时静默忽略，不做任何额外处理。
         * <p>
         * 启用后，若目标方法或构造函数查找失败，将直接跳过本次 Hook 操作，
         * 不会向调用方抛出任何异常。
         *
         * @return 当前 {@link ChainHook} 实例，便于继续链式配置
         */
        public ChainHook ignoreThrow() {
            chainData.isIgnoreThrow = true;
            return this;
        }
    }
}
