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
import static com.hchen.hooktool.log.LogExpand.getTag;
import static com.hchen.hooktool.log.XposedLog.logE;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.IntDef;

import com.hchen.hooktool.tool.itool.IContextGetter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 上下文获取工具
 *
 * @author 焕晨HChen
 */
@SuppressLint({"PrivateApi", "SoonBlockedPrivateApi", "DiscouragedPrivateApi"})
public class ContextTool {
    @IntDef(value = {
            FLAG_ALL,
            FLAG_CURRENT_APP,
            FlAG_ONLY_ANDROID
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface Duration {
    }

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
            logE(getTag(), e);
            return null;
        }
    }

    public static Context getContextNoLog(@Duration int flag) {
        try {
            return invokeMethod(flag);
        } catch (Throwable ignore) {
            return null;
        }
    }

    /**
     * 异步获取当前应用的 Context，为了防止过早获取导致的 null。
     * 使用方法:
     * <pre> {@code
     * handler = new Handler();
     * ContextTool.getAsyncContext(new IContextGetter() {
     *   @Override
     *   public void tryToFindContext(Context context) {
     *      handler.post(new Runnable() {
     *        @Override
     *        public void run() {
     *          Toast.makeText(context, "found context!", Toast.LENGTH_SHORT).show();
     *        }
     *      });
     *   }
     * }, true/false);
     * }
     * 当然 Handler 是可选项, 适用于 Toast 显示等场景。
     * @param iContextGetter 回调获取 Context
     * @author 焕晨HChen
     */
    public static void getAsyncContext(IContextGetter iContextGetter, boolean isSystem) {
        ThreadPool.getInstance().submit(() -> {
            Context context = getContextNoLog(isSystem ? FlAG_ONLY_ANDROID : FLAG_CURRENT_APP);
            if (context == null) {
                long time = System.currentTimeMillis();
                long timeout = 10000; // 10秒
                while (true) {
                    long nowTime = System.currentTimeMillis();
                    context = getContextNoLog(isSystem ? FlAG_ONLY_ANDROID : FLAG_CURRENT_APP);
                    if (context != null || nowTime - time > timeout) {
                        break;
                    }
                }
                // context 可能为 null 请注意判断
                iContextGetter.tryToFindContext(context);
            }
        });
        ThreadPool.getInstance().shutdown();
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
                throw new Throwable("Unexpected flag!" + getStackTrace());
            }
        }
        if (context == null) throw new Throwable("Context is null!" + getStackTrace());
        return context;
    }

    private static Context currentApp(Class<?> clz) {
        // 获取当前界面应用 Context
        return InvokeTool.callStaticMethod(clz, "currentApplication", new Class[]{});
    }

    private static Context android(Class<?> clz) {
        // 获取 Android
        Context context;
        Object o = InvokeTool.callStaticMethod(clz, "currentActivityThread", new Class[]{});
        context = InvokeTool.callMethod(o, "getSystemContext", new Class[]{});
        if (context == null) {
            o = InvokeTool.callStaticMethod(clz, "systemMain", new Class[]{});
            context = InvokeTool.callMethod(o, "getSystemContext", new Class[]{});
            // 这里获取的 context 可能存在问题。
            // 所以暂时注释。
            // Method getSystemUiContext = clz.getDeclaredMethod("getSystemUiContext");
            // getSystemUiContext.setAccessible(true);
            // context = (Context) getSystemContext.invoke(o);
        }
        return context;
    }
    
    private static class ThreadPool {
        private static final int NUM_THREADS = 5; // 定义线程池中线程的数量
        private static volatile ExecutorService executor;

        // 获取线程池实例
        public static ExecutorService getInstance() {
            if (executor == null || executor.isShutdown()) {
                synchronized (ThreadPool.class) {
                    if (executor == null || executor.isShutdown()) {
                        // 创建一个具有固定数量线程的线程池, 如果已经关机则重新创建
                        executor = Executors.newFixedThreadPool(NUM_THREADS);
                    }
                }
            }
            return executor;
        }
    }
}
