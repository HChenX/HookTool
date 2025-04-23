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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ConstructorHelper {
    private final Class<?> clazz;
    private int paramCount = -1;
    private Class<?>[] paramTypes = null;
    private int mods = -1;
    private Class<? extends Annotation> annotation = null;
    private Type[] genericParamTypes = null;
    private Class<? extends Throwable> exceptionType = null;
    private boolean withSuper = false;

    public ConstructorHelper(Class<?> clazz) {
        Objects.requireNonNull(clazz, "[ConstructorHelper]: class must not is null!");
        this.clazz = clazz;
    }

    public ConstructorHelper withParamCount(int paramCount) {
        this.paramCount = paramCount;
        return this;
    }

    public ConstructorHelper withParamTypes(@NonNull Class<?>... paramTypes) {
        this.paramTypes = paramTypes;
        return this;
    }

    public ConstructorHelper withMods(int mods) {
        this.mods = mods;
        return this;
    }

    public ConstructorHelper withAnnotation(@NonNull Class<? extends Annotation> annotation) {
        this.annotation = annotation;
        return this;
    }

    public ConstructorHelper withGenericParamTypes(@NonNull Type... genericParamTypes) {
        this.genericParamTypes = genericParamTypes;
        return this;
    }

    public ConstructorHelper withExceptionType(@NonNull Class<? extends Throwable> exceptionType) {
        this.exceptionType = exceptionType;
        return this;
    }

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

        return constructors.get(0);
    }

    @Nullable
    public Constructor<?> singleOrNull() {
        List<Constructor<?>> constructors = matches();
        if (constructors.size() == 1)
            return constructors.get(0);

        return null;
    }

    public Constructor<?> singleOrThrow(Supplier<RuntimeException> throwableSupplier) {
        List<Constructor<?>> constructors = matches();
        if (constructors.size() != 1)
            throw throwableSupplier.get();

        return constructors.get(0);
    }

    /**
     * 核心过滤逻辑
     */
    private List<Constructor<?>> matches() {
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
