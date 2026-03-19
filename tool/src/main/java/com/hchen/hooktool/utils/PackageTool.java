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
import android.os.Build;
import android.os.Parcelable;
import android.os.UserHandle;

import androidx.annotation.NonNull;

import com.hchen.hooktool.callback.IAppDataGetter;
import com.hchen.hooktool.data.AppData;
import com.hchen.hooktool.exception.UnexpectedException;
import com.hchen.hooktool.helper.TryHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 包工具
 *
 * @author 焕晨HChen
 */
public class PackageTool {
    private static final String TAG = "PackageTool";

    private PackageTool() {
    }

    /**
     * 判断应用是否已被安装
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
     * 判断应用是否被禁用
     */
    public static boolean isDisable(@NonNull Context context, @NonNull String packageName) {
        try {
            ApplicationInfo result = context.getPackageManager().getApplicationInfo(packageName, 0);
            return !result.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            throw new UnexpectedException("Failed to get application info for package: " + packageName, e);
        }
    }

    /**
     * 根据 uid 获取 user id
     * <p>
     * 获取失败则返回 -1
     */
    public static int getUserId(int uid) {
        return TryHelper.doTry(() -> {
            Object result = InvokeTool.callStaticMethod(
                UserHandle.class,
                "getUserId",
                new Class[]{int.class},
                uid
            );
            return result != null ? (int) result : -1;
        }).orElse(-1);
    }

    /**
     * 判断是否是系统应用
     */
    public static boolean isSystem(@NonNull ApplicationInfo app) {
        if (app.uid < 10000) {
            return true;
        }
        return (app.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    public static AppData[] getAppData(@NonNull Context context, @NonNull IAppDataGetter iAppDataGetter) {
        return getAppData(context, false, iAppDataGetter);
    }

    /**
     * 通过自定义代码获取 Package 信息
     * <p>
     * 支持: PackageInfo, ResolveInfo, ActivityInfo, ApplicationInfo, ProviderInfo. 类型的返回值
     * <p>
     * 示例：
     * <p>
     * <pre>{@code
     * AppData[] appData = PackageTool.getAppData(context, false, new IAppDataGetter() {
     *      @Override
     *      @NonNull
     *      public Parcelable[] getPackages(@NonNull PackageManager pm) throws PackageManager.NameNotFoundException {
     *          PackageInfo packageInfo = null;
     *          ArrayList<PackageInfo> arrayList = new ArrayList<>();
     *          arrayList.add(packageInfo);
     *          return arrayList.toArray(new PackageInfo[0]);
     *      }
     * });
     *
     * PackageTool.getAppData(context, true, new IAppDataGetter() {
     *      @Override
     *      @NonNull
     *      public Parcelable[] getPackages(@NonNull PackageManager pm) throws PackageManager.NameNotFoundException {
     *          PackageInfo packageInfo = null;
     *          ArrayList<PackageInfo> arrayList = new ArrayList<>();
     *          arrayList.add(packageInfo);
     *          return arrayList.toArray(new PackageInfo[0]);
     *      }
     *
     *      @Override
     *      public void getAsyncAppData(@NonNull AppData[] appData) {
     *          IAppDataGetter.super.getAsyncAppData(appData);
     *      }
     * });
     * }
     *
     * @return AppData[] 应用详细信息
     * @see #createAppData(Parcelable, PackageManager)
     */
    public static AppData[] getAppData(@NonNull Context context, boolean async, @NonNull IAppDataGetter iAppDataGetter) {
        PackageManager packageManager = context.getPackageManager();
        try {
            if (async) {
                ExecutorService service = null;
                try {
                    // noinspection resource
                    service = Executors.newSingleThreadExecutor();
                    service.execute(() -> {
                        try {
                            Parcelable[] packages = iAppDataGetter.getPackages(packageManager);
                            AppData[] appDataArray = new AppData[packages.length];
                            for (int i = 0; i < packages.length; i++) {
                                appDataArray[i] = createAppData(packages[i], packageManager);
                            }
                            iAppDataGetter.getAsyncAppData(appDataArray);
                        } catch (PackageManager.NameNotFoundException e) {
                            throw new UnexpectedException("Failed to get packages", e);
                        }
                    });
                } finally {
                    if (service != null) {
                        service.shutdown();
                    }
                }
                return null;
            } else {
                Parcelable[] packages = iAppDataGetter.getPackages(packageManager);
                AppData[] appDataArray = new AppData[packages.length];
                for (int i = 0; i < packages.length; i++) {
                    appDataArray[i] = createAppData(packages[i], packageManager);
                }
                return appDataArray;
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new UnexpectedException("Failed to get packages", e);
        }
    }

    /**
     * 获取指定包名的应用信息
     */
    public static AppData getTargetAppData(@NonNull Context context, @NonNull String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            return createAppData(applicationInfo, packageManager);
        } catch (PackageManager.NameNotFoundException e) {
            throw new UnexpectedException("Failed to get application info for package: " + packageName, e);
        }
    }

    /**
     * @noinspection IfCanBeSwitch
     */
    @NonNull
    public static AppData createAppData(@NonNull Parcelable parcelable, @NonNull PackageManager pm) {
        AppData appData = new AppData();
        ApplicationInfo applicationInfo = null;
        
        // 根据不同类型的 Parcelable 对象获取 ApplicationInfo
        if (parcelable instanceof PackageInfo packageInfo) {
            applicationInfo = packageInfo.applicationInfo;
            appData.setVersionName(packageInfo.versionName);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                appData.setVersionCode(Long.toString(packageInfo.getLongVersionCode()));
            } else {
                appData.setVersionCode(Integer.toString(packageInfo.versionCode));
            }
        } else if (parcelable instanceof ResolveInfo resolveInfo) {
            applicationInfo = aboutResolveInfo(resolveInfo).applicationInfo;
        } else if (parcelable instanceof ActivityInfo activityInfo) {
            applicationInfo = activityInfo.applicationInfo;
        } else if (parcelable instanceof ApplicationInfo appInfo) {
            applicationInfo = appInfo;
        } else if (parcelable instanceof ProviderInfo providerInfo) {
            applicationInfo = providerInfo.applicationInfo;
        }
        
        // 填充应用数据
        if (applicationInfo != null) {
            appData.setIcon(BitmapTool.drawableToBitmap(applicationInfo.loadIcon(pm)));
            appData.setLabel(applicationInfo.loadLabel(pm).toString());
            appData.setPackageName(applicationInfo.packageName);
            appData.setSystemApp(isSystem(applicationInfo));
            appData.setEnabled(applicationInfo.enabled);
            appData.setUser(getUserId(applicationInfo.uid));
            appData.setUid(applicationInfo.uid);
        }
        
        return appData;
    }

    @NonNull
    private static ComponentInfo aboutResolveInfo(ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo != null) return resolveInfo.activityInfo;
        if (resolveInfo.serviceInfo != null) return resolveInfo.serviceInfo;
        if (resolveInfo.providerInfo != null) return resolveInfo.providerInfo;
        throw new UnexpectedException("Unable to obtain application information.");
    }
}
