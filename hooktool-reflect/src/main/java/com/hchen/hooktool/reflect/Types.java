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
import java.util.Map;

/**
 * 类型可赋值性与重载匹配代价计算。
 *
 * @author 焕晨HChen
 */
final class Types {
    private static final Map<Class<?>, Class<?>> WRAPPERS = new HashMap<>();
    private static final Map<Class<?>, Class<?>> PRIMITIVES = new HashMap<>();

    static {
        box(boolean.class, Boolean.class);
        box(byte.class, Byte.class);
        box(char.class, Character.class);
        box(short.class, Short.class);
        box(int.class, Integer.class);
        box(long.class, Long.class);
        box(float.class, Float.class);
        box(double.class, Double.class);
        box(void.class, Void.class);
    }

    private Types() {
    }

    private static void box(Class<?> primitive, Class<?> wrapper) {
        WRAPPERS.put(primitive, wrapper);
        PRIMITIVES.put(wrapper, primitive);
    }

    /**
     * 获取基本类型的包装类型，非基本类型原样返回。
     *
     * @param type 源类型
     * @return 包装类型
     */
    static Class<?> wrapperOf(Class<?> type) {
        Class<?> wrapper = WRAPPERS.get(type);
        return wrapper == null ? type : wrapper;
    }

    /**
     * 获取包装类型对应的基本类型。
     *
     * @param type 包装类型
     * @return 基本类型，非包装类型返回 null
     */
    static Class<?> primitiveOf(Class<?> type) {
        return PRIMITIVES.get(type);
    }

    /**
     * 判断源类型数组能否逐一赋值给目标类型数组。
     *
     * @param from 源类型数组，元素可为 null
     * @param to 目标类型数组
     * @return 全部可赋值时返回 true
     */
    static boolean isAssignable(Class<?>[] from, Class<?>[] to) {
        if (from.length != to.length) return false;
        for (int i = 0; i < from.length; i++) {
            Class<?> fromType = from[i];
            if (fromType == null) {
                if (to[i].isPrimitive()) return false;
            } else if (!isAssignable(fromType, to[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断源类型能否赋值给目标类型，包含装箱与基本类型加宽。
     *
     * @param from 源类型
     * @param to 目标类型
     * @return 可赋值时返回 true
     */
    static boolean isAssignable(Class<?> from, Class<?> to) {
        if (to.isAssignableFrom(from)) return true;
        if (from.isPrimitive() && to.isPrimitive()) {
            return isPrimitiveAssignable(from, to);
        }
        if (from.isPrimitive()) {
            return to.isAssignableFrom(wrapperOf(from));
        }
        if (to.isPrimitive()) {
            Class<?> unboxed = primitiveOf(from);
            return unboxed != null && isPrimitiveAssignable(unboxed, to);
        }
        return false;
    }

    private static boolean isPrimitiveAssignable(Class<?> from, Class<?> to) {
        if (from == to) return true;
        return wideningSteps(from.getName(), to.getName()) != -1;
    }

    private static int wideningSteps(String src, String dest) {
        switch (src) {
            case "byte":
                return step(dest, "short", "int", "long", "float", "double");
            case "short":
            case "char":
                return step(dest, "int", "long", "float", "double");
            case "int":
                return step(dest, "long", "float", "double");
            case "long":
                return step(dest, "float", "double");
            case "float":
                return step(dest, "double");
            default:
                return -1;
        }
    }

    private static int step(String dest, String... order) {
        for (int i = 0; i < order.length; i++) {
            if (order[i].equals(dest)) return i + 1;
        }
        return -1;
    }

    private static float primitivePromotionCost(Class<?> srcType, Class<?> destType) {
        float cost = 0f;
        Class<?> cls = srcType;
        if (cls != null && !cls.isPrimitive()) {
            cost += 0.1f;
            cls = primitiveOf(cls);
        }
        if (cls == null) return 1.5f;
        if (cls == destType) return cost;
        if (cls == void.class || cls == boolean.class) return Float.MAX_VALUE;
        int steps = wideningSteps(cls.getName(), destType.getName());
        if (steps == -1) return Float.MAX_VALUE;
        cost += 0.1f * steps;
        return cost;
    }

    /**
     * 计算运行时类型转换为声明参数类型的代价。
     *
     * @param srcType 运行时类型，可为 null
     * @param destType 声明的参数类型
     * @return 转换代价，不兼容时返回 {@link Float#MAX_VALUE}
     */
    static float objectTransformationCost(Class<?> srcType, Class<?> destType) {
        if (destType.isPrimitive()) {
            if (srcType == null) return Float.MAX_VALUE;
            return primitivePromotionCost(srcType, destType);
        }
        if (srcType == null) return 1.5f;
        if (srcType == destType) return 0f;
        if (srcType.isArray() && destType.isArray()) {
            Class<?> srcComponent = srcType.getComponentType();
            Class<?> destComponent = destType.getComponentType();
            return objectTransformationCost(srcComponent, destComponent) + 0.1f;
        }
        Class<?> effectiveSrc =
            (srcType.isPrimitive() && !destType.isPrimitive()) ? wrapperOf(srcType) : srcType;
        float cost = 0f;
        Class<?> src = effectiveSrc;
        while (src != null && src != destType) {
            if (destType.isInterface() && destType.isAssignableFrom(src)) {
                cost += interfaceDistance(src, destType);
                break;
            }
            cost++;
            src = src.getSuperclass();
        }
        if (src == null) cost += 1.5f;
        return cost;
    }

    private static float interfaceDistance(Class<?> src, Class<?> dest) {
        if (src == dest) return 0f;
        float minCost = Float.MAX_VALUE;
        for (Class<?> iface : src.getInterfaces()) {
            if (dest.isAssignableFrom(iface)) {
                float cost = 0.25f + interfaceDistance(iface, dest);
                if (cost < minCost) minCost = cost;
            }
        }
        Class<?> superclass = src.getSuperclass();
        if (superclass != null && dest.isAssignableFrom(superclass)) {
            float cost = 1.0f + interfaceDistance(superclass, dest);
            if (cost < minCost) minCost = cost;
        }
        return minCost;
    }

    private static float totalTransformationCost(Class<?>[] srcArgs, Class<?>[] destArgs) {
        float total = 0f;
        for (int i = 0; i < srcArgs.length; i++) {
            total += objectTransformationCost(srcArgs[i], destArgs[i]);
        }
        return total;
    }

    /**
     * 比较两个候选参数列表与目标参数的匹配程度。
     *
     * @param candidate 候选参数类型
     * @param current 当前最优参数类型
     * @param wanted 目标参数类型，元素可为 null
     * @return 候选更优时返回负数
     */
    static int compareFit(Class<?>[] candidate, Class<?>[] current, Class<?>[] wanted) {
        float candidateCost = totalTransformationCost(wanted, candidate);
        float currentCost = totalTransformationCost(wanted, current);
        return Float.compare(candidateCost, currentCost);
    }

    /**
     * 生成区分类加载器的缓存签名。
     *
     * @param parameterTypes 参数类型，元素可为 null
     * @return 稳定的签名字符串
     */
    static String signature(Class<?>[] parameterTypes) {
        if (parameterTypes == null || parameterTypes.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) sb.append(',');
            Class<?> type = parameterTypes[i];
            if (type == null) {
                sb.append("null");
            } else {
                sb.append(type.getName())
                    .append('@')
                    .append(System.identityHashCode(type.getClassLoader()));
            }
        }
        return sb.toString();
    }
}
