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

import static com.hchen.hooktool.log.XposedLog.logI;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 请在 Hook 入口处初始化本类
 * <p>
 * 或者直接继承: {@link HCEntrance} ( 建议 )
 *
 * @author 焕晨HChen
 */
public final class HCInit {
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
            throw new RuntimeException(HCData.getInitTag() + "[E]: LoadPackageParam is null!!");
        }
        HCData.setIsXposed(true);
        HCData.setLoadPackageParam(loadPackageParam);
        HCData.setClassLoader(loadPackageParam.classLoader);
        BaseHC.lpparam = loadPackageParam;
        BaseHC.classLoader = loadPackageParam.classLoader;
        logI("Init classloader: [" + loadPackageParam.classLoader + "], pkg: " + loadPackageParam.packageName);
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
        HCData.setIsXposed(true);
        HCData.setStartupParam(startupParam);
        HCData.setClassLoader(startupParam.getClass().getClassLoader());
        BaseHC.lpparam = null;
        BaseHC.classLoader = startupParam.getClass().getClassLoader();
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
     *              .xPrefsAutoReload(true)
     *              .initLogExpand(new String[]{
     *                  "com.hchen.demo.hook"
     *              })
     *      );
     *  }
     * }
     */
    public static void initBasicData(BasicData basicData) {
        HCData.setSpareTag(basicData.tag);
        HCData.setInitTag("[" + basicData.tag + "]");
        HCData.setInitLogLevel(basicData.logLevel);
        HCData.setModulePackageName(basicData.packageName);
        HCData.setPrefsName(basicData.prefsName);
        HCData.setAutoReload(basicData.isAutoReload);
        HCData.setLogExpandPath(basicData.logExpandPath);
    }

    // ---------- END！----------
    public final static class BasicData {
        String tag = null;
        int logLevel = LOG_I;
        String packageName = null;
        String prefsName = null;
        boolean isAutoReload = true;
        String[] logExpandPath = null;

        // 设置模块包名。 Tip: 建议设置。
        public BasicData setModulePackageName(@NonNull String modulePackageName) {
            packageName = modulePackageName;
            return this;
        }

        // 设置日志 TAG。Tip: 建议设置。
        public BasicData setTag(@NonNull String tag) {
            this.tag = tag;
            return this;
        }

        // 设置日志输出等级。Tip: 建议设置。
        public BasicData setLogLevel(@LogLevel int level) {
            logLevel = level;
            return this;
        }

        // 设置共享首选项的储存名。
        public BasicData setPrefsName(@NonNull String prefsName) {
            this.prefsName = prefsName;
            return this;
        }

        /**
         * 是否自动更新 xprefs 数据。
         * <p>
         * 工具默认开启，但可能会增加耗时。
         */
        public BasicData xPrefsAutoReload(boolean auto) {
            this.isAutoReload = auto;
            return this;
        }

        /**
         * 是否使用日志增强功能，path 填写模块的 hook 文件所在目录，否则默认按照包名搜索。
         * <pre>{@code
         *      HCInit.initLogExpand(new String[]{"com.hchen.demo.hook"});
         *      // 填写 new String[]{} 则默认使用包名查找。
         * }<br/>
         * 同时加入混淆规则:
         * <pre>{@code
         *     -keep class com.hchen.demo.hook.**
         *     -keep class com.hchen.demo.hook.**$*
         * }
         *
         * Tip: 建议设置。
         */
        public BasicData initLogExpand(@NonNull String[] path) {
            this.logExpandPath = path;
            return this;
        }
    }
}
