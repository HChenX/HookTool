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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.log.AndroidLog;
import com.hchen.hooktool.log.LogExpand;

import java.lang.ref.WeakReference;
import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.libxposed.api.XposedInterface;

/**
 * 所有 Xposed 方法钩子的抽象基类。
 *
 * @author 焕晨HChen
 */
public abstract class AbsHook {
    /**
     * 钩子优先级，数值越大越先执行，默认值为 {@link XposedInterface#PRIORITY_DEFAULT}。
     */
    int mPriority;
    /**
     * 钩子唯一标识符，为 {@code null} 表示不参与原子替换。
     */
    String mId;
    /**
     * 钩子异常处理模式，为 {@code null} 表示使用框架默认行为。
     */
    XposedInterface.ExceptionMode mMode;

    /**
     * 声明类的全限定类名，热重载时用作 {@code mThisObject} 的存储键。
     */
    String mKey;
    /**
     * 当前钩子是否绑定到静态上下文。
     */
    boolean mIsStatic;
    /**
     * 最近一次拦截的宿主对象，仅供热重载保存使用；并发或嵌套拦截时只保留最后一次写入。
     * <p>
     * 以弱引用持有，避免已销毁的宿主对象被钩子实例长期强引用。
     */
    volatile WeakReference<Object> mThisObject;

    private final ThreadLocal<StateStack> mStackLocal = new ThreadLocal<StateStack>() {
        @Override
        protected StateStack initialValue() {
            return new StateStack();
        }
    };
    private final CopyOnWriteArrayList<XposedInterface.HookHandle> mHandles = new CopyOnWriteArrayList<>();

    private static class CallState {
        XposedInterface.Chain originalChain;
        final InnerChain innerChain;

        Object[] args;
        Object originalResult;
        Object replaceResult;
        Throwable throwable;
        /**
         * Per-invocation data slot exposed to hook implementations.
         */
        Object userData;
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
            this.userData = null;
            this.isArgsChanged = false;
            this.isResultChanged = false;
        }

        @NonNull
        @Override
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
                state.userData = null;
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

    /**
     * 钩子拦截的生命周期阶段枚举。
     */
    public enum Stage {
        /**
         * 前置拦截阶段，即原方法被调用之前。
         */
        BEFORE,
        /**
         * 原方法执行阶段，正在通过 proceed 调用目标方法。
         */
        PROCEED,
        /**
         * 后置拦截阶段，即原方法调用完成之后。
         */
        AFTER
    }

    public AbsHook() {
        this(PRIORITY_DEFAULT, null, null);
    }

    public AbsHook(int priority) {
        this(priority, null, null);
    }

    public AbsHook(@Nullable String id) {
        this(PRIORITY_DEFAULT, id, null);
    }

    public AbsHook(@Nullable XposedInterface.ExceptionMode mode) {
        this(PRIORITY_DEFAULT, null, mode);
    }

    public AbsHook(int priority, @Nullable String id) {
        this(priority, id, null);
    }

    public AbsHook(int priority, @Nullable XposedInterface.ExceptionMode mode) {
        this(priority, null, mode);
    }

    public AbsHook(@Nullable String id, @Nullable XposedInterface.ExceptionMode mode) {
        this(PRIORITY_DEFAULT, id, mode);
    }

    /**
     * 完整参数的钩子实例构造器。
     *
     * @param priority 钩子优先级，数值越大越先执行
     * @param id       钩子唯一标识符，用于在相同可执行对象上原子替换旧 Hook；为 {@code null} 表示不关心后续替换
     * @param mode     钩子异常处理模式；为 {@code null} 表示使用框架默认行为
     */
    public AbsHook(int priority, @Nullable String id, @Nullable XposedInterface.ExceptionMode mode) {
        this.mPriority = priority;
        this.mId = id;
        this.mMode = mode;
        HookRegistry.register(this);
    }

    /**
     * 在原方法执行之前调用的前置拦截回调。
     * <p>
     * 若在此阶段通过 {@link #setResult(Object)} 设置了返回值，则原方法被完全跳过且不执行 {@link #after()}。
     */
    public void before() {
    }

    /**
     * 原方法调用阶段的回调，为子类提供定制 proceed 行为的能力。
     * <p>
     * 返回值会作为原方法的默认执行结果被记录，后续可通过 {@link #setResult(Object)} 覆盖。
     *
     * @param chain 当前调用链对象，可用于传递修改后的 {@code this} 引用或参数
     * @return 原方法的执行结果
     * @throws Throwable 原方法执行过程中可能抛出的任意异常
     */
    public Object proceed(@NonNull XposedInterface.Chain chain) throws Throwable {
        return callProceed();
    }

