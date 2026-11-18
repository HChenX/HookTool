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
package com.hchen.hooktool.util.pkg;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.log.AndroidLog;
import com.hchen.hooktool.reflect.Reflect;
import com.hchen.hooktool.util.ui.BitmapTool;

import java.util.List;

/**
 * Android 应用包信息查询工具类。
 *
 * @author 焕晨HChen
 */
public final class PackageTool {
    private static final String TAG = "PackageTool";

    private PackageTool() {
    }

    /**
     * 判断指定包名的应用是否已安装在当前设备上。
     *
     * @param context     上下文
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
     * 包未安装时返回 {@code false}；{@link PackageManager#COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED}（首次使用自动启用）不算禁用。
     *
     * @param context     上下文
     * @param packageName 待查询的应用包名
     * @return 已被禁用返回 {@code true}
     */
    public static boolean isDisabled(@NonNull Context context, @NonNull String packageName) {
        try {
            int state = context.getPackageManager().getApplicationEnabledSetting(packageName);
            return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
        } catch (IllegalArgumentException e) { // 包未安装。
            return false;
        }
    }

    /**
     * 根据应用的 uid 获取所属的 user ID。
     *
     * @param uid 应用的 uid
     * @return 对应的 user ID，获取失败时返回 -1
     */
    public static int getUserId(int uid) {
        try {
            return (int) Reflect.callStaticMethod(UserHandle.class, "getUserId", uid);
        } catch (Throwable e) {
            AndroidLog.logE(TAG, "Unable to resolve UserHandle.getUserId.", e);
            return -1;
        }
    }

