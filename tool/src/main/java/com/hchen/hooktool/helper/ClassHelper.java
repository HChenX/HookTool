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

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.exception.NonSingletonException;
import com.hchen.hooktool.exception.UnexpectedException;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexFile;

/**
 * 类查找
 *
 * @author 焕晨HChen
 */
public class ClassHelper {
    private final ClassLoader loader;
    private String className = null;
    private String substring = null;
    private Pattern pattern = null;
    private String packagePath = null;
    private String[] fieldNames = null;
    private Class<?>[] fieldTypes = null;
    private String[] methodNames = null;
    private Class<? extends Annotation> annotation = null;
    private Class<?> superClass = null;
    private Class<?>[] interfaceClasses = null;
    private boolean includeAndroidClasses = false;
    private boolean cacheBuilt = false;
    private Set<String> classPathsCache = null;

    public ClassHelper(ClassLoader loader) {
        if (loader == null) this.loader = ClassLoader.getSystemClassLoader();
        else this.loader = loader;
    }

    /**
     * 查找指定类名
     */
    public ClassHelper withClassName(@NonNull String className) {
        this.className = className;
        return this;
    }

    /**
     * 类名包含的字段
     */
    public ClassHelper withSubstring(@NonNull String substring) {
        this.substring = substring;
        return this;
    }

    /**
     * 正则匹配查找类名
     */
    public ClassHelper withPattern(@NonNull Pattern pattern) {
        this.pattern = pattern;
        return this;
    }

    /**
     * 查找指定包内的类
     */
    public ClassHelper withPackage(@NonNull String packagePath) {
        this.packagePath = packagePath;
        return this;
    }

    /**
     * 查找包含指定字段集的类
     */
    public ClassHelper withFieldNames(@NonNull String... fieldNames) {
        this.fieldNames = fieldNames;
        return this;
    }

    /**
     * 查找包含指定字段集的类
     */
    public ClassHelper withFieldTypes(@NonNull Class<?>... fieldTypes) {
        this.fieldTypes = fieldTypes;
        return this;
    }

    /**
     * 查找包含指定方法集的类
     */
    public ClassHelper withMethodNames(@NonNull String... methodNames) {
        this.methodNames = methodNames;
        return this;
    }

    /**
     * 查找使用了指定注解的类
     */
    public ClassHelper withAnnotation(@NonNull Class<? extends Annotation> annotation) {
        this.annotation = annotation;
        return this;
    }

    /**
     * 查找继承了指定类的类
     */
    public ClassHelper withSuperClass(@NonNull Class<?> superClass) {
        this.superClass = superClass;
        return this;
    }

    /**
     * 查找实现了指定接口的类
     */
    public ClassHelper withInterfaces(@NonNull Class<?>... interfaceClasses) {
        this.interfaceClasses = interfaceClasses;
        return this;
    }

    /**
     * 包含 Android 系统类
     */
    public ClassHelper includeAndroidClasses(boolean includeAndroidClasses) {
        this.includeAndroidClasses = includeAndroidClasses;
        return this;
    }

    /**
     * 查找并返回唯一匹配，否则抛出异常
     */
    public Class<?> single() {
        List<Class<?>> list = matches();
        if (list.isEmpty())
            throw new NonSingletonException("[ClassHelper]: No result found for query!");
        if (list.size() > 1)
            throw new NonSingletonException("[ClassHelper]: Query did not return a unique result: " + list.size());
        return list.get(0);
    }

    /**
     * 查找并返回唯一匹配，否则返回 null
     */
    @Nullable
    public Class<?> singleOrNull() {
        List<Class<?>> list = matches();
        if (list.size() != 1) return null;
        return list.get(0);
    }

    /**
     * 获取查找到的对象，如果查找结果为空或不为单个则抛错
     */
    public Class<?> singleOrThrow(@NonNull Supplier<NonSingletonException> supplier) {
        List<Class<?>> list = matches();
        if (list.size() != 1) throw supplier.get();

        return list.get(0);
    }

