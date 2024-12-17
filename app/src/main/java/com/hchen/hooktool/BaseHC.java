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
package com.hchen.hooktool;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logI;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.tool.ChainTool;
import com.hchen.hooktool.tool.CoreTool;
import com.hchen.hooktool.tool.PrefsTool;
import com.hchen.hooktool.tool.additional.ResInjectTool;
import com.hchen.hooktool.tool.itool.IAsyncPrefs;
import com.hchen.hooktool.tool.itool.IPrefsApply;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 对需要使用 Hook 的类继承本类，可快速使用本工具
 *
 * @author 焕晨HChen
 */
public abstract class BaseHC extends CoreTool {
    protected String TAG = getClass().getSimpleName(); // 快捷获取类的简单名称作为 TAG, 为了效果建议配置相应的混淆规则。
    private static final List<BaseHC> mIApplications = new ArrayList<>();
    private static boolean isFirstHookApplication = true;
    private static final ChainTool mChainTool = new ChainTool();
    public static XC_LoadPackage.LoadPackageParam lpparam; // onZygote 状态下为 null。
    public static ClassLoader classLoader;

    /**
     * handleLoadPackage 阶段。
     * <p>
     * Tip: 作为覆写使用，请勿直接调用！
     */
    protected abstract void init();

    /**
     * 带 classLoader 的初始化。
     * <p>
     * Tip: 作为覆写使用，请勿直接调用！
     */
    protected void init(ClassLoader classLoader) {
    }

    /**
     * initZygote 阶段。
     * <p>
     * 如果 startupParam 为 null，请检查是否在正确的地方初始化。
     * <p>
     * 详见: {@link HCInit#initStartupParam(IXposedHookZygoteInit.StartupParam)}
     * <p>
     * Tip: 作为覆写使用，请勿直接调用！
     */
    protected void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
    }

    /**
     * Application Context 创建之前调用。
     * */
    protected void onApplicationBefore(Context context) {
    }

    /**
     * Application Context 创建之后调用。
     * */
    protected void onApplicationAfter(Context context) {
    }

    // 请在 handleLoadPackage 阶段调用。
    final public void onLoadPackage() {
        try {
            init();
        } catch (Throwable e) {
            logE(TAG, "Waring! will stop hook process!!", e);
        }
    }

    // 请传入自定义的 classLoader。
    final public void onClassLoader(ClassLoader classLoader) {
        try {
            init(classLoader);
        } catch (Throwable e) {
            logE(TAG, "Waring! will stop hook process!!", e);
        }
    }

    // Hook Application
    final public BaseHC onApplicationCreate() {
        if (!mIApplications.contains(this))
            mIApplications.add(this);
        initApplicationHook();
        return this;
    }

    // 请在 initZygote 阶段调用。
    final public void onZygote() {
        try {
            initZygote(HCData.getStartupParam());
        } catch (Throwable e) {
            logE(TAG, "Waring! will stop hook process!!", e);
        }
    }

    public static void chain(String clazz, ChainTool chain) {
        ChainTool.chain(clazz, chain);
    }

    public static void chain(String clazz, ClassLoader classLoader, ChainTool chain) {
        ChainTool.chain(clazz, classLoader, chain);
    }

    public static void chain(Class<?> clazz, ChainTool chain) {
        ChainTool.chain(clazz, chain);
    }

    public static ChainTool.ChainHook method(String name, Object... params) {
        return mChainTool.method(name, params);
    }

    public static ChainTool.ChainHook methodIfExist(String name, Object... params) {
        return mChainTool.methodIfExist(name, params);
    }

    public static ChainTool.ChainHook anyMethod(String name) {
        return mChainTool.anyMethod(name);
    }

    public static ChainTool.ChainHook constructor(Object... params) {
        return mChainTool.constructor(params);
    }

    public static ChainTool.ChainHook constructorIfExist(Object... params) {
        return mChainTool.constructorIfExist(params);
    }

    public static ChainTool.ChainHook anyConstructor() {
        return mChainTool.anyConstructor();
    }

    // --------------- prefs -----------------

    public static IPrefsApply prefs(Context context) {
        return PrefsTool.prefs(context);
    }

    public static IPrefsApply prefs(Context context, String prefsName) {
        return PrefsTool.prefs(context, prefsName);
    }

    public static IPrefsApply prefs() {
        return PrefsTool.prefs();
    }

    public static IPrefsApply prefs(String prefsName) {
        return PrefsTool.prefs(prefsName);
    }

    public static void asyncPrefs(IAsyncPrefs asyncPrefs) {
        PrefsTool.asyncPrefs(asyncPrefs);
    }

    // ------------ ResTool ----------------
    public static int getFakeResId(String resName) {
        return ResInjectTool.getFakeResId(resName);
    }

    public static int getFakeResId(Resources res, int id) {
        return ResInjectTool.getFakeResId(res, id);
    }

    public static void setResReplacement(String pkg, String type, String name, int replacementResId) {
        ResInjectTool.setResReplacement(pkg, type, name, replacementResId);
    }

    public static void setDensityReplacement(String pkg, String type, String name, float replacementResValue) {
        ResInjectTool.setDensityReplacement(pkg, type, name, replacementResValue);
    }

    public static void setObjectReplacement(String pkg, String type, String name, Object replacementResValue) {
        ResInjectTool.setObjectReplacement(pkg, type, name, replacementResValue);
    }

    // ------------ Application Hook --------------
    private static void initApplicationHook() {
        if (!isFirstHookApplication) return;
        hookMethod(Application.class, "attach", Context.class, new IHook() {
            @Override
            public void before() {
                mIApplications.forEach(iApplication -> {
                    try {
                        iApplication.onApplicationBefore((Context) getArgs(0));
                    } catch (Throwable e) {
                        logE("Application", "Failed to call iApplication: " + iApplication, e);
                    }
                });
            }

            @Override
            public void after() {
                mIApplications.forEach(iApplication -> {
                    try {
                        iApplication.onApplicationAfter((Context) getArgs(0));
                    } catch (Throwable e) {
                        logE("Application", "Failed to call iApplication: " + iApplication, e);
                    }
                });
                logI("Application", "Application is created! package: " + ((Context) getArgs(0)).getPackageName());
            }
        });
        isFirstHookApplication = false;
    }
}
