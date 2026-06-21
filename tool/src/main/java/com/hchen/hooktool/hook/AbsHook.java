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

import android.os.Bundle;

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.libxposed.api.XposedInterface;

/**
 * 所有 Xposed 方法钩子的抽象基类。
 * <p>
 * 本类为方法拦截提供了一套完整的生命周期模型，子类只需覆写对应的回调方法即可实现自定义拦截逻辑。
 * <p>
 * <b>核心拦截生命周期</b>（每次方法调用触发一轮）：
 * <ol>
 *     <li>{@link #before()} —— 前置拦截阶段，在原方法执行前触发</li>
 *     <li>{@link #proceed(XposedInterface.Chain)} —— 原方法调用阶段，执行被拦截的目标方法</li>
 *     <li>{@link #after()} —— 后置拦截阶段，在原方法执行完成后触发</li>
 * </ol>
 * <b>热重载生命周期</b>（模块热更新时触发）：
 * <ol>
 *     <li>{@link #onHotReloading(Bundle)} —— 热重载准备阶段，返回需保存的状态数据</li>
 *     <li>{@link #onHotReloaded(Object, Map)} —— 热重载完成阶段，恢复之前保存的状态</li>
 * </ol>
 * <p>
 * 各阶段中发生的异常会统一回调至 {@link #onThrow(StageEnum, Throwable)}，由子类决定是否消费。
 * <p>
 * 本类内部通过 {@link ThreadLocal} 维护线程独立的状态栈，因此支持同一线程内对同一钩子的嵌套（可重入）调用。
 * 在拦截过程中，子类可通过 {@link #getArgs()}、{@link #setResult(Object)} 等方法自由读写方法参数与返回值。
 * <p>
 * 每个钩子实例在创建时自动注册到 {@link HookRegistry}（弱引用），
 * 并会自动捕获最近一次调用时的宿主对象（{@link #thisObject}）。
 * 热重载时，{@link HookRegistry} 会按声明类名（{@link #key}）去重存储 {@link #thisObject}，
 * 并在新钩子实例上恢复。静态方法钩子（{@link #isStatic}）不参与此流程。
 *
 * @author 焕晨HChen
 * @see HookBridge
 * @see HookRegistry
 */
public abstract class AbsHook {
    int priority; // 钩子优先级
    String id; // 钩子 id
    XposedInterface.ExceptionMode mode; // 异常模式

    /**
     * 当前钩子绑定的声明类名，作为热重载时 {@code thisObject} 的存储键。
     * <p>
     * 由 {@link HookBridge} 在注册钩子时自动设置，取值为
     * {@link java.lang.reflect.Executable#getDeclaringClass()} 的全限定类名
     * （方法/构造函数），或 {@link Class#getName()}（类初始化器）。
     * <p>
     * 同一个类的所有方法/构造函数共享相同的类级 key，使得热重载时
     * {@code thisObject} 只需按类存储一份即可，避免多个方法重复保存同一对象。
     * 静态方法的 {@code thisObject} 始终为 {@code null}，因此不会实际保存。
     */
    String key; // 热重载用类级标识键

    /**
     * 标识当前钩子是否绑定到静态上下文。
     * <p>
     * 由 {@link HookBridge} 在注册钩子时自动设置：
     * <ul>
     *   <li>对于方法/构造函数，取 {@code java.lang.reflect.Modifier.isStatic(executable.getModifiers())}</li>
     *   <li>对于类初始化器 {@code <clinit>}，始终为 {@code true}</li>
     *   <li>通过无参或旧版构造器注册时，默认为 {@code false}</li>
     * </ul>
     * <p>
     * 此字段用于在热重载流程中区分静态与非静态上下文：
     * 静态方法的 {@link #thisObject} 始终为 {@code null}，且在
     * {@link HookRegistry#reloaded(Map)} 中不会从状态快照中查找
     * {@code thisObject}，避免静态钩子误读到同类的非静态实例数据。
     */
    boolean isStatic;

