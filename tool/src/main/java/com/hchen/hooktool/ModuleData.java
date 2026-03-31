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

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import com.hchen.hooktool.exception.UnexpectedException;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import io.github.libxposed.api.XposedInterfaceWrapper;

/**
 * ModuleData
 *
 * @author 焕晨HChen
 */
public final class ModuleData {
    private static boolean isXposedEnvironment;
    private static XposedInterfaceWrapper wrapper;
    private static ClassLoader classLoader;
    private static final Map<String, SharedPreferences> mRemotePreferences = new WeakHashMap<>();

    private ModuleData() {
    }

    static void setWrapper(@NonNull XposedInterfaceWrapper wrapper) {
        ModuleData.wrapper = wrapper;
    }

    public static void setClassLoader(@NonNull ClassLoader classLoader) {
        ModuleData.classLoader = classLoader;
    }

    @NonNull
    public static XposedInterfaceWrapper getWrapper() {
        Objects.requireNonNull(wrapper);
        return wrapper;
    }

    public static int getApiVersion() {
        return getWrapper().getApiVersion();
    }

    @NonNull
    public static String getFrameworkName() {
        return getWrapper().getFrameworkName();
    }

    @NonNull
    public static String getFrameworkVersion() {
        return getWrapper().getFrameworkVersion();
    }

    public static long getFrameworkVersionCode() {
        return getWrapper().getFrameworkVersionCode();
    }

    public static long getFrameworkProperties() {
        return getWrapper().getFrameworkProperties();
    }

    @NonNull
    public static SharedPreferences getRemotePreferences(@NonNull String name) {
        if (isXposedEnvironment()) {
            return getWrapper().getRemotePreferences(name);
        } else {
            return Objects.requireNonNull(mRemotePreferences.get(name));
        }
    }

    public static void addRemotePreferences(@NonNull String name, @NonNull SharedPreferences preferences) {
        if (isXposedEnvironment()) {
            throw new UnexpectedException("Please set up in the module environment.");
        }
        mRemotePreferences.put(name, preferences);
    }

    public static void clearRemotePreferences() {
        if (isXposedEnvironment()) {
            throw new UnexpectedException("Please set up in the module environment.");
        }
        mRemotePreferences.clear();
    }

    @NonNull
    public static ApplicationInfo getModuleApplicationInfo() {
        return getWrapper().getModuleApplicationInfo();
    }

    @NonNull
    public static String getModulePackageName() {
        return getWrapper().getModuleApplicationInfo().packageName;
    }

    @NonNull
    public static String getModulePath() {
        return getWrapper().getModuleApplicationInfo().sourceDir;
    }

    @NonNull
    public static String[] listRemoteFiles() {
        return getWrapper().listRemoteFiles();
    }

    @NonNull
    public static ParcelFileDescriptor openRemoteFile(@NonNull String name) throws FileNotFoundException {
        return getWrapper().openRemoteFile(name);
    }

    @NonNull
    public static ClassLoader getClassLoader() {
        Objects.requireNonNull(classLoader);
        return classLoader;
    }

    @NonNull
    public static ClassLoader getSystemClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }

    @SuppressWarnings("SameParameterValue")
    static void setXposedEnvironment(boolean isXposedEnvironment) {
        ModuleData.isXposedEnvironment = isXposedEnvironment;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isXposedEnvironment() {
        return isXposedEnvironment;
    }
}
