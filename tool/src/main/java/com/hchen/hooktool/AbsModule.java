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

import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.core.CoreTool;
import com.hchen.hooktool.log.XposedLog;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import io.github.libxposed.api.XposedModuleInterface;

/**
 * Hook 模块的抽象基类。
 *
 * @author 焕晨HChen
 */
public abstract class AbsModule extends CoreTool {

    // -------------------------- hook 类内使用 -----------------------------

    /**
     * 当前实例的日志标签，取值为子类的简单类名，用于标识日志来源。
     */
    @NonNull
    protected final String TAG = getClass().getSimpleName();

    /**
     * 模块生命周期阶段枚举，用于标识当前回调所处的处理节点。
     * <p>
     * 该枚举主要用于框架内部的事件分发与异常回溯，开发者无需直接操作此枚举。
     */
    public enum StageEnum {
        MODULE_LOADED, PACKAGE_LOADED, PACKAGE_READY,
        SYSTEM_SERVER_STARTING, ON_CLASSLOADER,
        ON_APPLICATION_CREATED,
        HOT_RELOADING, HOT_RELOADED
    }

    /**
     * 判断当前模块是否处于启用状态。
     *
     * @return {@code true} 表示模块已启用，{@code false} 表示模块已禁用
     */
    protected boolean isEnabled() {
        return true;
    }

    /**
     * 模块加载完成时的回调。
     *
     * @param param 模块加载参数，包含框架相关信息
     */
    protected void onModuleLoaded(@NonNull XposedModuleInterface.ModuleLoadedParam param) {
    }

    /**
     * 目标应用包加载完成时的回调。
     *
     * @param param 包加载参数，包含目标包的相关信息
     */
    protected void onPackageLoaded(@NonNull XposedModuleInterface.PackageLoadedParam param) {
    }

    /**
     * 目标应用包资源就绪时的回调。
     *
     * @param param 包就绪参数，包含目标包的类加载器等信息
     */
    protected abstract void onPackageReady(@NonNull XposedModuleInterface.PackageReadyParam param);

    /**
     * 系统服务器启动时的回调。
     *
     * @param param 系统服务器启动参数
     */
    protected void onSystemServerStarting(@NonNull XposedModuleInterface.SystemServerStartingParam param) {
    }

    /**
     * 模块即将被热更新时的回调（在旧代码中执行）。
     *
     * @param extras 热更新触发时传递的附加数据
     * @return 需要保存的状态键值对；若无需保存则返回空 {@link HashMap}
     */
    @NonNull
    protected Map<String, Object> onHotReloading(@Nullable Bundle extras) {
        return new HashMap<>();
    }

    /**
     * 模块热更新完成时的回调（在新代码中执行）。
     *
     * @param param 热更新完成参数，包含保存的状态等信息
     */
    protected void onHotReloaded(@NonNull XposedModuleInterface.HotReloadedParam param) {
    }

    /**
     * 当外部注入自定义 {@link ClassLoader} 时触发的回调。
     *
     * @param classLoader 由外部注入的目标应用类加载器
     */
    protected void onClassLoader(@NonNull ClassLoader classLoader) {
    }

    /**
     * 当目标应用的 {@link Application} 创建完成时触发的回调。
     *
     * @param context 目标应用的上下文对象
     */
    protected void onApplicationCreated(@NonNull Context context) {
    }

    /**
     * 当任意生命周期回调执行期间抛出未捕获异常时触发的回调。
     * <p>
     * 此方法仅用于执行必要的清理工作，请勿在其中继续执行业务逻辑。
     * 异常信息会同步记录至日志系统。
     *
     * @param stage 发生异常时所处的生命周期阶段
     * @param e     被捕获的异常对象
     */
    protected void onThrow(@NonNull StageEnum stage, @NonNull Throwable e) {
    }

    // ------------------------------ 入口类内统一调用 ------------------------------------

    /**
     * 生命周期回调的统一分发器。
     *
     * @param <T>    生命周期参数的泛型类型
     * @param stage  当前生命周期阶段
     * @param param  传递给回调的参数对象
     * @param action 待执行的回调逻辑
     */
    private <T> void dispatch(@NonNull StageEnum stage, @NonNull T param, @NonNull Consumer<T> action) {
        try {
            if (!isEnabled()) {
                return;
            }

            Objects.requireNonNull(param);
            action.accept(param);
        } catch (Throwable e) {
            try {
                onThrow(stage, e);
            } catch (Throwable onThrowError) {
                XposedLog.logW(TAG, "onThrow() threw an exception, ignored to preserve the original exception.", onThrowError);
            }
            CoreTool.throwIt(e);
        }
    }

    /**
     * 分发模块加载事件。
     *
     * @param param Xposed 框架传入的模块加载参数
     */
    final public void handleModuleLoaded(@NonNull XposedModuleInterface.ModuleLoadedParam param) {
        dispatch(StageEnum.MODULE_LOADED, param, this::onModuleLoaded);
    }

    /**
     * 分发目标应用包加载事件。
     *
     * @param param Xposed 框架传入的包加载参数
     */
    final public void handlePackageLoaded(@NonNull XposedModuleInterface.PackageLoadedParam param) {
        dispatch(StageEnum.PACKAGE_LOADED, param, this::onPackageLoaded);
    }

    /**
     * 分发目标应用包就绪事件。
     *
     * @param param Xposed 框架传入的包就绪参数
     */
    final public void handlePackageReady(@NonNull XposedModuleInterface.PackageReadyParam param) {
        dispatch(StageEnum.PACKAGE_READY, param, this::onPackageReady);
    }

    /**
     * 分发系统服务器启动事件。
     *
     * @param param Xposed 框架传入的系统服务器启动参数
     */
    final public void handleSystemServerStarting(@NonNull XposedModuleInterface.SystemServerStartingParam param) {
        dispatch(StageEnum.SYSTEM_SERVER_STARTING, param, this::onSystemServerStarting);
    }

    /**
     * 分发类加载器注入事件。
     *
     * @param classLoader 由外部注入的目标应用类加载器
     */
    final public void handleClassLoader(@NonNull ClassLoader classLoader) {
        dispatch(StageEnum.ON_CLASSLOADER, classLoader, this::onClassLoader);
    }

    /**
     * 分发目标应用创建事件。
     *
     * @param context 目标应用的上下文对象
     */
    final public void handleApplicationCreated(@NonNull Context context) {
        dispatch(StageEnum.ON_APPLICATION_CREATED, context, this::onApplicationCreated);
    }

    /**
     * 分发模块热更新前事件（在旧代码中执行）。
     *
     * @param extras 热更新附加数据 {@link Bundle}
     * @return 当前实例返回的状态数据；若被禁用或被跳过则返回空 {@link HashMap}
     */
    @NonNull final public Map<String, Object> handleHotReloading(@Nullable Bundle extras) {
        if (!isEnabled()) {
            return new HashMap<>();
        }
        try {
            return onHotReloading(extras);
        } catch (Throwable e) {
            try {
                onThrow(StageEnum.HOT_RELOADING, e);
            } catch (Throwable onThrowError) {
                XposedLog.logW(TAG, "onThrow() threw an exception, ignored to preserve the original exception.", onThrowError);
            }
            CoreTool.throwIt(e);
            return new HashMap<>();
        }
    }

    /**
     * 分发模块热更新完成事件（在新代码中执行）。
     *
     * @param param 热更新完成参数
     */
    final public void handleHotReloaded(@NonNull XposedModuleInterface.HotReloadedParam param) {
        dispatch(StageEnum.HOT_RELOADED, param, this::onHotReloaded);
    }
}
