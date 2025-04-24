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
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 方法查找
 *
 * @author 焕晨HChen
 */
public class MethodHelper {
    private final Class<?> clazz;
    private String methodName = null;
    private String substring = null;
    private Pattern pattern = null;
    private int paramCount = -1;
    private Class<?>[] paramTypes = null;
    private Class<?> returnType = null;
    private Class<?> superReturnType = null;
    private int mods = -1;
    private Class<? extends Annotation> annotation = null;
    private Type genericReturnType = null;
    private Type[] genericParamTypes = null;
    private Class<? extends Throwable> exceptionType = null;
    private boolean withSuper = false;

    public MethodHelper(Class<?> clazz) {
        Objects.requireNonNull(clazz, "[MethodHelper]: class must not is null!");
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
     * 方法参数数量
     */
    public MethodHelper withParamCount(int paramCount) {
        this.paramCount = paramCount;
        return this;
    }

    /**
     * 方法参数类型，可使用 Any.class 占位，表示任意类型
     */
    public MethodHelper withParamTypes(@NonNull Class<?>... paramTypes) {
        this.paramTypes = paramTypes;
        return this;
    }

    /**
     * 方法返回类型
     */
    public MethodHelper withReturnType(@NonNull Class<?> returnType) {
        this.returnType = returnType;
        return this;
    }

    /**
     * 方法返回类型的超类
     */
    public MethodHelper withSuperReturnType(@NonNull Class<?> superReturnType) {
        this.superReturnType = superReturnType;
        return this;
    }

    /**
     * 方法的修饰符
     */
    public MethodHelper withMods(int mods) {
        this.mods = mods;
        return this;
    }

    /**
     * 方法的注解
     */
    public MethodHelper withAnnotation(@NonNull Class<? extends Annotation> annotation) {
        this.annotation = annotation;
        return this;
    }

    /**
     * 方法的泛型返回类型
     */
    public MethodHelper withGenericReturnType(@NonNull Type genericReturnType) {
        this.genericReturnType = genericReturnType;
        return this;
    }

    /**
     * 方法的泛型参数
     */
    public MethodHelper withGenericParamTypes(@NonNull Type... genericParamTypes) {
        this.genericParamTypes = genericParamTypes;
        return this;
    }

    /**
     * 方法抛出的异常类型
     */
    public MethodHelper withExceptionType(@NonNull Class<? extends Throwable> exceptionType) {
        this.exceptionType = exceptionType;
        return this;
    }

    /**
     * 是否查找 Super 类的方法
     */
    public MethodHelper withSuper(boolean withSuper) {
        this.withSuper = withSuper;
        return this;
    }

    public Method single() {
        List<Method> methods = matches();
        if (methods.isEmpty())
            throw new NonSingletonException("[MethodHelper]: No result found for query!");

        if (methods.size() > 1)
            throw new NonSingletonException("[MethodHelper]: Query did not return a unique result: " + methods.size());

        return methods.get(0);
    }

    @Nullable
    public Method singleOrNull() {
        List<Method> methods = matches();
        if (methods.isEmpty()) return null;
        if (methods.size() > 1) return null;

        return methods.get(0);
    }

    public Method singleOrThrow(Supplier<RuntimeException> throwableSupplier) {
        List<Method> methods = matches();
        if (methods.isEmpty()) throw throwableSupplier.get();
        if (methods.size() > 1) throw throwableSupplier.get();

        return methods.get(0);
    }

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
            boolean matches = false;
            if (methodName != null) {
                matches = Objects.equals(methodName, method.getName());
            } else if (pattern != null) {
                matches = pattern.matcher(method.getName()).matches();
            } else if (substring != null) {
                matches = method.getName().contains(substring);
            }
            if (paramCount != -1) {
                matches = method.getParameterCount() == paramCount;
            }
            if (paramTypes != null) {
                if (paramTypes.length == method.getParameterCount()) {
                    boolean equals = true;
                    for (int i = 0; i < method.getParameterCount(); i++) {
                        Class<?> actual = method.getParameterTypes()[i];
                        Class<?> want = paramTypes[i];
                        if (Objects.equals(want, Any.class)) continue;
                        if (!Objects.equals(actual, want)) {
                            equals = false;
                            break;
                        }
                    }
                    matches = equals;
                } else matches = false;
            }
            if (returnType != null) {
                matches = Objects.equals(method.getReturnType(), returnType);
            }
            if (superReturnType != null) {
                matches = superReturnType.isAssignableFrom(method.getReturnType());
            }
            if (mods != -1) {
                matches = (method.getModifiers() & mods) != 0;
            }
            if (annotation != null) {
                matches = method.isAnnotationPresent(annotation);
            }
            if (genericReturnType != null) {
                matches = Objects.equals(method.getGenericReturnType(), genericReturnType);
            }
            if (genericParamTypes != null) {
                matches = Arrays.equals(method.getGenericParameterTypes(), genericParamTypes);
            }
            if (exceptionType != null) {
                matches = Arrays.asList(method.getExceptionTypes()).contains(exceptionType);
            }
            return matches;
        }).collect(Collectors.toList());
    }
}
