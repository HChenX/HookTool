/*
 * This file is part of HookTool.

 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HChenX
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
import com.hchen.hooktool.exception.UnexpectedException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 构造函数查找
 *
 * @author 焕晨HChen
 */
public class ConstructorHelper {
    private final Class<?> clazz;
    private int paramCount = -1;
    private Class<?>[] paramTypes = null;
    private int mods = -1;
    private Class<? extends Annotation> annotation = null;
    private Type[] genericParamTypes = null;
    private Class<? extends Throwable>[] exceptionTypes = null;
    private boolean withSuper = false;
    private Constructor<?> constructorCache = null;
    private final ConcurrentHashMap<Integer, Integer> paramCountVarMap = new ConcurrentHashMap<>();

    public ConstructorHelper(Class<?> clazz) {
        Objects.requireNonNull(clazz, "[ConstructorHelper]: Class must not is null!");
        this.clazz = clazz;
    }

    /**
     * 构造函数的参数数量
     */
    public ConstructorHelper withParamCount(int paramCount) {
        this.paramCount = paramCount;
        return this;
    }

    /**
     * 构造函数的参数数量是否在某个范围内
     */
    public ConstructorHelper withParamCount(int paramCount, @RangeHelper.RangeModeFlag int mode) {
        this.paramCountVarMap.put(mode, paramCount);
        return this;
    }

    /**
     * 构造函数的参数类型，可使用 Any.class 占位，表示任意类型
     */
    public ConstructorHelper withParamTypes(@NonNull Class<?>... paramTypes) {
        this.paramTypes = paramTypes;
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
     * Public
     */
    public ConstructorHelper withPublic() {
        this.mods = Modifier.PUBLIC;
        return this;
    }

    /**
     * Private
     */
    public ConstructorHelper withPrivate() {
        this.mods = Modifier.PRIVATE;
        return this;
    }

    /**
     * Protected
     */
    public ConstructorHelper withProtected() {
        this.mods = Modifier.PROTECTED;
        return this;
    }

    /**
     * 构造函数的注释
     */
    public ConstructorHelper withAnnotation(@NonNull Class<? extends Annotation> annotation) {
        this.annotation = annotation;
        return this;
    }

    /**
     * 构造函数的泛型参数
     */
    public ConstructorHelper withGenericParamTypes(@NonNull Type... genericParamTypes) {
        this.genericParamTypes = genericParamTypes;
        return this;
    }

    /**
     * 构造函数抛出的异常
     * @noinspection unchecked
     */
    public ConstructorHelper withExceptionTypes(@NonNull Class<? extends Throwable>... exceptionTypes) {
        this.exceptionTypes = exceptionTypes;
        return this;
    }

    /**
     * 是否查找 Super 类
     */
    public ConstructorHelper withSuper(boolean withSuper) {
        this.withSuper = withSuper;
        return this;
    }

    /**
     * 获取查找到的对象，如果查找结果为空或不为单个则抛错
     */
    public HookHelper<Constructor<?>> single() {
        List<Constructor<?>> constructors = matches();
        if (constructors.isEmpty())
            throw new NonSingletonException("[ConstructorHelper]: No result found for query!");
        if (constructors.size() > 1)
            throw new NonSingletonException("[ConstructorHelper]: Query did not return a unique result: " + constructors.size());

        constructorCache = constructors.get(0);
        return new HookHelper<>(constructorCache);
    }

    /**
     * 获取查找到的对象，如果查找结果为空或不为单个则返回 null
     */
    @Nullable
    public HookHelper<Constructor<?>> singleOrNull() {
        List<Constructor<?>> constructors = matches();
        if (constructors.size() == 1) {
            constructorCache = constructors.get(0);
            return new HookHelper<>(constructorCache);
        }

        return null;
    }

    /**
     * 获取查找到的对象，如果查找结果为空或不为单个则抛错
     */
    public HookHelper<Constructor<?>> singleOrThrow(@NonNull Supplier<NonSingletonException> throwableSupplier) {
        List<Constructor<?>> constructors = matches();
        if (constructors.size() != 1)
            throw throwableSupplier.get();

        constructorCache = constructors.get(0);
        return new HookHelper<>(constructorCache);
    }

    /**
     * 核心过滤逻辑
     *
     * @noinspection RedundantIfStatement
     */
    private List<Constructor<?>> matches() {
        if (constructorCache != null) {
            throw new UnexpectedException("[ConstructorHelper]: Do not reuse!");
        }

        List<Constructor<?>> constructors = new ArrayList<>(Arrays.asList(clazz.getDeclaredConstructors()));
        if (withSuper) {
            Class<?> sup = clazz.getSuperclass();
            while (sup != null) {
                constructors.addAll(Arrays.asList(sup.getDeclaredConstructors()));
                sup = sup.getSuperclass();
            }
        }

        return constructors.stream()
            .filter(constructor -> {
                if (paramTypes != null) {
                    if (paramTypes.length != constructor.getParameterCount()) {
                        return false;
                    } else {
                        for (int i = 0; i < paramTypes.length; i++) {
                            Class<?> want = paramTypes[i];
                            Class<?> actual = constructor.getParameterTypes()[i];
                            if (Objects.equals(want, Any.class)) continue;
                            if (!Objects.equals(want, actual)) {
                                return false;
                            }
                        }
                    }
                }

                if (paramCount != -1 && constructor.getParameterCount() != paramCount)
                    return false;
                if (!paramCountVarMap.isEmpty()) {
                    if (paramCountVarMap.containsKey(EQ)) {
                        if (!Objects.equals(constructor.getParameterCount(), paramCountVarMap.get(EQ)))
                            return false;
                    } else {
                        for (int mode : paramCountVarMap.keySet()) {
                            Integer count = paramCountVarMap.get(mode);
                            if (count == null) return false;

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
                    }
                }
                if (mods != -1 && (constructor.getModifiers() & mods) == 0)
                    return false;
                if (annotation != null && !constructor.isAnnotationPresent(annotation))
                    return false;
                if (genericParamTypes != null && !Arrays.equals(constructor.getGenericParameterTypes(), genericParamTypes))
                    return false;
                if (exceptionTypes != null && !Arrays.equals(constructor.getExceptionTypes(), exceptionTypes))
                    return false;

                return true;
            })
            .collect(Collectors.toList());
    }
}
