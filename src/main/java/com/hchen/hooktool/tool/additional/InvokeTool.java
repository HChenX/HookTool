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

 * Copyright (C) 2023-2024 HookTool Contributions
 */
package com.hchen.hooktool.tool.additional;

import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.LogExpand.getTag;

import com.hchen.hooktool.log.AndroidLog;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

/**
 * 反射工具，提供简易的反射功能
 *
 * @author 焕晨HChen
 */
public class InvokeTool {
    private static final HashMap<String, Method> methodCache = new HashMap<>();
    private static final HashMap<String, Field> fieldCache = new HashMap<>();

    // ----------------------------反射调用方法--------------------------------
    public static <T> T callMethod(Object instance, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(null, instance, method, param, value);
    }

    public static <T> T callStaticMethod(Class<?> clz, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(clz, null, method, param, value);
    }

    // ----------------------------设置字段--------------------------------
    public static <T> T setField(Object instance, String field, Object value) {
        return baseInvokeField(null, instance, field, true, value);
    }

    public static <T> T setStaticField(Class<?> clz, String field, Object value) {
        return baseInvokeField(clz, null, field, true, value);
    }

    public static <T> T getField(Object instance, String field) {
        return baseInvokeField(null, instance, field, false, null);
    }

    public static <T> T getStaticField(Class<?> clz, String field) {
        return baseInvokeField(clz, null, field, false, null);
    }

    /**
     * @noinspection unchecked
     */
    private static <T> T baseInvokeMethod(Class<?> clz /* 类 */, Object instance /* 实例 */, String method /* 方法名 */,
                                          Class<?>[] param /* 方法参数 */, Object... value /* 值 */) {
        Method declaredMethod;
        if (clz == null && instance == null) {
            AndroidLog.logW(getTag(), "Class and instance is null, can't invoke method: " + method + getStackTrace());
            return null;
        } else if (clz == null) {
            clz = instance.getClass();
        }
        try {
            String methodTag = clz.getName() + "#" + method + "#" + Arrays.toString(param);
            declaredMethod = methodCache.get(methodTag);
            if (declaredMethod == null) {
                declaredMethod = clz.getDeclaredMethod(method, param);
                methodCache.put(methodTag, declaredMethod);
            }
            declaredMethod.setAccessible(true);
            return (T) declaredMethod.invoke(instance, value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            AndroidLog.logE(getTag(), e);
            return null;
        }
    }

    /**
     * @noinspection unchecked
     */
    private static <T> T baseInvokeField(Class<?> clz /* 类 */, Object instance /* 实例 */, String field /* 字段名 */,
                                         boolean set /* 是否为 set 模式 */, Object value /* 指定值 */) {
        Field declaredField = null;
        if (clz == null && instance == null) {
            AndroidLog.logW(getTag(), "Class and instance is null, can't invoke field: " + field + getStackTrace());
            return null;
        } else if (clz == null) {
            clz = instance.getClass();
        }
        try {
            String fieldTag = clz.getName() + "#" + field;
            declaredField = fieldCache.get(fieldTag);
            if (declaredField == null) {
                try {
                    declaredField = clz.getDeclaredField(field);
                } catch (NoSuchFieldException e) {
                    while (true) {
                        clz = clz.getSuperclass();
                        if (clz == null || clz.equals(Object.class))
                            break;

                        try {
                            declaredField = clz.getDeclaredField(field);
                            break;
                        } catch (NoSuchFieldException ignored) {
                        }
                    }
                    if (declaredField == null) throw e;
                }
                fieldCache.put(fieldTag, declaredField);
            }
            declaredField.setAccessible(true);
            if (set) {
                declaredField.set(instance, value);
                return null;
            } else
                return (T) declaredField.get(instance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            AndroidLog.logE(getTag(), e);
            return null;
        }
    }

    public static Class<?> findClass(String className) {
        return findClass(className, null);
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        try {
            if (classLoader == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            AndroidLog.logE(getTag(), e);
        }
        return null;
    }
}
