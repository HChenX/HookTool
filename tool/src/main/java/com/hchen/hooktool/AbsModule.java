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
 * AbsModule
 * <p>
 * 推荐继承此类用以实现 Hook 内容
 *
 * @author 焕晨HChen
 */
public abstract class AbsModule extends CoreTool {

    // -------------------------- hook 类内使用 -----------------------------

    protected final String TAG = getClass().getSimpleName();

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

    final public void handleModuleLoaded(@NonNull XposedModuleInterface.ModuleLoadedParam param) {
        dispatch(StageEnum.MODULE_LOADED, param, new Consumer<XposedModuleInterface.ModuleLoadedParam>() {
            @Override
            public void accept(XposedModuleInterface.ModuleLoadedParam p) {
                onLoaded(StageEnum.MODULE_LOADED, p);
            }
        });
    }

    final public void handlePackageLoaded(@NonNull XposedModuleInterface.PackageLoadedParam param) {
        dispatch(StageEnum.PACKAGE_LOADED, param, new Consumer<XposedModuleInterface.PackageLoadedParam>() {
            @Override
            public void accept(XposedModuleInterface.PackageLoadedParam p) {
                onLoaded(StageEnum.PACKAGE_LOADED, p);
            }
        });
    }

    final public void handlePackageReady(@NonNull XposedModuleInterface.PackageReadyParam param) {
        dispatch(StageEnum.PACKAGE_READY, param, new Consumer<XposedModuleInterface.PackageReadyParam>() {
            @Override
            public void accept(XposedModuleInterface.PackageReadyParam p) {
                onLoaded(StageEnum.PACKAGE_READY, p);
            }
        });
    }

    final public void handleSystemServerStarting(@NonNull XposedModuleInterface.SystemServerStartingParam param) {
        dispatch(StageEnum.SYSTEM_SERVER_STARTING, param, new Consumer<XposedModuleInterface.SystemServerStartingParam>() {
            @Override
            public void accept(XposedModuleInterface.SystemServerStartingParam p) {
                onLoaded(StageEnum.SYSTEM_SERVER_STARTING, p);
            }
        });
    }

    final public void handleClassLoader(@NonNull ClassLoader classLoader) {
        dispatch(StageEnum.ON_CLASSLOADER, classLoader, new Consumer<ClassLoader>() {
            @Override
            public void accept(ClassLoader classLoader) {
                onClassLoader(classLoader);
            }
        });
    }

    final public void handleApplicationCreated(@NonNull Context context) {
        dispatch(StageEnum.ON_APPLICATION_CREATED, context, new Consumer<Context>() {
            @Override
            public void accept(Context context) {
                onApplicationCreated(context);
            }
        });
    }
}
