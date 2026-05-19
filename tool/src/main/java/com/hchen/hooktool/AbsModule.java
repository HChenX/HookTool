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

import android.content.Context;

import androidx.annotation.NonNull;

import com.hchen.hooktool.core.CoreTool;

import java.util.Objects;
import java.util.function.Consumer;

import io.github.libxposed.api.XposedModuleInterface;

/**
 * Hook 模块的抽象基类。
 * <p>
 * 推荐继承此类以实现 Hook 逻辑。提供了完整的模块生命周期管理，包括模块加载、
 * 包加载、包就绪、系统服务器启动、类加载器注入以及应用创建等阶段。
 * <p>
 * 子类通过重写 {@link #onLoaded(StageEnum, Object)} 方法来实现具体的 Hook 逻辑，
 * 并可通过 {@link StageEnum} 判断当前所处的生命周期阶段。
 * <p>
 * 所有生命周期回调均受 {@link #isEnabled()} 方法控制，当返回 {@code false} 时将跳过执行。
 *
 * @author 焕晨HChen
 * @see CoreTool
 * @see ModuleEntrance
 */
public abstract class AbsModule extends CoreTool {

    // -------------------------- hook 类内使用 -----------------------------

    /** 日志标签，取值为当前类的简单名称。 */
    protected final String TAG = getClass().getSimpleName();

    /**
     * 模块生命周期阶段枚举。
     * <p>
     * 用于标识当前回调所处的生命周期阶段，以便在 {@link #onLoaded(StageEnum, Object)} 中进行区分处理。
     */
    public enum StageEnum {
        /**
         * 模块已加载阶段。对应参数类型为 {@link XposedModuleInterface.ModuleLoadedParam}。
         */
        MODULE_LOADED,
        /**
         * 包已加载阶段。对应参数类型为 {@link XposedModuleInterface.PackageLoadedParam}。
         */
        PACKAGE_LOADED,
        /**
         * 包就绪阶段。对应参数类型为 {@link XposedModuleInterface.PackageReadyParam}。
         */
        PACKAGE_READY,
        /**
         * 系统服务器启动阶段。对应参数类型为 {@link XposedModuleInterface.SystemServerStartingParam}。
         */
        SYSTEM_SERVER_STARTING,
        /**
         * 类加载器注入阶段。对应参数类型为 {@link ClassLoader}。
         */
        ON_CLASSLOADER,
        /**
         * 应用创建阶段。对应参数类型为 {@link Context}。
         */
        ON_APPLICATION_CREATED
    }

    /**
     * 判断当前模块是否启用。
     * <p>
     * 所有生命周期回调在执行前都会检查此方法的返回值。当返回 {@code false} 时，
     * 对应的回调将被跳过。子类可重写此方法以实现动态启用/禁用逻辑。
     *
     * @return {@code true} 表示模块已启用，{@code false} 表示模块已禁用
     */
    protected boolean isEnabled() {
        return true;
    }

    /**
     * 模块应在此方法下编写 Hook 逻辑。
     * <p>
     * 使用 {@link StageEnum} 判断当前阶段。
     * <p>
     * {@link StageEnum#MODULE_LOADED} -> {@link XposedModuleInterface.ModuleLoadedParam}
     * <p>
     * {@link StageEnum#PACKAGE_LOADED} -> {@link XposedModuleInterface.PackageLoadedParam}
     * <p>
     * {@link StageEnum#PACKAGE_READY} -> {@link XposedModuleInterface.PackageLoadedParam}
     * <p>
     * {@link StageEnum#SYSTEM_SERVER_STARTING} -> {@link XposedModuleInterface.SystemServerStartingParam}。
     *
     * @param stage 当前生命周期阶段
     * @param param 生命周期参数对象
     */
    protected abstract void onLoaded(@NonNull StageEnum stage, @NonNull Object param);

    /**
     * 传入自定义 ClassLoader 参数时调用。
     * <p>
     * 请勿直接使用此方法传入 ClassLoader 参数，因为不受 isEnabled 控制。
     *
     * @param classLoader 注入的类加载器
     */
    protected void onClassLoader(@NonNull ClassLoader classLoader) {
    }

    /**
     * 目标应用 Application 创建时调用。
     *
     * @param context 应用上下文
     */
    protected void onApplicationCreated(@NonNull Context context) {
    }

    /**
     * 以上所有阶段发生崩溃时调用。
     * <p>
     * 请勿在此处继续执行任何任务，请执行清理工作。
     *
     * @param stage 异常发生的阶段
     * @param e     发生的异常
     */
    protected void onThrow(@NonNull StageEnum stage, @NonNull Throwable e) {
    }

