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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.exception.NonSingletonException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 字段查找
 *
 * @author 焕晨HChen
 * @noinspection SequencedCollectionMethodCanBeUsed, unused
 */
public class FieldHelper {
    private final Class<?> clazz;
    private int mods = -1; // 修饰符
    private String fieldName;
    private String substring;
    private Pattern pattern;
    private Class<?> fieldClass;
    private Class<? extends Annotation>[] annotations;
    private boolean withSuper = false;

    public FieldHelper(@NonNull Class<?> clazz) {
        Objects.requireNonNull(clazz, "Class must not be null.");
        this.clazz = clazz;
    }

    /**
     * 精确字段名
     */
    public FieldHelper withFieldName(@NonNull String fieldName) {
        Objects.requireNonNull(fieldName, "FieldName must not be null.");
        this.fieldName = fieldName;
        return this;
    }

    /**
     * 字段名包含子串
     */
    public FieldHelper withSubstring(@NonNull String substring) {
        Objects.requireNonNull(substring, "Substring must not be null.");
        this.substring = substring;
        return this;
    }

    /**
     * 字段名匹配正则
     */
    public FieldHelper withPattern(@NonNull Pattern pattern) {
        Objects.requireNonNull(pattern, "Pattern must not be null.");
        this.pattern = pattern;
        return this;
    }

    /**
     * 字段类型
     */
    public FieldHelper withFieldClass(@NonNull Class<?> fieldClass) {
        Objects.requireNonNull(fieldClass, "FieldClass must not be null.");
        this.fieldClass = fieldClass;
        return this;
    }

    /**
     * 字段修饰符
     */
    public FieldHelper withMods(@NonNull int... modsFlags) {
        Objects.requireNonNull(modsFlags, "ModsFlags must not be null.");
        int combined = 0;
        for (int f : modsFlags) combined |= f;
        this.mods = combined;
        return this;
    }

    /**
     * 设置字段为 public 修饰符
     */
    public FieldHelper withPublic() {
        return withMods(Modifier.PUBLIC);
    }

    /**
     * 设置字段为 private 修饰符
     */
    public FieldHelper withPrivate() {
        return withMods(Modifier.PRIVATE);
    }

    /**
     * 设置字段为 protected 修饰符
     */
    public FieldHelper withProtected() {
        return withMods(Modifier.PROTECTED);
    }

    /**
     * 设置字段为 static 修饰符
     */
    public FieldHelper withStatic() {
        return withMods(Modifier.STATIC);
    }

    /**
     * 设置字段为 final 修饰符
     */
    public FieldHelper withFinal() {
        return withMods(Modifier.FINAL);
    }

    /**
     * 字段注解
     */
    @SuppressWarnings("unchecked")
    public FieldHelper withAnnotations(@NonNull Class<? extends Annotation>... annotations) {
        Objects.requireNonNull(annotations, "Annotations must not be null.");
        this.annotations = annotations;
        return this;
    }

    /**
     * 是否包含父类字段
     */
    public FieldHelper withSuper(boolean withSuper) {
        this.withSuper = withSuper;
        return this;
    }

    /**
     * 单个匹配，无结果或多于一个抛异常
     */
    public Field single() {
        List<Field> list = matches();
        if (list.isEmpty())
            throw new NonSingletonException("No result found for query.");
        if (list.size() > 1)
            throw new NonSingletonException("Query did not return a unique result: " + list.size());
        return list.get(0);
    }

    /**
     * 单个匹配或返回 null
     */
    @Nullable
    public Field singleOrNull() {
        List<Field> list = matches();
        if (list.size() != 1) return null;
        return list.get(0);
    }

    /**
     * 单个匹配或抛指定异常
     */
    public Field singleOrThrow(@NonNull Supplier<NonSingletonException> supplier) {
        List<Field> list = matches();
        if (list.size() != 1) throw supplier.get();
        return list.get(0);
    }

    /**
     * 返回查找到的所有字段
     */
    public Field[] toArray() {
        return matches().toArray(new Field[0]);
    }

    /**
     * 重置查找器，清除所有设置的条件
     */
    public void reset() {
        mods = -1;
        fieldName = null;
        substring = null;
        pattern = null;
        fieldClass = null;
        annotations = null;
        withSuper = false;
    }

    /**
     * 查找核心逻辑
     */
    private List<Field> matches() {
        Field[] declaredFields = clazz.getDeclaredFields();
        ArrayList<Field> fields = new ArrayList<>(Arrays.asList(declaredFields));
        
        if (withSuper) {
            addSuperClassFields(fields);
        }

        ArrayList<Field> result = new ArrayList<>(fields.size());
        for (Field field : fields) {
            if (matchesFieldName(field) &&
                matchesModifiers(field) &&
                matchesFieldClass(field) &&
                matchesAnnotations(field)) {
                result.add(field);
            }
        }

        return result;
    }

    /**
     * 添加父类字段
     */
    private void addSuperClassFields(List<Field> fields) {
        Class<?> sup = clazz.getSuperclass();
        while (sup != null) {
            fields.addAll(Arrays.asList(sup.getDeclaredFields()));
            sup = sup.getSuperclass();
        }
    }

    /**
     * 匹配字段名
     */
    private boolean matchesFieldName(Field field) {
        if (fieldName != null && !fieldName.equals(field.getName())) {
            return false;
        }
        if (pattern != null && !pattern.matcher(field.getName()).matches()) {
            return false;
        }
        return substring == null || field.getName().contains(substring);
    }

    /**
     * 匹配修饰符
     */
    private boolean matchesModifiers(Field field) {
        return mods == -1 || (field.getModifiers() & mods) == mods;
    }

    /**
     * 匹配字段类型
     */
    private boolean matchesFieldClass(Field field) {
        return fieldClass == null || fieldClass.equals(field.getType());
    }

    /**
     * 匹配注解
     */
    private boolean matchesAnnotations(Field field) {
        return annotations == null || Arrays.stream(annotations).allMatch(field::isAnnotationPresent);
    }
}