    /**
     * 当前最新一次被拦截的方法调用的宿主对象实例（即 {@code this} 引用）。
     * <p>
     * 该字段会在每次进入钩子拦截上下文时，从当前调用链 {@link XposedInterface.Chain} 中获取最新值并自动更新。
     * 若被拦截的方法是静态方法，则该字段为 {@code null}。
     * <p>
     * 外部代码可通过此字段直接获取该钩子最近一次拦截到的目标对象实例，
     * 而无需在钩子回调中手动调用 {@link #getThisObject()}。
     * <p>
     * 注意：该字段的值是实时更新的，但仅在钩子生命周期内（即 {@link #before()} 至 {@link #after()} 之间）
     * 具有有效含义；在生命周期之外仍保留上一次拦截的值。
     * <p>
     * 使用 {@code volatile} 保证跨线程可见性：{@link #enter(XposedInterface.Chain)} 在回调线程中
     * 写入此字段，而 {@link HookRegistry#reloading} 在热更新触发线程中读取。
     * 若无 {@code volatile}，读取线程可能看到过期的 {@code null} 值，
     * 导致热重载时 {@code thisObject} 状态丢失。
     *
     * @see #getThisObject()
     * @see XposedInterface.Chain#getThisObject()
     */
    volatile Object thisObject;

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

    /**
     * 钩子拦截的生命周期阶段枚举。
     * <p>
     * 在 {@link #onThrow(StageEnum, Throwable)} 中用于标识异常发生的具体阶段，
     * 以便调用方根据阶段进行差异化的异常处理。
     */
    public enum StageEnum {
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

    /**
     * 使用全部默认参数构造钩子实例。
     * <p>
     * 等价于 {@code AbsHook(PRIORITY_DEFAULT, null, null)}。
     */
    public AbsHook() {
        this(PRIORITY_DEFAULT, null, null);
    }

    /**
     * 使用指定优先级构造钩子实例。
     *
     * @param priority 钩子优先级，数值越小优先级越高，越早被框架调用
     */
    public AbsHook(int priority) {
        this(priority, null, null);
    }

    /**
     * 使用指定标识符构造钩子实例，优先级为默认值。
     *
     * @param id 钩子唯一标识符，用于在相同可执行对象上原子替换旧 Hook；为 {@code null} 表示不关心后续替换
     */
    public AbsHook(@Nullable String id) {
        this(PRIORITY_DEFAULT, id, null);
    }

    /**
     * 使用指定异常处理模式构造钩子实例，优先级为默认值。
     *
     * @param mode 钩子异常处理模式；为 {@code null} 表示使用框架默认行为
     */
    public AbsHook(@Nullable XposedInterface.ExceptionMode mode) {
        this(PRIORITY_DEFAULT, null, mode);
    }

    /**
     * 使用指定优先级和标识符构造钩子实例。
     *
     * @param priority 钩子优先级，数值越小优先级越高
     * @param id       钩子唯一标识符；为 {@code null} 表示不关心后续替换
     */
    public AbsHook(int priority, @Nullable String id) {
        this(priority, id, null);
    }

    /**
     * 使用指定优先级和异常处理模式构造钩子实例。
     *
     * @param priority 钩子优先级，数值越小优先级越高
     * @param mode     钩子异常处理模式；为 {@code null} 表示使用框架默认行为
     */
    public AbsHook(int priority, @Nullable XposedInterface.ExceptionMode mode) {
        this(priority, null, mode);
    }

    /**
     * 使用指定标识符和异常处理模式构造钩子实例，优先级为默认值。
     *
     * @param id   钩子唯一标识符；为 {@code null} 表示不关心后续替换
     * @param mode 钩子异常处理模式；为 {@code null} 表示使用框架默认行为
     */
    public AbsHook(@Nullable String id, @Nullable XposedInterface.ExceptionMode mode) {
        this(PRIORITY_DEFAULT, id, mode);
    }

