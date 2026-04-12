package de.robv.android.xposed;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class XposedHelpers {
    private XposedHelpers() {
        throw new RuntimeException("Stub!");
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        throw new RuntimeException("Stub!");
    }

    public static Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        throw new RuntimeException("Stub!");
    }

    public static Field findField(Class<?> clazz, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static Field findFieldIfExists(Class<?> clazz, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Object... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Method findMethodExactIfExists(Class<?> clazz, String methodName, Object... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Method findMethodExact(String className, ClassLoader classLoader, String methodName, Object... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Method findMethodExactIfExists(String className, ClassLoader classLoader, String methodName, Object... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Object... args) {
        throw new RuntimeException("Stub!");
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object[] args) {
        throw new RuntimeException("Stub!");
    }

    public static Class<?>[] getParameterTypes(Object... args) {
        throw new RuntimeException("Stub!");
    }

    public static Constructor<?> findConstructorExact(Class<?> clazz, Object... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Constructor<?> findConstructorExactIfExists(Class<?> clazz, Object... parameterTypes) {
        throw new RuntimeException("Stub!");

    }

    public static Constructor<?> findConstructorExact(String className, ClassLoader classLoader, Object... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Constructor<?> findConstructorExactIfExists(String className, ClassLoader classLoader, Object... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Constructor<?> findConstructorExact(Class<?> clazz, Class<?>... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Class<?>... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Object... args) {
        throw new RuntimeException("Stub!");
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Class<?>[] parameterTypes, Object[] args) {
        throw new RuntimeException("Stub!");
    }

    //#################################################################################################

    public static void setObjectField(Object obj, String fieldName, Object value) {
        throw new RuntimeException("Stub!");

    }

    //#################################################################################################

    public static Object getObjectField(Object obj, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    //#################################################################################################

    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
        throw new RuntimeException("Stub!");
    }

    //#################################################################################################

    public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    //#################################################################################################

    public static Object callMethod(Object obj, String methodName, Object... args) {
        throw new RuntimeException("Stub!");
    }

    public static Object callMethod(Object obj, String methodName, Class<?>[] parameterTypes, Object... args) {
        throw new RuntimeException("Stub!");
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        throw new RuntimeException("Stub!");
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object... args) {
        throw new RuntimeException("Stub!");
    }

    //#################################################################################################

    public static Object newInstance(Class<?> clazz, Object... args) {
        throw new RuntimeException("Stub!");
    }

    public static Object newInstance(Class<?> clazz, Class<?>[] parameterTypes, Object... args) {
        throw new RuntimeException("Stub!");
    }

    //#################################################################################################

    public static Object setAdditionalInstanceField(Object obj, String key, Object value) {
        throw new RuntimeException("Stub!");
    }

    public static Object getAdditionalInstanceField(Object obj, String key) {
        throw new RuntimeException("Stub!");
    }

    public static Object removeAdditionalInstanceField(Object obj, String key) {
        throw new RuntimeException("Stub!");
    }


    public static Object setAdditionalStaticField(Object obj, String key, Object value) {
        throw new RuntimeException("Stub!");
    }


    public static Object getAdditionalStaticField(Object obj, String key) {
        throw new RuntimeException("Stub!");
    }


    public static Object removeAdditionalStaticField(Object obj, String key) {
        throw new RuntimeException("Stub!");
    }


    public static Object setAdditionalStaticField(Class<?> clazz, String key, Object value) {
        throw new RuntimeException("Stub!");
    }


    public static Object getAdditionalStaticField(Class<?> clazz, String key) {
        throw new RuntimeException("Stub!");
    }


    public static Object removeAdditionalStaticField(Class<?> clazz, String key) {
        throw new RuntimeException("Stub!");
    }
}
