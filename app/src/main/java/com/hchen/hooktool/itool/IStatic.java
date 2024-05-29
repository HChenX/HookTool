package com.hchen.hooktool.itool;

import java.lang.reflect.Field;

public interface IStatic {

    Class<?> findClass(String name);

    Class<?> findClass(String name, ClassLoader classLoader);

    // --------- 实例类 ------------
    <T, R> R newInstance(Class<?> clz, T objects);

    <R> R newInstance(Class<?> clz);

    // --------- 调用静态方法 ------------
    <T, R> R callStaticMethod(Class<?> clz, String name, T objs);

    <R> R callStaticMethod(Class<?> clz, String name);

    // --------- 获取静态字段 ------------
    <T> T getStaticField(Class<?> clz, String name);

    <T> T getStaticField(Field field);

    // --------- 设置静态字段 ------------
    boolean setStaticField(Class<?> clz, String name, Object value);

    boolean setStaticField(Field field, Object value);

    // --------- 设置获取删除自定义字段 ------------
    boolean setAdditionalStaticField(Class<?> clz, String key, Object value);

    <T> T getAdditionalStaticField(Class<?> clz, String key);

    boolean removeAdditionalStaticField(Class<?> clz, String key);
}
