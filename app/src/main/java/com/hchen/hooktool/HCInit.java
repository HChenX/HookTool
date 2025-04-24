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

import com.hchen.hooktool.exception.UnexpectedException;
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
    private @interface LogLevel {
    }

    private HCInit() {
    }

    public static void initLoadPackageParam(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam == null)
            throw new UnexpectedException("loadPackageParam must not is null!");

        HCData.setIsXposed(true);
        HCData.setLoadPackageParam(loadPackageParam);
        HCData.setClassLoader(loadPackageParam.classLoader);
        XposedLog.logI("Init classloader: [" + loadPackageParam.classLoader + "], packageName: " + loadPackageParam.packageName);
    }

    public static void initStartupParam(IXposedHookZygoteInit.StartupParam startupParam) {
        if (startupParam == null)
            throw new UnexpectedException("startupParam must not is null!");

        HCData.setIsXposed(true);
        HCData.setStartupParam(startupParam);
        HCData.setClassLoader(
            Optional.ofNullable(
                startupParam.getClass().getClassLoader()
            ).orElse(ClassLoader.getSystemClassLoader())
        );
    }

    public static void initBasicData(BasicData basicData) {
        HCData.setTag(basicData.tag);
        HCData.setLogLevel(basicData.logLevel);
        HCData.setModulePackageName(basicData.packageName);
        HCData.setPrefsName(basicData.prefsName);
        HCData.setAutoReload(basicData.isAutoReload);
        HCData.setLogExpandPath(basicData.logExpandPath);
    }

    public static void setClassLoader(ClassLoader classLoader) {
        HCData.setClassLoader(classLoader);
        BaseHC.classLoader = classLoader;
    }

    public final static class BasicData {
        String tag = null;
        int logLevel = LOG_I;
        String packageName = null;
        String prefsName = null;
        boolean isAutoReload = true;
        String[] logExpandPath = null;

        public BasicData setModulePackageName(@NonNull String modulePackageName) {
            packageName = modulePackageName;
            return this;
        }

        public BasicData setTag(@NonNull String tag) {
            this.tag = tag;
            return this;
        }

        public BasicData setLogLevel(@LogLevel int level) {
            logLevel = level;
            return this;
        }

        public BasicData setPrefsName(@NonNull String prefsName) {
            this.prefsName = prefsName;
            return this;
        }

        public BasicData setAutoReload(boolean auto) {
            this.isAutoReload = auto;
            return this;
        }

        public BasicData setLogExpandPath(@NonNull String... logExpandPath) {
            this.logExpandPath = logExpandPath;
            return this;
        }
    }
}
