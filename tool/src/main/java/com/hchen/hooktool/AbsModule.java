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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import io.github.libxposed.api.XposedModuleInterface;

/**
 * Hook 模块的抽象基类，提供 Xposed 模块生命周期的统一管理框架。
 * <p>
 * 本类继承自 {@link CoreTool}，将 Xposed 框架的原始生命周期回调统一封装为
 * 以 {@link StageEnum} 标识阶段的事件流。子类只需覆写 {@link #onLoaded(StageEnum, Object)}
 * 即可在不同生命周期节点插入自定义 Hook 逻辑。
 * <p>
 * 从 API 102 开始支持热更新生命周期：
 * <ul>
 *   <li>{@link StageEnum#HOT_RELOADING} — 热更新前在旧代码中执行，可保存状态</li>
 *   <li>{@link StageEnum#HOT_RELOADED} — 热更新完成后在新代码中执行，可重新挂钩</li>
 * </ul>
 * <p>
 * 每个生命周期回调在执行前均会经过 {@link #isEnabled()} 的守卫判断，
 * 若返回 {@code false} 则该回调及其后续逻辑将被整体跳过。回调执行期间
 * 抛出的异常会被捕获并转发至 {@link #onThrow(StageEnum, Throwable)}。
 *
 * @author 焕晨HChen
 * @see CoreTool
 * @see ModuleEntrance
 */
public abstract class AbsModule extends CoreTool {

    // -------------------------- hook 类内使用 -----------------------------

    /**
     * 当前实例的日志标签，取值为子类的简单类名，用于标识日志来源。
     */
    protected final String TAG = getClass().getSimpleName();

    /**
     * 模块生命周期阶段枚举，用于标识当前回调所处的处理节点。
     * <p>
     * 每个枚举常量对应 {@link #onLoaded(StageEnum, Object)} 中的一个回调时机，
     * 开发者可据此针对不同阶段编写差异化的 Hook 处理策略。
     */
    public enum StageEnum {
        /**
         * 模块已加载。回调参数类型为 {@link XposedModuleInterface.ModuleLoadedParam}。
         */
        MODULE_LOADED,
        /**
         * 目标应用包已加载。回调参数类型为 {@link XposedModuleInterface.PackageLoadedParam}。
         */
        PACKAGE_LOADED,
        /**
         * 目标应用包资源就绪。回调参数类型为 {@link XposedModuleInterface.PackageReadyParam}。
         */
        PACKAGE_READY,
        /**
         * 系统服务器正在启动。回调参数类型为 {@link XposedModuleInterface.SystemServerStartingParam}。
         */
        SYSTEM_SERVER_STARTING,
        /**
         * 自定义类加载器已注入。回调参数类型为 {@link ClassLoader}。
         */
        ON_CLASSLOADER,
        /**
         * 目标应用的 Application 已创建。回调参数类型为 {@link Context}。
         */
        ON_APPLICATION_CREATED,
        /**
         * 模块即将被热更新（在旧代码中执行）。回调参数类型为 {@link XposedModuleInterface.HotReloadingParam}。
         */
        HOT_RELOADING,
        /**
         * 模块已完成热更新（在新代码中执行）。回调参数类型为 {@link XposedModuleInterface.HotReloadedParam}。
         */
        HOT_RELOADED
    }

    /**
     * 判断当前模块是否处于启用状态。
     * <p>
     * 此方法作为所有生命周期回调的前置守卫，返回 {@code false} 时
     * 对应回调将被直接跳过。子类可覆写此方法实现条件化的动态启停控制。
     *
     * @return {@code true} 表示模块已启用，{@code false} 表示模块已禁用
     */
    protected boolean isEnabled() {
        return true;
    }

    /**
     * Hook 逻辑的核心入口，子类必须实现此方法以注册自定义 Hook 代码。
     * <p>
     * 根据 {@code stage} 参数可判断当前所处的生命周期阶段，并将 {@code param}
     * 转换为对应的类型后使用：
     * <ul>
     *   <li>{@link StageEnum#MODULE_LOADED} → {@link XposedModuleInterface.ModuleLoadedParam}</li>
     *   <li>{@link StageEnum#PACKAGE_LOADED} → {@link XposedModuleInterface.PackageLoadedParam}</li>
     *   <li>{@link StageEnum#PACKAGE_READY} → {@link XposedModuleInterface.PackageReadyParam}</li>
     *   <li>{@link StageEnum#SYSTEM_SERVER_STARTING} → {@link XposedModuleInterface.SystemServerStartingParam}</li>
     *   <li>{@link StageEnum#HOT_RELOADING} → {@link XposedModuleInterface.HotReloadingParam}</li>
     *   <li>{@link StageEnum#HOT_RELOADED} → {@link XposedModuleInterface.HotReloadedParam}</li>
     * </ul>
     *
     * @param stage 当前生命周期阶段
     * @param param 与当前阶段匹配的生命周期参数对象
     */
    protected abstract void onLoaded(@NonNull StageEnum stage, @NonNull Object param);

    /**
     * 当外部注入自定义 {@link ClassLoader} 时触发的回调。
     * <p>
     * 注意：此方法不受 {@link #isEnabled()} 守卫控制，调用方需自行管理使用场景。
     *
     * @param classLoader 由外部注入的目标应用类加载器
     */
    protected void onClassLoader(@NonNull ClassLoader classLoader) {
    }

    /**
     * 当目标应用的 {@link Application} 创建完成时触发的回调。
     * <p>
     * 此时应用的 {@link Context} 已可用，适合在此阶段执行依赖应用上下文的 Hook 操作。
     *
     * @param context 目标应用的上下文对象
     */
    protected void onApplicationCreated(@NonNull Context context) {
    }

