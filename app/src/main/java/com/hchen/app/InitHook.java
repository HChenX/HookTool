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
package com.hchen.app;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.app.hook.TestHook;
import com.hchen.app.hook.TestHookKt;
import com.hchen.hooktool.ModuleConfig;
import com.hchen.hooktool.ModuleData;
import com.hchen.hooktool.ModuleEntrance;
import com.hchen.hooktool.hook.HookRegistry;
import com.hchen.hooktool.log.AndroidLog;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * HookTool 模块的主入口类。
 *
 * <p>继承自 {@link ModuleEntrance}，是整个 Hook 模块生命周期的核心控制器。
 * 本类涵盖了模块从加载到运行的完整生命周期回调，包括：</p>
 * <ul>
 *   <li>模块全局配置初始化（{@link #initModuleConfig()}）</li>
 *   <li>忽略包名过滤（{@link #ignorePackages()}）</li>
 *   <li>模块加载、包加载、包就绪、系统服务启动、Application 创建等回调</li>
 * </ul>
 *
 * @see ModuleEntrance
 * @see TestHook
 */
public class InitHook extends ModuleEntrance {
    private static final String TAG = "InitHook";

    /**
     * 配置模块的全局参数。
     *
     * <p>在模块首次加载时由框架调用，完成以下配置：</p>
     * <ul>
     *   <li>日志标签：{@code "TestDemo"}</li>
     *   <li>日志等级：{@link ModuleConfig#LOG_D}（调试级别）</li>
     *   <li>偏好设置文件名：{@code "test_demo_prefs"}</li>
     *   <li>日志扩展路径：{@code "com.hchen.app.hook"}</li>
     *   <li>启用 Hook 成功日志输出</li>
     * </ul>
     */
    @Override
    public void initModuleConfig() {
        ModuleConfig.setLogTag("TestDemo");
        ModuleConfig.setLogLevel(ModuleConfig.LOG_D);
        ModuleConfig.setPrefsName("test_demo_prefs");
        ModuleConfig.setLogExpandPaths(new String[]{
            "com.hchen.app.hook"
        });
        ModuleConfig.setShowHookSuccessLog(true);
    }

    /**
     * 获取需要被模块忽略的包名列表。
     *
     * <p>返回的包名在模块加载流程中将被直接跳过，
     * 不会执行任何 Hook 操作。当前忽略的包为 {@code "com.miui.contentcatcher"}。</p>
     *
     * @return 包名字符串数组，表示需要排除的目标包
     */
    @NonNull
    @Override
    public String[] ignorePackages() {
        return new String[]{"com.miui.contentcatcher"};
    }

    /**
     * 模块自身加载完成时的回调。
     *
     * <p>当 Hook 框架完成模块的初始化加载后触发此方法。
     * 当前实现输出是否为系统服务进程及进程名称等调试信息，
     * 并委托父类执行默认处理。</p>
     *
     * @param param 模块加载参数，包含是否为系统服务进程及进程名称等信息
     * @see ModuleEntrance#handleModuleLoaded(ModuleLoadedParam)
     */
    @Override
    public void handleModuleLoaded(@NonNull ModuleLoadedParam param) {
        AndroidLog.logD(TAG, "handleModuleLoaded: " + param.isSystemServer() + ", " + param.getProcessName() + ", " + param);
        super.handleModuleLoaded(param);
    }

    /**
     * 目标应用包加载完成时的回调。
     *
     * <p>当目标应用的包被加载到内存后触发此方法。
     * 当前实现输出是否为首次加载、包名、ApplicationInfo、
     * 默认类加载器等调试信息，并委托父类执行默认处理。</p>
     *
     * @param param 包加载参数，包含包名、应用信息、类加载器及是否为首次加载等
     * @see ModuleEntrance#handlePackageLoaded(PackageLoadedParam)
     */
    @Override
    public void handlePackageLoaded(@NonNull PackageLoadedParam param) {
        AndroidLog.logD(TAG, "handlePackageLoaded: " + param.isFirstPackage() + ", " + param.getPackageName() +
            ", " + param.getApplicationInfo() + ", " + param.getDefaultClassLoader() + ", " + param);
        super.handlePackageLoaded(param);
    }

    /**
     * 目标应用包就绪时的回调。
     *
     * <p>当目标应用的包完全就绪（类加载器和组件工厂均已可用）后触发此方法。
     * 当前实现输出类加载器和组件工厂的调试信息，委托父类执行默认处理，
     * 并在此基础上完成以下操作：</p>
     * <ul>
     *   <li>通过 {@link ModuleData#setClassLoader} 设置全局类加载器</li>
     *   <li>创建并初始化 {@link TestHook} 模块实例</li>
     * </ul>
     *
     * @param param 包就绪参数，提供目标应用的类加载器和组件工厂等信息
     * @see ModuleEntrance#handlePackageReady(PackageReadyParam)
     * @see TestHook
     */
    @Override
    public void handlePackageReady(@NonNull PackageReadyParam param) {
        AndroidLog.logD(TAG, "handlePackageReady: " + param.getClassLoader() + ", " + param.getAppComponentFactory() + ", " + param);
        super.handlePackageReady(param);

        ModuleData.setClassLoader(param.getClassLoader());
        new TestHook().handlePackageReady(param);
    }

    /**
     * 系统服务进程启动时的回调。
     *
     * <p>当系统服务（system_server）进程启动时触发此方法。
     * 当前实现输出系统服务类加载器的调试信息，
     * 并委托父类执行默认处理。</p>
     *
     * @param param 系统服务启动参数，提供系统服务进程的类加载器
     * @see ModuleEntrance#handleSystemServerStarting(SystemServerStartingParam)
     */
    @Override
    public void handleSystemServerStarting(@NonNull SystemServerStartingParam param) {
        AndroidLog.logD(TAG, "handleSystemServerStarting: " + param.getClassLoader() + ", " + param);
        super.handleSystemServerStarting(param);
    }

    /**
     * 目标应用的 {@link android.app.Application} 创建完成时的回调。
     *
     * <p>当目标应用的 Application 对象实例化完成后触发此方法。
     * 当前实现输出应用上下文的调试信息，并委托父类执行默认处理。</p>
     *
     * @param context 目标应用已创建完成的 {@link Context} 上下文实例
     * @see ModuleEntrance#handleApplicationCreated(Context)
     */
    @Override
    public void handleApplicationCreated(@NonNull Context context) {
        AndroidLog.logD(TAG, "handleApplicationCreated: " + context);
        super.handleApplicationCreated(context);
    }

    /**
     * 分发模块热更新前事件（在旧代码中执行）。
     * <p>
     * 收集子模块（{@link TestHook} 和 {@link TestHookKt}）返回的状态数据，
     * 合并后返回给 {@link ModuleEntrance#onHotReloading(HotReloadingParam)}，
     * 由其统一与 {@link HookRegistry#reloading(Bundle)} 的结果合并。
     *
     * @param extras 热更新触发的附加数据 {@link Bundle}，可能为 {@code null}
     * @return 子模块级状态键值对合并后的 {@link Map}；异常时返回空 {@link HashMap}
     * @see ModuleEntrance#handleHotReloading(Bundle)
     */
    @NonNull
    @Override
    public Map<String, Object> handleHotReloading(@Nullable Bundle extras) {
        AndroidLog.logD(TAG, "handleHotReloading: " + extras);

        Map<String, Object> map = new HashMap<>();
        map.putAll(Objects.requireNonNull(new TestHook().handleHotReloading(extras)));
        map.putAll(Objects.requireNonNull(new TestHookKt().handleHotReloading(extras)));
        return map;
    }

    /**
     * 分发模块热更新完成事件（在新代码中执行）。
     * <p>
     * 先通过父类恢复框架级状态（ClassLoader、Xposed 环境等），
     * 再将恢复的 ClassLoader 设置到 {@link ModuleData} 中供后续 Hook 使用，
     * 最后依次通知子模块执行其内部状态恢复。
     * <p>
     * 注意：旧 Hook 句柄的解除由 {@code onHotReloaded} 的 {@code finally} 块统一处理，
     * 此处不再需要手动调用 {@link HookRegistry#reloaded(HotReloadedParam)}。
     *
     * @param param       热更新完成参数，不为 {@code null}
     * @param classLoader 从旧代码保存的状态中恢复的宿主应用 ClassLoader，不为 {@code null}
     * @see ModuleEntrance#handleHotReloaded(HotReloadedParam, ClassLoader)
     */
    @Override
    public void handleHotReloaded(@NonNull HotReloadedParam param, @NonNull ClassLoader classLoader) {
        AndroidLog.logD(TAG, "handleHotReloaded: " + param);
        super.handleHotReloaded(param, classLoader);
        ModuleData.setClassLoader(classLoader);

        new TestHook().handleHotReloaded(param);
        new TestHookKt().handleHotReloaded(param);
    }
}
