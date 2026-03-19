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

import android.annotation.SuppressLint;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.exception.NonSingletonException;
import com.hchen.hooktool.exception.UnexpectedException;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexFile;

/**
 * 类查找工具
 * 用于根据各种条件查找和筛选类
 *
 * @author 焕晨HChen
 * @noinspection SequencedCollectionMethodCanBeUsed, unused
 */
public class ClassHelper {
    private final ClassLoader loader;
    // ----------- Name ------------
    private String className;
    private String substring;
    private Pattern pattern;
    // ---------- package -----------
    private String packagePath;
    // ----------- field -------------
    private Class<?>[] fieldClasses;
    private String[] fieldNames;
    private int expectedFieldCount = -1;
    // ------------ method ----------------
    private String[] methodNames;
    private int expectedMethodCount = -1;
    // ------------- constructor ---------------
    private Class<?>[] constructorClasses;
    private int expectedConstructorCount = -1;
    // -------------- other --------------------
    private Class<? extends Annotation>[] annotations;
    private Class<?>[] interfaceClasses;
    private Class<?> superClass;

    private boolean cacheBuilt = false;
    private List<String> classPathsCache;

    public ClassHelper(ClassLoader loader) {
        if (loader == null) this.loader = ClassLoader.getSystemClassLoader();
        else this.loader = loader;
    }

    /**
     * 设置要查找的类名
     */
    public ClassHelper withClassName(@NonNull String className) {
        Objects.requireNonNull(className, "ClassName must not be null.");
        this.className = className;
        return this;
    }

    /**
     * 设置类名中要包含的子字符串
     */
    public ClassHelper withSubstring(@NonNull String substring) {
        Objects.requireNonNull(substring, "Substring must not be null.");
        this.substring = substring;
        return this;
    }

    /**
     * 设置用于匹配类名的正则表达式
     */
    public ClassHelper withPattern(@NonNull Pattern pattern) {
        Objects.requireNonNull(pattern, "Pattern must not be null.");
        this.pattern = pattern;
        return this;
    }

    /**
     * 设置要查找的包路径
     */
    public ClassHelper withPackage(@NonNull String packagePath) {
        Objects.requireNonNull(packagePath, "PackagePath must not be null.");
        this.packagePath = packagePath;
        return this;
    }

    /**
     * 设置要查找的字段类型
     */
    public ClassHelper withFieldClasses(@NonNull Class<?>... fieldClasses) {
        Objects.requireNonNull(fieldClasses, "FieldClasses must not be null.");
        this.fieldClasses = fieldClasses;
        return this;
    }

    /**
     * 设置要查找的字段名称
     */
    public ClassHelper withFieldNames(@NonNull String... fieldNames) {
        Objects.requireNonNull(fieldNames, "FieldNames must not be null.");
        this.fieldNames = fieldNames;
        return this;
    }

    /**
     * 设置要查找的字段数量
     */
    public ClassHelper withFieldCount(int count) {
        this.expectedFieldCount = count;
        return this;
    }

    /**
     * 设置要查找的方法名称
     */
    public ClassHelper withMethodNames(@NonNull String... methodNames) {
        Objects.requireNonNull(methodNames, "MethodNames must not be null.");
        this.methodNames = methodNames;
        return this;
    }

    /**
     * 设置要查找的方法数量
     */
    public ClassHelper withMethodCount(int count) {
        this.expectedMethodCount = count;
        return this;
    }

    /**
     * 设置要查找的构造函数参数类型
     */
    public ClassHelper withConstructorClasses(@NonNull Class<?>... classes) {
        Objects.requireNonNull(classes, "Classes must not be null.");
        this.constructorClasses = classes;
        return this;
    }

    /**
     * 设置要查找的构造函数数量
     */
    public ClassHelper withConstructorCount(int count) {
        this.expectedConstructorCount = count;
        return this;
    }

    /**
     * 设置要查找的注解
     */
    @SuppressWarnings("unchecked")
    public ClassHelper withAnnotations(@NonNull Class<? extends Annotation>... annotations) {
        Objects.requireNonNull(annotations, "Annotations must not be null.");
        this.annotations = annotations;
        return this;
    }

    /**
     * 设置要查找的接口
     */
    public ClassHelper withInterfaces(@NonNull Class<?>... interfaceClasses) {
        Objects.requireNonNull(interfaceClasses, "InterfaceClasses must not be null.");
        this.interfaceClasses = interfaceClasses;
        return this;
    }

