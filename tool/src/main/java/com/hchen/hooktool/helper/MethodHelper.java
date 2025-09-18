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
 * Copyright (C) 2023–2025 HChenX
 */
package com.hchen.hooktool.helper;

import static com.hchen.hooktool.helper.RangeHelper.EQ;
import static com.hchen.hooktool.helper.RangeHelper.GE;
import static com.hchen.hooktool.helper.RangeHelper.GT;
import static com.hchen.hooktool.helper.RangeHelper.LE;
import static com.hchen.hooktool.helper.RangeHelper.LT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.exception.NonSingletonException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 方法查找
 *
 * @author 焕晨HChen
 * @noinspection SequencedCollectionMethodCanBeUsed, unused
 */
public class MethodHelper {
    private final Class<?> clazz;
    private int mods = -1; // 修饰符
    private String methodName = null;
    private String substring = null;
    private Pattern pattern = null;

    private Class<?>[] paramClasses = null;
    private final HashMap<Integer, Integer> paramCountVarMap = new HashMap<>();

    private Class<?> returnClass = null;
    private Class<? extends Annotation>[] annotations = null;
    private Class<? extends Throwable>[] exceptionClasses = null;
    private boolean withSuper = false;

    public MethodHelper(@NonNull Class<?> clazz) {
        Objects.requireNonNull(clazz, "[MethodHelper]: Class must not be null!!");
        this.clazz = clazz;
    }

    /**
     * 方法名
     */
    public MethodHelper withMethodName(@NonNull String methodName) {
        this.methodName = methodName;
        return this;
    }

    /**
     * 方法名包含的字段
     */
    public MethodHelper withSubstring(@NonNull String substring) {
        this.substring = substring;
        return this;
    }

    /**
     * 匹配方法名
     */
    public MethodHelper withPattern(@NonNull Pattern pattern) {
        this.pattern = pattern;
        return this;
    }

    /**
     * 方法参数数量是否在某个范围内
     */
    public MethodHelper withParamCount(int paramCount, @RangeHelper.RangeModeFlag int mode) {
        this.paramCountVarMap.put(mode, paramCount);
        return this;
    }

    /**
     * 方法参数类型，可使用 Any.class 占位，表示任意类型
     */
    public MethodHelper withParamClasses(@NonNull Class<?>... paramClasses) {
        this.paramClasses = paramClasses;
        return this;
    }

    /**
     * 方法返回类型
     */
    public MethodHelper withReturnClass(@NonNull Class<?> returnType) {
        this.returnClass = returnType;
        return this;
    }

    /**
     * 方法的修饰符
     */
    public MethodHelper withMods(@NonNull int... modsFlags) {
        int combined = 0;
        for (int f : modsFlags) {
            combined |= f;
        }
        this.mods = combined;
        return this;
    }

    /**
     * Public
     */
    public MethodHelper withPublic() {
        return withMods(Modifier.PUBLIC);
    }

    /**
     * Private
     */
    public MethodHelper withPrivate() {
        return withMods(Modifier.PRIVATE);
    }

    /**
     * Protected
     */
    public MethodHelper withProtected() {
        return withMods(Modifier.PROTECTED);
    }

    /**
     * Static
     */
    public MethodHelper withStatic() {
        return withMods(Modifier.STATIC);
    }

    /**
     * Synchronized
     */
    public MethodHelper withSynchronized() {
        return withMods(Modifier.SYNCHRONIZED);
    }

    /**
     * Native
     */
    public MethodHelper withNative() {
        return withMods(Modifier.NATIVE);
    }

    /**
     * Abstract
     */
    public MethodHelper withAbstract() {
        return withMods(Modifier.ABSTRACT);
    }

    /**
     * 方法的注解
     */
    public MethodHelper withAnnotations(@NonNull Class<? extends Annotation>... annotations) {
        this.annotations = annotations;
        return this;
    }

    /**
     * 方法抛出的异常类型
     *
     * @noinspection unchecked
     */
    public MethodHelper withExceptionClasses(@NonNull Class<? extends Throwable>... exceptionClasses) {
        this.exceptionClasses = exceptionClasses;
        return this;
    }

    /**
     * 是否查找 Super 类的方法
     */
    public MethodHelper withSuper(boolean withSuper) {
        this.withSuper = withSuper;
        return this;
    }

