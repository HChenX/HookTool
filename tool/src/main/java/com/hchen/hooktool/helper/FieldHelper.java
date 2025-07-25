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
import java.util.stream.Collectors;

/**
 * 字段查找
 *
 * @author 焕晨HChen
 * @noinspection SequencedCollectionMethodCanBeUsed
 */
public class FieldHelper {
    private final Class<?> clazz;
    private String fieldName = null;
    private String substring = null;
    private Pattern pattern = null;
    private Class<?> fieldType = null;
    private int mods = -1;
    private Class<? extends Annotation> annotation = null;
    private boolean withSuper = false;

    public FieldHelper(@NonNull Class<?> clazz) {
        Objects.requireNonNull(clazz, "[FieldHelper]: Class must not be null!!");
        this.clazz = clazz;
    }

    /**
     * 精确字段名
     */
    public FieldHelper withFieldName(@NonNull String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    /**
     * 字段名包含子串
     */
    public FieldHelper withSubstring(@NonNull String substring) {
        this.substring = substring;
        return this;
    }

    /**
     * 字段名匹配正则
     */
    public FieldHelper withPattern(@NonNull Pattern pattern) {
        this.pattern = pattern;
        return this;
    }

    /**
     * 字段类型
     */
    public FieldHelper withFieldType(@NonNull Class<?> fieldType) {
        this.fieldType = fieldType;
        return this;
    }

    /**
     * 字段修饰符
     */
    public FieldHelper withMods(@NonNull int... modsFlags) {
        int combined = 0;
        for (int f : modsFlags) combined |= f;
        this.mods = combined;
        return this;
    }

    public FieldHelper withPublic() {
        return withMods(Modifier.PUBLIC);
    }

    public FieldHelper withPrivate() {
        return withMods(Modifier.PRIVATE);
    }

    public FieldHelper withProtected() {
        return withMods(Modifier.PROTECTED);
    }

    public FieldHelper withStatic() {
        return withMods(Modifier.STATIC);
    }

    public FieldHelper withFinal() {
        return withMods(Modifier.FINAL);
    }

    /**
     * 字段注解
     */
    public FieldHelper withAnnotation(@NonNull Class<? extends Annotation> annotation) {
        this.annotation = annotation;
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
     * 单个匹配, 无结果或多于一个抛异常
     */
    public Field single() {
        List<Field> list = matches();
        if (list.isEmpty())
            throw new NonSingletonException("[FieldHelper]: No result found for query!!");
        if (list.size() > 1)
            throw new NonSingletonException("[FieldHelper]: Query did not return a unique result: " + list.size());
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
     * 返回查找到的全部对象
     */
    public Field[] list() {
        return matches().toArray(new Field[0]);
    }

    /**
     * 重置查找器
     */
    public void reset() {
        fieldName = null;
        substring = null;
        pattern = null;
        fieldType = null;
        mods = -1;
        annotation = null;
        withSuper = false;
    }

    /**
     * 查找核心逻辑
     *
     * @noinspection RedundantIfStatement
     */
    private List<Field> matches() {
        List<Field> fields = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));
        if (withSuper) {
            Class<?> sup = clazz.getSuperclass();
            while (sup != null) {
                fields.addAll(Arrays.asList(sup.getDeclaredFields()));
                sup = sup.getSuperclass();
            }
        }

        return fields.stream().filter(field -> {
            if (fieldName != null && !Objects.equals(field.getName(), fieldName)) return false;
            else if (substring != null && !field.getName().contains(substring)) return false;
            else if (pattern != null && !pattern.matcher(field.getName()).matches()) return false;
            if (fieldType != null && !Objects.equals(field.getType(), fieldType)) return false;
            if (mods != -1 && (field.getModifiers() & mods) != mods) return false;
            if (annotation != null && !field.isAnnotationPresent(annotation)) return false;

            return true;
        }).collect(Collectors.toCollection(ArrayList::new));
    }
}
