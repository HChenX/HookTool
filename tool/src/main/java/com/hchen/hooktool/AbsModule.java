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

    protected boolean isEnabled() {
        return true;
    }

    /**
     * 模块加载时调用
     */
    protected void onModuleLoaded(@NonNull XposedModuleInterface.ModuleLoadedParam param) {
    }

    /**
     * 目标应用加载包时调用
     */
    protected void onPackageLoaded(@NonNull XposedModuleInterface.PackageLoadedParam param) {
    }

    /**
     * 目标应用包加载完毕时调用
     * <p>
     * 推荐在此时编写 Hook
     */
    protected abstract void onPackageReady(@NonNull XposedModuleInterface.PackageReadyParam param);

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
    protected void onThrow(@NonNull Throwable e) {
    }

    // ------------------------------ 入口类内统一调用 ------------------------------------

    final public void handleModuleLoaded(@NonNull XposedModuleInterface.ModuleLoadedParam param) {
        try {
            if (!isEnabled()) return;

            Objects.requireNonNull(param);
            onModuleLoaded(param);
        } catch (Throwable e) {
            onThrow(e);
            logE(TAG, e);
        }
    }

    final public void handlePackageLoaded(@NonNull XposedModuleInterface.PackageLoadedParam param) {
        try {
            if (!isEnabled()) return;

            Objects.requireNonNull(param);
            onPackageLoaded(param);
        } catch (Throwable e) {
            onThrow(e);
            logE(TAG, e);
        }
    }

    final public void handlePackageReady(@NonNull XposedModuleInterface.PackageReadyParam param) {
        try {
            if (!isEnabled()) return;

            Objects.requireNonNull(param);
            onPackageReady(param);
        } catch (Throwable e) {
            onThrow(e);
            logE(TAG, e);
        }
    }

    final public void handleClassLoader(@NonNull ClassLoader classLoader) {
        try {
            if (!isEnabled()) return;

            Objects.requireNonNull(classLoader);
            onClassLoader(classLoader);
        } catch (Throwable e) {
            onThrow(e);
            logE(TAG, e);
        }
    }

    final public void handleApplicationCreated(@NonNull Context context) {
        try {
            if (!isEnabled()) return;

            Objects.requireNonNull(context);
            onApplicationCreated(context);
        } catch (Throwable e) {
            onThrow(e);
            logE(TAG, e);
        }
    }
}
