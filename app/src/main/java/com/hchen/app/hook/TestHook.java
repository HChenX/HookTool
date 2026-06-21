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
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.AbsModule;
import com.hchen.hooktool.hook.AbsHook;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * Hook 功能的示例模块实现。
 *
 * <p>继承自 {@link AbsModule}，是 HookTool 框架中具体 Hook 逻辑的承载单元。
 * 开发者应在 {@link #onLoaded(StageEnum, Object)} 方法中根据不同的加载阶段
 * 注册相应的方法 Hook、类 Hook 或其他拦截操作。</p>
 *
 * <p>当前实现为空模板，可作为新 Hook 模块的参考起点。</p>
 *
 * @see AbsModule
 */
public class TestHook extends AbsModule {
    private Context context;

    /**
     * 模块加载阶段的回调入口。
     *
     * <p>由 HookTool 框架在目标模块加载完成时调用。
     * 开发者应在此方法中根据 {@code stage} 参数判断当前所处的生命周期阶段，
     * 并注册具体的 Hook 逻辑（如方法前置/后置拦截、类替换等）。</p>
     *
     * @param stage 当前模块所处的加载阶段枚举值，用于区分不同的生命周期节点
     * @param param 与当前阶段关联的参数对象，具体类型取决于所处阶段
     * @see AbsModule#onLoaded(StageEnum, Object)
     */
    @SuppressLint("XposedNewApi")
    @Override
    protected void onLoaded(@NonNull StageEnum stage, @NonNull Object param) {
        if (stage == StageEnum.PACKAGE_READY || stage == StageEnum.HOT_RELOADED) {
            if (stage == StageEnum.HOT_RELOADED) {
                XposedModuleInterface.HotReloadedParam hot = (XposedModuleInterface.HotReloadedParam) param;
                // noinspection unchecked
                context = (Context) ((Map<String, Object>) Objects.requireNonNull(hot.getSavedInstanceState())).get("CONTEXT");
            }

            hookMethod("com.hchen.test.Test",
                "test",
                String.class,
                new AbsHook() {
                    private Context context;

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
                        setField(getThisObject(), "field", true);
                    }

                    @Override
                    public boolean onThrow(@NonNull StageEnum stage, @NonNull Throwable e) {
                        return super.onThrow(stage, e);
                    }

                    @NonNull
                    @Override
                    public Map<String, Object> onHotReloading(@Nullable Bundle extras) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("CONTEXT_INNER", context);
                        return map;
                    }

                    @Override
                    public void onHotReloaded(@Nullable Object thisObject, @NonNull Map<String, Object> inState) {
                        super.onHotReloaded(thisObject, inState);
                        context = (Context) inState.get("CONTEXT_INNER");
                        if (thisObject != null) {
                            setField(thisObject, "field", true);
                        }
                    }
                }
            );
        }
    }

    /**
     * 模块级热重载准备回调。
     * <p>
     * 在此保存需要在热重载后恢复的模块级状态数据。
     * 返回的 {@link Map} 会被 {@link com.hchen.hooktool.hook.HookRegistry#reloading(Bundle)}
     * 合并到全局快照中。
     *
     * @param extras 热重载附加数据，可能为 {@code null}
     * @return 模块级状态键值对
     */
    @Override
    protected Map<String, Object> onHotReloading(@Nullable Bundle extras) {
        Map<String, Object> map = new HashMap<>();
        map.put("CONTEXT", context);
        return map;
    }
}