    /**
     * 模块即将被热更新时的回调（在旧代码中执行）。
     * <p>
     * 此方法在旧模块代码中运行，用于收集需要在热更新后恢复的状态数据。
     *
     * @param extras 热更新触发时传递的附加数据，可能为 {@code null}
     *              （当通过应用更新触发热更新或未传递额外数据时）
     * @return 需要保存的状态键值对；若无需保存则返回空 {@link HashMap}
     */
    protected Map<String, Object> onHotReloading(@Nullable Bundle extras) {
        return new HashMap<>();
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
     * <p>
     * 执行流程如下：
     * <ol>
     *   <li>通过 {@link #isEnabled()} 检查模块是否启用</li>
     *   <li>对参数执行非空校验</li>
     *   <li>执行指定的回调动作</li>
     *   <li>捕获异常并转发至 {@link #onThrow(StageEnum, Throwable)}</li>
     * </ol>
     *
     * @param <T>    生命周期参数的泛型类型
     * @param stage  当前生命周期阶段
     * @param param  传递给回调的参数对象
     * @param action 待执行的回调逻辑
     */
    private <T> void dispatch(@NonNull StageEnum stage, @NonNull T param, @NonNull Consumer<T> action) {
        try {
            if (!isEnabled()) return;

            Objects.requireNonNull(param);
            action.accept(param);
        } catch (Throwable e) {
            onThrow(stage, e);
            CoreTool.throwIt(e);
        }
    }

    /**
     * 分发模块加载事件。
     * <p>
     * 由模块入口类调用，通过 {@link #dispatch} 将事件转发至
     * {@link #onLoaded(StageEnum, Object)}，阶段标识为 {@link StageEnum#MODULE_LOADED}。
     *
     * @param param Xposed 框架传入的模块加载参数
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
     * 分发目标应用包加载事件。
     * <p>
     * 由模块入口类调用，通过 {@link #dispatch} 将事件转发至
     * {@link #onLoaded(StageEnum, Object)}，阶段标识为 {@link StageEnum#PACKAGE_LOADED}。
     *
     * @param param Xposed 框架传入的包加载参数
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
     * 分发目标应用包就绪事件。
     * <p>
     * 由模块入口类调用，通过 {@link #dispatch} 将事件转发至
     * {@link #onLoaded(StageEnum, Object)}，阶段标识为 {@link StageEnum#PACKAGE_READY}。
     *
     * @param param Xposed 框架传入的包就绪参数
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
     * 分发系统服务器启动事件。
     * <p>
     * 由模块入口类调用，通过 {@link #dispatch} 将事件转发至
     * {@link #onLoaded(StageEnum, Object)}，阶段标识为 {@link StageEnum#SYSTEM_SERVER_STARTING}。
     *
     * @param param Xposed 框架传入的系统服务器启动参数
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
     * 分发类加载器注入事件。
     * <p>
     * 由模块入口类调用，通过 {@link #dispatch} 将事件转发至
     * {@link #onClassLoader(ClassLoader)}，阶段标识为 {@link StageEnum#ON_CLASSLOADER}。
     *
     * @param classLoader 由外部注入的目标应用类加载器
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
     * 分发目标应用创建事件。
     * <p>
     * 由模块入口类调用，通过 {@link #dispatch} 将事件转发至
     * {@link #onApplicationCreated(Context)}，阶段标识为 {@link StageEnum#ON_APPLICATION_CREATED}。
     *
     * @param context 目标应用的上下文对象
     */
    final public void handleApplicationCreated(@NonNull Context context) {
        dispatch(StageEnum.ON_APPLICATION_CREATED, context, new Consumer<Context>() {
            @Override
            public void accept(Context context) {
                onApplicationCreated(context);
            }
        });
    }

    /**
     * 分发模块热更新前事件（在旧代码中执行）。
     * <p>
     * 由模块入口类调用，将事件转发至 {@link #onHotReloading(Bundle)}，
     * 并传入 {@code extras} Bundle。若当前实例已被禁用则会跳过此次回调。
     * <p>
     * 该方法返回类型已标注 {@link NonNull}，异常路径通过
     * {@link CoreTool#throwIt(Throwable)} 重新抛出异常，因此调用方
     * 不应假定异常情况下会返回有效值。
     *
     * @param extras 热更新附加数据 {@link Bundle}，可能为 {@code null}
     * @return 当前实例返回的状态数据；若被禁用或被跳过则返回空 {@link HashMap}
     */
    @NonNull final public Map<String, Object> handleHotReloading(@Nullable Bundle extras) {
        if (!isEnabled()) return new HashMap<>();
        try {
            return onHotReloading(extras);
        } catch (Throwable e) {
            onThrow(StageEnum.HOT_RELOADING, e);
            CoreTool.throwIt(e);
            return new HashMap<>();
        }
    }

    /**
     * 分发模块热更新完成事件（在新代码中执行）。
     * <p>
     * 由模块入口类调用，通过 {@link #dispatch} 将事件转发至
     * {@link #onLoaded(StageEnum, Object)}，阶段标识为 {@link StageEnum#HOT_RELOADED}。
     *
     * @param param 热更新完成参数
     */
    final public void handleHotReloaded(@NonNull XposedModuleInterface.HotReloadedParam param) {
        dispatch(StageEnum.HOT_RELOADED, param, new Consumer<XposedModuleInterface.HotReloadedParam>() {
            @Override
            public void accept(XposedModuleInterface.HotReloadedParam p) {
                onLoaded(StageEnum.HOT_RELOADED, p);
            }
        });
    }
}
