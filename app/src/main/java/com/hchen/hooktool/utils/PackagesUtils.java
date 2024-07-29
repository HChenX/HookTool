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

 * Copyright (C) 2023-2024 HookTool Contributions
 */
package com.hchen.hooktool.utils;

import static com.hchen.hooktool.utils.LogExpand.getStackTrace;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;
import android.os.UserHandle;

import com.hchen.hooktool.data.AppData;
import com.hchen.hooktool.log.AndroidLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 软件包实用程序
 * <p>
 * Package utility
 */
public class PackagesUtils {
    private static final String TAG = "PackagesUtils";

    public static boolean isUninstall(String pkg) {
        return isUninstall(context(), pkg);
    }

    /**
     * 判断目标包名应用是否已经被卸载。
     * <p>
     * Check whether the application with the target package name has been uninstalled.
     */
    public static boolean isUninstall(Context context, String pkg) {
        if (context == null) return false;
        PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getPackageInfo(pkg, PackageManager.MATCH_ALL);
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            AndroidLog.logE(TAG,
                    "didn't find this app on the machine, it may have been uninstalled! pkg: " + pkg, e);
            return true;
        }
    }

    public static boolean isDisable(String pkg) {
        return isDisable(context(), pkg);
    }

    /**
     * 获取包名应用是否被禁用。
     * <p>
     * Get the package name and whether the app is disabled.
     */
    public static boolean isDisable(Context context, String pkg) {
        if (context == null) return false;
        PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo result = packageManager.getApplicationInfo(pkg, 0);
            if (!result.enabled) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return false;
    }

    public static boolean isHidden(String pkg) {
        return isHidden(context(), pkg);
    }

    /**
     * 获取包名应用是否被 Hidden，一般来说被隐藏视为未安装，可以使用 isUninstall() 来判断。
     * <p>
     * Whether the application is hidden, generally speaking, it is considered uninstalled if it is hidden, and you can use isUninstall() to determine whether the application is hidden.
     */
    public static boolean isHidden(Context context, String pkg) {
        try {
            if (context == null) return false;
            PackageManager packageManager = context.getPackageManager();
            packageManager.getApplicationInfo(pkg, 0);
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

    /**
     * 通过自定义代码获取 Package 信息，
     * 支持
     * <p>
     * PackageInfo
     * <p>
     * ResolveInfo
     * <p>
     * ActivityInfo
     * <p>
     * ApplicationInfo
     * <p>
     * ProviderInfo
     * <p>
     * 类型的返回值.
     * 返回使用 return new ArrayList<>(XX); 包裹。
     * <p>
     * Get the package information through custom code.
     *
     * @param iCode 需要执行的代码
     * @return ListAppData 包含各种应用详细信息
     * @see #addAppData(Parcelable, PackageManager)
     */
    public static List<AppData> getPackagesByCode(ICode iCode) {
        List<AppData> appDataList = new ArrayList<>();
        Context context = context();
        if (context == null) return appDataList;
        PackageManager packageManager = context.getPackageManager();
        Parcelable parcelable = iCode.getPackageCode(packageManager);
        List<Parcelable> packageCodeList = iCode.getPackageCodeList(packageManager);
        try {
            if (parcelable != null) {
                appDataList.add(addAppData(parcelable, packageManager));
            } else {
                if (packageCodeList != null) {
                    for (Parcelable get : packageCodeList) {
                        appDataList.add(addAppData(get, packageManager));
                    }
                }
            }
        } catch (Throwable e) {
            AndroidLog.logE(TAG, "failed to get package via code!", e);
        }
        return appDataList;
    }

    @SuppressLint("QueryPermissionsNeeded")
    public static List<AppData> getInstalledPackages(int flag) {
        List<AppData> appDataList = new ArrayList<>();
        Context context = context();
        if (context == null) return appDataList;
        try {
            PackageManager packageManager = context.getPackageManager();
            List<PackageInfo> packageInfos = packageManager.getInstalledPackages(flag);
            for (PackageInfo packageInfo : packageInfos) {
                appDataList.add(addAppData(packageInfo, packageManager));
            }
            return appDataList;
        } catch (Throwable e) {
            AndroidLog.logE(TAG, "failed to get the list of installed apps via flag!", e);
        }
        return appDataList;
    }

    private static AppData addAppData(Parcelable parcelable, PackageManager pm) throws Throwable {
        AppData appData = new AppData();
        try {
            if (parcelable instanceof PackageInfo) {
                appData.icon = BitmapUtils.drawableToBitmap(((PackageInfo) parcelable).applicationInfo.loadIcon(pm));
                appData.label = ((PackageInfo) parcelable).applicationInfo.loadLabel(pm).toString();
                appData.packageName = ((PackageInfo) parcelable).applicationInfo.packageName;
                appData.versionName = ((PackageInfo) parcelable).versionName;
                appData.versionCode = Long.toString(((PackageInfo) parcelable).getLongVersionCode());
                appData.isSystemApp = isSystem(((PackageInfo) parcelable).applicationInfo);
                appData.enabled = ((PackageInfo) parcelable).applicationInfo.enabled;
                appData.user = getUserId(((PackageInfo) parcelable).applicationInfo.uid);
                appData.uid = ((PackageInfo) parcelable).applicationInfo.uid;
            } else if (parcelable instanceof ResolveInfo) {
                appData.icon = BitmapUtils.drawableToBitmap(aboutResolveInfo((ResolveInfo) parcelable).applicationInfo.loadIcon(pm));
                appData.label = aboutResolveInfo((ResolveInfo) parcelable).applicationInfo.loadLabel(pm).toString();
                appData.packageName = aboutResolveInfo((ResolveInfo) parcelable).applicationInfo.packageName;
                appData.isSystemApp = isSystem(aboutResolveInfo((ResolveInfo) parcelable).applicationInfo);
                appData.enabled = aboutResolveInfo((ResolveInfo) parcelable).applicationInfo.enabled;
                appData.user = getUserId(aboutResolveInfo((ResolveInfo) parcelable).applicationInfo.uid);
                appData.uid = aboutResolveInfo((ResolveInfo) parcelable).applicationInfo.uid;
            } else if (parcelable instanceof ActivityInfo) {
                appData.icon = BitmapUtils.drawableToBitmap(((ActivityInfo) parcelable).applicationInfo.loadIcon(pm));
                appData.label = ((ActivityInfo) parcelable).applicationInfo.loadLabel(pm).toString();
                appData.packageName = ((ActivityInfo) parcelable).applicationInfo.packageName;
                appData.isSystemApp = isSystem(((ActivityInfo) parcelable).applicationInfo);
                appData.activityName = ((ActivityInfo) parcelable).name;
                appData.enabled = ((ActivityInfo) parcelable).applicationInfo.enabled;
                appData.user = getUserId(((ActivityInfo) parcelable).applicationInfo.uid);
                appData.uid = ((ActivityInfo) parcelable).applicationInfo.uid;
            } else if (parcelable instanceof ApplicationInfo) {
                appData.icon = BitmapUtils.drawableToBitmap(((ApplicationInfo) parcelable).loadIcon(pm));
                appData.label = ((ApplicationInfo) parcelable).loadLabel(pm).toString();
                appData.packageName = ((ApplicationInfo) parcelable).packageName;
                appData.isSystemApp = isSystem(((ApplicationInfo) parcelable));
                appData.enabled = ((ApplicationInfo) parcelable).enabled;
                appData.user = getUserId(((ApplicationInfo) parcelable).uid);
                appData.uid = ((ApplicationInfo) parcelable).uid;
            } else if (parcelable instanceof ProviderInfo) {
                appData.icon = BitmapUtils.drawableToBitmap(((ProviderInfo) parcelable).applicationInfo.loadIcon(pm));
                appData.label = ((ProviderInfo) parcelable).applicationInfo.loadLabel(pm).toString();
                appData.packageName = ((ProviderInfo) parcelable).applicationInfo.packageName;
                appData.isSystemApp = isSystem(((ProviderInfo) parcelable).applicationInfo);
                appData.enabled = ((ProviderInfo) parcelable).applicationInfo.enabled;
                appData.user = getUserId(((ProviderInfo) parcelable).applicationInfo.uid);
                appData.uid = ((ProviderInfo) parcelable).applicationInfo.uid;
            }
        } catch (Throwable e) {
            throw new Throwable("error in obtaining application information: " + parcelable, e);
        }
        return appData;
    }

    private static ComponentInfo aboutResolveInfo(ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo != null) return resolveInfo.activityInfo;
        if (resolveInfo.serviceInfo != null) return resolveInfo.serviceInfo;
        if (resolveInfo.providerInfo != null) return resolveInfo.providerInfo;
        return null;
    }

    /**
     * 根据 uid 获取 user id。
     * <p>
     * Get user id based on uid.
     */
    public static int getUserId(int uid) {
        return InvokeUtils.callStaticMethod(UserHandle.class, "getUserId", new Class[]{int.class}, uid);
    }

    /**
     * 可用于判断是否是系统应用。
     * 如果 app 为 null 则固定返回 false，请注意检查 app 是否为 null。
     * <p>
     * It can be used to determine whether it is a system application. If the app is null, it will always return false, so be careful to check if the app is null.
     */
    public static boolean isSystem(ApplicationInfo app) {
        if (Objects.isNull(app)) {
            AndroidLog.logE(TAG, "isSystem app is null, will return false" + getStackTrace());
            return false;
        }
        if (app.uid < 10000) {
            return true;
        }
        return (app.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    private static Context context() {
        try {
            return getContext();
        } catch (Throwable e) {
            AndroidLog.logE(TAG, e);
            return null;
        }
    }

    private static Context getContext() throws Throwable {
        Context context = ContextUtils.getContext(ContextUtils.FLAG_CURRENT_APP);
        if (context == null) {
            context = ContextUtils.getContext(ContextUtils.FlAG_ONLY_ANDROID);
        }
        if (context == null) {
            throw new Throwable("context is null" + getStackTrace());
        }
        return context;
    }

    public interface ICode {
        default Parcelable getPackageCode(PackageManager pm) {
            return null;
        }

        default List<Parcelable> getPackageCodeList(PackageManager pm) {
            return null;
        }
    }
}
