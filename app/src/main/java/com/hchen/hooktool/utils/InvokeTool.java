package com.hchen.hooktool.utils;

import androidx.annotation.NonNull;

import com.hchen.hooktool.exception.UnexpectedException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

public class InvokeTool {
    private static final String TAG = "InvokeTool";
    private static final HashMap<String, Method> mMethodCache = new HashMap<>();
    private static final HashMap<String, Field> mFieldCache = new HashMap<>();

    // ----------------------------反射调用方法--------------------------------
    public static <T> T callMethod(Object instance, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(null, instance, method, param, value);
    }

    public static <T> T callStaticMethod(Class<?> clz, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(clz, null, method, param, value);
    }

    public static <T> T callStaticMethod(String clz, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(findClass(clz), null, method, param, value);
    }

    public static <T> T callStaticMethod(String clz, ClassLoader classLoader, String method, Class<?>[] param, Object... value) {
        return baseInvokeMethod(findClass(clz, classLoader), null, method, param, value);
    }

    // ----------------------------设置字段--------------------------------
    public static <T> T setField(Object instance, String field, Object value) {
        return baseInvokeField(null, instance, field, true, value);
    }

    public static <T> T setStaticField(Class<?> clz, String field, Object value) {
        return baseInvokeField(clz, null, field, true, value);
    }

    public static <T> T setStaticField(String clz, String field, Object value) {
        return baseInvokeField(findClass(clz), null, field, true, value);
    }

    public static <T> T setStaticField(String clz, ClassLoader classLoader, String field, Object value) {
        return baseInvokeField(findClass(clz, classLoader), null, field, true, value);
    }

    public static <T> T getField(Object instance, String field) {
        return baseInvokeField(null, instance, field, false, null);
    }

    public static <T> T getStaticField(Class<?> clz, String field) {
        return baseInvokeField(clz, null, field, false, null);
    }

    public static <T> T getStaticField(String clz, String field) {
        return baseInvokeField(findClass(clz), null, field, false, null);
    }

    public static <T> T getStaticField(String clz, ClassLoader classLoader, String field) {
        return baseInvokeField(findClass(clz, classLoader), null, field, false, null);
    }

    /**
     * @noinspection unchecked
     */
    private static <T> T baseInvokeMethod(Class<?> clz /* 类 */, Object instance /* 实例 */, String method /* 方法名 */,
                                          Class<?>[] param /* 方法参数 */, Object... value /* 值 */) {
        Method declaredMethod;
        if (clz == null && instance == null) {
            throw new NullPointerException("[InvokeTool]: Class or instance must not is null, can't invoke method: " + method);
        } else if (clz == null) {
            clz = instance.getClass();
        }
        try {
            String methodTag = clz.getName() + "#" + method + "#" + Arrays.toString(param);
            declaredMethod = mMethodCache.get(methodTag);
            if (declaredMethod == null) {
                declaredMethod = clz.getDeclaredMethod(method, param);
                mMethodCache.put(methodTag, declaredMethod);
            }
            declaredMethod.setAccessible(true);
            return (T) declaredMethod.invoke(instance, value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * @noinspection unchecked
     */
    private static <T> T baseInvokeField(Class<?> clz /* 类 */, Object instance /* 实例 */, String field /* 字段名 */,
                                         boolean set /* 是否为 set 模式 */, Object value /* 指定值 */) {
        Field declaredField = null;
        if (clz == null && instance == null) {
            throw new NullPointerException("[InvokeTool]: Class or instance must not is null, can't invoke field: " + field);
        } else if (clz == null) {
            clz = instance.getClass();
        }
        try {
            String fieldTag = clz.getName() + "#" + field;
            declaredField = mFieldCache.get(fieldTag);
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
                mFieldCache.put(fieldTag, declaredField);
            }
            declaredField.setAccessible(true);
            if (set) {
                declaredField.set(instance, value);
                return null;
            } else
                return (T) declaredField.get(instance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new UnexpectedException(e);
        }
    }

    @NonNull
    public static Class<?> findClass(String className) {
        return findClass(className, null);
    }

    @NonNull
    public static Class<?> findClass(String className, ClassLoader classLoader) {
        try {
            if (classLoader == null)
                classLoader = ClassLoader.getSystemClassLoader();

            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