    /**
     * 返回查找到的全部对象
     */
    public Class<?>[] list() {
        return matches().toArray(new Class[0]);
    }

    /**
     * 查找所有匹配
     */
    private List<Class<?>> matches() {
        Set<String> names = buildCache();
        return names.stream().filter(path -> {
            try {
                Class<?> cls = loader.loadClass(path);
                if (!includeAndroidClasses && path.startsWith("andorid.")) return false;
                if (className != null && !Objects.equals(cls.getSimpleName(), className))
                    return false;
                else if (substring != null && !cls.getSimpleName().contains(substring))
                    return false;
                else if (pattern != null && !pattern.matcher(cls.getSimpleName()).matches())
                    return false;
                if (packagePath != null && cls.getPackage() != null && !cls.getPackage().getName().startsWith(packagePath))
                    return false;
                if (fieldNames != null) {
                    Set<String> fieldNameSet = Arrays.stream(cls.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
                    if (!Arrays.stream(fieldNames).allMatch(fieldNameSet::contains))
                        return false;
                } else if (fieldTypes != null) {
                    Set<Class<?>> fieldTypeSet = Arrays.stream(cls.getDeclaredFields()).map(Field::getType).collect(Collectors.toSet());
                    if (!Arrays.stream(fieldTypes).allMatch(fieldTypeSet::contains))
                        return false;
                }
                if (methodNames != null) {
                    Set<String> methodNameSet = Arrays.stream(cls.getDeclaredMethods()).map(Method::getName).collect(Collectors.toSet());
                    if (!Arrays.stream(methodNames).allMatch(methodNameSet::contains))
                        return false;
                }
                if (annotation != null && !cls.isAnnotationPresent(annotation)) return false;
                if (superClass != null && !superClass.isAssignableFrom(cls)) return false;
                if (interfaceClasses != null && !Arrays.equals(cls.getInterfaces(), interfaceClasses))
                    return false;
            } catch (Throwable ignore) {
                return false;
            }
            return true;
        }).map((Function<String, Class<?>>) path -> {
            try {
                return loader.loadClass(path);
            } catch (ClassNotFoundException e) {
                throw new UnexpectedException(e);
            }
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 构建类名缓存，仅执行一次
     */
    private Set<String> buildCache() {
        if (cacheBuilt) return classPathsCache;
        Set<String> paths = new HashSet<>();
        // Android Dex
        try {
            if (loader instanceof BaseDexClassLoader) {
                @SuppressLint("DiscouragedPrivateApi")
                Field pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
                pathListField.setAccessible(true);
                Object pathList = pathListField.get(loader);
                assert pathList != null;
                Field dexElementsField = pathList.getClass().getDeclaredField("dexElements");
                dexElementsField.setAccessible(true);
                Object[] dexElements = (Object[]) dexElementsField.get(pathList);
                assert dexElements != null;
                for (Object element : dexElements) {
                    DexFile dex;
                    Field dexFileField = element.getClass().getDeclaredField("dexFile");
                    dexFileField.setAccessible(true);
                    dex = (DexFile) dexFileField.get(element);
                    if (dex == null) {
                        Field pathField = element.getClass().getDeclaredField("path");
                        pathField.setAccessible(true);
                        File apk = (File) pathField.get(element);
                        dex = new DexFile(apk);
                    }
                    for (Enumeration<String> iter = dex.entries(); iter.hasMoreElements(); ) {
                        paths.add(iter.nextElement());
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // URLClassLoader
        try {
            // fallback: scan jars from classpath if URLClassLoader
            if (loader instanceof URLClassLoader urlClassLoader) {
                for (URL url : urlClassLoader.getURLs()) {
                    String path = url.getPath();
                    if (path.endsWith(".jar")) {
                        JarFile jar = new JarFile(path);
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            String name = entries.nextElement().getName();
                            if (name.endsWith(".class")) {
                                paths.add(name.replace('/', '.').substring(0, name.length() - 6));
                            }
                        }
                        jar.close();
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        cacheBuilt = true;
        classPathsCache = paths;
        return paths;
    }
}
