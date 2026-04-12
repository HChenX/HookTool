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

import io.github.libxposed.api.XposedModuleInterface;

/**
 * AbsModule
 * <p>
 * 推荐继承此类用以实现 Hook 内容
 *
 * @author 焕晨HChen
 */
public abstract class AbsModule extends CoreTool {

    // -------------------------- hook 类内使用 -----------------------------

    protected String TAG = getClass().getSimpleName();

    public enum StageEnum {
        MODULE_LOADED,
        PACKAGE_LOADED,
        PACKAGE_READY,
        SYSTEM_SERVER_STARTING,
        ON_CLASSLOADER,
        ON_APPLICATION_CREATED
    }

    protected boolean isEnabled() {
        return true;
    }

    /**
     * 模块应在此方法下编写 Hook 逻辑
     * <p>
     * 使用 {@link StageEnum} 判断当前阶段
     * <p>
     * {@link StageEnum#MODULE_LOADED} -> {@link XposedModuleInterface.ModuleLoadedParam}
     * <p>
     * {@link StageEnum#PACKAGE_LOADED} -> {@link XposedModuleInterface.PackageLoadedParam}
     * <p>
     * {@link StageEnum#PACKAGE_READY} -> {@link XposedModuleInterface.PackageLoadedParam}
     * <p>
     * {@link StageEnum#SYSTEM_SERVER_STARTING} -> {@link XposedModuleInterface.SystemServerStartingParam}
     */
    protected abstract void onLoaded(@NonNull StageEnum stage, @NonNull Object param);

    /**
     * 传入自定义 ClassLoader 参数时调用
     * <p>
     * 请勿直接使用此方法传入 ClassLoader 参数，因为不受 isEnabled 控制
     */
    protected void onClassLoader(@NonNull ClassLoader classLoader) {
    }

    /**
     * 目标应用 Application 创建时调用
     */
    protected void onApplicationCreated(@NonNull Context context) {
    }

    /**
     * 以上所有阶段发生崩溃时调用
     * <p>
     * 请勿在此处继续执行任何任务，请执行清理工作
     */
    protected void onThrow(@NonNull StageEnum stage, @NonNull Throwable e) {
    }

    // ------------------------------ 入口类内统一调用 ------------------------------------

    final public void handleModuleLoaded(@NonNull XposedModuleInterface.ModuleLoadedParam param) {
        try {
            if (!isEnabled()) return;

            Objects.requireNonNull(param);
            onLoaded(StageEnum.MODULE_LOADED, param);
        } catch (Throwable e) {
            onThrow(StageEnum.MODULE_LOADED, e);
            logE(TAG, e);
        }
    }

    final public void handlePackageLoaded(@NonNull XposedModuleInterface.PackageLoadedParam param) {
        try {
            if (!isEnabled()) return;

            Objects.requireNonNull(param);
            onLoaded(StageEnum.PACKAGE_LOADED, param);
        } catch (Throwable e) {
            onThrow(StageEnum.PACKAGE_LOADED, e);
            logE(TAG, e);
        }
    }

    final public void handlePackageReady(@NonNull XposedModuleInterface.PackageReadyParam param) {
        try {
            if (!isEnabled()) return;

            Objects.requireNonNull(param);
            onLoaded(StageEnum.PACKAGE_READY, param);
        } catch (Throwable e) {
            onThrow(StageEnum.PACKAGE_READY, e);
            logE(TAG, e);
        }
    }

    final public void handleSystemServerStarting(@NonNull XposedModuleInterface.SystemServerStartingParam param) {
        try {
            if (!isEnabled()) return;

            Objects.requireNonNull(param);
            onLoaded(StageEnum.SYSTEM_SERVER_STARTING, param);
        } catch (Throwable e) {
            onThrow(StageEnum.SYSTEM_SERVER_STARTING, e);
            logE(TAG, e);
        }
    }

    final public void handleClassLoader(@NonNull ClassLoader classLoader) {
        try {
            if (!isEnabled()) return;

            Objects.requireNonNull(classLoader);
            onClassLoader(classLoader);
        } catch (Throwable e) {
            onThrow(StageEnum.ON_CLASSLOADER, e);
            logE(TAG, e);
        }
    }

    final public void handleApplicationCreated(@NonNull Context context) {
        try {
            if (!isEnabled()) return;

            Objects.requireNonNull(context);
            onApplicationCreated(context);
        } catch (Throwable e) {
            onThrow(StageEnum.ON_APPLICATION_CREATED, e);
            logE(TAG, e);
        }
    }
}
