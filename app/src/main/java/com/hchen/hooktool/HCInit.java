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

import com.hchen.hooktool.data.ToolData;
import com.hchen.hooktool.tool.CoreTool;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 请在 Hook 入口处初始化本类
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

    @IntDef(value = {
            LOG_NONE,
            LOG_I,
            LOG_W,
            LOG_E,
            LOG_D
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface LogLevel {
    }

    // ---------- 初始化工具 ----------

    /**
     * 务必设置！
     *
     * <pre>{@code
     *  @Override
     *  public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
     *      HCInit.initLoadPackageParam(lpparam);
     *  }
     * }
     */
    public static void initLoadPackageParam(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam == null) {
            throw new RuntimeException(ToolData.mInitTag + "[E]: LoadPackageParam is null!!");
        }
        ToolData.isXposed = true;
        ToolData.lpparam = loadPackageParam;
        ClassLoader classLoader = loadPackageParam.classLoader;
        ToolData.classLoader = classLoader;
        String packageName = loadPackageParam.packageName;
        logI("Init classloader: [" + classLoader + "], pkg: " + packageName);
    }

    /**
     * 务必设置！
     *
     * <pre>{@code
     *  @Override
     *  public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
     *      HCInit.initStartupParam(startupParam);
     *  }
     * }
     */
    public static void initStartupParam(IXposedHookZygoteInit.StartupParam startupParam) {
        ToolData.isXposed = true;
        ToolData.startupParam = startupParam;
        ToolData.classLoader = startupParam.getClass().getClassLoader();
    }

    /**
     * 务必设置！建议在 initZygote 中第一位设置，因为时机很早。
     *
     * <pre>{@code
     *  @Override
     *  public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
     *      HCInit.initBasicData(
     *          new BasicData()
     *              .setModulePackageName("com.hchen.demo")
     *              .setTag("HChenDemo")
     *              .setLogLevel(LOG_D)
     *              .setPrefsName("hchen_prefs")
     *      );
     *  }
     * }
     */
    public static void initBasicData(BasicData basicData) {
        setTag(basicData.tag);
        ToolData.mInitLogLevel = basicData.logLevel;
        ToolData.modulePackageName = basicData.packageName;
        ToolData.mPrefsName = basicData.prefsName;
    }

    /**
     * 是否自动更新 xprefs 数据。
     * <p>
     * 工具默认开启，但可能会增加耗时。
     */
    public static void xPrefsAutoReload(boolean auto) {
        ToolData.autoReload = auto;
    }

    /**
     * 是否使用日志增强功能，path 填写模块的 hook 文件所在目录，否则默认按照包名搜索。
     * <pre>{@code
     *      HCInit.useLogExpand(new String[]{"com.hchen.demo.hook"});
     *      // 填写 new String[]{} 则默认使用包名查找。
     * }<br/>
     * 同时加入混淆规则:
     * <p>
     * -keep class com.hchen.demo.hook.**
     * <p>
     * -keep class com.hchen.demo.hook.**$*
     */
    public static void useLogExpand(@NotNull String[] path) {
        ToolData.logExpandPath = path;
    }
    // ---------- END！----------

    private static void setTag(String tag) {
        ToolData.mInitTag = "[" + tag + "]";
        ToolData.spareTag = tag;
    }

    /**
     * 模块是否被激活。
     * 使用方法：<br/>
     * lpparam: 传入模块本身。<br/>
     * path: 传入指定类。<br/>
     * fieldName: 传入字段名。<br/>
     * value: 输入值。<br/>
     * 随后模块本身检查这个字段是否被更改即可。
     */
    public static boolean isXposedModuleActive(XC_LoadPackage.LoadPackageParam lpparam,
                                               String path, String fieldName, Object value) {
        return CoreTool.setStaticField(path, lpparam.classLoader, fieldName, value);
    }

    public static class BasicData {
        String packageName = null;
        String tag = null;
        int logLevel = LOG_I;
        String prefsName = null;

        // 设置包名
        public BasicData setModulePackageName(String modulePackageName) {
            packageName = modulePackageName;
            return this;
        }

        // 设置日志 TAG
        public BasicData setTag(String tag) {
            this.tag = tag;
            return this;
        }

        // 设置日志输出等级
        public BasicData setLogLevel(@LogLevel int level) {
            logLevel = level;
            return this;
        }

        // 设置共享首选项的储存名
        public BasicData setPrefsName(String prefsName) {
            this.prefsName = prefsName;
            return this;
        }
    }
}
