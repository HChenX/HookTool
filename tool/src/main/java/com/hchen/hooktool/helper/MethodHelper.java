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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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
    private int paramCountFinal = -1;
    private Class<?>[] paramTypes = null;
    private Class<?> returnType = null;
    private Class<?> superReturnType = null;
    private int mods = -1;
    private Class<? extends Annotation> annotation = null;
    private Type genericReturnType = null;
    private Type[] genericParamTypes = null;
    private Class<? extends Throwable>[] exceptionTypes = null;
    private boolean withSuper = false;
    private Method methodCache = null;
    private final ConcurrentHashMap<Integer, Integer> paramCountVar = new ConcurrentHashMap<>();

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
    public MethodHelper withParamCountFinal(int paramCountFinal) {
        this.paramCountFinal = paramCountFinal;
        return this;
    }

    /**
     * 方法参数数量是否在某个范围内
     */
    public MethodHelper withParamCountVar(int paramCountVar, @RangeHelper.RangeModeFlag int mode) {
        this.paramCountVar.put(mode, paramCountVar);
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
     * @noinspection unchecked
     */
    public MethodHelper withExceptionType(@NonNull Class<? extends Throwable>... exceptionTypes) {
        this.exceptionTypes = exceptionTypes;
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
            throw new NonSingletonException("[MethodHelper]: No result found for query!");

        if (methods.size() > 1)
            throw new NonSingletonException("[MethodHelper]: Query did not return a unique result: " + methods.size());

        methodCache = methods.get(0);
        return new HookHelper<>(methodCache);
    }

    /**
     * 获取查找到的对象，如果查找结果为空或不为单个则返回 null
     */
    @Nullable
    public HookHelper<Method> singleOrNull() {
        List<Method> methods = matches();
        if (methods.isEmpty()) return null;
        if (methods.size() > 1) return null;

        methodCache = methods.get(0);
        return new HookHelper<>(methodCache);
    }

    /**
     * 获取查找到的对象，如果查找结果为空或不为单个则抛错
     */
    public HookHelper<Method> singleOrThrow(@NonNull Supplier<NonSingletonException> throwableSupplier) {
        List<Method> methods = matches();
        if (methods.isEmpty()) throw throwableSupplier.get();
        if (methods.size() > 1) throw throwableSupplier.get();

        methodCache = methods.get(0);
        return new HookHelper<>(methodCache);
    }

    /**
     * 核心过滤逻辑
     *
     * @noinspection RedundantIfStatement
     */
    private List<Method> matches() {
        if (methodCache != null) {
            throw new UnexpectedException("[MethodHelper]: Do not reuse!");
        }

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

            if (paramTypes != null) {
                if (paramTypes.length != method.getParameterCount()) {
                    return false;
                }
                for (int i = 0; i < method.getParameterCount(); i++) {
                    Class<?> actual = method.getParameterTypes()[i];
                    Class<?> want = paramTypes[i];
                    if (Objects.equals(want, Any.class)) continue;
                    if (!Objects.equals(actual, want)) {
                        return false;
                    }
                }
            }
            if (paramCountFinal != -1 && method.getParameterCount() != paramCountFinal)
                return false;
            if (!paramCountVar.isEmpty()) {
                if (paramCountVar.containsKey(EQ)) {
                    if (!Objects.equals(method.getParameterCount(), paramCountVar.get(EQ)))
                        return false;
                } else {
                    for (int mode : paramCountVar.keySet()) {
                        Integer count = paramCountVar.get(mode);
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
            if (returnType != null && !Objects.equals(method.getReturnType(), returnType))
                return false;
            if (superReturnType != null && !superReturnType.isAssignableFrom(method.getReturnType()))
                return false;
            if (mods != -1 && (method.getModifiers() & mods) != mods)
                return false;
            if (annotation != null && !method.isAnnotationPresent(annotation))
                return false;
            if (genericReturnType != null && !Objects.equals(method.getGenericReturnType(), genericReturnType))
                return false;
            if (genericParamTypes != null && !Arrays.equals(method.getGenericParameterTypes(), genericParamTypes))
                return false;
            if (exceptionTypes != null && !Arrays.asList(method.getExceptionTypes()).contains(exceptionTypes))
                return false;

            return true;
        }).collect(Collectors.toList());
    }
}