    /**
     * 完整参数的钩子实例构造器。
     *
     * @param priority 钩子优先级，数值越小优先级越高
     * @param id       钩子唯一标识符，用于在相同可执行对象上原子替换旧 Hook；为 {@code null} 表示不关心后续替换
     * @param mode     钩子异常处理模式；为 {@code null} 表示使用框架默认行为
     */
    public AbsHook(int priority, @Nullable String id, @Nullable XposedInterface.ExceptionMode mode) {
        this.priority = priority;
        this.id = id;
        this.mode = mode;
        HookRegistry.register(this);
    }

    /**
     * 在原方法执行之前调用的前置拦截回调。
     * <p>
     * 子类应覆写此方法以实现参数修改、前置校验等自定义逻辑。
     * 若在此阶段调用 {@link #setResult(Object)} 设置了返回值，
     * 则原方法将被完全跳过，直接进入后置拦截阶段。
     */
    public void before() {
    }

    /**
     * 原方法调用阶段的回调，为子类提供定制 proceed 行为的能力。
     * <p>
     * 默认实现直接委托至 {@link #callProceed()} 调用原方法。
     * 子类可覆写此方法以在原方法调用前后注入额外逻辑，
     * 或根据条件决定是否真正调用原方法。
     * <p>
     * 该方法的返回值会作为原方法的默认执行结果被记录，
     * 后续可通过 {@link #setResult(Object)} 进行覆盖。
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
     * <p>
     * 子类应覆写此方法以实现返回值读取/修改、资源清理、日志记录等后置处理逻辑。
     */
    public void after() {
    }

    /**
     * 钩子生命周期中发生异常时的统一回调。
     * <p>
     * 当生命周期的任意阶段抛出异常时，框架会优先调用此方法。
     * 返回 {@code true} 表示异常已被消费，框架不再继续传播；
     * 返回 {@code false} 则由框架按默认策略处理（可能直接抛出）。
     *
     * @param stage 异常发生的生命周期阶段，不为 {@code null}
     * @param e     被捕获的异常对象，不为 {@code null}
     * @return {@code true} 表示异常已被处理，{@code false} 表示交由框架继续处理
     */
    public boolean onThrow(@NonNull StageEnum stage, @NonNull Throwable e) {
        return false;
    }

    /**
     * 热重载准备回调，在当前模块即将被热重载时触发。
     * <p>
     * 子类可覆写此方法以返回需要保存的状态数据。返回的 {@link Map} 中的键值对
     * 会在 {@link HookRegistry#reloading(Bundle)} 的阶段一中被合并到全局状态快照中。
     * 若返回的 Map 中存在与其他实例重复的键，合并过程会显式抛出
     * {@link IllegalStateException} 以提示冲突。
     * <p>
     * {@link #thisObject} 的自动保存由框架在阶段二中以声明类名（{@link #key}）
     * 去重处理，无需在此方法中手动保存。
     * 若无需保存任何额外状态，返回空 {@link HashMap} 即可。
     * <p>
     * 默认实现返回一个空的 {@link HashMap}（始终非 {@code null}）。
     *
     * @param extra 热重载的附加信息，包含触发重载的上下文数据；可能为 {@code null}
     *               （当框架未传递额外数据时）
     * @return 需要保存的状态键值对，不为 {@code null}；默认返回空 {@link HashMap}
     * @see HookRegistry#reloading(Bundle)
     */
    @NonNull
    public Map<String, Object> onHotReloading(@Nullable Bundle extra) {
        return new HashMap<>();
    }

    /**
     * 热重载完成回调，在模块热重载完成后触发。
     * <p>
     * 子类可覆写此方法以从传入的状态快照中恢复之前保存的数据。
     * 传入的 {@code thisObject} 是 {@link HookRegistry#reloaded(Map)} 根据当前实例的
     * {@link #key} 从全局状态快照中查找到的宿主对象实例。
     * 传入的 {@code inState} 是在 {@link #onHotReloading(Bundle)} 阶段由
     * 所有旧钩子实例收集并合并后的全局状态快照。
     * <p>
     * 此回调在 {@link HookRegistry#reloaded(Map)} 中被调用，
     * 此时注册表中已注册的是热重载后新创建的钩子实例。
     *
     * @param thisObject 从全局状态快照中恢复的宿主对象实例，
     *                   可能为 {@code null}（若保存时 {@link #thisObject} 为 {@code null} 或
     *                   {@link #key} 未设置）
     * @param inState    之前通过 {@link #onHotReloading(Bundle)} 保存并合并后的全局状态快照，
     *                   不为 {@code null}
     * @see HookRegistry#reloaded(Map)
     * @see #key
     */
    public void onHotReloaded(@NonNull Object thisObject, @NonNull Map<String, Object> inState) {
    }

