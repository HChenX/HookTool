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

import android.content.Context;

import com.hchen.hooktool.data.ToolData;
import com.hchen.hooktool.tool.ChainTool;
import com.hchen.hooktool.tool.CoreTool;
import com.hchen.hooktool.tool.PrefsTool;
import com.hchen.hooktool.tool.itool.IPrefs;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 对需要使用工具的类继承本类，可快速使用工具
 *
 * @author 焕晨HChen
 */
public abstract class BaseHC extends CoreTool {
    public String TAG = getClass().getSimpleName();
    private final ChainTool chainTool = new ChainTool();
    public static XC_LoadPackage.LoadPackageParam lpparam;  // onZygote 阶段为 null
    public static ClassLoader classLoader;

    /**
     * 正常阶段。
     */
    public abstract void init();

    /**
     * zygote 阶段。
     * <p>
     * 如果 startupParam 为 null，请检查是否为工具初始化。
     */
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
    }

    final public void onLoadPackage() {
        try {
            initBaseHC();
            init();
        } catch (Throwable e) {
            logE(TAG, e);
        }
    }

    final public void onZygote() {
        try {
            initBaseHC();
            initZygote(ToolData.startupParam);
        } catch (Throwable e) {
            logE(TAG, e);
        }
    }

    private void initBaseHC() {
        lpparam = ToolData.lpparam;
        classLoader = ToolData.classLoader;
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

    final public ChainTool.ChainHook method(String name, Object... params) {
        return chainTool.method(name, params);
    }

    final public ChainTool.ChainHook anyMethod(String name) {
        return chainTool.anyMethod(name);
    }

    final public ChainTool.ChainHook constructor(Object... params) {
        return chainTool.constructor(params);
    }

    final public ChainTool.ChainHook anyConstructor() {
        return chainTool.anyConstructor();
    }

    // --------------- prefs -----------------

    public static IPrefs prefs(Context context) {
        return PrefsTool.prefs(context);
    }

    public static IPrefs prefs(Context context, String prefsName) {
        return PrefsTool.prefs(context, prefsName);
    }

    public static IPrefs prefs() {
        return PrefsTool.prefs();
    }

    public static IPrefs prefs(String prefsName) {
        return PrefsTool.prefs(prefsName);
    }

    public static void asyncPrefs(PrefsTool.IAsyncPrefs asyncPrefs) {
        PrefsTool.asyncPrefs(asyncPrefs);
    }
}
