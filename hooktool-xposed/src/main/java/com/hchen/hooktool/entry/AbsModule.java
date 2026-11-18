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
package com.hchen.hooktool.entry;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.log.AndroidLog;
import com.hchen.hooktool.reflect.Throwables;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import io.github.libxposed.api.XposedModuleInterface;

/**
 * 模块业务逻辑基类。
 *
 * @author 焕晨HChen
 */
public abstract class AbsModule {
    /**
     * 当前实例的日志标签，取值为子类的简单类名。
     */
    @NonNull
    protected final String TAG = getClass().getSimpleName();

    /**
     * 模块生命周期阶段。
     */
    public enum LifecycleStage {
        MODULE_LOADED, PACKAGE_LOADED, PACKAGE_READY,
        SYSTEM_SERVER_STARTING, ON_CLASSLOADER,
        ON_APPLICATION_CREATED,
        HOT_RELOADING, HOT_RELOADED
    }

    /**
     * 判断模块是否启用。
     *
     * @return 启用时返回 true
     */
    protected boolean isEnabled() {
        return true;
    }

    /**
     * 模块加载完成回调。
     *
     * @param param 模块加载参数
     */
    protected void onModuleLoaded(@NonNull XposedModuleInterface.ModuleLoadedParam param) {
    }

    /**
     * 目标包加载完成回调。
     *
     * @param param 包加载参数
     */
    protected void onPackageLoaded(@NonNull XposedModuleInterface.PackageLoadedParam param) {
    }

    /**
     * 目标包就绪回调，Hook 安装应在此完成。
     *
     * @param param 包就绪参数
     */
    protected abstract void onPackageReady(
        @NonNull XposedModuleInterface.PackageReadyParam param);

    /**
     * 系统服务进程启动回调。
     *
     * @param param 系统服务参数
     */
    protected void onSystemServerStarting(
        @NonNull XposedModuleInterface.SystemServerStartingParam param) {
    }

    /**
     * 热更新准备回调。
     *
     * @param extras 附加数据，可为 null
     * @return 待保存的状态
     */
    @NonNull
    protected Map<String, Object> onHotReloading(@Nullable Bundle extras) {
        return new HashMap<>();
    }

    /**
     * 热更新完成回调。
     *
     * @param param 热更新参数
     */
    protected void onHotReloaded(@NonNull XposedModuleInterface.HotReloadedParam param) {
    }

    /**
     * 类加载器注入回调。
     *
     * @param classLoader 注入的类加载器
     */
    protected void onClassLoader(@NonNull ClassLoader classLoader) {
    }

    /**
     * 目标应用创建回调。
     *
     * @param context 应用上下文
     */
    protected void onApplicationCreated(@NonNull Context context) {
    }

    /**
     * 生命周期回调抛出异常时触发。
     *
     * @param stage     发生异常的阶段
     * @param throwable 异常信息
     */
    protected void onThrow(@NonNull LifecycleStage stage, @NonNull Throwable throwable) {
    }

    private <T> void dispatch(@NonNull LifecycleStage stage, @NonNull T param,
                              @NonNull Consumer<T> action) {
        try {
            if (!isEnabled()) return;
            Objects.requireNonNull(param);
            action.accept(param);
        } catch (Throwable throwable) {
            try {
                onThrow(stage, throwable);
            } catch (Throwable onThrowError) {
                AndroidLog.logW(TAG,
                    "onThrow() threw an exception, ignored to preserve the original exception.",
                    onThrowError);
            }
            Throwables.throwIt(throwable);
        }
    }

    /**
     * 分发模块加载事件。
     *
     * @param param 框架参数
     */
    public final void handleModuleLoaded(@NonNull XposedModuleInterface.ModuleLoadedParam param) {
        dispatch(LifecycleStage.MODULE_LOADED, param, this::onModuleLoaded);
    }

    /**
     * 分发包加载事件。
     *
     * @param param 框架参数
     */
    public final void handlePackageLoaded(@NonNull XposedModuleInterface.PackageLoadedParam param) {
        dispatch(LifecycleStage.PACKAGE_LOADED, param, this::onPackageLoaded);
    }

    /**
     * 分发包就绪事件。
     *
     * @param param 框架参数
     */
    public final void handlePackageReady(@NonNull XposedModuleInterface.PackageReadyParam param) {
        dispatch(LifecycleStage.PACKAGE_READY, param, this::onPackageReady);
    }

    /**
     * 分发系统服务启动事件。
     *
     * @param param 框架参数
     */
    public final void handleSystemServerStarting(
        @NonNull XposedModuleInterface.SystemServerStartingParam param) {
        dispatch(LifecycleStage.SYSTEM_SERVER_STARTING, param, this::onSystemServerStarting);
    }

    /**
     * 分发类加载器注入事件。
     *
     * @param classLoader 注入的类加载器
     */
    public final void handleClassLoader(@NonNull ClassLoader classLoader) {
        dispatch(LifecycleStage.ON_CLASSLOADER, classLoader, this::onClassLoader);
    }

    /**
     * 分发应用创建事件。
     *
     * @param context 应用上下文
     */
    public final void handleApplicationCreated(@NonNull Context context) {
        dispatch(LifecycleStage.ON_APPLICATION_CREATED, context, this::onApplicationCreated);
    }

    /**
     * 分发热更新准备事件。
     *
     * @param extras 附加数据，可为 null
     * @return 收集到的状态
     */
    @NonNull
    public final Map<String, Object> handleHotReloading(@Nullable Bundle extras) {
        if (!isEnabled()) return new HashMap<>();
        try {
            return onHotReloading(extras);
        } catch (Throwable throwable) {
            try {
                onThrow(LifecycleStage.HOT_RELOADING, throwable);
            } catch (Throwable onThrowError) {
                AndroidLog.logW(TAG,
                    "onThrow() threw an exception, ignored to preserve the original exception.",
                    onThrowError);
            }
            return Throwables.throwIt(throwable);
        }
    }

    /**
     * 分发热更新完成事件。
     *
     * @param param 框架参数
     */
    public final void handleHotReloaded(@NonNull XposedModuleInterface.HotReloadedParam param) {
        dispatch(LifecycleStage.HOT_RELOADED, param, this::onHotReloaded);
    }
}