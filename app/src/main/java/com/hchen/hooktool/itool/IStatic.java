package com.hchen.hooktool.itool;

import androidx.annotation.Nullable;

import java.lang.reflect.Field;

public interface IStatic {
    @Nullable
    Class<?> findClass(String name);

    @Nullable
    Class<?> findClass(String name, ClassLoader classLoader);

    // --------- 实例类 ------------
    @Nullable
    <T, R> R newInstance(Class<?> clz, T objects);

    @Nullable
    <R> R newInstance(Class<?> clz);

    // --------- 调用静态方法 ------------
    @Nullable
    <T, R> R callStaticMethod(Class<?> clz, String name, T objs);

    @Nullable
    <R> R callStaticMethod(Class<?> clz, String name);

    // --------- 获取静态字段 ------------
    @Nullable
    <T> T getStaticField(Class<?> clz, String name);

    @Nullable
    <T> T getStaticField(Field field);

    // --------- 设置静态字段 ------------
    boolean setStaticField(Class<?> clz, String name, Object value);

    boolean setStaticField(Field field, Object value);

    // --------- 设置获取删除自定义字段 ------------
    boolean setAdditionalStaticField(Class<?> clz, String key, Object value);

    @Nullable
    <T> T getAdditionalStaticField(Class<?> clz, String key);

    boolean removeAdditionalStaticField(Class<?> clz, String key);
}
