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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.ModuleEntrance;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import io.github.libxposed.api.XposedModuleInterface;

/**
 * 全局钩子注册表，以弱引用的方式存储所有 {@link AbsHook} 实例。
 * <p>
 * 本类维护一个全局的弱引用集合，用于追踪当前进程中所有已创建的 {@link AbsHook} 实例。
 * 当 {@link AbsHook} 实例不再被外部强引用时，GC 会自动将其从注册表中移除，
 * 无需手动注销，从而避免内存泄漏。
 * <p>
 * 该注册表采用 {@link Collections#newSetFromMap} 配合 {@link WeakHashMap} 实现，
 * 确保对钩子实例的引用是<strong>最轻量</strong>的——仅持有足以判断存活状态的弱引用，
 * 不会阻止 GC 回收不再使用的钩子实例。集合中的每个元素都是唯一的，不会重复存储。
 * <p>
 * 典型使用场景包括：
 * <ul>
 *     <li>遍历所有活跃钩子以统一更新其内部状态（如 {@link AbsHook#thisObject}）</li>
 *     <li>在模块卸载或热重载时释放所有钩子资源</li>
 *     <li>调试与监控当前已部署的钩子数量及状态</li>
 * </ul>
 * <p>
 * 本类的所有公开方法都是线程安全的，支持在多线程环境下并发访问。
 *
 * @author 焕晨HChen
 * @see AbsHook
 * @see WeakHashMap
 */
public final class HookRegistry {
    private static final Set<AbsHook> hooks =
        Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private HookRegistry() {
        throw new AssertionError("No instances!");
    }

    /**
     * 将一个 {@link AbsHook} 实例注册到全局注册表中。
     * <p>
     * 注册后该实例会被弱引用持有。当实例不再被任何外部强引用引用时，
     * GC 会自动将其从注册表中移除。重复注册同一个实例不会产生重复条目。
     * <p>
     * 通常情况下，无需手动调用此方法——{@link AbsHook} 的构造器会自动完成注册。
     * 此方法仅在需要手动管理注册时机时使用。
     *
     * @param hook 要注册的 {@link AbsHook} 实例，不为 {@code null}
     */
    static void register(@NonNull AbsHook hook) {
        hooks.add(hook);
    }

    /**
     * 从全局注册表中移除指定的 {@link AbsHook} 实例。
     * <p>
     * 移除后，该实例将不再出现在 {@link #getActiveHooks()} 的返回结果中，
     * 但其仍可正常工作直至被 GC 回收。
     * <p>
     * 通常无需手动调用此方法，因为 {@link WeakHashMap} 会在实例被 GC 后自动清理。
     * 此方法适用于需要在实例被回收前立即将其从注册表中移除的场景。
     *
     * @param hook 要移除的 {@link AbsHook} 实例，不为 {@code null}
     */
    static void unregister(@NonNull AbsHook hook) {
        hooks.remove(hook);
    }

    /**
     * 获取当前所有活跃的 {@link AbsHook} 实例的快照。
     * <p>
     * 返回的 {@link Set} 是对当前注册表中所有未被 GC 回收的钩子实例的副本，
     * 对该副本的修改不会影响全局注册表。每次调用都会创建新的集合，
     * 因此在高频调用场景下应注意性能开销。
     * <p>
     * 注意：返回的快照代表了调用时刻的状态，后续的注册或注销操作不会反映在该快照中。
     *
     * @return 当前所有活跃钩子实例的快照，不会包含已被 GC 回收的实例，不为 {@code null}
     */
    @NonNull
    public static Set<AbsHook> getActiveHooks() {
        synchronized (hooks) {
            return new HashSet<>(hooks);
        }
    }

    /**
     * 清空全局注册表中的所有钩子实例。
     * <p>
     * 调用此方法后，所有已被注册的 {@link AbsHook} 实例将从注册表中移除，
     * 但不会影响钩子实例本身的存活状态——它们仍可被外部强引用继续使用。
     * <p>
     * 此方法在模块卸载或热重载时尤为有用，可快速清理钩子注册信息。
     */
    public static void clear() {
        synchronized (hooks) {
            hooks.clear();
        }
    }

    /**
     * 获取当前注册表中活跃钩子的数量。
     * <p>
     * 返回值为当前未被 GC 回收的 {@link AbsHook} 实例个数。
     * 由于 GC 的异步性，该数值可能在调用前后发生变化。
     *
     * @return 当前活跃钩子的数量
     */
    public static int getActiveCount() {
        synchronized (hooks) {
            return hooks.size();
        }
    }

