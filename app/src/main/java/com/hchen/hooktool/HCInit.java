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

import static com.hchen.hooktool.log.XposedLog.logI;

import androidx.annotation.IntDef;

import com.hchen.hooktool.utils.ToolData;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 初始化类，请在 Hook 入口处初始化本类。
 */
public class HCInit {
    // ------- 可选日志等级 ------
    public static final int LOG_NONE = 0;
    public static final int LOG_E = 1;
    public static final int LOG_W = 2;
    public static final int LOG_I = 3;
    public static final int LOG_D = 4;
    // ------- END -------------
    private static XC_LoadPackage.LoadPackageParam lpparam = null;
    private static ClassLoader classLoader = null;
    private static boolean canUseSystemClassLoader = false;

    @IntDef(value = {
            LOG_NONE,
            LOG_I,
            LOG_W,
            LOG_E,
            LOG_D
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface Duration {
    }

    // ---------- 初始化工具 ----------

    /**
     * 务必设置！
     */
    public static void initLoadPackageParam(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam == null) {
            throw new RuntimeException(ToolData.mInitTag + "[E]: load package param is null!!");
        }
        lpparam = loadPackageParam;
        classLoader = loadPackageParam.classLoader;
        String packageName = lpparam.packageName;
        logI(ToolData.spareTag, "init lpparam: [" + lpparam + "]," +
                " classLoader: [" + classLoader + "], pkg name: " + packageName);
    }

    /**
     * 务必设置！
     */
    public static void initStartupParam(IXposedHookZygoteInit.StartupParam startupParam) {
        ToolData.startupParam = startupParam;
    }

    /**
     * 务必设置！
     */
    public static void initOther(String modulePackageName, String tag, @Duration int level) {
        setTag(tag); /* 设置 TAG */
        ToolData.mInitLogLevel = level; /* 设置日志等级 */
        ToolData.modulePackageName = modulePackageName; /* 设置模块包名 */
    }

    public static void canUseSystemClassLoader(boolean use) {
        canUseSystemClassLoader = use; /* 允许使用系统 classloader */
    }

    public static void logFilter(boolean use, String[] filter) {
        ToolData.useFieldObserver = use; /* 使用全局日志过滤 */
        ToolData.filter = filter; /* 过滤规则 */
    }

    public static void filedObserver(boolean use) {
        ToolData.useFieldObserver = use; /* 使用字段设置观察 */
    }
    // ---------- END！----------

    private static void setTag(String tag) {
        ToolData.mInitTag = "[" + tag + "]";
        ToolData.spareTag = tag;
    }


    protected static XC_LoadPackage.LoadPackageParam getLoadPackageParam() {
        if (lpparam != null) return lpparam;
        throw new RuntimeException(ToolData.mInitTag + "[E]: failed to obtain LoadPackageParam, it is null!");
    }

    protected static ClassLoader getClassLoader() {
        if (classLoader != null) return classLoader;
        if (canUseSystemClassLoader) {
            return ClassLoader.getSystemClassLoader();
        }
        throw new RuntimeException(ToolData.mInitTag + "[E]: failed to obtain ClassLoader! it is null!");
    }

    /**
     * 模块是否被激活。
     * 使用方法：<br/>
     * lpparam 传入模块本身。<br/>
     * path 传入指定类。<br/>
     * fieldName 传入字段名。<br/>
     * value 输入值。<br/>
     * 随后模块本身检查这个字段是否被更改即可。
     */
    public static boolean isXposedModuleActive(XC_LoadPackage.LoadPackageParam lpparam,
                                               String path, String fieldName, Object value) {
        try {
            XposedHelpers.setStaticObjectField(XposedHelpers.findClass(path, lpparam.classLoader), fieldName, value);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
