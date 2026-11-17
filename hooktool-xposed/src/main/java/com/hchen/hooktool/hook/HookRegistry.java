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

import com.hchen.hooktool.entry.ModuleEntrance;
import com.hchen.hooktool.log.AndroidLog;

import java.lang.ref.WeakReference;
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
 *
 * @author 焕晨HChen
 */
public final class HookRegistry {
    private static final String TAG = "HookRegistry";
    private static final Set<AbsHook> sHooks =
        Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    /**
     * {@code thisObject} 自动保存/恢复使用的键命名空间前缀，避免与用户自定义状态键碰撞。
     */
    private static final String THIS_OBJECT_PREFIX = "__hooktool_thisObject:";

    private HookRegistry() {
        throw new AssertionError("No instances!");
    }

    /**
     * 获取当前所有活跃的 {@link AbsHook} 实例的快照。
     * <p>
     * 快照仅代表调用时刻的状态，后续的注册或注销不会反映在其中。
     *
     * @return 当前所有活跃钩子实例的快照，不会包含已被 GC 回收的实例
     */
    @NonNull
    public static Set<AbsHook> getActiveHooks() {
        synchronized (sHooks) {
            return new HashSet<>(sHooks);
        }
    }

    /**
     * 清空全局注册表中的所有钩子实例。
     * <p>
     * 仅移除注册信息，不影响钩子实例本身的存活状态。
     */
    public static void clear() {
        synchronized (sHooks) {
            sHooks.clear();
        }
    }

    /**
     * 获取当前注册表中活跃钩子的数量。
     * <p>
     * 由于 GC 的异步性，该数值可能在调用前后发生变化。
     *
     * @return 当前活跃钩子的数量
     */
    public static int getActiveCount() {
        synchronized (sHooks) {
            return sHooks.size();
        }
    }

    /**
     * 触发所有已注册钩子的热重载准备阶段，收集并合并各实例填写的状态快照。
     * <p>
     * 注册表的清空由 {@link ModuleEntrance#onHotReloading} 在状态保存成功后统一调用 {@link #clear()} 完成。
     *
     * @param extras 热重载的附加信息；可能为 {@code null}（当框架未传递额外数据时）
     * @return 所有实例填写的状态数据合并后的全局快照；若所有实例均未写入任何数据则返回空 {@link HashMap}
     * @throws IllegalStateException 当不同实例填写的 {@link Map} 中存在重复的键时抛出
     */
    @NonNull
    public static Map<String, Object> reloading(@Nullable Bundle extras) {
        Object[] snapshot = hookSnapshot();

        Map<String, Object> merged = new HashMap<>();

        // 收集用户自定义状态。
        for (Object o : snapshot) {
            AbsHook hook = (AbsHook) o;
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

        // 自动保存 thisObject，键前缀避免与其他状态键碰撞。
        for (Object o : snapshot) {
            AbsHook hook = (AbsHook) o;
            Object thisObject = hook.mThisObject == null ? null : hook.mThisObject.get();
            if (hook.mKey != null && thisObject != null) {
                merged.putIfAbsent(THIS_OBJECT_PREFIX + hook.mKey, thisObject);
            }
        }

        return merged;
    }

    /**
     * 触发所有已注册钩子的热重载完成阶段，将之前保存的状态快照分发给各实例。
     * <p>
     * 与 {@link #reloading(Bundle)} 不同，此方法不会清空注册表。
     *
     * @param param 热重载完成参数，包含之前通过 {@code param.setSavedInstanceState(merged)}
     *              保存的全局状态快照
     * @throws NullPointerException 如果 {@code param} 或
     *                              {@code param.getSavedInstanceState()} 为 {@code null}
     */
    @SuppressWarnings("unchecked")
    public static void reloaded(@NonNull XposedModuleInterface.HotReloadedParam param) {
        Objects.requireNonNull(param);
        Object[] snapshot = hookSnapshot();
        Map<String, Object> inState = (Map<String, Object>) param.getSavedInstanceState();
        Objects.requireNonNull(inState);

        for (Object o : snapshot) {
            AbsHook hook = (AbsHook) o;
            try {
                Object tracked = hook.mThisObject == null ? null : hook.mThisObject.get();
                Object savedThisObject = (hook.mKey != null && !hook.mIsStatic)
                    ? inState.get(THIS_OBJECT_PREFIX + hook.mKey) : null;

                if (!hook.mIsStatic) {
                    if (tracked != null) {
                        savedThisObject = tracked;
                    } else {
                        hook.mThisObject = savedThisObject == null
                            ? null : new WeakReference<>(savedThisObject);
                    }
                }
                hook.onHotReloaded(savedThisObject, inState);
            } catch (Throwable e) {
                // 单个钩子的状态恢复失败不阻断其余钩子，避免热更新后集体半初始化。
                AndroidLog.logW(TAG, "onHotReloaded failed for hook: " + hook + ".", e);
            }
        }
    }

    /**
     * 将一个 {@link AbsHook} 实例注册到全局注册表中。
     * <p>
     * 通常无需手动调用此方法，{@link AbsHook} 的构造器会自动完成注册。
     *
     * @param hook 要注册的 {@link AbsHook} 实例
     */
    static void register(@NonNull AbsHook hook) {
        sHooks.add(hook);
    }

    /**
     * 从注册表中移除 Hook，使其不再接收热更新回调。
     *
     * @param hook 待移除的 Hook 实例
     */
    public static void unregister(@NonNull AbsHook hook) {
        synchronized (sHooks) {
            sHooks.remove(hook);
        }
    }

    /**
     * 获取当前所有活跃钩子的快照数组。
     * <p>
     * 调用方应在锁外遍历该数组执行用户回调。
     *
     * @return 当前活跃钩子实例的快照数组；若没有活跃钩子则返回长度为 0 的数组
     */
    static Object[] hookSnapshot() {
        synchronized (sHooks) {
            return sHooks.toArray();
        }
    }
}