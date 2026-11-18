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

import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * 附加字段存储。
 * <p>
 * 用于在不修改类定义的前提下为对象附加额外字段；键按 {@code equals} 匹配，重写 equals 的对象（如 String、Uri）会被视为同一个目标；值若直接或间接引用键，该条目不会被回收，需要调用方自行调用 {@link #remove(Object, String)}。
 *
 * @author 焕晨HChen
 */
final class AdditionalFields {
    private static final WeakHashMap<Object, HashMap<String, Object>> FIELDS = new WeakHashMap<>();

    private AdditionalFields() {
    }

    /**
     * 设置附加字段。
     *
     * @param target 目标对象
     * @param key    字段键
     * @param value  字段值
     * @return 该键原有的值
     */
    static Object set(Object target, String key, Object value) {
        synchronized (FIELDS) {
            HashMap<String, Object> map = FIELDS.computeIfAbsent(target, k -> new HashMap<>());
            return map.put(key, value);
        }
    }

    /**
     * 读取附加字段。
     *
     * @param target 目标对象
     * @param key    字段键
     * @return 字段值，不存在时返回 null
     */
    static Object get(Object target, String key) {
        synchronized (FIELDS) {
            HashMap<String, Object> map = FIELDS.get(target);
            if (map == null) return null;
            return map.get(key);
        }
    }

    /**
     * 移除附加字段。
     *
     * @param target 目标对象
     * @param key    字段键
     * @return 被移除的值，不存在时返回 null
     */
    static Object remove(Object target, String key) {
        synchronized (FIELDS) {
            HashMap<String, Object> map = FIELDS.get(target);
            if (map == null) return null;
            Object removed = map.remove(key);
            if (map.isEmpty()) FIELDS.remove(target);
            return removed;
        }
    }
}
