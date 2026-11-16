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
package com.hchen.app.hook;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.AbsModule;
import com.hchen.hooktool.hook.AbsHook;

import java.util.HashMap;
import java.util.Map;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * Hook 功能的示例模块实现。
 *
 * @author 焕晨HChen
 */
public class TestHook extends AbsModule {
    /**
     * 包就绪回调：注册示例 Hook。
     */
    @Override
    protected void onPackageReady(@NonNull XposedModuleInterface.PackageReadyParam param) {
        registerHooks();
    }

    /**
     * 热更新完成回调：重新注册 Hook（旧句柄已由框架解除）。
     */
    @Override
    protected void onHotReloaded(@NonNull XposedModuleInterface.HotReloadedParam param) {
        registerHooks();
    }

    @SuppressLint("XposedNewApi")
    private void registerHooks() {
        hookMethod("com.hchen.test.Test",
            "test",
            String.class,
            new AbsHook() {
                @Override
                public void before() {
                    super.before();
                }

                @Override
                public Object proceed(@NonNull XposedInterface.Chain chain) throws Throwable {
                    return super.proceed(chain);
                }

                @Override
                public void after() {
                    super.after();
                    Object thisObject = getThisObject();
                    if (thisObject != null) {
                        setField(thisObject, "test", true);
                    }
                }

                @Override
                public boolean onThrow(@NonNull AbsHook.StageEnum stage, @NonNull Throwable e) {
                    return super.onThrow(stage, e);
                }

                @Override
                public void onHotReloaded(@Nullable Object thisObject, @NonNull Map<String, Object> inState) {
                    super.onHotReloaded(thisObject, inState);
                    if (thisObject != null) {
                        setField(thisObject, "test", true);
                    }
                }
            }
        );
    }

    /**
     * 模块级热重载准备回调。
     *
     * @param extras 热重载附加数据，可能为 {@code null}
     * @return 模块级状态键值对
     */
    @Override
    protected Map<String, Object> onHotReloading(@Nullable Bundle extras) {
        return new HashMap<>();
    }
}
