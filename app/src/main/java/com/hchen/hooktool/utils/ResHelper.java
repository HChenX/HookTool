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

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;

/**
 * 资源注入工具，
 * 可能不是很稳定。
 */
public class ResHelper {
    private static ResourcesLoader resourcesLoader = null;
    private static final String TAG = "ResHelper";
    private static String mModulePath = null;

    /**
     * 请在 initZygote 中初始化。
     *
     * @param modulePath startupParam.modulePath 即可
     */
    public static void initResHelper(String modulePath) {
        mModulePath = modulePath;
    }

    /**
     * 来自 QA 的方法
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    private static boolean loadResAboveApi30(Context context) {
        if (resourcesLoader == null) {
            try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(new File(mModulePath),
                    ParcelFileDescriptor.MODE_READ_ONLY)) {
                ResourcesProvider provider = ResourcesProvider.loadFromApk(pfd);
                ResourcesLoader loader = new ResourcesLoader();
                loader.addProvider(provider);
                resourcesLoader = loader;
            } catch (IOException e) {
                logE(TAG, "failed to add resource!: " + e);
                return false;
            }
        }
        // if (Looper.myLooper() == Looper.getMainLooper()) {
        context.getResources().addLoaders(resourcesLoader);
        // } else {
        //     if (mHandler != null) {
        //         mHandler.post(() -> context.getResources().addLoaders(resourcesLoader));
        //     } else {
        //         return false;
        //     }
        // }
        return true;
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
     * `0x64` is the resource id, you can change it to any value you want.(recommended [0x30 to 0x6F])
     * @noinspection UnusedReturnValue
     */
    public static Resources loadModuleRes(Context context) {
        boolean load = false;
        if (context == null) {
            logE(TAG, "context can't is null!!");
            return null;
        }
        if (mModulePath == null) {
            mModulePath = ToolData.startupParam.modulePath;
            if (mModulePath == null) {
                logE(TAG, "module path is null! can't load module res! please call initResHelper!");
                return null;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            load = loadResAboveApi30(context);
        } else {
            logW(TAG, "sdk so low, can't load module res!");
        }
        if (!load) {
            /*try {
                return getModuleRes(context);
            } catch (PackageManager.NameNotFoundException e) {
                logE(TAG, "failed to load resource! critical error!! scope may crash!!", e);
            }*/
        }
        return context.getResources();
    }

    // 下面注入方法存在风险，可能导致资源混乱，暂抛弃。
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
