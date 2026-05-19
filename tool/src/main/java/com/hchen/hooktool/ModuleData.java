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
package com.hchen.hooktool;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import com.hchen.hooktool.exception.UnexpectedException;

import java.io.FileNotFoundException;
import java.util.Objects;

import io.github.libxposed.api.XposedInterfaceWrapper;

/**
 * 模块数据存储类。
 * <p>
 * 静态数据持有者，用于存储和访问 Xposed 环境中的模块运行时状态，包括
 * {@link XposedInterfaceWrapper} 实例、目标应用的 {@link ClassLoader} 以及环境标志等。
 * <p>
 * 通过此类可获取 API 版本、框架名称与版本、远程共享首选项、模块信息、模块路径、
 * 远程文件列表以及类加载器等信息。
 *
 * @author 焕晨HChen
 * @see ModuleConfig
 * @see ModuleEntrance
 */
public final class ModuleData {
    private static volatile boolean isXposedEnvironment;
    private static volatile XposedInterfaceWrapper wrapper;
    private static volatile ClassLoader classLoader;

    private ModuleData() {
    }

    /**
     * 设置 Xposed 接口包装器。
     *
     * @param wrapper Xposed 接口包装器实例
     */
    static void setWrapper(@NonNull XposedInterfaceWrapper wrapper) {
        ModuleData.wrapper = wrapper;
    }

    /**
     * 设置目标应用的类加载器。
     *
     * @param classLoader 目标应用的类加载器
     */
    public static void setClassLoader(@NonNull ClassLoader classLoader) {
        ModuleData.classLoader = classLoader;
    }

    /**
     * 获取 Xposed 接口包装器。
     *
     * @return Xposed 接口包装器实例
     * @throws UnexpectedException 如果当前不在 Xposed 环境中
     */
    @NonNull
    public static XposedInterfaceWrapper getWrapper() {
        if (!isXposedEnvironment()) {
            throw new UnexpectedException("Please call in the xposed environment.");
        }

        Objects.requireNonNull(wrapper);
        return wrapper;
    }

    /**
     * 获取 Xposed API 版本号。
     *
     * @return API 版本号
     */
    public static int getApiVersion() {
        return getWrapper().getApiVersion();
    }

    /**
     * 获取 Xposed 框架名称。
     *
     * @return 框架名称
     */
    @NonNull
    public static String getFrameworkName() {
        return getWrapper().getFrameworkName();
    }

    /**
     * 获取 Xposed 框架版本。
     *
     * @return 框架版本字符串
     */
    @NonNull
    public static String getFrameworkVersion() {
        return getWrapper().getFrameworkVersion();
    }

    /**
     * 获取 Xposed 框架版本号。
     *
     * @return 框架版本号
     */
    public static long getFrameworkVersionCode() {
        return getWrapper().getFrameworkVersionCode();
    }

    /**
     * 获取 Xposed 框架属性。
     *
     * @return 框架属性值
     */
    public static long getFrameworkProperties() {
        return getWrapper().getFrameworkProperties();
    }

    /**
     * 获取远程共享首选项。
     *
     * @param name 共享首选项名称
     * @return 远程共享首选项实例
     */
    @NonNull
    public static SharedPreferences getRemotePreferences(@NonNull String name) {
        return getWrapper().getRemotePreferences(name);
    }

    /**
     * 获取模块的 ApplicationInfo。
     *
     * @return 模块的 ApplicationInfo 实例
     */
    @NonNull
    public static ApplicationInfo getModuleApplicationInfo() {
        return getWrapper().getModuleApplicationInfo();
    }

    /**
     * 获取模块的包名。
     *
     * @return 模块包名
     */
    @NonNull
    public static String getModulePackageName() {
        return getWrapper().getModuleApplicationInfo().packageName;
    }

    /**
     * 获取模块 APK 的路径。
     *
     * @return 模块 APK 文件路径
     */
    @NonNull
    public static String getModulePath() {
        return getWrapper().getModuleApplicationInfo().sourceDir;
    }

    /**
     * 列出远程文件列表。
     *
     * @return 远程文件名数组
     */
    @NonNull
    public static String[] listRemoteFiles() {
        return getWrapper().listRemoteFiles();
    }

    /**
     * 打开远程文件。
     *
     * @param name 远程文件名
     * @return 远程文件的文件描述符
     * @throws FileNotFoundException 如果文件不存在
     */
    @NonNull
    public static ParcelFileDescriptor openRemoteFile(@NonNull String name) throws FileNotFoundException {
        return getWrapper().openRemoteFile(name);
    }

    /**
     * 获取目标应用的类加载器。
     *
     * @return 目标应用的类加载器
     * @throws UnexpectedException 如果类加载器尚未设置
     */
    @NonNull
    public static ClassLoader getClassLoader() {
        ClassLoader cl = classLoader;
        if (cl == null) {
            throw new UnexpectedException("ClassLoader has not been set. Ensure ModuleData.setClassLoader() is called before use.");
        }
        return cl;
    }

    /**
     * 获取系统类加载器。
     *
     * @return 系统类加载器
     */
    @NonNull
    public static ClassLoader getSystemClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }

    /**
     * 设置 Xposed 环境标志。
     *
     * @param isXposedEnvironment 是否处于 Xposed 环境
     */
    @SuppressWarnings("SameParameterValue")
    static void setXposedEnvironment(boolean isXposedEnvironment) {
        ModuleData.isXposedEnvironment = isXposedEnvironment;
    }

    /**
     * 判断当前是否处于 Xposed 环境中。
     *
     * @return {@code true} 表示处于 Xposed 环境，{@code false} 表示不处于
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isXposedEnvironment() {
        return isXposedEnvironment;
    }
}
