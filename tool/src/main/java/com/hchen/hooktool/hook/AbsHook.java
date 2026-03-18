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
package com.hchen.hooktool.hook;

import androidx.annotation.NonNull;

import com.hchen.hooktool.log.LogExpand;

import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;

import io.github.libxposed.api.XposedInterface;

/**
 * AbsHook
 *
 * @author 焕晨HChen
 */
public abstract class AbsHook {
    private XposedInterface.HookHandle handle;
    private XposedInterface.Chain chain;
    private Object[] args;
    private Object oriResult;
    private Object newResult;
    private Object replaceObject;
    private StageEnum currentStage;
    private boolean isProceedCalled = false;
    private boolean isArgsChanged = false;
    private boolean isResultChanged = false;

    public enum StageEnum {
        BEFORE,
        PROCEED,
        AFTER
    }

    /**
     * 钩子执行前的回调方法
     * <p>
     * 子类可以重写此方法来在目标方法执行前进行处理
     */
    public void before() {
    }

    /**
     * 钩子执行后的回调方法
     * <p>
     * 子类可以重写此方法来在目标方法执行后进行处理
     */
    public void after() {
    }

    /**
     * 异常处理回调方法
     * <p>
     * 子类可以重写此方法来处理钩子执行过程中的异常
     *
     * @param stage 异常发生的阶段
     * @param e     发生的异常
     * @return 是否已经处理了该异常，返回 true 表示已处理，false 表示未处理
     */
    public boolean onThrow(@NonNull StageEnum stage, @NonNull Throwable e) {
        return false;
    }

    /**
     * 获取被钩子的可执行对象（方法或构造函数）
     *
     * @return 被钩子的可执行对象
     */
    @NonNull
    public final Executable getExecutable() {
        Objects.requireNonNull(chain);
        return chain.getExecutable();
    }

    /**
     * 获取被钩子方法的 this 对象
     * <p>
     * 对于静态方法，返回 null
     *
     * @return this 对象
     */
    public final Object getThisObject() {
        Objects.requireNonNull(chain);
        return chain.getThisObject();
    }

    /**
     * 获取被钩子方法的参数列表
     * <p>
     * 为保持与旧 API 一致，将返回 Array 数组
     * <p>
     * 提醒：修改此数组内容无效，请使用 setArg 或 setArgs
     *
     * @return 参数列表
     */
    @NonNull
    public final Object[] getArgs() {
        Objects.requireNonNull(chain);
        return chain.getArgs().toArray(new Object[0]);
    }

    /**
     * 获取指定索引的参数
     *
     * @param index 参数索引
     * @return 参数值
     * @throws IndexOutOfBoundsException 如果索引超出范围
     * @throws ClassCastException        如果参数类型转换失败
     */
    public final Object getArg(int index) throws IndexOutOfBoundsException, ClassCastException {
        Objects.requireNonNull(chain);
        return chain.getArg(index);
    }

    /**
     * 设置指定索引的参数值
     *
     * @param index 参数索引
     * @param value 新的参数值
     */
    public final void setArg(int index, Object value) {
        Objects.requireNonNull(chain);
        if (args == null) {
            args = getArgs();
        }

        args[index] = value;
        isArgsChanged = true;
    }

    /**
     * 设置所有参数
     *
     * @param args 新的参数数组
     */
    public final void setArgs(Object... args) {
        Objects.requireNonNull(chain);
        this.args = args;
        isArgsChanged = true;
    }

    /**
     * 获取方法执行结果
     * <p>
     * 如果通过 setResult 设置了新结果，则返回新结果，否则返回原始结果
     * <p>
     * 在 Before 阶段始终返回 null，保持和旧 API 逻辑一致
     *
     * @return 方法执行结果
     */
    public final Object getResult() {
        Objects.requireNonNull(chain);

        if (currentStage == StageEnum.BEFORE) {
            return null;
        }

        if (isResultChanged) {
            return newResult;
        }

        return oriResult;
    }

    /**
     * 设置方法执行结果
     * <p>
     * 设置后 getResult 将返回此结果
     *
     * @param result 新的方法执行结果
     */
    public final void setResult(Object result) {
        Objects.requireNonNull(chain);
        newResult = result;
        isResultChanged = true;
    }

    /**
     * 设置替换对象
     * <p>
     * 用于在非静态方法中替换 this 对象
     *
     * @param replaceObject 替换的对象
     */
    public final void setReplaceObject(@NonNull Object replaceObject) {
        Objects.requireNonNull(chain);
        Objects.requireNonNull(replaceObject);
        this.replaceObject = replaceObject;
    }

    final void setHandle(@NonNull XposedInterface.HookHandle handle) {
        this.handle = handle;
    }

    final void setChain(@NonNull XposedInterface.Chain chain) {
        this.chain = chain;
    }

    /**
     * 调用原始方法
     * <p>
     * 此方法只能调用一次，多次调用将抛出异常
     * 如果已经通过 setResult 设置了结果，则此方法不会执行原始方法
     *
     * @throws Throwable 原始方法可能抛出的异常
     */
    final void callProceed() throws Throwable {
        Objects.requireNonNull(chain);
        if (isProceedCalled) {
            throw new RuntimeException("The 'proceed/proceedWith' method can only be called once.");
        }
        if (isResultChanged) {
            return;
        }

        isProceedCalled = true;
        if (Modifier.isStatic(getExecutable().getModifiers())) {
            if (isArgsChanged) {
                oriResult = chain.proceed(args);
            } else {
                oriResult = chain.proceed();
            }
        } else {
            if (isArgsChanged) {
                if (replaceObject != null) {
                    oriResult = chain.proceedWith(replaceObject, args);
                } else {
                    oriResult = chain.proceed(args);
                }
            } else {
                if (replaceObject != null) {
                    oriResult = chain.proceedWith(replaceObject);
                } else {
                    oriResult = chain.proceed();
                }
            }
        }
    }

    final void setCurrentStage(@NonNull StageEnum currentStage) {
        this.currentStage = currentStage;
    }

    /**
     * 重置钩子状态
     * <p>
     * 清除所有状态变量，准备下一次钩子执行
     */
    final void reset() {
        args = null;
        oriResult = null;
        newResult = null;
        replaceObject = null;
        isProceedCalled = false;
        isResultChanged = false;
        isArgsChanged = false;
        currentStage = null;
    }

    /**
     * 解除自身的钩子
     */
    final public void unHookSelf() {
        Objects.requireNonNull(handle);
        handle.unhook();
    }

    /**
     * 观察方法调用
     * <p>
     * 返回方法调用的详细信息
     *
     * @return 方法调用的详细信息
     */
    final public String observeCall() {
        return LogExpand.observeCall(this);
    }

    @NonNull
    @Override
    public String toString() {
        return "AbsHook{" +
            "handle=" + handle +
            ", chain=" + chain +
            ", args=" + Arrays.toString(args) +
            ", oriResult=" + oriResult +
            ", newResult=" + newResult +
            ", replaceObject=" + replaceObject +
            ", isProceedCalled=" + isProceedCalled +
            ", isArgsChanged=" + isArgsChanged +
            ", isResultChanged=" + isResultChanged +
            '}';
    }
}
