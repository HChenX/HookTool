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

import androidx.annotation.NonNull;

import com.hchen.hooktool.core.CoreTool;
import com.hchen.hooktool.hook.IHook;

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
public abstract class BaseHC extends CoreTool {
    public String TAG = getClass().getSimpleName();
    public static ClassLoader classLoader;
    public static XC_LoadPackage.LoadPackageParam loadPackageParam;
    public static ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
    private static boolean isHookedApplication = false;
    private static final List<BaseHC> mIApplications = new ArrayList<>();

    /**
     * 是否启用
     */
    protected boolean isEnabled() {
        return true;
    }

    protected abstract void init();

    protected void init(@NonNull ClassLoader classLoader) {
    }

    protected void initZygote(@NonNull IXposedHookZygoteInit.StartupParam startupParam) {
    }

    protected void onApplication(@NonNull Context context) {
    }

    protected void onThrowable(@NonNull Throwable e) {
    }

    final public void onLoadPackage() {
        try {
            if (!isEnabled()) return;
            init();
        } catch (Throwable e) {
            onThrowable(e);
            logE(TAG, "Waring! will stop hook process!!", e);
        }
    }

    final public void onClassLoader(ClassLoader classLoader) {
        try {
            if (!isEnabled()) return;
            init(classLoader);
        } catch (Throwable e) {
            onThrowable(e);
            logE(TAG, "Waring! will stop hook process!!", e);
        }
    }

    final public BaseHC onApplicationCreate() {
        try {
            if (!isEnabled()) return this;

            if (!mIApplications.contains(this))
                mIApplications.add(this);
            initApplicationHook();
        } catch (Throwable e) {
            onThrowable(e);
            logE(TAG, "Waring! can't hook application!!", e);
        }
        return this;
    }

    final public void onZygote() {
        try {
            if (!isEnabled()) return;

            assert HCData.getStartupParam() != null;
            initZygote(HCData.getStartupParam());
        } catch (Throwable e) {
            onThrowable(e);
            logE(TAG, "Waring! will stop hook process!!", e);
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
                        logE("Application", "Failed to call iApplication: " + iApplication, e);
                    }
                });
                logI("Application", "Application created, package name: " + (context != null ? context.getPackageName() : "unknown"));
            }
        });
        isHookedApplication = true;
    }
}
