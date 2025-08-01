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
 * Copyright (C) 2023–2025 HChenX
 */
package com.hchen.hooktool.utils;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.callback.IContextGetter;
import com.hchen.hooktool.exception.UnexpectedException;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executors;

/**
 * 上下文获取工具
 *
 * @author 焕晨HChen
 */
@SuppressLint({"PrivateApi", "SoonBlockedPrivateApi", "DiscouragedPrivateApi"})
public class ContextTool {
    public static final int FLAG_ALL = 0;
    public static final int FLAG_CURRENT_APP = 1;
    public static final int FLAG_ONLY_ANDROID = 2;

    @IntDef(value = {
        FLAG_ALL,
        FLAG_CURRENT_APP,
        FLAG_ONLY_ANDROID
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface ContextFlag {
    }

    private ContextTool() {
    }

    /**
     * 获取上下文对象
     */
    @NonNull
    public static Context getContext(@ContextFlag int flag) {
        Context context = invokeMethod(flag);
        if (context == null)
            throw new NullPointerException("[ContextTool]: Failed to get context!!");
        return context;
    }

    /**
     * 获取上下文对象，不会抛错
     */
    @Nullable
    private static Context getContextNonThrow(@ContextFlag int flag) {
        try {
            return invokeMethod(flag);
        } catch (Throwable ignore) {
            return null;
        }
    }

    /**
     * 异步获取当前应用的 Context，为了防止过早获取导致的 null
     */
    public static void getAsyncContext(@NonNull IContextGetter iContextGetter, @ContextFlag int flag) {
        getAsyncContext(iContextGetter, flag, 10000 /* 10 秒 */);
    }

    /**
     * 异步获取当前应用的 Context，为了防止过早获取导致的 null
     * <p>
     * 使用方法:
     * <pre> {@code
     * handler = new Handler();
     * ContextTool.getAsyncContext(new IContextGetter() {
     *   @Override
     *   public void onContext(@Nullable Context context) {
     *      handler.post(new Runnable() {
     *        @Override
     *        public void run() {
     *          Toast.makeText(context, "found context!!", Toast.LENGTH_SHORT).show();
     *        }
     *      });
     *   }
     * }, FLAG_ALL, 10000);
     * }
     * 当然 Handler 是可选项, 适用于 Toast 显示等场景
     */
    public static void getAsyncContext(@NonNull IContextGetter iContextGetter, @ContextFlag int flag, int timeout) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Context context = getContextNonThrow(flag);
            if (context == null) {
                long time = System.currentTimeMillis();
                while (true) {
                    long nowTime = System.currentTimeMillis();
                    context = getContextNonThrow(flag);
                    if (context != null || nowTime - time > timeout) {
                        break;
                    }
                }
            }
            iContextGetter.onContext(context);
        });
    }

    @Nullable
    private static Context invokeMethod(@ContextFlag int flag) {
        Context context;
        Class<?> clazz = InvokeTool.findClass("android.app.ActivityThread");
        switch (flag) {
            case FLAG_ALL -> {
                if ((context = getCurrentAppContext(clazz)) == null) {
                    context = getAndroidContext(clazz);
                }
            }
            case FLAG_CURRENT_APP -> {
                context = getCurrentAppContext(clazz);
            }
            case FLAG_ONLY_ANDROID -> {
                context = getAndroidContext(clazz);
            }
            default -> {
                throw new UnexpectedException("[ContextTool]: Unexpected flag: " + flag);
            }
        }
        return context;
    }

    private static Context getCurrentAppContext(@NonNull Class<?> clazz) {
        return InvokeTool.callStaticMethod(clazz, "currentApplication", new Class[]{});
    }

    private static Context getAndroidContext(@NonNull Class<?> clazz) {
        Context context;
        Object o = InvokeTool.callStaticMethod(clazz, "currentActivityThread", new Class[]{});
        context = InvokeTool.callMethod(o, "getSystemContext", new Class[]{});
        if (context == null) {
            o = InvokeTool.callStaticMethod(clazz, "systemMain", new Class[]{});
            context = InvokeTool.callMethod(o, "getSystemContext", new Class[]{});
        }
        return context;
    }
}
