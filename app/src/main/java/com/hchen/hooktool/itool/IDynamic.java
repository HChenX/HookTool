package com.hchen.hooktool.itool;

import androidx.annotation.Nullable;

import java.lang.reflect.Field;

public interface IDynamic {

    @Nullable
    <T, R> R callMethod(Object instance, String name, T ts);

    @Nullable
    <R> R callMethod(Object instance, String name);

    @Nullable
    <T> T getField(Object instance, String name);

    @Nullable
    <T> T getField(Object instance, Field field);

    boolean setField(Object instance, String name, Object key);

    boolean setField(Object instance, Field field, Object key);

    boolean setAdditionalInstanceField(Object instance, String name, Object key);

    @Nullable
    <T> T getAdditionalInstanceField(Object instance, String name);

    boolean removeAdditionalInstanceField(Object instance, String name);
}
