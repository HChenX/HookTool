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

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

/**
 * 本类为 context 上下文获取工具
 */
@SuppressLint({"PrivateApi", "SoonBlockedPrivateApi", "DiscouragedPrivateApi"})
public class ContextUtils {
    @IntDef(value = {
            FLAG_ALL,
            FLAG_CURRENT_APP,
            FlAG_ONLY_ANDROID
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface Duration {
    }

    private static final String TAG = "ContextUtils";
    // 尝试全部
    public static final int FLAG_ALL = 0;
    // 仅获取当前应用
    public static final int FLAG_CURRENT_APP = 1;
    // 获取 Android 系统
    public static final int FlAG_ONLY_ANDROID = 2;

    public static Context getContext(@Duration int flag) {
        try {
            return invokeMethod(flag);
        } catch (Throwable e) {
            logE(TAG, "get context failed!", e);
            return null;
        }
    }

    public static Context getContextNoError(@Duration int flag) {
        try {
            return invokeMethod(flag);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * 循环获取当前应用的 Context 为了防止过早获取导致的 null.
     * 使用方法:
     * <pre> {@code
     * handler = new Handler();
     * ContextUtils.getWaitContext(new ContextUtils.IContext() {
     *   @Override
     *   public void findContext(Context context) {
     *      handler.post(new Runnable() {
     *        @Override
     *        public void run() {
     *          ToastHelper.makeText(context, "getContext");
     *        }
     *      });
     *   }
     * }, true/false);
     * }
     * 当然 Handler 是可选项, 适用于 Toast 显示等场景。
     * @param iContext 回调获取 Context
     * @author 焕晨HChen
     */
    public static void getWaitContext(IContext iContext, boolean isSystem) {
        ThreadPool.getInstance().submit(() -> {
            Context context = getContextNoError(isSystem ? FlAG_ONLY_ANDROID : FLAG_CURRENT_APP);
            if (context == null) {
                long time = System.currentTimeMillis();
                long timeout = 10000; // 10秒
                while (true) {
                    long nowTime = System.currentTimeMillis();
                    context = getContextNoError(isSystem ? FlAG_ONLY_ANDROID : FLAG_CURRENT_APP);
                    // AndroidLogUtils.LogI(TAG, "getWaitContext: " + context);
                    if (context != null || nowTime - time > timeout) {
                        break;
                    }
                }
                // context 可能为 null 请注意判断
                iContext.findContext(context);
            }
        });
        ThreadPool.getInstance().shutdown();
    }

    public interface IContext {
        void findContext(Context context);
    }

    private static Context invokeMethod(int flag) throws Throwable {
        Context context;
        Class<?> clz = Class.forName("android.app.ActivityThread");
        switch (flag) {
            case 0 -> {
                if ((context = currentApp(clz)) == null) {
                    context = android(clz);
                }
            }
            case 1 -> {
                context = currentApp(clz);
            }
            case 2 -> {
                context = android(clz);
            }
            default -> {
                throw new Throwable("unexpected flag");
            }
        }
        if (context == null) throw new Throwable("context is null");
        return context;
    }

    private static Context currentApp(Class<?> clz) throws Throwable {
        // 获取当前界面应用 Context
        Method currentApplication = clz.getDeclaredMethod("currentApplication");
        currentApplication.setAccessible(true);
        return (Application) currentApplication.invoke(null);
    }

    private static Context android(Class<?> clz) throws Throwable {
        // 获取 Android
        Context context;
        Method currentActivityThread = clz.getDeclaredMethod("currentActivityThread");
        currentActivityThread.setAccessible(true);
        Object o = currentActivityThread.invoke(null);
        Method getSystemContext = clz.getDeclaredMethod("getSystemContext");
        getSystemContext.setAccessible(true);
        context = (Context) getSystemContext.invoke(o);
        if (context == null) {
            // 这里获取的 context 可能存在问题。
            // 所以暂时注释。
            // Method getSystemUiContext = clz.getDeclaredMethod("getSystemUiContext");
            // getSystemUiContext.setAccessible(true);
            // context = (Context) getSystemContext.invoke(o);
        }
        return context;
    }

}