    /**
     * 触发所有已注册钩子的热重载准备阶段，收集并合并各实例填写的状态快照。
     * <p>
     * 收集过程分为两个阶段：
     * <ol>
     *   <li><b>用户自定义状态</b>：遍历所有钩子，依次调用
     *       {@link AbsHook#onHotReloading(Bundle, Map)}，将各实例填写的状态数据
     *       合并到全局快照中。此阶段严格检测重复键——不同实例填写的 Map 中存在
     *       相同键时抛出 {@link IllegalStateException}。</li>
     *   <li><b>{@code thisObject} 自动保存</b>：遍历所有钩子，对每个非静态实例的钩子
     *       将其 {@link AbsHook#thisObject} 按 {@link AbsHook#key}（类名）存入全局快照。
     *       同一类的多个方法共享同一个类级 key，使用
     *       {@link Map#putIfAbsent(Object, Object)} 自动去重，
     *       避免同类的多个方法重复存储同一份 {@code thisObject}。</li>
     * </ol>
     * <p>
     * 静态方法的 {@code thisObject} 始终为 {@code null}，在此阶段不会存储任何数据。
     * <p>
     * 合并完成后注册表会被清空（因为热重载结束后旧注册表已废弃）。
     *
     * @param extras 热重载的附加信息，包含触发重载的上下文数据；可能为 {@code null}
     *               （当框架未传递额外数据时）
     * @return 所有实例填写的状态数据合并后的全局快照；若所有实例均未写入任何数据则返回空 {@link HashMap}
     * @throws IllegalStateException 当不同实例填写的 {@link Map} 中存在重复的键时抛出
     * @see AbsHook#onHotReloading(Bundle, Map)
     * @see AbsHook#key
     */
    @NonNull
    public static Map<String, Object> reloading(@Nullable Bundle extras) {
        synchronized (hooks) {
            Map<String, Object> merged = new HashMap<>();

            // Phase 1: 收集用户自定义状态（严格去重检测）
            for (AbsHook hook : hooks) {
                // 为每个钩子单独创建 state map，传入钩子由其填写。
                // 各钩子写入的 state 互不影响，合并时的碰撞检测由下方循环保证。
                Map<String, Object> state = new HashMap<>();
                hook.onHotReloading(extras, state);
                for (Map.Entry<String, Object> entry : state.entrySet()) {
                    if (merged.containsKey(entry.getKey())) {
                        throw new IllegalStateException(
                            "Duplicate key found while merging hot reload state: " + entry.getKey());
                    }
                    merged.put(entry.getKey(), entry.getValue());
                }
            }

            // Phase 2: 自动保存 thisObject（以类名去重，同一类的多个方法共享一份）
            // 使用 putIfAbsent 避免重复，因为同一类的多个方法具有相同的类级 key，
            // 无需多次保存，第一次写入后后续的 putIfAbsent 不会覆盖。
            // 静态方法的 thisObject 始终为 null，此处不会存储任何数据。
            for (AbsHook hook : hooks) {
                if (hook.key != null && hook.thisObject != null) {
                    merged.putIfAbsent(hook.key, hook.thisObject);
                }
            }

            hooks.clear();
            return merged;
        }
    }

    /**
     * 触发所有已注册钩子的热重载完成阶段，将之前保存的状态快照分发给各实例。
     * <p>
     * 从 {@link XposedModuleInterface.HotReloadedParam} 中提取之前通过
     * {@link #reloading(Bundle)} 保存的 {@code savedInstanceState}，
     * 并遍历当前注册表中所有未被 GC 回收的 {@link AbsHook} 实例，
     * 依次调用各实例的 {@link AbsHook#onHotReloaded(Object, Map)} 方法。
     * <p>
     * <b>{@code thisObject} 恢复逻辑：</b>
     * <ol>
     *   <li>优先使用 {@link AbsHook#thisObject} 字段的已有值（如果非 {@code null}，
     *       表示该钩子实例已经过至少一次实际调用，持有最新的宿主对象）。</li>
     *   <li>若字段值为 {@code null}，则根据该实例的 {@link AbsHook#key} 从
     *       {@code inState} 中查找之前保存的值并回写，同时更新
     *       {@code hook.thisObject} 以便后续子类可读。</li>
     *   <li>对于静态方法或 {@code <clinit>} 的钩子（{@link AbsHook#isStatic}
     *       为 {@code true}），不会从 {@code inState} 中查找 {@code thisObject}，
     *       而是直接传入 {@code null}，避免静态钩子误读到同类的非静态实例数据。</li>
     * </ol>
     * <p>
     * 与 {@link #reloading(Bundle)} 不同，此方法<strong>不会</strong>清空注册表，
     * 因为热重载完成后新创建的钩子实例需要继续被追踪。
     * 注册表的清空在 {@link #reloading(Bundle)} 中已完成。
     *
     * @param param 热重载完成参数，包含之前通过 {@code param.setSavedInstanceState(merged)}
     *              保存的全局状态快照，不为 {@code null}
     * @throws NullPointerException 如果 {@code param} 或
     *                              {@code param.getSavedInstanceState()} 为 {@code null}
     * @see AbsHook#onHotReloaded(Object, Map)
     * @see AbsHook#key
     * @see ModuleEntrance#onHotReloaded(XposedModuleInterface.HotReloadedParam)
     */
    public static void reloaded(@NonNull XposedModuleInterface.HotReloadedParam param) {
        Objects.requireNonNull(param);
        synchronized (hooks) {
            Map<String, Object> inState = (Map<String, Object>) param.getSavedInstanceState();
            Objects.requireNonNull(inState);

            for (AbsHook hook : hooks) {
                Object savedThisObject = (hook.key != null && !hook.isStatic)
                    ? inState.get(hook.key) : null;

                if (!hook.isStatic) {
                    if (hook.thisObject != null) savedThisObject = hook.thisObject;
                    else hook.thisObject = savedThisObject;
                }
                hook.onHotReloaded(savedThisObject, inState);
            }
        }
    }
}