    /**
     * 在原方法执行完成之后调用的后置拦截回调。
     */
    public void after() {
    }

    /**
     * 钩子生命周期中发生异常时的统一回调。
     * <p>
     * 返回 {@code true} 表示异常已被消费，框架不再继续传播；返回 {@code false} 则由框架按默认策略处理。
     *
     * @param stage 异常发生的生命周期阶段
     * @param e     被捕获的异常对象
     * @return {@code true} 表示异常已被处理，{@code false} 表示交由框架继续处理
     */
    public boolean onThrow(@NonNull Stage stage, @NonNull Throwable e) {
        return false;
    }

    /**
     * 热重载准备回调，在当前模块即将被热重载时触发。
     * <p>
     * 若不同钩子实例的 state 中存在重复的键，合并过程会显式抛出 {@link IllegalStateException} 以提示冲突。
     *
     * @param extras 热重载的附加信息，可能为 {@code null}（框架未传递额外数据时）
     * @param state  用于写入状态数据的可变 Map，由框架为每个钩子实例单独创建，不会与其他钩子共享
     */
    public void onHotReloading(@Nullable Bundle extras, @NonNull Map<String, Object> state) {
    }

    /**
     * 热重载完成回调，在模块热重载完成后触发。
     *
     * @param thisObject 从全局状态快照中恢复的宿主对象实例，可能为 {@code null}（静态方法或 {@code <clinit>} 钩子）
     * @param inState    之前通过 {@link #onHotReloading(Bundle, Map)} 保存并合并后的全局状态快照
     */
    public void onHotReloaded(@Nullable Object thisObject, @NonNull Map<String, Object> inState) {
    }

    /**
     * 从线程本地状态栈中获取当前调用上下文。
     *
     * @return 当前线程对应的调用状态对象
     * @throws IllegalStateException 如果当前线程不在钩子拦截生命周期内（状态栈为空）
     */
    @NonNull
    private CallState getState() {
        StateStack stack = mStackLocal.get();
        CallState state = stack != null ? stack.current() : null;
        if (state == null) {
            throw new IllegalStateException("Hook state has been lost or is not being called within the interception lifecycle.");
        }
        return state;
    }

    /**
     * 获取当前被拦截方法对应的可执行对象。
     *
     * @return 当前被拦截方法的可执行对象
     */
    @NonNull
    public final Executable getExecutable() {
        return getState().originalChain.getExecutable();
    }

    /**
     * 获取调用被拦截方法时的目标对象实例（即 {@code this} 引用）。
     * <p>
     * 若被拦截的方法是静态方法，则返回 {@code null}。
     *
     * @return 调用被拦截方法的对象实例；静态方法时为 {@code null}
     */
    @Nullable
    public final Object getThisObject() {
        return getState().originalChain.getThisObject();
    }

    /**
     * 获取当前被拦截方法的全部参数。
     * <p>
     * 返回副本；修改参数请使用 {@link #setArg(int, Object)} 或 {@link #setArgs(Object...)}。
     *
     * @return 方法参数数组的副本
     */
    @NonNull
    public final Object[] getArgs() {
        CallState state = getState();
        if (state.args == null) {
            state.args = state.originalChain.getArgs().toArray(new Object[0]);
        }
        return state.args.clone();
    }

    /**
     * 获取当前被拦截方法在指定索引位置的参数值。
     * <p>
     * 若之前已通过 {@link #setArg(int, Object)} 修改过该位置的参数，则返回修改后的值。
     *
     * @param index 参数索引（从 0 开始）
     * @return 该索引位置的参数值
     * @throws IndexOutOfBoundsException 当 {@code index} 超出参数数组的有效范围时抛出
     * @throws ClassCastException        当返回值类型与预期不符时可能抛出
     */
    @Nullable
    public final Object getArg(int index) throws IndexOutOfBoundsException, ClassCastException {
        CallState state = getState();
        if (state.args == null) {
            state.args = state.originalChain.getArgs().toArray(new Object[0]);
        }
        return state.args[index];
    }

    /**
     * 设置当前被拦截方法在指定索引位置的参数值。
     * <p>
     * 调用此方法后，框架将使用修改后的参数调用原方法。
     *
     * @param index 要修改的参数索引（从 0 开始）
     * @param value 新的参数值
     */
    public final void setArg(int index, @Nullable Object value) {
        CallState state = getState();
        if (state.args == null) {
            state.args = state.originalChain.getArgs().toArray(new Object[0]);
        }
        state.args[index] = value;
        state.isArgsChanged = true;
    }

