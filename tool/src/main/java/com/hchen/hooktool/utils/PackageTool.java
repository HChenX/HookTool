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
package com.hchen.hooktool.utils;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.UserHandle;

import androidx.annotation.NonNull;

import com.hchen.hooktool.callback.IAppDataGetter;
import com.hchen.hooktool.callback.IDecomposer;
import com.hchen.hooktool.core.CoreTool;
import com.hchen.hooktool.data.AppData;
import com.hchen.hooktool.exception.UnexpectedException;
import com.hchen.hooktool.helper.TryHelper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Android 应用包信息查询工具类。
 * <p>
 * 提供以下功能：
 * <ul>
 *     <li>应用安装状态检测与启用/禁用状态查询</li>
 *     <li>系统应用判定</li>
 *     <li>根据应用 uid 获取所属 user ID</li>
 *     <li>将多种包信息类型（{@link PackageInfo}、{@link ApplicationInfo}、{@link ResolveInfo} 等）
 *         统一转换为 {@link AppData} 数据结构</li>
 *     <li>支持同步与异步两种模式获取应用数据</li>
 * </ul>
 * <p>
 * 该类为纯工具类，所有方法均为静态方法，不允许实例化。
 *
 * @author 焕晨HChen
 */
public final class PackageTool {
    private static final String TAG = "PackageTool";

    private PackageTool() {
    }

