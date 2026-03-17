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

import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.function.Predicate;

import io.github.libxposed.api.XposedModule;

/**
 * 模块入口
 * <p>
 * 推荐模块实现此类
 *
 * @author 焕晨HChen
 */
public abstract class ModuleEntrance extends XposedModule {
    // 跳过被 ignorePackages 匹配上的包
    private boolean shouldSkip = false;

    public abstract void initModuleConfig();

    @NonNull
    public String[] ignorePackages() {
        return new String[]{};
    }

    public void handleModuleLoaded(@NonNull ModuleLoadedParam param) {
    }

    public void handlePackageLoaded(@NonNull PackageLoadedParam param) {
    }

    public void handlePackageReady(@NonNull PackageReadyParam param) {
    }

    public void handleSystemServerStarting(@NonNull SystemServerStartingParam param) {
    }

    // ------------------------- Inner -----------------------------
    @Override
    public final void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        initModuleConfig(); // 初始化工具
        ModuleData.setWrapper(this);

        handleModuleLoaded(param);
    }

    @Override
    public final void onPackageLoaded(@NonNull PackageLoadedParam param) {
        if (ignorePackages().length > 0) {
            shouldSkip = Arrays.stream(ignorePackages()).anyMatch(
                new Predicate<String>() {
                    @Override
                    public boolean test(String packageName) {
                        return TextUtils.equals(packageName, param.getPackageName());
                    }
                }
            );
        }

        if (shouldSkip) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ModuleData.setClassLoader(param.getDefaultClassLoader());
        }
        handlePackageLoaded(param);
    }

    @Override
    public final void onPackageReady(@NonNull PackageReadyParam param) {
        if (shouldSkip) {
            return;
        }

        ModuleData.setClassLoader(param.getClassLoader());
        handlePackageReady(param);
    }

    @Override
    public final void onSystemServerStarting(@NonNull SystemServerStartingParam param) {
        ModuleData.setClassLoader(param.getClassLoader());
        handleSystemServerStarting(param);
    }
}