    /**
     * 整体替换当前被拦截方法的全部参数。
     * <p>
     * 传入的数组长度必须与原始参数个数一致，否则将抛出异常。
     *
     * @param args 新的参数数组，长度须与原方法参数列表匹配
     * @throws IllegalArgumentException 当传入数组的长度与原始参数个数不一致时抛出
     */
    public final void setArgs(@NonNull Object... args) {
        if (args == null) {
            args = new Object[]{null};
        }

        CallState state = getState();
        if (state.args == null) {
            state.args = state.originalChain.getArgs().toArray(new Object[0]);
        }
        if (state.args.length != args.length) {
            throw new IllegalArgumentException("Parameter quantity mismatch. " +
                "Target length:" + state.args.length + ", Actual length: " + args.length);
        }
        state.args = args.clone();
        state.isArgsChanged = true;
    }

    /**
     * 获取当前被拦截方法的返回值。
     * <p>
     * 若之前已通过 {@link #setResult(Object)} 替换了返回值，则返回替换后的值；否则返回原方法执行完成后的原始返回值。
     *
     * @return 当前方法的返回值，可能为 {@code null}
     */
    @Nullable
    public final Object getResult() {
        CallState state = getState();
        return state.isResultChanged ? state.replaceResult : state.originalResult;
    }

    /**
     * 替换当前被拦截方法的返回值。
     * <p>
     * 设置后原方法的原始返回值将被忽略；若在 {@link #before()} 阶段调用，原方法将被完全跳过且不执行 {@link #after()}。
     *
     * @param result 要设置的新返回值
     */
    public final void setResult(@Nullable Object result) {
        CallState state = getState();
        state.replaceResult = result;
        state.isResultChanged = true;
    }

    /**
     * 设置当前被拦截方法的待抛出异常。
     * <p>
     * 设置后该异常将在后续阶段抛出，可通过 {@link #getThrowable()} 读取。异常能否到达调用方取决于框架异常模式：{@code PASSTHROUGH} 会原样抛给调用方，默认的 {@code PROTECTIVE} 只记录日志并继续执行原方法。
     *
     * @param throwable 要设置的异常对象
     */
    public final void setThrowable(@NonNull Throwable throwable) {
        getState().throwable = throwable;
    }

    /**
     * 记录非主动抛出中首个未消费的异常。
     * <p>
     * 仅在当前尚未记录任何异常时生效，保证主动设置的异常不会被后续阶段的异常覆盖。
     *
     * @param throwable 非主动抛出的异常
     */
    final void setNonActiveThrowable(@NonNull Throwable throwable) {
        CallState state = getState();
        if (state.throwable == null) {
            state.throwable = throwable;
        }
    }

    /**
     * 获取当前被拦截方法执行过程中关联的异常信息。
     *
     * @return 当前关联的异常对象；若无异常则返回 {@code null}
     */
    @Nullable
    public final Throwable getThrowable() {
        return getState().throwable;
    }

    /**
     * 获取当前调用的数据槽。
     * <p>
     * 数据存放在调用帧上而非 Hook 实例上，重入与并发调用互不影响。
     *
     * @return 数据槽内容，未使用时为 null
     */
    @Nullable
    public final Object getFrameData() {
        return getState().userData;
    }

    /**
     * 设置当前调用的数据槽。
     *
     * @param data 供本次调用后续阶段读取的数据
     */
    public final void setFrameData(@Nullable Object data) {
        getState().userData = data;
    }

    /**
     * 将钩子句柄注册到内部管理列表中。
     * <p>
     * 注册后的句柄可供 {@link #unhook()} 使用，以便一次性解除所有钩子。
     *
     * @param handle 框架返回的钩子句柄
     */
    final void setHandle(@NonNull XposedInterface.HookHandle handle) {
        this.mHandles.add(handle);
    }

    /**
     * 设置当前钩子绑定的声明类名作为热重载存储键。
     * <p>
     * 同一类的所有方法/构造函数共享相同的类级 key，避免热重载时重复保存同一 {@link #mThisObject}。
     *
     * @param key 声明类的全限定类名
     */
    final void setKey(@NonNull String key) {
        this.mKey = key;
    }

    // --- 生命周期管理 ---