    // ------------------------------ 入口类内统一调用 ------------------------------------

    /**
     * 统一的生命周期分发方法。
     * <p>
     * 在执行回调前检查 {@link #isEnabled()} 状态，并在回调执行过程中捕获异常，
     * 将异常转发至 {@link #onThrow(StageEnum, Throwable)} 处理。
     *
     * @param <T>    参数类型
     * @param stage  当前生命周期阶段
     * @param param  生命周期参数对象
     * @param action 要执行的回调动作
     */
    private <T> void dispatch(@NonNull StageEnum stage, @NonNull T param, @NonNull Consumer<T> action) {
        try {
            if (!isEnabled()) return;

            Objects.requireNonNull(param);
            action.accept(param);
        } catch (Throwable e) {
            onThrow(stage, e);
            logE(TAG, e);
        }
    }

    /**
     * 处理模块加载事件。
     * <p>
     * 由入口类调用，内部通过 {@link #dispatch} 方法转发至 {@link #onLoaded(StageEnum, Object)}。
     *
     * @param param 模块加载参数
     */
    final public void handleModuleLoaded(@NonNull XposedModuleInterface.ModuleLoadedParam param) {
        dispatch(StageEnum.MODULE_LOADED, param, new Consumer<XposedModuleInterface.ModuleLoadedParam>() {
            @Override
            public void accept(XposedModuleInterface.ModuleLoadedParam p) {
                onLoaded(StageEnum.MODULE_LOADED, p);
            }
        });
    }

    /**
     * 处理包加载事件。
     * <p>
     * 由入口类调用，内部通过 {@link #dispatch} 方法转发至 {@link #onLoaded(StageEnum, Object)}。
     *
     * @param param 包加载参数
     */
    final public void handlePackageLoaded(@NonNull XposedModuleInterface.PackageLoadedParam param) {
        dispatch(StageEnum.PACKAGE_LOADED, param, new Consumer<XposedModuleInterface.PackageLoadedParam>() {
            @Override
            public void accept(XposedModuleInterface.PackageLoadedParam p) {
                onLoaded(StageEnum.PACKAGE_LOADED, p);
            }
        });
    }

    /**
     * 处理包就绪事件。
     * <p>
     * 由入口类调用，内部通过 {@link #dispatch} 方法转发至 {@link #onLoaded(StageEnum, Object)}。
     *
     * @param param 包就绪参数
     */
    final public void handlePackageReady(@NonNull XposedModuleInterface.PackageReadyParam param) {
        dispatch(StageEnum.PACKAGE_READY, param, new Consumer<XposedModuleInterface.PackageReadyParam>() {
            @Override
            public void accept(XposedModuleInterface.PackageReadyParam p) {
                onLoaded(StageEnum.PACKAGE_READY, p);
            }
        });
    }

    /**
     * 处理系统服务器启动事件。
     * <p>
     * 由入口类调用，内部通过 {@link #dispatch} 方法转发至 {@link #onLoaded(StageEnum, Object)}。
     *
     * @param param 系统服务器启动参数
     */
    final public void handleSystemServerStarting(@NonNull XposedModuleInterface.SystemServerStartingParam param) {
        dispatch(StageEnum.SYSTEM_SERVER_STARTING, param, new Consumer<XposedModuleInterface.SystemServerStartingParam>() {
            @Override
            public void accept(XposedModuleInterface.SystemServerStartingParam p) {
                onLoaded(StageEnum.SYSTEM_SERVER_STARTING, p);
            }
        });
    }

    /**
     * 处理类加载器注入事件。
     * <p>
     * 由入口类调用，内部通过 {@link #dispatch} 方法转发至 {@link #onClassLoader(ClassLoader)}。
     *
     * @param classLoader 注入的类加载器
     */
    final public void handleClassLoader(@NonNull ClassLoader classLoader) {
        dispatch(StageEnum.ON_CLASSLOADER, classLoader, new Consumer<ClassLoader>() {
            @Override
            public void accept(ClassLoader classLoader) {
                onClassLoader(classLoader);
            }
        });
    }

    /**
     * 处理应用创建事件。
     * <p>
     * 由入口类调用，内部通过 {@link #dispatch} 方法转发至 {@link #onApplicationCreated(Context)}。
     *
     * @param context 应用上下文
     */
    final public void handleApplicationCreated(@NonNull Context context) {
        dispatch(StageEnum.ON_APPLICATION_CREATED, context, new Consumer<Context>() {
            @Override
            public void accept(Context context) {
                onApplicationCreated(context);
            }
        });
    }
}