    /**
     * 判断指定包名的应用是否已安装在当前设备上。
     * <p>
     * 内部通过 {@link PackageManager#getPackageInfo(String, int)} 进行查询，
     * 若抛出 {@link PackageManager.NameNotFoundException} 则视为未安装。
     *
     * @param context     上下文对象，不得为 {@code null}
     * @param packageName 待查询的应用包名
     * @return 已安装返回 {@code true}，未安装返回 {@code false}
     */
    public static boolean isInstalled(@NonNull Context context, @NonNull String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * 判断指定包名的应用是否已被禁用。
     * <p>
     * 通过 {@link PackageManager#getApplicationInfo(String, int)} 获取 {@link ApplicationInfo}
     * 并检查其 {@link ApplicationInfo#enabled} 标志位。
     *
     * @param context     上下文对象，不得为 {@code null}
     * @param packageName 待查询的应用包名
     * @return 已被禁用返回 {@code true}
     * @throws RuntimeException 若包名对应的包不存在（{@link PackageManager.NameNotFoundException}）
     */
    public static boolean isDisable(@NonNull Context context, @NonNull String packageName) {
        try {
            ApplicationInfo result = context.getPackageManager().getApplicationInfo(packageName, 0);
            return !result.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            CoreTool.throwIt(e);
            return false; // Not actually executed
        }
    }

    /**
     * 根据应用的 uid 获取所属的 user ID。
     * <p>
     * 内部通过反射调用 {@code UserHandle.getUserId(int)} 方法实现转换。
     * 若调用失败，返回 -1。
     *
     * @param uid 应用的 uid
     * @return 对应的 user ID，获取失败时返回 -1
     */
    public static int getUserId(int uid) {
        return TryHelper.doTry(new IDecomposer<Integer>() {
            @Override
            public Integer get() throws Throwable {
                Object result = InvokeTool.callStaticMethod(
                    UserHandle.class,
                    "getUserId",
                    new Class[]{int.class},
                    uid
                );
                return result != null ? (int) result : -1;
            }
        }).getOrDefault(-1);
    }

    /**
     * 判断给定的 {@link ApplicationInfo} 是否属于系统应用。
     * <p>
     * 满足以下任一条件即视为系统应用：
     * <ul>
     *     <li>uid 小于 10000</li>
     *     <li>flags 包含 {@link ApplicationInfo#FLAG_SYSTEM}</li>
     *     <li>flags 包含 {@link ApplicationInfo#FLAG_UPDATED_SYSTEM_APP}</li>
     * </ul>
     *
     * @param app {@link ApplicationInfo} 对象，不得为 {@code null}
     * @return 是系统应用返回 {@code true}
     */
    public static boolean isSystem(@NonNull ApplicationInfo app) {
        if (app.uid < 10000) {
            return true;
        }
        return (app.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    /**
     * 通过自定义查询逻辑同步获取应用数据。
     * <p>
     * 此方法为 {@link #getAppData(Context, boolean, IAppDataGetter)} 的便捷重载，
     * 默认使用同步模式（{@code async = false}）。
     *
     * @param context        上下文对象，不得为 {@code null}
     * @param iAppDataGetter 自定义的应用数据查询回调，不得为 {@code null}
     * @param <T>            包信息类型（如 {@link PackageInfo}、{@link ApplicationInfo} 等）
     * @return 包含查询结果的 {@link AppData} 数组
     */
    public static <T> AppData[] getAppData(@NonNull Context context, @NonNull IAppDataGetter<T> iAppDataGetter) {
        return getAppData(context, false, iAppDataGetter);
    }

    /**
     * 通过自定义查询逻辑获取应用数据，支持同步或异步模式。
     * <p>
     * 泛型参数 {@code T} 支持以下类型：{@link PackageInfo}、{@link ResolveInfo}、{@link ActivityInfo}、
     * {@link ApplicationInfo}、{@link ProviderInfo}。
     * <p>
     * <b>同步模式</b>（{@code async = false}）：在当前线程中执行查询并直接返回 {@link AppData} 数组。
     * <p>
     * <b>异步模式</b>（{@code async = true}）：在独立线程中执行查询，结果通过
     * {@link IAppDataGetter#getAsyncAppData} 回调返回，本方法返回 {@code null}。
     * <p>
     * 使用示例：
     * <pre>{@code
     * // 同步模式
     * AppData[] appData = PackageTool.getAppData(context, false, new IAppDataGetter<PackageInfo>() {
     *      @Override
     *      @NonNull
     *      public List<PackageInfo> getPackages(@NonNull PackageManager pm) throws PackageManager.NameNotFoundException {
     *          PackageInfo packageInfo = null;
     *          ArrayList<PackageInfo> arrayList = new ArrayList<>();
     *          arrayList.add(packageInfo);
     *          return arrayList;
     *      }
     * });
     *
     * // 异步模式
     * PackageTool.getAppData(context, true, new IAppDataGetter<PackageInfo>() {
     *      @Override
     *      @NonNull
     *      public List<PackageInfo> getPackages(@NonNull PackageManager pm) throws PackageManager.NameNotFoundException {
     *          PackageInfo packageInfo = null;
     *          ArrayList<PackageInfo> arrayList = new ArrayList<>();
     *          arrayList.add(packageInfo);
     *          return arrayList;
     *      }
     *
     *      @Override
     *      public void getAsyncAppData(@NonNull AppData[] appData, @Nullable PackageManager.NameNotFoundException e) {
     *          IAppDataGetter.super.getAsyncAppData(appData, e);
     *      }
     * });
     * }
     *
     * @param context        上下文对象，不得为 {@code null}
     * @param async          是否使用异步模式
     * @param iAppDataGetter 自定义的应用数据查询回调，不得为 {@code null}
     * @param <T>            包信息类型
     * @return 同步模式下返回 {@link AppData} 数组；异步模式下返回 {@code null}
     * @see #createAppData(PackageManager, Object)
     */
    public static <T> AppData[] getAppData(@NonNull Context context, boolean async, @NonNull IAppDataGetter<T> iAppDataGetter) {
        PackageManager packageManager = context.getPackageManager();
        try {
            if (async) {
                ExecutorService service = null;
                try {
                    // noinspection resource
                    service = Executors.newSingleThreadExecutor();
                    service.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                List<T> ts = iAppDataGetter.getPackages(packageManager);
                                AppData[] appDataArray = new AppData[ts.size()];
                                for (int i = 0; i < ts.size(); i++) {
                                    appDataArray[i] = createAppData(packageManager, ts.get(i));
                                }
                                iAppDataGetter.getAsyncAppData(appDataArray, null);
                            } catch (PackageManager.NameNotFoundException e) {
                                iAppDataGetter.getAsyncAppData(new AppData[]{}, e);
                                CoreTool.throwIt(e);
                            }
                        }
                    });
                } finally {
                    if (service != null) {
                        service.shutdown();
                    }
                }
                return null;
            } else {
                List<T> ts = iAppDataGetter.getPackages(packageManager);
                AppData[] appDataArray = new AppData[ts.size()];
                for (int i = 0; i < ts.size(); i++) {
                    appDataArray[i] = createAppData(packageManager, ts.get(i));
                }
                return appDataArray;
            }
        } catch (PackageManager.NameNotFoundException e) {
            CoreTool.throwIt(e);
            return null; // Not actually executed
        }
    }

    /**
     * 将包信息对象转换为统一的 {@link AppData} 数据结构。
     * <p>
     * 支持的输入类型包括：{@link PackageInfo}、{@link ApplicationInfo}、{@link ResolveInfo}、
     * {@link ActivityInfo}、{@link ServiceInfo}、{@link ProviderInfo}。
     * <p>
     * 转换过程中自动填充以下字段：应用图标、标签、包名、版本号、系统应用标记、启用状态及用户 ID。
     *
     * @param pm  {@link PackageManager} 实例，用于加载应用图标和标签
     * @param t   待转换的包信息对象
     * @param <T> 包信息类型
     * @return 填充完毕的 {@link AppData} 实例
     * @throws UnexpectedException 若传入的对象类型不受支持
     */
    @NonNull
    public static <T> AppData createAppData(@NonNull PackageManager pm, @NonNull T t) {
        AppData appData = new AppData();
        PackageInfo packageInfo = null;
        ApplicationInfo applicationInfo = null;

        // 根据不同类型的 T 对象获取 ApplicationInfo
        if (t instanceof PackageInfo info) {
            packageInfo = info;
            applicationInfo = info.applicationInfo;
            appData.versionName = info.versionName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                appData.versionCode = Long.toString(info.getLongVersionCode());
            } else {
                appData.versionCode = Integer.toString(info.versionCode);
            }
        } else if (t instanceof ApplicationInfo appInfo) {
            applicationInfo = appInfo;
        } else if (t instanceof ResolveInfo resolveInfo) {
            applicationInfo = aboutResolveInfo(resolveInfo).applicationInfo;
        } else if (t instanceof ActivityInfo activityInfo) {
            applicationInfo = activityInfo.applicationInfo;
        } else if (t instanceof ServiceInfo serviceInfo) {
            applicationInfo = serviceInfo.applicationInfo;
        } else if (t instanceof ProviderInfo providerInfo) {
            applicationInfo = providerInfo.applicationInfo;
        } else {
            throw new UnexpectedException("Unknown type: " + t);
        }

        // 填充应用数据
        if (applicationInfo != null) {
            appData.packageInfo = packageInfo;
            appData.applicationInfo = applicationInfo;
            appData.icon = BitmapTool.drawableToBitmap(applicationInfo.loadIcon(pm));
            appData.label = applicationInfo.loadLabel(pm).toString();
            appData.packageName = applicationInfo.packageName;
            appData.isSystemApp = isSystem(applicationInfo);
            appData.isEnabled = applicationInfo.enabled;
            appData.user = getUserId(applicationInfo.uid);
            appData.uid = applicationInfo.uid;
        }

        return appData;
    }

    /**
     * 从 {@link ResolveInfo} 中提取 {@link ComponentInfo}。
     * <p>
     * 按优先级依次尝试获取 {@code activityInfo}、{@code serviceInfo}、{@code providerInfo}。
     * 若三者均为 {@code null}，则抛出异常。
     *
     * @param resolveInfo {@link ResolveInfo} 对象
     * @return 包含 {@link ApplicationInfo} 的 {@link ComponentInfo}
     * @throws UnexpectedException 若无法从 ResolveInfo 中获取任何应用组件信息
     */
    @NonNull
    private static ComponentInfo aboutResolveInfo(@NonNull ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo != null) return resolveInfo.activityInfo;
        if (resolveInfo.serviceInfo != null) return resolveInfo.serviceInfo;
        if (resolveInfo.providerInfo != null) return resolveInfo.providerInfo;
        throw new UnexpectedException("Unable to obtain application information.");
    }
}