    /**
     * 从线程本地状态栈中获取当前调用上下文。
     *
     * @return 当前线程对应的调用状态对象，不为 {@code null}
     * @throws IllegalStateException 如果当前线程不在钩子拦截生命周期内（状态栈为空）
     */
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
     * 获取当前被拦截方法对应的可执行对象。
     * <p>
     * 返回值可能是 {@link java.lang.reflect.Method} 或 {@link java.lang.reflect.Constructor}。
     *
     * @return 当前被拦截方法的可执行对象，不为 {@code null}
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
    public final Object getThisObject() {
        return getState().originalChain.getThisObject();
    }

    /**
     * 获取当前被拦截方法的全部参数。
     * <p>
     * 首次调用时会从原始调用链中缓存参数列表。
     * 若之前已通过 {@link #setArg(int, Object)} 或 {@link #setArgs(Object...)} 修改过参数，
     * 则返回修改后的参数值。
     *
     * @return 方法参数数组，不为 {@code null}
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
     * 获取当前被拦截方法在指定索引位置的参数值。
     * <p>
     * 若之前已通过 {@link #setArg(int, Object)} 修改过该位置的参数，
     * 则返回修改后的值。
     *
     * @param index 参数索引（从 0 开始）
     * @return 该索引位置的参数值
     * @throws IndexOutOfBoundsException 当 {@code index} 超出参数数组的有效范围时抛出
     * @throws ClassCastException        当返回值类型与预期不符时可能抛出
     */
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
    public final void setArg(int index, Object value) {
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
        CallState state = getState();
        if (state.args == null) {
            state.args = state.originalChain.getArgs().toArray(new Object[0]);
        }
        if (state.args.length != args.length) {
            throw new IllegalArgumentException("Parameter quantity mismatch. " +
                "Target length:" + state.args.length + ", Actual length: " + args.length);
        }
        state.args = args;
        state.isArgsChanged = true;
    }

    /**
     * 获取当前被拦截方法的返回值。
     * <p>
     * 若之前已通过 {@link #setResult(Object)} 替换了返回值，则返回替换后的值；
     * 否则返回原方法执行完成后的原始返回值。
     *
     * @return 当前方法的返回值，可能为 {@code null}
     */
    public final Object getResult() {
        CallState state = getState();
        return state.isResultChanged ? state.replaceResult : state.originalResult;
    }

    /**
     * 替换当前被拦截方法的返回值。
     * <p>
     * 设置后原方法的原始返回值将被忽略，框架会将此处设置的值作为最终返回值。
     * 若在 {@link #before()} 阶段调用此方法，原方法将被完全跳过。
     *
     * @param result 要设置的新返回值
     */
    public final void setResult(Object result) {
        CallState state = getState();
        state.replaceResult = result;
        state.isResultChanged = true;
    }