    /**
     * 进入钩子拦截上下文，将指定的调用链压入当前线程的状态栈。
     * <p>
     * 调用此方法后，当前线程内的所有钩子生命周期回调均可访问该次调用的上下文信息。
     *
     * @param chain 本次拦截对应的原始调用链
     */
    final void enter(@NonNull XposedInterface.Chain chain) {
        Object thisObject = chain.getThisObject();
        WeakReference<Object> tracked = mThisObject;
        if (thisObject == null) {
            mThisObject = null;
        } else if (tracked == null || tracked.get() != thisObject) {
            mThisObject = new WeakReference<>(thisObject);
        }
        Objects.requireNonNull(mStackLocal.get()).push(chain);
    }

    /**
     * 退出钩子拦截上下文，从当前线程的状态栈中弹出调用状态。
     */
    final void exit() {
        StateStack stack = mStackLocal.get();
        if (stack != null) {
            stack.pop();
        }
    }

    /**
     * 获取当前钩子的内部调用链包装器。
     * <p>
     * 读取参数时会优先返回已被修改的值。
     *
     * @return 内部调用链实例
     */
    @NonNull final XposedInterface.Chain getChain() {
        return getState().innerChain;
    }

    /**
     * 调用被拦截的原始方法并返回执行结果。
     * <p>
     * 若参数已被修改（通过 {@link #setArg(int, Object)} 或 {@link #setArgs(Object...)}），则使用修改后的参数调用原方法。
     *
     * @return 原始方法的返回值
     * @throws Throwable 原方法执行过程中可能抛出的任意异常
     */
    final Object callProceed() throws Throwable {
        CallState state = getState();
        if (state.isArgsChanged && state.args != null) {
            return state.originalChain.proceed(state.args);
        } else {
            return state.originalChain.proceed();
        }
    }

    /**
     * 记录原方法的原始执行结果。
     *
     * @param originalResult 原方法的原始返回值
     */
    final void setOriginalResult(Object originalResult) {
        getState().originalResult = originalResult;
    }

    /**
     * 判断方法返回值是否已被替换。
     *
     * @return 若已通过 {@link #setResult(Object)} 替换返回值则返回 {@code true}，否则返回 {@code false}
     */
    final boolean isResultChanged() {
        return getState().isResultChanged;
    }

    /**
     * 解除当前钩子实例注册的所有方法拦截。
     * <p>
     * 任一句柄解除失败不会阻断其余句柄，解除完成后内部句柄列表必然被清空。
     *
     * @throws IllegalStateException 当钩子尚未生效（句柄列表为空）时调用此方法将抛出
     */
    final public void unhook() {
        if (mHandles.isEmpty()) {
            throw new IllegalStateException("Hook handle is not initialized. Cannot unhook before the hook is applied.");
        }
        try {
            for (XposedInterface.HookHandle handle : mHandles) {
                try {
                    handle.unhook();
                } catch (Throwable e) {
                    AndroidLog.logW(LogExpand.getTag(), "Failed to unhook a hook handle.", e);
                }
            }
        } finally {
            mHandles.clear();
        }
    }

    /**
     * 获取当前钩子实例已注册的全部 Hook 句柄。
     * <p>
     * 返回的列表是快照副本，可用于在热更新等场景中逐一处理旧 Hook。
     *
     * @return 已注册的句柄数组；若尚未注册任何句柄则返回空数组
     */
    @NonNull final public XposedInterface.HookHandle[] getHookHandles() {
        return mHandles.toArray(new XposedInterface.HookHandle[0]);
    }

    /**
     * 生成当前调用的可观测信息字符串。
     * <p>
     * 返回内容包含被调用方法的类名、方法名、参数列表及返回值，便于调试与日志记录。
     *
     * @return 格式化的调用信息字符串
     */
    @NonNull final public String describeCall() {
        return HookLog.describe(this);
    }

    @NonNull
    @Override
    public String toString() {
        StateStack stack = mStackLocal.get();
        CallState state = stack != null ? stack.current() : null;
        return "AbsHook{" +
            "handles=" + mHandles +
            ", callState=" + (state != null ? state.toString() : "null") +
            '}';
    }

    /**
     * 内部调用链实现，对原始 {@link XposedInterface.Chain} 进行包装。
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
            return state.isArgsChanged ? Arrays.asList(state.args) : state.originalChain.getArgs();
        }

        @Override
        public Object getArg(int index) throws IndexOutOfBoundsException, ClassCastException {
            return state.isArgsChanged ? state.args[index] : state.originalChain.getArg(index);
        }

        @Override
        public Object proceed() throws Throwable {
            return state.isArgsChanged ? state.originalChain.proceed(state.args) : state.originalChain.proceed();
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