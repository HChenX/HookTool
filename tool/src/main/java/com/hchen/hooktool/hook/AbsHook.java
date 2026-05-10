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

import static io.github.libxposed.api.XposedInterface.PRIORITY_DEFAULT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.log.LogExpand;

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.libxposed.api.XposedInterface;

/**
 * AbsHook
 *
 * @author 焕晨HChen
 */
public abstract class AbsHook {
    int priority; // 钩子优先级

    private static class CallState {
        XposedInterface.Chain originalChain;
        final InnerChain innerChain;

        Object[] args;
        Object originalResult;
        Object replaceResult;
        Throwable throwable;
        boolean isArgsChanged = false;
        boolean isResultChanged = false;

        CallState() {
            this.innerChain = new InnerChain(this);
        }

        void reset(@NonNull XposedInterface.Chain chain) {
            this.originalChain = chain;
            this.args = null;
            this.originalResult = null;
            this.replaceResult = null;
            this.throwable = null;
            this.isArgsChanged = false;
            this.isResultChanged = false;
        }

        @Override
        @NonNull
        public String toString() {
            return "CallState{" +
                "originalChain=" + originalChain +
                ", innerChain=" + innerChain +
                ", args=" + Arrays.toString(args) +
                ", originalResult=" + originalResult +
                ", replaceResult=" + replaceResult +
                ", throwable=" + throwable +
                ", isArgsChanged=" + isArgsChanged +
                ", isResultChanged=" + isResultChanged +
                '}';
        }
    }

    private static class StateStack {
        private CallState[] states = new CallState[4];
        private int depth = -1;

        void push(@NonNull XposedInterface.Chain chain) {
            int newDepth = depth + 1;
            if (newDepth >= states.length) {
                states = Arrays.copyOf(states, states.length * 2);
            }
            if (states[newDepth] == null) {
                states[newDepth] = new CallState();
            }
            states[newDepth].reset(chain);
            depth = newDepth;
        }

        void pop() {
            if (depth >= 0 && depth < states.length) {
                CallState state = states[depth];
                state.originalChain = null;
                state.args = null;
                state.originalResult = null;
                state.replaceResult = null;
                state.throwable = null;
                state.isArgsChanged = false;
                state.isResultChanged = false;
                depth--;
            }
        }

        CallState current() {
            if (depth < 0 || depth >= states.length) return null;
            return states[depth];
        }
    }

    private final ThreadLocal<StateStack> stackLocal = new ThreadLocal<StateStack>() {
        @Override
        protected StateStack initialValue() {
            return new StateStack();
        }
    };
    private final CopyOnWriteArrayList<XposedInterface.HookHandle> handles = new CopyOnWriteArrayList<>();

    public enum StageEnum {
        BEFORE,
        PROCEED,
        AFTER
    }

    public AbsHook() {
        this(PRIORITY_DEFAULT);
    }

    public AbsHook(int priority) {
        this.priority = priority;
    }

    /**
     * 钩子执行前的回调方法
     * <p>
     * 在原始方法执行前被调用，可用于修改参数或执行前置逻辑
     */
    public void before() {
    }

    /**
     * 桥接新 API 的特殊方法
     * <p>
     * 重写此方法可以自由调用 proceed 呼叫原方法
     * <p>
     * 此方法的返回值将作为原方法的默认返回值使用，除非设置了 setResult
     */
    public Object proceed(@NonNull XposedInterface.Chain chain) throws Throwable {
        return callProceed();
    }

    /**
     * 钩子执行后的回调方法
     * 在原始方法执行后被调用，可用于修改返回值或执行后置逻辑
     */
    public void after() {
    }

    /**
     * 异常处理回调方法
     * <p>
     * 当钩子执行过程中发生异常时被调用
     *
     * @param stage 异常发生的阶段
     * @param e     发生的异常
     * @return 是否处理了异常
     */
    public boolean onThrow(@NonNull StageEnum stage, @NonNull Throwable e) {
        return false;
    }

    @NonNull
    private CallState getState() {
        StateStack stack = stackLocal.get();
        CallState state = stack != null ? stack.current() : null;
        if (state == null) {
            throw new IllegalStateException("Hook state has been lost or is not being called within the interception lifecycle.");
        }
        return state;
    }

    /**
     * 获取当前被钩住的可执行对象
     *
     * @return 当前被钩住的可执行对象（方法或构造函数）
     */
    @NonNull
    public final Executable getExecutable() {
        return getState().originalChain.getExecutable();
    }

    /**
     * 获取当前被钩住方法的 this 对象
     *
     * @return 当前被钩住方法的 this 对象
     */
    public final Object getThisObject() {
        return getState().originalChain.getThisObject();
    }

    /**
     * 获取当前被钩住方法的参数数组
     * <p>
     * 如果参数已被修改，则返回修改后的值
     *
     * @return 当前被钩住方法的参数数组
     */
    @NonNull
    public final Object[] getArgs() {
        CallState state = getState();
        if (state.args == null) {
            state.args = state.originalChain.getArgs().toArray(new Object[0]);
        }
        return state.args;
    }

    /**
     * 获取当前被钩住方法的指定索引参数
     * <p>
     * 如果参数已被修改，则返回修改后的值
     *
     * @param index 参数索引
     * @return 指定索引的参数值
     * @throws IndexOutOfBoundsException 当索引超出范围时抛出
     * @throws ClassCastException        当参数类型转换失败时抛出
     */
    public final Object getArg(int index) throws IndexOutOfBoundsException, ClassCastException {
        CallState state = getState();
        if (state.args == null) {
            state.args = state.originalChain.getArgs().toArray(new Object[0]);
        }
        return state.args[index];
    }

