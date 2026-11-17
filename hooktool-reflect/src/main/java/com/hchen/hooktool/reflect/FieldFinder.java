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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 字段查找工具。
 * <p>
 * 沿继承链与接口树查找字段，结果按类缓存。
 *
 * @author 焕晨HChen
 */
final class FieldFinder {
    private static final MemberCache<Field> CACHE = new MemberCache<>();

    private FieldFinder() {
    }

    /**
     * 查找指定名称的字段。
     *
     * @param clazz 目标类
     * @param fieldName 字段名
     * @return 已设置为可访问的字段
     */
    static Field find(Class<?> clazz, String fieldName) {
        return CACHE.get(clazz, fieldName,
            key -> {
                try {
                    Field field = findRecursive(clazz, key);
                    field.setAccessible(true);
                    return MemberCache.Slot.of(field);
                } catch (NoSuchFieldException e) {
                    return MemberCache.Slot.empty();
                }
            },
            () -> new ReflectException(ReflectException.Kind.FIELD_NOT_FOUND,
                clazz.getName() + "#" + fieldName,
                "No field " + clazz.getName() + "#" + fieldName + " in the class hierarchy."));
    }

    /**
     * 查找指定名称的字段，不存在时返回 null。
     *
     * @param clazz 目标类
     * @param fieldName 字段名
     * @return 字段或 null
     */
    static Field findOrNull(Class<?> clazz, String fieldName) {
        try {
            return find(clazz, fieldName);
        } catch (ReflectException e) {
            return null;
        }
    }

    private static Field findRecursive(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> clz = clazz;
        while (clz != null && clz != Object.class) {
            try {
                return clz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                // 继续向上查找
            }
            clz = clz.getSuperclass();
        }

        clz = clazz;
        while (clz != null && clz != Object.class) {
            for (Class<?> iface : clz.getInterfaces()) {
                Field field = findInterfaceFieldRecursive(iface, fieldName);
                if (field != null) return field;
            }
            clz = clz.getSuperclass();
        }
        throw new NoSuchFieldException(fieldName);
    }

    private static Field findInterfaceFieldRecursive(Class<?> iface, String fieldName) {
        try {
            Field field = iface.getDeclaredField(fieldName);
            if (Modifier.isStatic(field.getModifiers())) return field;
        } catch (NoSuchFieldException ignored) {
            // 继续查找父接口
        }
        for (Class<?> superInterface : iface.getInterfaces()) {
            Field field = findInterfaceFieldRecursive(superInterface, fieldName);
            if (field != null) return field;
        }
        return null;
    }
}