    /**
     * 判断给定的 {@link ApplicationInfo} 是否属于系统应用。
     *
     * @param app {@link ApplicationInfo} 对象
     * @return 是系统应用返回 {@code true}
     */
    public static boolean isSystem(@NonNull ApplicationInfo app) {
        if (app.uid >= 0 && app.uid < 10000) {
            return true;
        }
        return (app.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    /**
     * 按包名判断应用是否为系统应用。
     *
     * @param context     上下文
     * @param packageName 待查询的应用包名
     * @return 是系统应用返回 {@code true}；包未安装返回 {@code false}
     */
    public static boolean isSystem(@NonNull Context context, @NonNull String packageName) {
        try {
            return isSystem(context.getPackageManager().getApplicationInfo(packageName, 0));
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * 通过包名获取应用的 uid。
     *
     * @param context     上下文
     * @param packageName 待查询的应用包名
     * @return 应用 uid；包未安装返回 -1
     */
    public static int getPackageUid(@NonNull Context context, @NonNull String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return pm.getPackageUid(packageName, PackageManager.PackageInfoFlags.of(0));
            }
            return pm.getPackageUid(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    /**
     * 获取应用的版本名（versionName），包未安装时返回 {@code null}。
     *
     * @param context     上下文
     * @param packageName 待查询的应用包名
     * @return 版本名字符串；包未安装返回 {@code null}
     */
    @Nullable
    public static String getVersionName(@NonNull Context context, @NonNull String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * 获取应用的版本号（versionCode）。
     *
     * @param context     上下文
     * @param packageName 待查询的应用包名
     * @return 版本号；包未安装返回 -1
     */
    public static long getVersionCode(@NonNull Context context, @NonNull String packageName) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            return info.getLongVersionCode();
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    /**
     * 通过自定义查询逻辑同步获取应用数据。
     * <p>
     * 默认不加载图标（{@code loadIcon = false}）。
     *
     * @param context       上下文
     * @param appDataGetter 自定义的应用数据查询回调
     * @param <T>           包信息类型（如 {@link PackageInfo}、{@link ApplicationInfo} 等）
     * @return 包含查询结果的 {@link AppData} 数组
     * @throws PackageManager.NameNotFoundException 查询失败时以原始异常向上抛出
     */
    @NonNull
    public static <T> AppData[] getAppData(@NonNull Context context, @NonNull IAppDataGetter<T> appDataGetter) {
        return getAppData(context, false, appDataGetter);
    }

    /**
     * 通过自定义查询逻辑同步获取应用数据。
     * <p>
     * 泛型参数 {@code T} 支持 {@link PackageInfo}、{@link ResolveInfo}、{@link ActivityInfo}、{@link ApplicationInfo} 与 {@link ProviderInfo}。
     *
     * @param context       上下文
     * @param loadIcon      是否加载应用图标并填充 {@link AppData#icon}
     * @param appDataGetter 自定义的应用数据查询回调
     * @param <T>           包信息类型
     * @return 查询并转换后的 {@link AppData} 数组
     * @throws PackageManager.NameNotFoundException 查询失败时以原始异常向上抛出
     */
    @NonNull
    public static <T> AppData[] getAppData(@NonNull Context context, boolean loadIcon,
                                           @NonNull IAppDataGetter<T> appDataGetter) {
        PackageManager packageManager = context.getPackageManager();
        List<T> ts = appDataGetter.getPackages(packageManager);
        AppData[] appDataArray = new AppData[ts.size()];
        int i = 0;
        for (T t : ts) {
            appDataArray[i++] = createAppData(packageManager, t, loadIcon);
        }
        return appDataArray;
    }

    /**
     * 将包信息对象转换为统一的 {@link AppData} 数据结构。
     * <p>
     * 默认不加载图标（{@code loadIcon = false}），{@link AppData#icon} 为 {@code null}。
     *
     * @param pm  {@link PackageManager} 实例，用于加载应用图标和标签
     * @param t   待转换的包信息对象
     * @param <T> 包信息类型
     * @return 填充完毕的 {@link AppData} 实例
     * @throws IllegalArgumentException 若传入的对象类型不受支持
     */
    @NonNull
    public static <T> AppData createAppData(@NonNull PackageManager pm, @NonNull T t) {
        return createAppData(pm, t, false);
    }

    /**
     * 将包信息对象转换为统一的 {@link AppData} 数据结构，可选是否加载图标。
     * <p>
     * 注意：{@link AppData#isEnabled} 取自 {@link ApplicationInfo#enabled} 的静态清单标志，不等价于 {@link #isDisabled(Context, String)} 的运行时启用设置状态。
     *
     * @param pm       {@link PackageManager} 实例，用于加载应用图标和标签
     * @param t        待转换的包信息对象
     * @param loadIcon 是否加载应用图标并填充 {@link AppData#icon}
     * @param <T>      包信息类型
     * @return 填充完毕的 {@link AppData} 实例
     * @throws IllegalArgumentException 若传入的对象类型不受支持
     */
    @NonNull
    public static <T> AppData createAppData(@NonNull PackageManager pm, @NonNull T t, boolean loadIcon) {
        AppData appData = new AppData();
        PackageInfo packageInfo = null;
        ApplicationInfo applicationInfo;

        // noinspection IfCanBeSwitch
        if (t instanceof PackageInfo info) {
            packageInfo = info;
            applicationInfo = info.applicationInfo;
            appData.versionName = info.versionName;
            appData.versionCode = Long.toString(info.getLongVersionCode());
        } else if (t instanceof ApplicationInfo appInfo) {
            applicationInfo = appInfo;
        } else if (t instanceof ResolveInfo resolveInfo) {
            applicationInfo = aboutResolveInfo(resolveInfo).applicationInfo;
        } else if (t instanceof ComponentInfo componentInfo) {
            // 统一覆盖 ActivityInfo、ServiceInfo、ProviderInfo 及其任何 ComponentInfo 子类
            applicationInfo = componentInfo.applicationInfo;
        } else {
            throw new IllegalArgumentException(
                "Unsupported package info type: " + t.getClass().getName() +
                    ". Supported types: PackageInfo, ApplicationInfo, ResolveInfo, " +
                    "ActivityInfo, ServiceInfo, ProviderInfo."
            );
        }

        if (applicationInfo != null) {
            appData.packageInfo = packageInfo;
            appData.applicationInfo = applicationInfo;
            if (loadIcon) {
                try {
                    appData.icon = BitmapTool.drawableToBitmap(applicationInfo.loadIcon(pm));
                } catch (RuntimeException e) {
                    // 图标加载失败只降级为 null，不中断整体转换。
                    AndroidLog.logW(TAG, "Failed to load icon for package: " + applicationInfo.packageName + ".", e);
                    appData.icon = null;
                }
            }
            try {
                appData.label = applicationInfo.loadLabel(pm).toString();
            } catch (RuntimeException e) {
                // 个别应用资源解析失败（如畸形 label）时降级为 null。
                AndroidLog.logE(TAG, "Failed to load label for package: " + applicationInfo.packageName + ".", e);
                appData.label = null;
            }
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
     *
     * @param resolveInfo {@link ResolveInfo} 对象
     * @return 包含 {@link ApplicationInfo} 的 {@link ComponentInfo}
     * @throws IllegalStateException 若无法从 ResolveInfo 中获取任何应用组件信息
     */
    @NonNull
    private static ComponentInfo aboutResolveInfo(@NonNull ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo != null) return resolveInfo.activityInfo;
        if (resolveInfo.serviceInfo != null) return resolveInfo.serviceInfo;
        if (resolveInfo.providerInfo != null) return resolveInfo.providerInfo;
        throw new IllegalStateException("Unable to obtain application information.");
    }
}