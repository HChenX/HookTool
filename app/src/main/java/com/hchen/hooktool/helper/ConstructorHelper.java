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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.exception.NonSingletonException;
import com.hchen.hooktool.exception.UnexpectedException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
    private Class<? extends Throwable> exceptionType = null;
    private boolean withSuper = false;
    private Constructor<?> constructorCache = null;

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
     */
    public ConstructorHelper withExceptionType(@NonNull Class<? extends Throwable> exceptionType) {
        this.exceptionType = exceptionType;
        return this;
    }

    /**
     * 是否查找 Super 类
     */
    public ConstructorHelper withSuper(boolean withSuper) {
        this.withSuper = withSuper;
        return this;
    }

    public Constructor<?> single() {
        List<Constructor<?>> constructors = matches();
        if (constructors.isEmpty())
            throw new NonSingletonException("[ConstructorHelper]: No result found for query!");
        if (constructors.size() > 1)
            throw new NonSingletonException("[ConstructorHelper]: Query did not return a unique result: " + constructors.size());

        return constructorCache = constructors.get(0);
    }

    @Nullable
    public Constructor<?> singleOrNull() {
        List<Constructor<?>> constructors = matches();
        if (constructors.size() == 1)
            return constructorCache = constructors.get(0);

        return null;
    }

    public Constructor<?> singleOrThrow(Supplier<RuntimeException> throwableSupplier) {
        List<Constructor<?>> constructors = matches();
        if (constructors.size() != 1)
            throw throwableSupplier.get();

        return constructorCache = constructors.get(0);
    }

    /**
     * 核心过滤逻辑
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
                boolean matches = false;

                if (paramCount != -1) {
                    matches = (constructor.getParameterCount() == paramCount);
                }
                if (paramTypes != null) {
                    if (paramTypes.length != constructor.getParameterCount()) {
                        matches = false;
                    } else {
                        boolean equals = true;
                        for (int i = 0; i < paramTypes.length; i++) {
                            Class<?> want = paramTypes[i];
                            Class<?> actual = constructor.getParameterTypes()[i];
                            if (Objects.equals(want, Any.class)) continue;
                            if (!Objects.equals(want, actual)) {
                                equals = false;
                                break;
                            }
                        }
                        matches = equals;
                    }
                }
                if (mods != -1) {
                    matches = ((constructor.getModifiers() & mods) != 0);
                }
                if (annotation != null) {
                    matches = constructor.isAnnotationPresent(annotation);
                }
                if (genericParamTypes != null) {
                    matches = Arrays.equals(constructor.getGenericParameterTypes(), genericParamTypes);
                }
                if (exceptionType != null) {
                    matches = Arrays.asList(constructor.getExceptionTypes()).contains(exceptionType);
                }
                return matches;
            })
            .collect(Collectors.toList());
    }
}
