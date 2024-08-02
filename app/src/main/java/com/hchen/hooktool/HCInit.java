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

import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.XposedLog.logI;

import androidx.annotation.IntDef;

import com.hchen.hooktool.utils.ToolData;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 请在 Hook 入口处初始化本类
 * <p>
 * Initialize this class at the hook entry
 * 
 * @author 焕晨HChen
 */
public class HCInit {
    // ------- 可选日志等级 ------
    public static final int LOG_NONE = 0;
    public static final int LOG_E = 1;
    public static final int LOG_W = 2;
    public static final int LOG_I = 3;
    public static final int LOG_D = 4;
    // ------- END --------------

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
     * <p>
     * Be sure to set it up!
     */
    public static void initLoadPackageParam(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam == null) {
            throw new RuntimeException(ToolData.mInitTag + "[E]: load package param is null!!");
        }
        ToolData.lpparam = loadPackageParam;
        classLoader = loadPackageParam.classLoader;
        String packageName = loadPackageParam.packageName;
        putClassLoader();
        logI("HCInit: init classLoader: [" + classLoader + "], pkg name: " + packageName);
    }

    /**
     * 务必设置！
     * <p>
     * Be sure to set it up!
     */
    public static void initStartupParam(IXposedHookZygoteInit.StartupParam startupParam) {
        ToolData.startupParam = startupParam;
    }

    /**
     * 务必设置！
     * <p>
     * Be sure to set it up!
     */
    public static void initBasicData(String modulePackageName, String tag, @Duration int level) {
        setTag(tag); /* 设置 TAG */
        ToolData.mInitLogLevel = level; /* 设置日志等级 */
        ToolData.modulePackageName = modulePackageName; /* 设置模块包名 */
    }

    /**
     * 是否允许使用系统的 classloader，一般不需要开启。
     * <p>
     * Whether to allow the use of the system's classloader, generally does not need to be enabled.
     */
    public static void canUseSystemClassLoader(boolean use) {
        canUseSystemClassLoader = use; /* 允许使用系统 classloader */
    }

    /**
     * 是否自动更新 xprefs 数据。
     * <p>
     * 工具默认开启，但可能会增加耗时。
     * <p>
     * Whether xPrefs data is automatically updated.
     * <p>
     * The tool is enabled by default, but it can take more time.
     */
    public static void xPrefsAutoReload(boolean auto) {
        ToolData.autoReload = auto;
    }
    // ---------- END！----------

    private static void setTag(String tag) {
        ToolData.mInitTag = "[" + tag + "]";
        ToolData.spareTag = tag;
    }

    private static void putClassLoader() {
        if (classLoader != null) {
            ToolData.classLoader = classLoader;
            return;
        }
        if (canUseSystemClassLoader) {
            ToolData.classLoader = ClassLoader.getSystemClassLoader();
            return;
        }
        throw new RuntimeException(ToolData.mInitTag + "[E]: HCInit: failed to obtain ClassLoader! it is null!" + getStackTrace());
    }

    /**
     * 模块是否被激活。
     * 使用方法：<br/>
     * lpparam 传入模块本身。<br/>
     * path 传入指定类。<br/>
     * fieldName 传入字段名。<br/>
     * value 输入值。<br/>
     * 随后模块本身检查这个字段是否被更改即可。
     * <p>
     * Whether the module is activated or not. How to use:
     * <br> lpparam is passed into the module itself.
     * <br> path to the specified class.
     * <br> fieldName is passed in the field name.
     * <br> value to enter a value.
     * <br> The module itself then checks to see if this field has been changed.
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
