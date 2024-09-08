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
package com.hchen.hooktool.tool.additional;

import static com.hchen.hooktool.log.AndroidLog.logW;
import static com.hchen.hooktool.log.LogExpand.getStackTrace;

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
import java.util.Optional;

/**
 * 软件包实用程序
 *
 * @author 焕晨HChen
 */
public class PackagesTool {
    private static final String TAG = "PackagesTool";

    public static boolean isUninstall(String pkg) {
        return isUninstall(context(), pkg);
    }

    /**
     * 判断目标包名应用是否已经被卸载。
     */
    public static boolean isUninstall(Context context, String pkg) {
        if (context == null) {
            logW(TAG, "Context is null, can't check if the app is uninstalled!" + getStackTrace());
            return false;
        }
        PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getPackageInfo(pkg, PackageManager.MATCH_ALL);
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            AndroidLog.logE(TAG, e);
            return true;
        }
    }

    public static boolean isDisable(String pkg) {
        return isDisable(context(), pkg);
    }

    /**
     * 获取包名应用是否被禁用。
     */
    public static boolean isDisable(Context context, String pkg) {
        if (context == null) {
            logW(TAG, "Context is null, can't check if an app is disabled!" + getStackTrace());
            return false;
        }
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
     */
    public static boolean isHidden(Context context, String pkg) {
        try {
            if (context == null) {
                logW(TAG, "Context is null, can't check if an app is hidden!" + getStackTrace());
                return false;
            }
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
                        InvokeTool.callStaticMethod(UserHandle.class, "getUserId", new Class[]{int.class}, uid))
                .orElse(-1);
    }

    /**
     * 可用于判断是否是系统应用。
     * 如果 app 为 null 则固定返回 false，请注意检查 app 是否为 null。
     */
    public static boolean isSystem(ApplicationInfo app) {
        if (Objects.isNull(app)) {
            AndroidLog.logE(TAG, "ApplicationInfo is null, can't check if it's a system app!" + getStackTrace());
            return false;
        }
        if (app.uid < 10000) {
            return true;
        }
        return (app.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    @SuppressLint("QueryPermissionsNeeded")
    public static List<AppData> getInstalledPackages(Context context, int flag) {
        List<AppData> appDataList = new ArrayList<>();
        if (context == null) {
            logW(TAG, "Context is null, can't get install packages!" + getStackTrace());
            return appDataList;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            List<PackageInfo> packageInfos = packageManager.getInstalledPackages(flag);
            for (PackageInfo packageInfo : packageInfos) {
                appDataList.add(addAppData(packageInfo, packageManager));
            }
            return appDataList;
        } catch (Throwable e) {
            AndroidLog.logE(TAG, e);
        }
        return new ArrayList<>();
    }

    public static List<AppData> getInstalledPackages(int flag) {
        return getInstalledPackages(context(), flag);
    }

    /**
     * 通过自定义代码获取 Package 信息，
     * 支持: PackageInfo, ResolveInfo, ActivityInfo, ApplicationInfo, ProviderInfo. 类型的返回值.
     * 返回使用 return new ArrayList<>(XX); 包裹。
     *
     * @param iCode 需要执行的代码
     * @return ListAppData 包含各种应用详细信息
     * @see #addAppData(Parcelable, PackageManager)
     */
    public static List<AppData> getPackagesByCode(Context context, ICode iCode) {
        List<AppData> appDataList = new ArrayList<>();
        if (context == null) {
            logW(TAG, "Context is null, can't get packages by code!" + getStackTrace());
            return appDataList;
        }
        PackageManager packageManager = context.getPackageManager();
        List<Parcelable> packageCodeList = iCode.action(packageManager);
        try {
            if (packageCodeList != null) {
                for (Parcelable get : packageCodeList) {
                    appDataList.add(addAppData(get, packageManager));
                }
            }
            return appDataList;
        } catch (Throwable e) {
            AndroidLog.logE(TAG, e);
        }
        return new ArrayList<>();
    }

    public static List<AppData> getPackagesByCode(ICode iCode) {
        return getPackagesByCode(context(), iCode);
    }

    private static AppData addAppData(Parcelable parcelable, PackageManager pm) throws
            Throwable {
        AppData appData = new AppData();
        try {
            if (parcelable instanceof PackageInfo) {
                appData.icon = BitmapTool.drawableToBitmap(((PackageInfo) parcelable).applicationInfo.loadIcon(pm));
                appData.label = ((PackageInfo) parcelable).applicationInfo.loadLabel(pm).toString();
                appData.packageName = ((PackageInfo) parcelable).applicationInfo.packageName;
                appData.versionName = ((PackageInfo) parcelable).versionName;
                appData.versionCode = Long.toString(((PackageInfo) parcelable).getLongVersionCode());
                appData.isSystemApp = isSystem(((PackageInfo) parcelable).applicationInfo);
                appData.enabled = ((PackageInfo) parcelable).applicationInfo.enabled;
                appData.user = getUserId(((PackageInfo) parcelable).applicationInfo.uid);
                appData.uid = ((PackageInfo) parcelable).applicationInfo.uid;
            } else if (parcelable instanceof ResolveInfo) {
                appData.icon = BitmapTool.drawableToBitmap(aboutResolveInfo((ResolveInfo) parcelable).applicationInfo.loadIcon(pm));
                appData.label = aboutResolveInfo((ResolveInfo) parcelable).applicationInfo.loadLabel(pm).toString();
                appData.packageName = aboutResolveInfo((ResolveInfo) parcelable).applicationInfo.packageName;
                appData.isSystemApp = isSystem(aboutResolveInfo((ResolveInfo) parcelable).applicationInfo);
                appData.enabled = aboutResolveInfo((ResolveInfo) parcelable).applicationInfo.enabled;
                appData.user = getUserId(aboutResolveInfo((ResolveInfo) parcelable).applicationInfo.uid);
                appData.uid = aboutResolveInfo((ResolveInfo) parcelable).applicationInfo.uid;
            } else if (parcelable instanceof ActivityInfo) {
                appData.icon = BitmapTool.drawableToBitmap(((ActivityInfo) parcelable).applicationInfo.loadIcon(pm));
                appData.label = ((ActivityInfo) parcelable).applicationInfo.loadLabel(pm).toString();
                appData.packageName = ((ActivityInfo) parcelable).applicationInfo.packageName;
                appData.isSystemApp = isSystem(((ActivityInfo) parcelable).applicationInfo);
                appData.enabled = ((ActivityInfo) parcelable).applicationInfo.enabled;
                appData.user = getUserId(((ActivityInfo) parcelable).applicationInfo.uid);
                appData.uid = ((ActivityInfo) parcelable).applicationInfo.uid;
            } else if (parcelable instanceof ApplicationInfo) {
                appData.icon = BitmapTool.drawableToBitmap(((ApplicationInfo) parcelable).loadIcon(pm));
                appData.label = ((ApplicationInfo) parcelable).loadLabel(pm).toString();
                appData.packageName = ((ApplicationInfo) parcelable).packageName;
                appData.isSystemApp = isSystem(((ApplicationInfo) parcelable));
                appData.enabled = ((ApplicationInfo) parcelable).enabled;
                appData.user = getUserId(((ApplicationInfo) parcelable).uid);
                appData.uid = ((ApplicationInfo) parcelable).uid;
            } else if (parcelable instanceof ProviderInfo) {
                appData.icon = BitmapTool.drawableToBitmap(((ProviderInfo) parcelable).applicationInfo.loadIcon(pm));
                appData.label = ((ProviderInfo) parcelable).applicationInfo.loadLabel(pm).toString();
                appData.packageName = ((ProviderInfo) parcelable).applicationInfo.packageName;
                appData.isSystemApp = isSystem(((ProviderInfo) parcelable).applicationInfo);
                appData.enabled = ((ProviderInfo) parcelable).applicationInfo.enabled;
                appData.user = getUserId(((ProviderInfo) parcelable).applicationInfo.uid);
                appData.uid = ((ProviderInfo) parcelable).applicationInfo.uid;
            }
        } catch (Throwable e) {
            throw new Throwable(e);
        }
        return appData;
    }

    private static ComponentInfo aboutResolveInfo(ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo != null) return resolveInfo.activityInfo;
        if (resolveInfo.serviceInfo != null) return resolveInfo.serviceInfo;
        if (resolveInfo.providerInfo != null) return resolveInfo.providerInfo;
        return null;
    }

    private static Context context() {
        return ContextTool.getContextNoLog(ContextTool.FLAG_ALL);
    }

    public interface ICode {
        List<Parcelable> action(PackageManager pm);
    }
}
