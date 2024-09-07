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

import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import androidx.annotation.RequiresApi;

import com.hchen.hooktool.data.ToolData;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * 资源注入工具
 *
 * @author 焕晨HChen
 */
public class ResTool {
    private static ResourcesLoader resourcesLoader = null;
    private static final String TAG = "ResTool";
    private static String mModulePath = null;
    private static Handler mHandler = null;

    /**
     * 请在 initZygote 中初始化。
     *
     * @param modulePath startupParam.modulePath 即可
     */
    public static void initResHelper(String modulePath) {
        mModulePath = modulePath;
    }

    /**
     * 把本项目资源注入目标作用域上下文。一般调用本方法即可。<br/>
     * 请在 build.gradle 添加如下代码。
     * <pre> {@code
     * Kotlin Gradle DSL:
     *
     * androidResources.additionalParameters("--allow-reserved-package-id", "--package-id", "0x64")
     *
     * Groovy:
     *
     * aaptOptions.additionalParameters '--allow-reserved-package-id', '--package-id', '0x64'
     *
     * }<br/>
     * Tip: `0x64` is the resource id, you can change it to any value you want.(recommended [0x30 to 0x6F])
     * @noinspection UnusedReturnValue
     */
    public static Resources loadModuleRes(Resources resources, boolean doOnMainLooper) {
        boolean load = false;
        if (resources == null) {
            logW(TAG, "Context can't is null!" + getStackTrace());
            return null;
        }
        if (mModulePath == null) {
            mModulePath = ToolData.startupParam.modulePath;
            if (mModulePath == null) {
                logW(TAG, "Module path is null, can't load module res!" + getStackTrace());
                return null;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            load = loadResAboveApi30(resources, doOnMainLooper);
        } else {
            load = loadResBelowApi30(resources);
        }
        if (!load) {
            /*try {
                return getModuleRes(context);
            } catch (PackageManager.NameNotFoundException e) {
                logE(TAG, "failed to load resource! critical error!! scope may crash!!", e);
            }*/
        }
        return resources;
    }

    public static Resources loadModuleRes(Resources resources) {
        return loadModuleRes(resources, false);
    }

    public static Resources loadModuleRes(Context context, boolean doOnMainLooper) {
        return loadModuleRes(context.getResources(), doOnMainLooper);
    }

    public static Resources loadModuleRes(Context context) {
        return loadModuleRes(context, false);
    }

    /**
     * 来自 QA 的方法
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    private static boolean loadResAboveApi30(Resources resources, boolean doOnMainLooper) {
        if (resourcesLoader == null) {
            try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(new File(mModulePath),
                    ParcelFileDescriptor.MODE_READ_ONLY)) {
                ResourcesProvider provider = ResourcesProvider.loadFromApk(pfd);
                ResourcesLoader loader = new ResourcesLoader();
                loader.addProvider(provider);
                resourcesLoader = loader;
            } catch (IOException e) {
                logE(TAG, "Failed to add resource! debug: above api 30.", e);
                return false;
            }
        }
        if (doOnMainLooper)
            if (Looper.myLooper() == Looper.getMainLooper()) {
                return addLoaders(resources);
            } else {
                if (mHandler == null) {
                    mHandler = new Handler(Looper.getMainLooper());
                }
                mHandler.post(() -> {
                    addLoaders(resources);
                });
                return true; // 此状态下保持返回 true，请观察日志是否有报错来判断是否成功。
            }
        else
            return addLoaders(resources);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private static boolean addLoaders(Resources resources) {
        try {
            resources.addLoaders(resourcesLoader);
        } catch (IllegalArgumentException e) {
            String expected1 = "Cannot modify resource loaders of ResourcesImpl not registered with ResourcesManager";
            if (expected1.equals(e.getMessage())) {
                // fallback to below API 30
                return loadResBelowApi30(resources);
            } else {
                logE(TAG, "Failed to add loaders!", e);
                return false;
            }
        }
        return true;
    }

    /** @noinspection JavaReflectionMemberAccess */
    @SuppressLint("DiscouragedPrivateApi")
    private static boolean loadResBelowApi30(Resources resources) {
        try {
            AssetManager assets = resources.getAssets();
            Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            Integer cookie = (Integer) addAssetPath.invoke(assets, mModulePath);
            if (cookie == null || cookie == 0) {
                logW(TAG, "Method 'addAssetPath' result 0, maybe load res failed!" + getStackTrace());
                return false;
            }
        } catch (Throwable e) {
            logE(TAG, "Failed to add resource! debug: below api 30.", e);
            return false;
        }
        return true;
    }

    // 下面注入方法存在风险，可能导致资源混乱，抛弃。
    /*public static Context getModuleContext(Context context)
            throws PackageManager.NameNotFoundException {
        return getModuleContext(context, null);
    }

    public static Context getModuleContext(Context context, Configuration config)
            throws PackageManager.NameNotFoundException {
        Context mModuleContext;
        mModuleContext = context.createPackageContext(mProjectPkg, Context.CONTEXT_IGNORE_SECURITY).createDeviceProtectedStorageContext();
        return config == null ? mModuleContext : mModuleContext.createConfigurationContext(config);
    }

    public static Resources getModuleRes(Context context)
            throws PackageManager.NameNotFoundException {
        Configuration config = context.getResources().getConfiguration();
        Context moduleContext = getModuleContext(context);
        return (config == null ? moduleContext.getResources() : moduleContext.createConfigurationContext(config).getResources());
    }*/
}
