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
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 构造函数查找
 *
 * @author 焕晨HChen
 * @noinspection SequencedCollectionMethodCanBeUsed, unused
 */
public class ConstructorHelper {
    private final Class<?> clazz;
    private int mods = -1; // 修饰符
    private Class<?>[] paramClasses;
    private final HashMap<Integer, Integer> paramCountVarMap = new HashMap<>();
    private Class<? extends Annotation>[] annotations;
    private Class<? extends Throwable>[] exceptionClasses;

    public ConstructorHelper(@NonNull Class<?> clazz) {
        Objects.requireNonNull(clazz, "Class must not be null.");
        this.clazz = clazz;
    }

    /**
     * 构造函数的参数数量是否在某个范围内
     */
    public ConstructorHelper withParamCount(int paramCount, @RangeHelper.RangeModeFlag int mode) {
        this.paramCountVarMap.put(mode, paramCount);
        return this;
    }

    /**
     * 构造函数的参数类型，可使用 null 占位，表示任意类型
     */
    public ConstructorHelper withParamClasses(@NonNull Class<?>... paramClasses) {
        Objects.requireNonNull(paramClasses, "ParamClasses must not be null.");
        this.paramClasses = paramClasses;
        return this;
    }

    /**
     * 构造函数的修饰符
     */
    public ConstructorHelper withMods(int mods) {
        this.mods = mods;
        return this;
    }

    /**
     * 设置构造函数为 public 修饰符
     */
    public ConstructorHelper withPublic() {
        this.mods = Modifier.PUBLIC;
        return this;
    }

    /**
     * 设置构造函数为 private 修饰符
     */
    public ConstructorHelper withPrivate() {
        this.mods = Modifier.PRIVATE;
        return this;
    }

    /**
     * 设置构造函数为 protected 修饰符
     */
    public ConstructorHelper withProtected() {
        this.mods = Modifier.PROTECTED;
        return this;
    }

    /**
     * 构造函数的注解
     */
    @SuppressWarnings("unchecked")
    public ConstructorHelper withAnnotations(@NonNull Class<? extends Annotation>... annotations) {
        Objects.requireNonNull(annotations, "Annotations must not be null.");
        this.annotations = annotations;
        return this;
    }

    /**
     * 构造函数抛出的异常
     *
     * @noinspection unchecked
     */
    public ConstructorHelper withExceptionClasses(@NonNull Class<? extends Throwable>... exceptionClasses) {
        Objects.requireNonNull(exceptionClasses, "ExceptionClasses must not be null.");
        this.exceptionClasses = exceptionClasses;
        return this;
    }

    /**
     * 获取查找到的构造函数，如果查找结果为空或不为单个则抛错
     */
    public Constructor<?> single() {
        List<Constructor<?>> constructors = matches();
        if (constructors.isEmpty())
            throw new NonSingletonException("No result found for query.");
        if (constructors.size() > 1)
            throw new NonSingletonException("Query did not return a unique result: " + constructors.size());

        return constructors.get(0);
    }

    /**
     * 获取查找到的构造函数，如果查找结果为空或不为单个则返回 null
     */
    @Nullable
    public Constructor<?> singleOrNull() {
        List<Constructor<?>> constructors = matches();
        if (constructors.size() == 1)
            return constructors.get(0);

        return null;
    }

    /**
     * 获取查找到的构造函数，如果查找结果为空或不为单个则抛错
     */
    public Constructor<?> singleOrThrow(@NonNull Supplier<NonSingletonException> throwableSupplier) {
        List<Constructor<?>> constructors = matches();
        if (constructors.size() != 1)
            throw throwableSupplier.get();

        return constructors.get(0);
    }

    /**
     * 返回查找到的所有构造函数
     */
    public Constructor<?>[] toArray() {
        return matches().toArray(new Constructor[0]);
    }

    /**
     * 重置查找器，清除所有设置的条件
     */
    public void reset() {
        mods = -1;
        paramClasses = null;
        annotations = null;
        exceptionClasses = null;
        paramCountVarMap.clear();
    }

    /**
     * 核心过滤逻辑
     */
    private List<Constructor<?>> matches() {
        Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
        List<Constructor<?>> result = new ArrayList<>(declaredConstructors.length);

        for (Constructor<?> constructor : declaredConstructors) {
            if (matchesParamClasses(constructor) &&
                matchesParamCount(constructor) &&
                matchesModifiers(constructor) &&
                matchesAnnotations(constructor) &&
                matchesExceptions(constructor)) {
                result.add(constructor);
            }
        }

        return result;
    }

    /**
     * 匹配参数类型
     */
    private boolean matchesParamClasses(Constructor<?> constructor) {
        if (paramClasses == null) {
            return true;
        }

        if (paramClasses.length != constructor.getParameterCount()) {
            return false;
        }

        for (int i = 0; i < paramClasses.length; i++) {
            Class<?> want = paramClasses[i];
            Class<?> actual = constructor.getParameterTypes()[i];
            if (want != null && !want.equals(actual)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 匹配参数数量
     */
    private boolean matchesParamCount(Constructor<?> constructor) {
        if (paramCountVarMap.isEmpty()) {
            return true;
        }

        if (paramCountVarMap.containsKey(EQ)) {
            // noinspection DataFlowIssue
            return constructor.getParameterCount() == paramCountVarMap.get(EQ);
        }

        for (int mode : paramCountVarMap.keySet()) {
            Integer count = paramCountVarMap.get(mode);
            if (count == null) {
                return false;
            }

            switch (mode) {
                case GT -> {
                    if (!(constructor.getParameterCount() > count)) return false;
                }
                case LT -> {
                    if (!(constructor.getParameterCount() < count)) return false;
                }
                case GE -> {
                    if (!(constructor.getParameterCount() >= count)) return false;
                }
                case LE -> {
                    if (!(constructor.getParameterCount() <= count)) return false;
                }
                default -> {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 匹配修饰符
     */
    private boolean matchesModifiers(Constructor<?> constructor) {
        return mods == -1 || (constructor.getModifiers() & mods) == mods;
    }

    /**
     * 匹配注解
     */
    private boolean matchesAnnotations(Constructor<?> constructor) {
        return annotations == null || Arrays.stream(annotations).allMatch(constructor::isAnnotationPresent);
    }

    /**
     * 匹配异常
     */
    private boolean matchesExceptions(Constructor<?> constructor) {
        if (exceptionClasses == null) {
            return true;
        }

        List<Class<?>> declaredExceptions = Arrays.asList(constructor.getExceptionTypes());
        return Arrays.stream(exceptionClasses).allMatch(declaredExceptions::contains);
    }
}
