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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.hchen.hooktool.log.XposedLog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 初始化工具
 *
 * @author 焕晨HChen
 */
public class HCInit {
    // ------- 可选日志等级 ------
    public static final int LOG_E = 1;
    public static final int LOG_W = 2;
    public static final int LOG_I = 3;
    public static final int LOG_D = 4;

    @IntDef(value = {
        LOG_I,
        LOG_W,
        LOG_E,
        LOG_D
    })
    @Retention(RetentionPolicy.SOURCE)
    protected @interface LogLevel {
    }

    private HCInit() {
    }

    /**
     * 初始化 loadPackageParam
     */
    public static void initLoadPackageParam(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam == null)
            throw new NullPointerException("[HCInit]: loadPackageParam must not is null!");

        HCData.setIsXposed(true);
        HCData.setLoadPackageParam(loadPackageParam);
        HCData.setClassLoader(loadPackageParam.classLoader);
        XposedLog.logI("Init classloader: [" + loadPackageParam.classLoader + "], packageName: " + loadPackageParam.packageName);
    }

    /**
     * 初始化 startupParam
     */
    public static void initStartupParam(IXposedHookZygoteInit.StartupParam startupParam) {
        if (startupParam == null)
            throw new NullPointerException("[HCInit]: startupParam must not is null!");

        HCData.setIsXposed(true);
        HCData.setStartupParam(startupParam);
        HCData.setClassLoader(
            Optional.ofNullable(
                startupParam.getClass().getClassLoader()
            ).orElse(ClassLoader.getSystemClassLoader())
        );
    }

    /**
     * 初始化模块基本设置
     */
    public static void initBasicData(@NonNull BasicData basicData) {
        HCData.setTag(basicData.tag);
        HCData.setLogLevel(basicData.logLevel);
        HCData.setModulePackageName(basicData.packageName);
        HCData.setPrefsName(basicData.prefsName);
        HCData.setAutoReload(basicData.isAutoReload);
        HCData.setLogExpandPath(basicData.logExpandPath);
        HCData.setLogExpandIgnoreClassNames(basicData.logExpandIgnoreClassNames);
    }

    public final static class BasicData {
        String tag = null;
        int logLevel = LOG_I;
        String packageName = null;
        String prefsName = null;
        boolean isAutoReload = true;
        String[] logExpandPath = null;
        String[] logExpandIgnoreClassNames = null;

        /**
         * 设置工具日志 TAG
         */
        public BasicData setTag(@NonNull String tag) {
            this.tag = tag;
            return this;
        }

        /**
         * 设置日志等级
         */
        public BasicData setLogLevel(@LogLevel int level) {
            logLevel = level;
            return this;
        }

        /**
         * 设置本模块包名
         */
        public BasicData setModulePackageName(@NonNull String modulePackageName) {
            packageName = modulePackageName;
            return this;
        }

        /**
         * 设置通用共享首选项名称
         * <p>
         * 不设置默认使用：模块包名_prefs
         */
        public BasicData setPrefsName(@NonNull String prefsName) {
            this.prefsName = prefsName;
            return this;
        }

        /**
         * 是否自动更新 Xprefs
         * <p>
         * 默认开启
         */
        public BasicData setAutoReload(boolean auto) {
            this.isAutoReload = auto;
            return this;
        }

        /**
         * 指定使用日志增强功能的路径
         * <p>
         * 开启的路径可正常获取类名作为日志 TAG
         * <p>
         * 请注意本功能存在性能消耗，介意勿开
         * <p>
         * 同时加入类似的混淆规则:
         * <pre>{@code
         *     -keep class com.hchen.demo.hook.**
         *     -keep class com.hchen.demo.hook.**$*
         * }
         */
        public BasicData setLogExpandPath(@NonNull String... logExpandPath) {
            this.logExpandPath = logExpandPath;
            return this;
        }

        /**
         * 设置使用日志增强时应忽略的类名
         */
        public BasicData setLogExpandIgnoreClassNames(@NonNull String... logExpandIgnoreClassNames) {
            this.logExpandIgnoreClassNames = logExpandIgnoreClassNames;
            return this;
        }
    }
}