    /**
     * 设置要查找的父类
     */
    public ClassHelper withSuperClass(@NonNull Class<?> superClass) {
        Objects.requireNonNull(superClass, "SuperClass must not be null.");
        this.superClass = superClass;
        return this;
    }

    /**
     * 查找并返回唯一匹配的类
     * <p>
     * 如果没有找到匹配的类或找到多个匹配的类，则抛出异常
     */
    public Class<?> single() {
        List<Class<?>> list = matches();
        if (list.isEmpty())
            throw new NonSingletonException("No result found for query.");
        if (list.size() > 1)
            throw new NonSingletonException("Query did not return a unique result: " + list.size());
        return list.get(0);
    }

    /**
     * 查找并返回唯一匹配的类
     * <p>
     * 如果没有找到匹配的类或找到多个匹配的类，则返回 null
     */
    @Nullable
    public Class<?> singleOrNull() {
        List<Class<?>> list = matches();
        if (list.size() != 1) return null;
        return list.get(0);
    }

    /**
     * 查找并返回唯一匹配的类
     * <p>
     * 如果没有找到匹配的类或找到多个匹配的类，则抛出由供应商提供的异常
     */
    public Class<?> singleOrThrow(@NonNull Supplier<NonSingletonException> supplier) {
        Objects.requireNonNull(supplier, "Supplier must not be null.");
        List<Class<?>> list = matches();
        if (list.size() != 1) throw supplier.get();

        return list.get(0);
    }

    /**
     * 返回所有匹配的类
     */
    public Class<?>[] toArray() {
        return matches().toArray(new Class[0]);
    }

    /**
     * 重置查找器
     * <p>
     * 清除所有查找条件，但不会重置类路径的缓存列表
     */
    public void reset() {
        className = null;
        substring = null;
        pattern = null;
        packagePath = null;
        fieldNames = null;
        expectedFieldCount = -1;
        fieldClasses = null;
        methodNames = null;
        expectedMethodCount = -1;
        constructorClasses = null;
        expectedConstructorCount = -1;
        annotations = null;
        superClass = null;
        interfaceClasses = null;
    }

    /**
     * 查找所有匹配的类
     * <p>
     * 根据设置的条件筛选出符合要求的类
     */
    private List<Class<?>> matches() {
        List<String> paths = getAllClassPath();
        List<Class<?>> result = new ArrayList<>(paths.size());

        for (String path : paths) {
            if (!filterByPackage(path)) {
                continue;
            }

            Class<?> cls = loadClassSafely(path);
            if (cls == null) {
                continue;
            }

            if (filterByClassInfo(cls) &&
                filterByFields(cls) &&
                filterByMethods(cls) &&
                filterByAnnotations(cls) &&
                filterBySuperClass(cls) &&
                filterByInterfaces(cls) &&
                filterByConstructors(cls)) {
                result.add(cls);
            }
        }

        return result;
    }

    /**
     * 按包路径筛选
     */
    private boolean filterByPackage(String path) {
        return packagePath == null || path.startsWith(packagePath);
    }

    /**
     * 安全加载类
     */
    private Class<?> loadClassSafely(String path) {
        try {
            return loader.loadClass(path);
        } catch (Throwable ignore) {
            return null;
        }
    }

    /**
     * 按类信息筛选
     */
    private boolean filterByClassInfo(Class<?> cls) {
        if (className != null && !TextUtils.equals(cls.getSimpleName(), className)) {
            return false;
        }
        if (substring != null && !cls.getSimpleName().contains(substring)) {
            return false;
        }
        return pattern == null || pattern.matcher(cls.getSimpleName()).matches();
    }

