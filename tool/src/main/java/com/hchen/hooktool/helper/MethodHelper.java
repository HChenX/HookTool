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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

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
        Objects.requireNonNull(clazz, "Class must not be null.");
        this.clazz = clazz;
    }

    /**
     * 方法名
     */
    public MethodHelper withMethodName(@NonNull String methodName) {
        Objects.requireNonNull(methodName, "MethodName must not be null.");
        this.methodName = methodName;
        return this;
    }

    /**
     * 方法名包含的子串
     */
    public MethodHelper withSubstring(@NonNull String substring) {
        Objects.requireNonNull(substring, "Substring must not be null.");
        this.substring = substring;
        return this;
    }

    /**
     * 匹配方法名
     */
    public MethodHelper withPattern(@NonNull Pattern pattern) {
        Objects.requireNonNull(pattern, "Pattern must not be null.");
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
     * 方法参数类型，可使用 null 占位，表示任意类型
     */
    public MethodHelper withParamClasses(@NonNull Class<?>... paramClasses) {
        Objects.requireNonNull(paramClasses, "ParamClasses must not be null.");
        this.paramClasses = paramClasses;
        return this;
    }

    /**
     * 方法返回类型
     */
    public MethodHelper withReturnClass(@NonNull Class<?> returnClass) {
        Objects.requireNonNull(returnClass, "ReturnClass must not be null.");
        this.returnClass = returnClass;
        return this;
    }

    /**
     * 方法的修饰符
     */
    public MethodHelper withMods(@NonNull int... modsFlags) {
        Objects.requireNonNull(modsFlags, "ModsFlags must not be null.");
        int combined = 0;
        for (int f : modsFlags) {
            combined |= f;
        }
        this.mods = combined;
        return this;
    }

    /**
     * 设置方法为 public 修饰符
     */
    public MethodHelper withPublic() {
        return withMods(Modifier.PUBLIC);
    }

    /**
     * 设置方法为 private 修饰符
     */
    public MethodHelper withPrivate() {
        return withMods(Modifier.PRIVATE);
    }

    /**
     * 设置方法为 protected 修饰符
     */
    public MethodHelper withProtected() {
        return withMods(Modifier.PROTECTED);
    }

    /**
     * 设置方法为 static 修饰符
     */
    public MethodHelper withStatic() {
        return withMods(Modifier.STATIC);
    }

    /**
     * 设置方法为 synchronized 修饰符
     */
    public MethodHelper withSynchronized() {
        return withMods(Modifier.SYNCHRONIZED);
    }

    /**
     * 设置方法为 native 修饰符
     */
    public MethodHelper withNative() {
        return withMods(Modifier.NATIVE);
    }

    /**
     * 设置方法为 abstract 修饰符
     */
    public MethodHelper withAbstract() {
        return withMods(Modifier.ABSTRACT);
    }

    /**
     * 方法的注解
     */
    @SuppressWarnings("unchecked")
    public MethodHelper withAnnotations(@NonNull Class<? extends Annotation>... annotations) {
        Objects.requireNonNull(annotations, "Annotations must not be null.");
        this.annotations = annotations;
        return this;
    }

    /**
     * 方法抛出的异常类型
     *
     * @noinspection unchecked
     */
    public MethodHelper withExceptionClasses(@NonNull Class<? extends Throwable>... exceptionClasses) {
        Objects.requireNonNull(exceptionClasses, "ExceptionClasses must not be null.");
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
     * 获取查找到的方法，如果查找结果为空或不为单个则抛错
     */
    public Method single() {
        List<Method> methods = matches();
        if (methods.isEmpty())
            throw new NonSingletonException("No result found for query.");

        if (methods.size() > 1)
            throw new NonSingletonException("Query did not return a unique result: " + methods.size());

        return methods.get(0);
    }

    /**
     * 获取查找到的方法，如果查找结果为空或不为单个则返回 null
     */
    @Nullable
    public Method singleOrNull() {
        List<Method> methods = matches();
        if (methods.isEmpty()) return null;
        if (methods.size() > 1) return null;

        return methods.get(0);
    }

    /**
     * 获取查找到的方法，如果查找结果为空或不为单个则抛错
     */
    public Method singleOrThrow(@NonNull Supplier<NonSingletonException> throwableSupplier) {
        List<Method> methods = matches();
        if (methods.isEmpty()) throw throwableSupplier.get();
        if (methods.size() > 1) throw throwableSupplier.get();

        return methods.get(0);
    }

    /**
     * 返回查找到的所有方法
     */
    public Method[] toArray() {
        return matches().toArray(new Method[0]);
    }

    /**
     * 重置查找器，清除所有设置的条件
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
     */
    private List<Method> matches() {
        Method[] declaredMethods = clazz.getDeclaredMethods();
        ArrayList<Method> methods = new ArrayList<>(Arrays.asList(declaredMethods));
        
        if (withSuper) {
            addSuperClassMethods(methods);
        }

        ArrayList<Method> result = new ArrayList<>(methods.size());
        for (Method method : methods) {
            if (matchesMethodName(method) &&
                matchesParamClasses(method) &&
                matchesParamCount(method) &&
                matchesModifiers(method) &&
                matchesReturnType(method) &&
                matchesAnnotations(method) &&
                matchesExceptions(method)) {
                result.add(method);
            }
        }

        return result;
    }

    /**
     * 添加父类方法
     */
    private void addSuperClassMethods(ArrayList<Method> methods) {
        Class<?> clazzWithSuper = clazz.getSuperclass();
        while (clazzWithSuper != null) {
            methods.addAll(Arrays.asList(clazzWithSuper.getDeclaredMethods()));
            clazzWithSuper = clazzWithSuper.getSuperclass();
        }
    }

    /**
     * 匹配方法名
     */
    private boolean matchesMethodName(Method method) {
        if (methodName != null && !methodName.equals(method.getName())) {
            return false;
        }
        if (pattern != null && !pattern.matcher(method.getName()).matches()) {
            return false;
        }
        return substring == null || method.getName().contains(substring);
    }

    /**
     * 匹配参数类型
     */
    private boolean matchesParamClasses(Method method) {
        if (paramClasses == null) {
            return true;
        }

        if (paramClasses.length != method.getParameterCount()) {
            return false;
        }

        for (int i = 0; i < paramClasses.length; i++) {
            Class<?> want = paramClasses[i];
            Class<?> actual = method.getParameterTypes()[i];
            if (want != null && !want.equals(actual)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 匹配参数数量
     */
    private boolean matchesParamCount(Method method) {
        if (paramCountVarMap.isEmpty()) {
            return true;
        }

        if (paramCountVarMap.containsKey(EQ)) {
            // noinspection DataFlowIssue
            return method.getParameterCount() == paramCountVarMap.get(EQ);
        }

        for (int mode : paramCountVarMap.keySet()) {
            Integer count = paramCountVarMap.get(mode);
            if (count == null) {
                return false;
            }

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

        return true;
    }

    /**
     * 匹配修饰符
     */
    private boolean matchesModifiers(Method method) {
        return mods == -1 || (method.getModifiers() & mods) == mods;
    }

    /**
     * 匹配返回类型
     */
    private boolean matchesReturnType(Method method) {
        if (returnClass == null) {
            return true;
        }

        Class<?> methodReturnType = method.getReturnType();
        return returnClass.equals(methodReturnType) || returnClass.isAssignableFrom(methodReturnType);
    }

    /**
     * 匹配注解
     */
    private boolean matchesAnnotations(Method method) {
        return annotations == null || Arrays.stream(annotations).allMatch(method::isAnnotationPresent);
    }

    /**
     * 匹配异常
     */
    private boolean matchesExceptions(Method method) {
        if (exceptionClasses == null) {
            return true;
        }

        List<Class<?>> declaredExceptions = Arrays.asList(method.getExceptionTypes());
        return Arrays.stream(exceptionClasses).allMatch(declaredExceptions::contains);
    }
}
