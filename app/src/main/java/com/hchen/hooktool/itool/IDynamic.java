package com.hchen.hooktool.itool;

import java.lang.reflect.Field;

public interface IDynamic {

    <T, R> R callMethod(Object instance, String name, T ts);

    <R> R callMethod(Object instance, String name);

    <T> T getField(Object instance, String name);

    <T> T getField(Object instance, Field field);

    boolean setField(Object instance, String name, Object key);

    boolean setField(Object instance, Field field, Object key);

    boolean setAdditionalInstanceField(Object instance, String name, Object key);

    <T> T getAdditionalInstanceField(Object instance, String name);

    boolean removeAdditionalInstanceField(Object instance, String name);
}