    /**
     * 设置当前被拦截方法的待抛出异常。
     * <p>
     * 设置后该异常将在后续阶段由框架自动抛出。可通过 {@link #getThrowable()} 读取。
     *
     * @param throwable 要设置的异常对象
     */
    public final void setThrowable(Throwable throwable) {
        getState().throwable = throwable;
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
     * 将钩子句柄注册到内部管理列表中。
     * <p>
     * 注册后的句柄可供 {@link #unHookSelf()} 使用，以便一次性解除所有钩子。
     *
     * @param handle 框架返回的钩子句柄，不为 {@code null}
     */
    final void setHandle(@NonNull XposedInterface.HookHandle handle) {
        this.handles.add(handle);
    }

    /**
     * 设置当前钩子绑定的声明类名作为热重载存储键。
     * <p>
     * 此键由 {@link HookBridge} 在注册钩子时调用，取值为声明类的全限定类名。
     * 同一类的所有方法/构造函数共享相同的类级 key，使得热重载时
     * {@link #thisObject} 只需按类存储一份即可，避免重复。
     *
     * @param key 声明类的全限定类名，不为 {@code null}
     * @see #key
     * @see HookBridge
     */
    final void setKey(@NonNull String key) {
        this.key = key;
    }

    // --- 生命周期管理 ---

    /**
     * 进入钩子拦截上下文，将指定的调用链压入当前线程的状态栈。
     * <p>
     * 调用此方法后，当前线程内的所有钩子生命周期回调均可访问该次调用的上下文信息
     * （包括方法参数、返回值等）。
     *
     * @param chain 本次拦截对应的原始调用链，不为 {@code null}
     */
    final void enter(@NonNull XposedInterface.Chain chain) {
        thisObject = chain.getThisObject();
        Objects.requireNonNull(stackLocal.get()).push(chain);
    }

    /**
     * 退出钩子拦截上下文，从当前线程的状态栈中弹出调用状态。
     * <p>
     * 此方法会释放与本次拦截关联的上下文资源，使状态栈恢复到上一层嵌套调用。
     */
    final void exit() {
        StateStack stack = stackLocal.get();
        if (stack != null) {
            stack.pop();
        }
    }

    /**
     * 获取当前钩子的内部调用链包装器。
     * <p>
     * 返回的 {@link XposedInterface.Chain} 实例对原始调用链进行了包装，
     * 在读取参数时会优先返回已被修改的值，保证拦截生命周期内参数读写的一致性。
     *
     * @return 内部调用链实例，不为 {@code null}
     */
    @NonNull final XposedInterface.Chain getChain() {
        return getState().innerChain;
    }

    /**
     * 调用被拦截的原始方法并返回执行结果。
     * <p>
     * 若参数已被修改（通过 {@link #setArg(int, Object)} 或 {@link #setArgs(Object...)}），
     * 则使用修改后的参数调用原方法；否则以原始参数调用。
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
     * <p>
     * 由框架在 proceed 阶段内部调用，将原方法的返回值存储到当前调用状态中，
     * 以供后续 {@link #after()} 阶段及 {@link #getResult()} 读取。
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
     * 遍历内部已注册的全部钩子句柄并逐一解除。
     * 解除完成后内部句柄列表将被清空，以便后续重新注册。
     *
     * @throws IllegalStateException 当钩子尚未生效（句柄列表为空）时调用此方法将抛出
     */
    final public void unHookSelf() {
        if (handles.isEmpty()) {
            throw new IllegalStateException("Hook handle is not initialized. Cannot unhook before the hook is applied.");
        }
        for (XposedInterface.HookHandle handle : handles) {
            handle.unhook();
        }
        handles.clear();
    }

    /**
     * 获取当前钩子实例已注册的全部 Hook 句柄。
     * <p>
     * 返回的列表是线程安全的快照副本，可用于在热更新等场景中逐一处理旧 Hook。
     *
     * @return 已注册的句柄数组；若尚未注册任何句柄则返回空数组
     */
    @NonNull final public XposedInterface.HookHandle[] getHookHandles() {
        return handles.toArray(new XposedInterface.HookHandle[0]);
    }

    /**
     * 生成当前调用的可观测信息字符串。
     * <p>
     * 返回内容包含被调用方法的类名、方法名、参数列表及返回值，便于调试与日志记录。
     *
     * @return 格式化的调用信息字符串，不为 {@code null}
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
     * 内部调用链实现，对原始 {@link XposedInterface.Chain} 进行包装。
     * <p>
     * 该实现会在读取参数时优先返回已被修改的值，从而保证在同一拦截生命周期内
     * 参数读写行为的一致性。其余操作（{@code proceed}、{@code proceedWith} 等）
     * 直接委托给原始调用链处理。
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
