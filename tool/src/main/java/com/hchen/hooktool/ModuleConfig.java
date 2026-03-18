/*
 * This file is part of HookTool.
 *
 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * HookTool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with HookTool. If not, see <https://www.gnu.org/licenses/lgpl-2.1>.
 *
 * Copyright (C) 2024–2026 HChenX
 */
package com.hchen.hooktool;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.exception.UnexpectedException;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * ModuleConfig
 * <p>
 * 此类存储模块的基本配置
 *
 * @author 焕晨HChen
 */
public class ModuleConfig {
    @NonNull
    private static String logTag;
    @LogLevel
    private static int logLevel;
    @NonNull
    private static String modulePackageName;
    @NonNull
    private static String prefsName;
    private static boolean isAutoReload;
    @Nullable
    private static String[] logExpandPaths;
    @Nullable
    private static String[] logExpandIgnoreClassNames;
    private static boolean isXposedEnvironment;

    // -------- 可选日志等级 --------
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

    static {
        logTag = "unknown";
        logLevel = LOG_I;
        modulePackageName = "";
        prefsName = "";
        isAutoReload = true;
        logExpandPaths = null;
        logExpandIgnoreClassNames = null;
        isXposedEnvironment = false;
    }

    public static void setLogTag(@NonNull String logTag) {
        ModuleConfig.logTag = logTag;
    }

    public static void setLogLevel(@LogLevel int logLevel) {
        ModuleConfig.logLevel = logLevel;
    }

    public static void setModulePackageName(@NonNull String modulePackageName) {
        ModuleConfig.modulePackageName = modulePackageName;
    }

    public static void setPrefsName(@NonNull String prefsName) {
        ModuleConfig.prefsName = prefsName;
    }

    public static void setAutoReload(boolean isAutoReload) {
        ModuleConfig.isAutoReload = isAutoReload;
    }

    public static void setLogExpandPaths(@Nullable String[] logExpandPaths) {
        ModuleConfig.logExpandPaths = logExpandPaths;
    }

    public static void setLogExpandIgnoreClassNames(@Nullable String[] logExpandIgnoreClassNames) {
        ModuleConfig.logExpandIgnoreClassNames = logExpandIgnoreClassNames;
    }

    public static void setXposedEnvironment(boolean isXposedEnvironment) {
        ModuleConfig.isXposedEnvironment = isXposedEnvironment;
    }

    @NonNull
    public static String getLogTag() {
        return logTag;
    }

    @LogLevel
    public static int getLogLevel() {
        return logLevel;
    }

    @NonNull
    public static String getModulePackageName() {
        if (modulePackageName.isEmpty()) {
            throw new UnexpectedException("module package name Must not be empty.");
        }

        return modulePackageName;
    }

    @NonNull
    public static String getPrefsName() {
        return prefsName;
    }

    public static boolean isAutoReload() {
        return isAutoReload;
    }

    @Nullable
    public static String[] getLogExpandPaths() {
        return logExpandPaths;
    }

    @Nullable
    public static String[] getLogExpandIgnoreClassNames() {
        return logExpandIgnoreClassNames;
    }

    public static boolean isXposedEnvironment() {
        return isXposedEnvironment;
    }
}