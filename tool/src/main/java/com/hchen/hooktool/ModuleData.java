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
 * 模块运行时数据存储类，集中管理 Xposed 模块的核心运行状态。
 * <p>
 * 采用静态持有者模式，维护 {@link XposedInterfaceWrapper} 接口实例、
 * 目标应用的 {@link ClassLoader} 以及 Xposed 环境标识等关键数据。
 * <p>
 * 通过本类提供的静态方法可便捷访问 Xposed API 版本信息、框架名称与版本号、
 * 远程 SharedPreferences、模块 ApplicationInfo、模块 APK 路径、远程文件列表
 * 以及类加载器等运行时信息。
 *
 * @author 焕晨HChen
 * @see ModuleConfig
 * @see ModuleEntrance
 */
public final class ModuleData {
    /**
     * 在热更新（Hot Reload）流程中，用于保存和恢复宿主应用 ClassLoader 的键名。
     * <p>
     * 热更新 {@code onHotReloading} 阶段，{@link ModuleEntrance} 会将当前
     * {@link #getClassLoader()} 返回的 ClassLoader 以此键存入 {@code Bundle}；
     * 在 {@code onHotReloaded} 阶段，再以此键从 {@code Bundle} 中取出并恢复，
     * 确保新代码能继续使用之前正确的类加载器进行反射或 Hook 操作。
     *
     * @see ModuleEntrance#onHotReloading(io.github.libxposed.api.XposedModuleInterface.HotReloadingParam)
     * @see ModuleEntrance#onHotReloaded(io.github.libxposed.api.XposedModuleInterface.HotReloadedParam)
     */
    public static final String MODULE_HOST_CLASSLOADER = "module_host_classloader";
    private static volatile boolean isXposedEnvironment;
    private static volatile XposedInterfaceWrapper wrapper;
    private static volatile ClassLoader classLoader;

    private ModuleData() {
    }

    /**
     * 设置 Xposed 接口包装器实例。
     * <p>
     * 包级私有方法，仅供框架内部调用，外部代码不应直接使用。
     *
     * @param wrapper Xposed 接口包装器实例，不可为 null
     */
    static void setWrapper(@NonNull XposedInterfaceWrapper wrapper) {
        ModuleData.wrapper = wrapper;
    }

    /**
     * 设置目标应用的类加载器。
     * <p>
     * 该类加载器用于在 Hook 环境中加载目标应用的类，
     * 须在调用 {@link #getClassLoader()} 之前完成设置。
     *
     * @param classLoader 目标应用的 ClassLoader 实例，不可为 null
     */
    public static void setClassLoader(@NonNull ClassLoader classLoader) {
        ModuleData.classLoader = classLoader;
    }

    /**
     * 获取当前的 Xposed 接口包装器实例。
     *
     * @return 已设置的 XposedInterfaceWrapper 实例
     * @throws UnexpectedException 当前未处于 Xposed 运行环境时抛出
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
     * 获取当前 Xposed 框架的 API 版本号。
     *
     * @return API 版本号整数值
     */
    public static int getApiVersion() {
        return getWrapper().getApiVersion();
    }

    /**
     * 获取当前运行的 Xposed 框架名称。
     *
     * @return 框架名称字符串，例如 "LSPosed"
     */
    @NonNull
    public static String getFrameworkName() {
        return getWrapper().getFrameworkName();
    }

    /**
     * 获取当前运行的 Xposed 框架版本描述。
     *
     * @return 框架版本描述字符串
     */
    @NonNull
    public static String getFrameworkVersion() {
        return getWrapper().getFrameworkVersion();
    }

    /**
     * 获取当前运行的 Xposed 框架版本号。
     *
     * @return 框架版本号长整型值
     */
    public static long getFrameworkVersionCode() {
        return getWrapper().getFrameworkVersionCode();
    }

    /**
     * 获取当前 Xposed 框架的属性标志位。
     *
     * @return 框架属性值
     */
    public static long getFrameworkProperties() {
        return getWrapper().getFrameworkProperties();
    }

    /**
     * 获取指定名称的远程 SharedPreferences 实例。
     * <p>
     * 可用于跨进程访问模块的 SharedPreferences 数据。
     *
     * @param name SharedPreferences 文件名称
     * @return 对应名称的远程 SharedPreferences 实例
     */
    @NonNull
    public static SharedPreferences getRemotePreferences(@NonNull String name) {
        return getWrapper().getRemotePreferences(name);
    }

    /**
     * 获取当前模块的 {@link ApplicationInfo} 信息。
     *
     * @return 模块的 ApplicationInfo 实例
     */
    @NonNull
    public static ApplicationInfo getModuleApplicationInfo() {
        return getWrapper().getModuleApplicationInfo();
    }

    /**
     * 获取当前模块的包名。
     *
     * @return 模块包名字符串
     */
    @NonNull
    public static String getModulePackageName() {
        return getWrapper().getModuleApplicationInfo().packageName;
    }

    /**
     * 获取当前模块 APK 文件的绝对路径。
     *
     * @return 模块 APK 路径字符串
     */
    @NonNull
    public static String getModulePath() {
        return getWrapper().getModuleApplicationInfo().sourceDir;
    }

    /**
     * 列出远程文件系统中所有可用的文件名。
     *
     * @return 远程文件名数组
     */
    @NonNull
    public static String[] listRemoteFiles() {
        return getWrapper().listRemoteFiles();
    }

    /**
     * 以文件描述符方式打开指定的远程文件。
     *
     * @param name 要打开的远程文件名称
     * @return 远程文件对应的 {@link ParcelFileDescriptor} 文件描述符
     * @throws FileNotFoundException 指定名称的远程文件不存在时抛出
     */
    @NonNull
    public static ParcelFileDescriptor openRemoteFile(@NonNull String name) throws FileNotFoundException {
        return getWrapper().openRemoteFile(name);
    }

    /**
     * 获取目标应用的类加载器。
     *
     * @return 目标应用的 ClassLoader 实例
     * @throws UnexpectedException 类加载器尚未通过 {@link #setClassLoader(ClassLoader)} 设置时抛出
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
     * 获取 Java 系统类加载器。
     *
     * @return 系统 ClassLoader 实例
     */
    @NonNull
    public static ClassLoader getSystemClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }

    /**
     * 设置 Xposed 运行环境标识。
     * <p>
     * 包级私有方法，仅供框架内部调用，用于标记当前是否运行于 Xposed 环境。
     *
     * @param isXposedEnvironment {@code true} 表示处于 Xposed 环境，{@code false} 表示非 Xposed 环境
     */
    @SuppressWarnings("SameParameterValue")
    static void setXposedEnvironment(boolean isXposedEnvironment) {
        ModuleData.isXposedEnvironment = isXposedEnvironment;
    }

    /**
     * 判断当前是否运行在 Xposed 框架环境中。
     *
     * @return {@code true} 表示处于 Xposed 环境，{@code false} 表示不在 Xposed 环境
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isXposedEnvironment() {
        return isXposedEnvironment;
    }
}