    /**
     * 按字段相关条件筛选
     */
    private boolean filterByFields(Class<?> cls) {
        Field[] declaredFields = cls.getDeclaredFields();
        if (expectedFieldCount != -1 && declaredFields.length != expectedFieldCount) {
            return false;
        }
        if (fieldClasses != null) {
            boolean[] found = new boolean[fieldClasses.length];
            for (Field field : declaredFields) {
                Class<?> fieldType = field.getType();
                for (int i = 0; i < fieldClasses.length; i++) {
                    Class<?> c = fieldClasses[i];
                    if (c != null && c.equals(fieldType)) {
                        found[i] = true;
                    }
                }
            }
            for (boolean b : found) {
                if (!b) {
                    return false;
                }
            }
        }
        if (fieldNames != null) {
            boolean[] found = new boolean[fieldNames.length];
            for (Field field : declaredFields) {
                String fieldName = field.getName();
                for (int i = 0; i < fieldNames.length; i++) {
                    if (fieldNames[i].equals(fieldName)) {
                        found[i] = true;
                    }
                }
            }
            for (boolean b : found) {
                if (!b) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 按方法相关条件筛选
     */
    private boolean filterByMethods(Class<?> cls) {
        Method[] declaredMethods = cls.getDeclaredMethods();
        if (expectedMethodCount != -1 && declaredMethods.length != expectedMethodCount) {
            return false;
        }
        if (methodNames != null) {
            boolean[] found = new boolean[methodNames.length];
            for (Method method : declaredMethods) {
                String methodName = method.getName();
                for (int i = 0; i < methodNames.length; i++) {
                    if (methodNames[i].equals(methodName)) {
                        found[i] = true;
                    }
                }
            }
            for (boolean b : found) {
                if (!b) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 按注解筛选
     */
    private boolean filterByAnnotations(Class<?> cls) {
        return annotations == null || Arrays.stream(annotations).allMatch(cls::isAnnotationPresent);
    }

    /**
     * 按父类筛选
     */
    private boolean filterBySuperClass(Class<?> cls) {
        return superClass == null || superClass.isAssignableFrom(cls);
    }

    /**
     * 按接口筛选
     */
    private boolean filterByInterfaces(Class<?> cls) {
        if (interfaceClasses == null) {
            return true;
        }
        List<Class<?>> list = Arrays.asList(cls.getInterfaces());
        return Arrays.stream(interfaceClasses).allMatch(list::contains);
    }

    /**
     * 按构造函数相关条件筛选
     */
    private boolean filterByConstructors(Class<?> cls) {
        if (expectedConstructorCount != -1 && cls.getDeclaredConstructors().length != expectedConstructorCount) {
            return false;
        }
        if (constructorClasses != null) {
            boolean foundMatchingConstructor = false;
            Constructor<?>[] constructors = cls.getDeclaredConstructors();
            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterCount() != constructorClasses.length) {
                    continue;
                }

                boolean currentConstructorMatches = true;
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                for (int i = 0; i < parameterTypes.length; i++) {
                    Class<?> want = constructorClasses[i];
                    if (Objects.equals(want, null)) continue;
                    if (!Objects.equals(parameterTypes[i], want)) {
                        currentConstructorMatches = false;
                        break;
                    }
                }

                if (currentConstructorMatches) {
                    foundMatchingConstructor = true;
                    break;
                }
            }
            return foundMatchingConstructor;
        }
        return true;
    }

    /**
     * 获取所有类路径
     */
    private List<String> getAllClassPath() {
        if (cacheBuilt) return classPathsCache;

        List<String> paths = new ArrayList<>();
        try {
            if (loader instanceof BaseDexClassLoader) {
                @SuppressWarnings("JavaReflectionMemberAccess")
                @SuppressLint("DiscouragedPrivateApi")
                Field pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
                pathListField.setAccessible(true);
                Object pathList = pathListField.get(loader);
                if (pathList == null) {
                    return paths;
                }

                Field dexElementsField = pathList.getClass().getDeclaredField("dexElements");
                dexElementsField.setAccessible(true);
                Object[] dexElements = (Object[]) dexElementsField.get(pathList);
                if (dexElements == null) {
                    return paths;
                }

                for (Object element : dexElements) {
                    DexFile dex = getDexFileFromElement(element);
                    addDexEntriesToPaths(dex, paths);
                }
            }
        } catch (Throwable e) {
            throw new UnexpectedException(e);
        }

        cacheBuilt = true;
        classPathsCache = paths;
        return paths;
    }

    /**
     * 从 dexElements 中的元素获取 DexFile
     */
    private DexFile getDexFileFromElement(Object element) throws Exception {
        Field dexFileField = element.getClass().getDeclaredField("dexFile");
        dexFileField.setAccessible(true);
        DexFile dex = (DexFile) dexFileField.get(element);

        if (dex == null) {
            Field pathField = element.getClass().getDeclaredField("path");
            pathField.setAccessible(true);
            File apk = (File) pathField.get(element);
            dex = new DexFile(apk);
        }

        return dex;
    }

    /**
     * 将 DexFile 中的所有类名添加到 paths 列表中
     */
    private void addDexEntriesToPaths(DexFile dex, List<String> paths) {
        for (Enumeration<String> iter = dex.entries(); iter.hasMoreElements(); ) {
            paths.add(iter.nextElement());
        }
    }
}
