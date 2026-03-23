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
 * 包工具
 *
 * @author 焕晨HChen
 */
public final class PackageTool {
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
            CoreTool.throwIt(e);
            return false; // Not actually executed
        }
    }

    /**
     * 根据 uid 获取 user id
     * <p>
     * 获取失败则返回 -1
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
     * 判断是否是系统应用
     */
    public static boolean isSystem(@NonNull ApplicationInfo app) {
        if (app.uid < 10000) {
            return true;
        }
        return (app.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    public static <T> AppData[] getAppData(@NonNull Context context, @NonNull IAppDataGetter<T> iAppDataGetter) {
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
     * @return AppData[] 应用详细信息
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
     * @noinspection IfCanBeSwitch
     */
    @NonNull
    public static <T> AppData createAppData(@NonNull PackageManager pm, @NonNull T t) {
        AppData appData = new AppData();
        ApplicationInfo applicationInfo = null;

        // 根据不同类型的 T 对象获取 ApplicationInfo
        if (t instanceof PackageInfo packageInfo) {
            applicationInfo = packageInfo.applicationInfo;
            appData.setVersionName(packageInfo.versionName);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                appData.setVersionCode(Long.toString(packageInfo.getLongVersionCode()));
            } else {
                appData.setVersionCode(Integer.toString(packageInfo.versionCode));
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
            appData.setInfo(applicationInfo);
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
    private static ComponentInfo aboutResolveInfo(@NonNull ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo != null) return resolveInfo.activityInfo;
        if (resolveInfo.serviceInfo != null) return resolveInfo.serviceInfo;
        if (resolveInfo.providerInfo != null) return resolveInfo.providerInfo;
        throw new UnexpectedException("Unable to obtain application information.");
    }
}
