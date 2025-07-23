/*
 * This file is part of HookTool.

 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HChenX
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

import java.util.Arrays;
import java.util.Optional;
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
            if (!result.enabled) return true;
        } catch (PackageManager.NameNotFoundException e) {
            throw new UnexpectedException(e);
        }
        return false;
    }

    /**
     * 根据 uid 获取 user id
     * <p>
     * 获取失败则返回 -1
     */
    public static int getUserId(int uid) {
        return TryHelper.doTry(() ->
            (int) Optional.ofNullable(
                InvokeTool.callStaticMethod(
                    UserHandle.class,
                    "getUserId",
                    new Class[]{int.class},
                    uid
                )
            ).orElse(-1)
        ).orElse(-1);
    }

    /**
     * 判断是否是系统应用
     */
    public static boolean isSystem(@NonNull ApplicationInfo app) {
        if (app.uid < 10000) return true;
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
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        iAppDataGetter.getAsyncAppData(
                            Arrays.stream(iAppDataGetter.getPackages(packageManager))
                                .map(parcelable -> createAppData(parcelable, packageManager))
                                .toArray(AppData[]::new)
                        );
                    } catch (PackageManager.NameNotFoundException e) {
                        throw new UnexpectedException(e);
                    }
                });
                return null;
            } else {
                return Arrays.stream(iAppDataGetter.getPackages(packageManager))
                    .map(parcelable -> createAppData(parcelable, packageManager))
                    .toArray(AppData[]::new);
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * 获取指定包名的应用信息
     */
    public static AppData getTargetAppData(@NonNull Context context, @NonNull String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            return createAppData(packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES), packageManager);
        } catch (PackageManager.NameNotFoundException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * @noinspection IfCanBeSwitch
     */
    @NonNull
    private static AppData createAppData(@NonNull Parcelable parcelable, @NonNull PackageManager pm) {
        AppData appData = new AppData();
        if (parcelable instanceof PackageInfo packageInfo) {
            appData.icon = BitmapTool.drawableToBitmap(packageInfo.applicationInfo.loadIcon(pm));
            appData.label = packageInfo.applicationInfo.loadLabel(pm).toString();
            appData.packageName = packageInfo.applicationInfo.packageName;
            appData.versionName = packageInfo.versionName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                appData.versionCode = Long.toString(packageInfo.getLongVersionCode());
            else appData.versionCode = Integer.toString(packageInfo.versionCode);
            appData.isSystemApp = isSystem(packageInfo.applicationInfo);
            appData.isEnabled = packageInfo.applicationInfo.enabled;
            appData.user = getUserId(packageInfo.applicationInfo.uid);
            appData.uid = packageInfo.applicationInfo.uid;
        } else if (parcelable instanceof ResolveInfo resolveInfo) {
            ApplicationInfo applicationInfo = aboutResolveInfo(resolveInfo).applicationInfo;
            appData.icon = BitmapTool.drawableToBitmap(applicationInfo.loadIcon(pm));
            appData.label = applicationInfo.loadLabel(pm).toString();
            appData.packageName = applicationInfo.packageName;
            appData.isSystemApp = isSystem(applicationInfo);
            appData.isEnabled = applicationInfo.enabled;
            appData.user = getUserId(applicationInfo.uid);
            appData.uid = applicationInfo.uid;
        } else if (parcelable instanceof ActivityInfo activityInfo) {
            appData.icon = BitmapTool.drawableToBitmap(activityInfo.applicationInfo.loadIcon(pm));
            appData.label = activityInfo.applicationInfo.loadLabel(pm).toString();
            appData.packageName = activityInfo.applicationInfo.packageName;
            appData.isSystemApp = isSystem(activityInfo.applicationInfo);
            appData.isEnabled = activityInfo.applicationInfo.enabled;
            appData.user = getUserId(activityInfo.applicationInfo.uid);
            appData.uid = activityInfo.applicationInfo.uid;
        } else if (parcelable instanceof ApplicationInfo applicationInfo) {
            appData.icon = BitmapTool.drawableToBitmap(applicationInfo.loadIcon(pm));
            appData.label = applicationInfo.loadLabel(pm).toString();
            appData.packageName = applicationInfo.packageName;
            appData.isSystemApp = isSystem(applicationInfo);
            appData.isEnabled = applicationInfo.enabled;
            appData.user = getUserId(applicationInfo.uid);
            appData.uid = applicationInfo.uid;
        } else if (parcelable instanceof ProviderInfo providerInfo) {
            appData.icon = BitmapTool.drawableToBitmap(providerInfo.applicationInfo.loadIcon(pm));
            appData.label = providerInfo.applicationInfo.loadLabel(pm).toString();
            appData.packageName = providerInfo.applicationInfo.packageName;
            appData.isSystemApp = isSystem(providerInfo.applicationInfo);
            appData.isEnabled = providerInfo.applicationInfo.enabled;
            appData.user = getUserId(providerInfo.applicationInfo.uid);
            appData.uid = providerInfo.applicationInfo.uid;
        }
        return appData;
    }

    @NonNull
    private static ComponentInfo aboutResolveInfo(ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo != null) return resolveInfo.activityInfo;
        if (resolveInfo.serviceInfo != null) return resolveInfo.serviceInfo;
        if (resolveInfo.providerInfo != null) return resolveInfo.providerInfo;
        throw new UnexpectedException("[PackageTool]: Unable to obtain application information!!");
    }
}
