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
package com.hchen.hooktool;

import android.app.Application;
import android.content.Context;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.hchen.hooktool.core.CoreTool;
import com.hchen.hooktool.hook.IHook;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * BaseHC
 * <p>
 * 继承此类即可使用
 *
 * @author 焕晨HChen
 */
public abstract class HCBase extends CoreTool {
    public String TAG = getClass().getSimpleName();
    public static ClassLoader classLoader;
    public static XC_LoadPackage.LoadPackageParam loadPackageParam;
    public static final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
    private static boolean isHookedApplication = false;
    private static final List<HCBase> mIApplications = new ArrayList<>();
    public static final int ON_LOAD_PACKAGE = 1;
    public static final int ON_ZYGOTE = 2;
    public static final int ON_APPLICATION = 3;

    @IntDef(value = {
        ON_LOAD_PACKAGE,
        ON_ZYGOTE,
        ON_APPLICATION
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface BaseFlag {
    }

    /**
     * 是否启用
     */
    protected boolean isEnabled() {
        return true;
    }

    /**
     * onLoadPackage 阶段调用
     */
    protected abstract void init();

    /**
     * onLoadPackage 阶段调用
     * <p>
     * 支持自定义类加载器
     */
    protected void init(@NonNull ClassLoader classLoader) {
    }

    /**
     * onZygote 阶段调用
     */
    protected void initZygote(@NonNull IXposedHookZygoteInit.StartupParam startupParam) {
    }

    /**
     * Application 创建时调用
     */
    protected void onApplication(@NonNull Context context) {
    }

    /**
     * Hook 流程抛错时调用
     * <p>
     * 在这里进行抛错清理操作
     *
     * @param flag 抛错的时机
     */
    protected void onThrowable(@BaseFlag int flag, @NonNull Throwable e) {
    }

    final public void onLoadPackage() {
        try {
            if (!isEnabled()) return;
            init();
        } catch (Throwable e) {
            onThrowable(ON_LOAD_PACKAGE, e);
            logE(TAG, "[onLoadPackage]: Waring! will stop hook process!", e);
        }
    }

    final public void onLoadPackage(@NonNull ClassLoader classLoader) {
        try {
            if (!isEnabled()) return;
            init(classLoader);
        } catch (Throwable e) {
            onThrowable(ON_LOAD_PACKAGE, e);
            logE(TAG, "[onLoadPackage/classLoader]: Waring! will stop hook process!", e);
        }
    }

    final public HCBase onApplication() {
        try {
            if (isEnabled()) {
                if (!mIApplications.contains(this))
                    mIApplications.add(this);

                initApplicationHook();
            }
        } catch (Throwable e) {
            onThrowable(ON_APPLICATION, e);
            logE(TAG, "[onApplication]: Waring! failed to hook application!", e);
        }
        return this;
    }

    final public void onZygote() {
        try {
            if (!isEnabled()) return;

            assert HCData.getStartupParam() != null;
            initZygote(HCData.getStartupParam());
        } catch (Throwable e) {
            onThrowable(ON_ZYGOTE, e);
            logE(TAG, "[onZygote]: Waring! will stop hook process!", e);
        }
    }

    private static void initApplicationHook() {
        if (isHookedApplication) return;
        hookMethod(Application.class, "attach", Context.class, new IHook() {
            @Override
            public void after() {
                Context context = (Context) getArg(0);
                mIApplications.forEach(iApplication -> {
                    try {
                        iApplication.onApplication(context);
                    } catch (Throwable e) {
                        logE("Application", e);
                    }
                });
                logI("Application", "Application created, package name: " + (context != null ? context.getPackageName() : "unknown"));
            }
        });
        isHookedApplication = true;
    }
}
