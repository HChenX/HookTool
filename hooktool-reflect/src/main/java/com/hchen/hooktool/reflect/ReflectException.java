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
package com.hchen.hooktool.reflect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * 反射操作失败时抛出的异常。
 * <p>
 * 所有查找失败均使用该类型，并通过 {@link Kind} 区分失败原因。
 *
 * @author 焕晨HChen
 */
public class ReflectException extends RuntimeException {
    /**
     * 失败原因分类。
     */
    public enum Kind {
        /**
         * 目标类无法加载。
         */
        CLASS_NOT_FOUND,
        /**
         * 类中存在同名方法，但没有匹配的参数类型。
         */
        METHOD_NOT_FOUND,
        /**
         * 继承链中不存在该字段。
         */
        FIELD_NOT_FOUND,
        /**
         * 没有匹配参数类型的构造函数。
         */
        CONSTRUCTOR_NOT_FOUND,
        /**
         * 参数类型既不是 Class 也不是类名。
         */
        PARAM_MISMATCH,
        /**
         * 同一成员被重复安装。
         */
        DUPLICATE,
        /**
         * 其他失败，例如调用异常或访问受限。
         */
        OTHER
    }

    private final Kind kind;
    private final String target;

    /**
     * 创建异常。
     *
     * @param kind 失败原因分类
     * @param target 目标描述，例如 {@code com.x.Foo#bar(int)}
     * @param message 详细信息
     */
    public ReflectException(@NonNull Kind kind, @NonNull String target, @NonNull String message) {
        this(kind, target, message, null);
    }

    /**
     * 创建带原因的异常。
     *
     * @param kind 失败原因分类
     * @param target 目标描述
     * @param message 详细信息
     * @param cause 原始异常
     */
    public ReflectException(@NonNull Kind kind, @NonNull String target, @NonNull String message,
                            @Nullable Throwable cause) {
        super(message, cause);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.target = Objects.requireNonNull(target, "target");
    }

    /**
     * 获取失败原因分类。
     *
     * @return 失败原因分类
     */
    @NonNull
    public Kind kind() {
        return kind;
    }

    /**
     * 获取目标描述。
     *
     * @return 目标描述
     */
    @NonNull
    public String target() {
        return target;
    }

    @Override
    public String toString() {
        return "ReflectException[" + kind + "] " + getMessage();
    }
}
