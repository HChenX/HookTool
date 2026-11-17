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

/**
 * 反射调用的默认类加载器持有者。
 * <p>
 * 未显式指定类加载器时使用此处设置的值，未设置时回落到线程上下文类加载器。
 *
 * @author 焕晨HChen
 */
public final class ReflectEnv {
    private static volatile ClassLoader sDefaultClassLoader;

    private ReflectEnv() {
    }

    /**
     * 设置默认类加载器。
     *
     * @param classLoader 类加载器，传入 null 表示清除
     */
    public static void setDefaultClassLoader(@Nullable ClassLoader classLoader) {
        sDefaultClassLoader = classLoader;
    }

    /**
     * 获取已设置的类加载器。
     *
     * @return 类加载器，未设置时为 null
     */
    @Nullable
    public static ClassLoader defaultClassLoaderOrNull() {
        return sDefaultClassLoader;
    }

    /**
     * 获取可用的类加载器。
     * <p>
     * 依次尝试已设置的值、线程上下文类加载器、系统类加载器。
     *
     * @return 可用的类加载器
     */
    @NonNull
    public static ClassLoader defaultClassLoader() {
        ClassLoader loader = sDefaultClassLoader;
        if (loader != null) return loader;
        loader = Thread.currentThread().getContextClassLoader();
        if (loader != null) return loader;
        return ClassLoader.getSystemClassLoader();
    }
}