    /**
     * 设置当前被钩住方法的指定索引参数
     *
     * @param index 参数索引
     * @param value 新的参数值
     */
    public final void setArg(int index, Object value) {
        CallState state = getState();
        if (state.args == null) {
            state.args = state.originalChain.getArgs().toArray(new Object[0]);
        }
        state.args[index] = value;
        state.isArgsChanged = true;
    }

    /**
     * 设置当前被钩住方法的所有参数
     *
     * @param args 新的参数数组
     * @throws IndexOutOfBoundsException 当参数数量不匹配时抛出
     */
    public final void setArgs(@NonNull Object... args) {
        CallState state = getState();
        if (state.args == null) {
            state.args = state.originalChain.getArgs().toArray(new Object[0]);
        }
        if (state.args.length != args.length) {
            throw new IndexOutOfBoundsException("Parameter quantity mismatch. " +
                "Target length:" + state.args.length + ", Actual length: " + args.length);
        }
        state.args = args;
        state.isArgsChanged = true;
    }

    /**
     * 获取当前被钩住方法的返回值
     * <p>
     * 如果返回值已被修改，则返回修改后的值
     *
     * @return 当前被钩住方法的返回值
     */
    public final Object getResult() {
        CallState state = getState();
        return state.isResultChanged ? state.replaceResult : state.originalResult;
    }

    /**
     * 设置当前被钩住方法的返回值
     *
     * @param result 新的返回值
     */
    public final void setResult(Object result) {
        CallState state = getState();
        state.replaceResult = result;
        state.isResultChanged = true;
    }

    /**
     * 设置当前被钩住方法的异常
     *
     * @param throwable 要抛出的异常
     */
    public final void setThrowable(Throwable throwable) {
        getState().throwable = throwable;
    }

    /**
     * 获取当前被钩住方法的异常
     *
     * @return 当前被钩住方法的异常
     */
    @Nullable
    public final Throwable getThrowable() {
        return getState().throwable;
    }

    final void setHandle(@NonNull XposedInterface.HookHandle handle) {
        this.handles.add(handle);
    }

    // --- 生命周期管理 ---

    final void enter(@NonNull XposedInterface.Chain chain) {
        Objects.requireNonNull(stackLocal.get()).push(chain);
    }

    final void exit() {
        StateStack stack = stackLocal.get();
        if (stack != null) {
            stack.pop();
        }
    }

    @NonNull final XposedInterface.Chain getChain() {
        return getState().innerChain;
    }

    /**
     * 调用原始方法
     * <p>
     * 根据参数是否修改，决定使用修改后的参数还是原始参数调用原始方法
     *
     * @return 原始方法的返回值
     * @throws Throwable 执行过程中可能抛出的异常
     */
    final Object callProceed() throws Throwable {
        CallState state = getState();
        if (state.isArgsChanged && state.args != null) {
            return state.originalChain.proceed(state.args);
        } else {
            return state.originalChain.proceed();
        }
    }

    final void setOriginalResult(Object originalResult) {
        getState().originalResult = originalResult;
    }

    final boolean isResultChanged() {
        return getState().isResultChanged;
    }

    /**
     * 解除自身钩子
     * <p>
     * 移除当前钩子的所有拦截
     */
    final public void unHookSelf() {
        if (handles.isEmpty()) {
            throw new IllegalStateException("Hook handle is not initialized. Cannot unhook before the hook is applied.");
        }
        for (XposedInterface.HookHandle handle : handles) {
            handle.unhook();
        }
    }

    /**
     * 观察调用信息
     * <p>
     * 生成当前调用的详细信息字符串
     */
    @NonNull final public String observeCall() {
        return LogExpand.observeCall(this);
    }

    @Override
    @NonNull
    public String toString() {
        StateStack stack = stackLocal.get();
        CallState state = stack != null ? stack.current() : null;
        return "AbsHook{" +
            "handles=" + handles +
            ", callState=" + (state != null ? state.toString() : "null") +
            '}';
    }

    /**
     * 内部调用链实现
     * <p>
     * 用于包装原始调用链，处理参数和返回值的修改
     */
    @SuppressWarnings("ClassCanBeRecord")
    private static class InnerChain implements XposedInterface.Chain {
        private final CallState state;

        InnerChain(@NonNull CallState state) {
            this.state = state;
        }

        @NonNull
        @Override
        public Executable getExecutable() {
            return state.originalChain.getExecutable();
        }

        @Override
        public Object getThisObject() {
            return state.originalChain.getThisObject();
        }

        @NonNull
        @Override
        public List<Object> getArgs() {
            // 保持与旧 API 逻辑同步
            return state.isArgsChanged ? Arrays.asList(state.args) : state.originalChain.getArgs();
        }

        @Override
        public Object getArg(int index) throws IndexOutOfBoundsException, ClassCastException {
            // 保持与旧 API 逻辑同步
            return state.isArgsChanged ? state.args[index] : state.originalChain.getArg(index);
        }

        @Override
        public Object proceed() throws Throwable {
            return state.originalChain.proceed();
        }

        @Override
        public Object proceed(@NonNull Object[] args) throws Throwable {
            return state.originalChain.proceed(args);
        }

        @Override
        public Object proceedWith(@NonNull Object thisObject) throws Throwable {
            return state.originalChain.proceedWith(thisObject);
        }

        @Override
        public Object proceedWith(@NonNull Object thisObject, @NonNull Object[] args) throws Throwable {
            return state.originalChain.proceedWith(thisObject, args);
        }
    }
}