    /**
     * 获取查找到的对象，如果查找结果为空或不为单个则抛错
     */
    public HookHelper<Method> single() {
        List<Method> methods = matches();
        if (methods.isEmpty())
            throw new NonSingletonException("[MethodHelper]: No result found for query!!");

        if (methods.size() > 1)
            throw new NonSingletonException("[MethodHelper]: Query did not return a unique result: " + methods.size());

        return new HookHelper<>(methods.get(0));
    }

    /**
     * 获取查找到的对象，如果查找结果为空或不为单个则返回 null
     */
    @Nullable
    public HookHelper<Method> singleOrNull() {
        List<Method> methods = matches();
        if (methods.isEmpty()) return null;
        if (methods.size() > 1) return null;

        return new HookHelper<>(methods.get(0));
    }

    /**
     * 获取查找到的对象，如果查找结果为空或不为单个则抛错
     */
    public HookHelper<Method> singleOrThrow(@NonNull Supplier<NonSingletonException> throwableSupplier) {
        List<Method> methods = matches();
        if (methods.isEmpty()) throw throwableSupplier.get();
        if (methods.size() > 1) throw throwableSupplier.get();

        return new HookHelper<>(methods.get(0));
    }

    /**
     * 返回查找到的全部对象
     */
    public Method[] list() {
        return matches().toArray(new Method[0]);
    }

    /**
     * 重置查找器
     */
    public void reset() {
        mods = -1;
        methodName = null;
        substring = null;
        pattern = null;
        paramClasses = null;
        returnClass = null;
        annotations = null;
        exceptionClasses = null;
        paramCountVarMap.clear();
        withSuper = false;
    }

    /**
     * 核心过滤逻辑
     *
     * @noinspection RedundantIfStatement
     */
    private List<Method> matches() {
        ArrayList<Method> methods = new ArrayList<>(Arrays.asList(clazz.getDeclaredMethods()));
        if (withSuper) {
            Class<?> clazzWithSuper = clazz.getSuperclass();
            do {
                if (clazzWithSuper == null) break;
                methods.addAll(Arrays.asList(clazzWithSuper.getDeclaredMethods()));
            } while ((clazzWithSuper = clazzWithSuper.getSuperclass()) != null);
        }

        return methods.stream().filter(method -> {
            if (methodName != null && !Objects.equals(methodName, method.getName()))
                return false;
            else if (pattern != null && !pattern.matcher(method.getName()).matches())
                return false;
            else if (substring != null && !method.getName().contains(substring))
                return false;

            if (paramClasses != null) {
                if (paramClasses.length != method.getParameterCount()) {
                    return false;
                }
                for (int i = 0; i < method.getParameterCount(); i++) {
                    Class<?> actual = method.getParameterTypes()[i];
                    Class<?> want = paramClasses[i];
                    if (Objects.equals(want, Any.class)) continue;
                    if (!Objects.equals(actual, want)) {
                        return false;
                    }
                }
            }

            if (!paramCountVarMap.isEmpty()) {
                if (paramCountVarMap.containsKey(EQ)) {
                    if (!Objects.equals(method.getParameterCount(), paramCountVarMap.get(EQ)))
                        return false;
                } else {
                    for (int mode : paramCountVarMap.keySet()) {
                        Integer count = paramCountVarMap.get(mode);
                        if (count == null) return false;

                        switch (mode) {
                            case GT -> {
                                if (!(method.getParameterCount() > count)) return false;
                            }
                            case LT -> {
                                if (!(method.getParameterCount() < count)) return false;
                            }
                            case GE -> {
                                if (!(method.getParameterCount() >= count)) return false;
                            }
                            case LE -> {
                                if (!(method.getParameterCount() <= count)) return false;
                            }
                            default -> {
                                return false;
                            }
                        }
                    }
                }
            }

            if (mods != -1 && (method.getModifiers() & mods) != mods)
                return false;
            if (returnClass != null &&
                !(Objects.equals(method.getReturnType(), returnClass) || returnClass.isAssignableFrom(method.getReturnType())))
                return false;
            if (annotations != null && !Arrays.stream(annotations).allMatch(method::isAnnotationPresent))
                return false;
            if (exceptionClasses != null && !Arrays.equals(method.getExceptionTypes(), exceptionClasses))
                return false;

            return true;
        }).collect(Collectors.toList());
    }
}
