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
package com.hchen.hooktool.reflect;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 按类缓存的成员查找结果，同时记录查找失败。
 * <p>
 * 缓存使用弱引用，类的类加载器仍可被回收。
 *
 * @param <T> 缓存的成员类型
 * @author 焕晨HChen
 */
final class MemberCache<T> {
    private final Map<Class<?>, ConcurrentHashMap<String, Slot<T>>> mMaps = new WeakHashMap<>();

    private ConcurrentHashMap<String, Slot<T>> mapFor(Class<?> owner) {
        synchronized (mMaps) {
            return mMaps.computeIfAbsent(owner, key -> new ConcurrentHashMap<>());
        }
    }

    /**
     * 获取缓存的成员，弱引用被回收时重新查找。
     *
     * @param owner     成员所属的类
     * @param key       缓存键
     * @param recompute 缓存缺失时的查找逻辑
     * @param error     缓存为空时生成失败异常
     * @return 解析出的成员
     */
    T get(Class<?> owner, String key,
          Function<String, Slot<T>> recompute, Supplier<Throwable> error) {
        ConcurrentHashMap<String, Slot<T>> map = mapFor(owner);
        Slot<T> cached = map.get(key);
        if (cached != null) {
            if (cached.isEmpty()) {
                Throwables.throwIt(error.get());
            }
            T value = cached.getOrNull();
            if (value != null) return value;
            Slot<T> fresh = recompute.apply(key);
            map.put(key, fresh);
            T recomputed = fresh.getOrNull();
            if (recomputed != null) return recomputed;
            Throwables.throwIt(error.get());
        }
        Slot<T> computed = map.computeIfAbsent(key, recompute);
        T value = computed.getOrNull();
        if (value != null) return value;
        return computed.orElseThrow(error);
    }

    /**
     * 判断该键是否已被缓存为查找失败，不构造异常。
     *
     * @param owner 成员所属的类
     * @param key   缓存键
     * @return 已确定查找失败时返回 {@code true}
     */
    boolean isMissing(Class<?> owner, String key) {
        Slot<T> cached = mapFor(owner).get(key);
        return cached != null && cached.isEmpty();
    }

    /**
     * 缓存项，区分未查找与查找失败。
     *
     * @param <T> 成员类型
     *
     * @author 焕晨HChen
     */
    static final class Slot<T> {
        private static final Slot<?> EMPTY = new Slot<>(null);

        private final WeakReference<T> mRef;

        private Slot(T value) {
            mRef = value == null ? null : new WeakReference<>(value);
        }

        /**
         * 创建持有有效值的缓存项。
         *
         * @param value 缓存值
         * @param <T>   成员类型
         * @return 缓存项
         */
        static <T> Slot<T> of(T value) {
            return new Slot<>(value);
        }

        /**
         * 创建表示查找失败的缓存项。
         *
         * @param <T> 成员类型
         * @return 空缓存项
         */
        @SuppressWarnings("unchecked")
        static <T> Slot<T> empty() {
            return (Slot<T>) EMPTY;
        }

        /**
         * 判断是否为查找失败。
         *
         * @return 失败时返回 true
         */
        boolean isEmpty() {
            return mRef == null;
        }

        /**
         * 获取缓存值。
         *
         * @return 缓存值，被回收时返回 null
         */
        T getOrNull() {
            return mRef == null ? null : mRef.get();
        }

        /**
         * 获取缓存值，为空时抛出异常。
         *
         * @param error 生成异常的逻辑
         * @return 缓存值
         */
        T orElseThrow(Supplier<Throwable> error) {
            T value = getOrNull();
            if (value == null) {
                Throwables.throwIt(error.get());
            }
            return value;
        }
    }
}