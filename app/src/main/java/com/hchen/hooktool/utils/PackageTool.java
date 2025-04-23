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

import static com.hchen.hooktool.log.AndroidLog.logE;

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

import com.hchen.hooktool.callback.IPackageInfoGetter;
import com.hchen.hooktool.data.AppData;
import com.hchen.hooktool.log.AndroidLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PackageTool {
    private static final String TAG = "PackageTool";

    /**
     * 判断目标包名应用是否已经被卸载。
     */
    public static boolean isUninstall(Context context, String pkg) {
        try {
            Objects.requireNonNull(context, "[PackageTool]: Context must not is null!");
            PackageManager packageManager = context.getPackageManager();
            packageManager.getPackageInfo(pkg, PackageManager.MATCH_ALL);
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

    /**
     * 获取包名应用是否被禁用。
     */
    public static boolean isDisable(Context context, String pkg) {
        try {
            Objects.requireNonNull(context, "[PackageTool]: Context must not is null!");
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo result = packageManager.getApplicationInfo(pkg, 0);
            if (!result.enabled) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return false;
    }

    /**
     * 获取包名应用是否被 Hidden，一般来说被隐藏视为未安装，可以使用 isUninstall() 来判断。
     */
    public static boolean isHidden(Context context, String pkg) {
        try {
            Objects.requireNonNull(context, "[PackageTool]: Context must not is null!");
            PackageManager packageManager = context.getPackageManager();
            packageManager.getApplicationInfo(pkg, 0);
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

    /**
     * 根据 uid 获取 user id。
     */
    public static int getUserId(int uid) {
        return (int) Optional.ofNullable(
            InvokeTool.callStaticMethod(UserHandle.class, "getUserId", new Class[]{int.class}, uid)
        ).orElse(-1);
    }

    /**
     * 可用于判断是否是系统应用。
     */
    public static boolean isSystem(ApplicationInfo app) {
        Objects.requireNonNull(app, "[PackageTool]: ApplicationInfo must not is null!");
        if (app.uid < 10000) {
            return true;
        }
        return (app.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    /**
     * 通过自定义代码获取 Package 信息，
     * 支持: PackageInfo, ResolveInfo, ActivityInfo, ApplicationInfo, ProviderInfo. 类型的返回值.
     * <p>
     * 示例：
     * <p>
     * <pre>{@code
     * AppData appData = PackageTool.getPackagesByCode(new IPackageInfoGetter() {
     *             @Override
     *             public Parcelable[] packageInfoGetter(PackageManager pm) throws PackageManager.NameNotFoundException {
     *                 PackageInfo packageInfo = null;
     *                 ArrayList<PackageInfo> arrayList = new ArrayList<>();
     *                 arrayList.add(packageInfo);
     *                 return arrayList.toArray(new PackageInfo[0]);
     *             }
     *         })[0];
     * }
     *
     * @param infoGetter 需要执行的代码
     * @return AppData[] 包含各种应用详细信息
     * @see #createAppData(Parcelable, PackageManager)
     */
    public static AppData[] getPackagesByCode(Context context, IPackageInfoGetter infoGetter) {
        Objects.requireNonNull(context, "[PackageTool]: Context must not is null!");

        PackageManager packageManager = context.getPackageManager();
        Parcelable[] parcelables;
        try {
            parcelables = infoGetter.packageInfoGetter(packageManager);
        } catch (PackageManager.NameNotFoundException e) {
            logE(TAG, e);
            return new AppData[0];
        }

        List<AppData> appDataList = new ArrayList<>();
        if (parcelables != null) {
            for (Parcelable parcelable : parcelables) {
                try {
                    appDataList.add(createAppData(parcelable, packageManager));
                } catch (Throwable e) {
                    AndroidLog.logE(TAG, "Failed to create app data!", e);
                }
            }
        }
        return appDataList.toArray(new AppData[0]);
    }

    /**
     * 获取指定包名的 APP 信息。
     */
    public static AppData getTargetPackage(Context context, String packageName) throws PackageManager.NameNotFoundException {
        PackageManager packageManager = context.getPackageManager();
        return createAppData(packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES), packageManager);
    }

    private static AppData createAppData(Parcelable parcelable, PackageManager pm) {
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
            appData.icon = BitmapTool.drawableToBitmap(aboutResolveInfo(resolveInfo).applicationInfo.loadIcon(pm));
            appData.label = aboutResolveInfo(resolveInfo).applicationInfo.loadLabel(pm).toString();
            appData.packageName = aboutResolveInfo(resolveInfo).applicationInfo.packageName;
            appData.isSystemApp = isSystem(aboutResolveInfo(resolveInfo).applicationInfo);
            appData.isEnabled = aboutResolveInfo(resolveInfo).applicationInfo.enabled;
            appData.user = getUserId(aboutResolveInfo(resolveInfo).applicationInfo.uid);
            appData.uid = aboutResolveInfo(resolveInfo).applicationInfo.uid;
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

    private static ComponentInfo aboutResolveInfo(ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo != null) return resolveInfo.activityInfo;
        if (resolveInfo.serviceInfo != null) return resolveInfo.serviceInfo;
        if (resolveInfo.providerInfo != null) return resolveInfo.providerInfo;
        return null;
    }
}
