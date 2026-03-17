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

import androidx.annotation.NonNull;

import com.hchen.hooktool.core.CoreTool;

import java.util.Objects;

/**
 * AbsModule
 * <p>
 * 推荐继承此类用以实现 Hook 内容
 *
 * @author 焕晨HChen
 */
public abstract class AbsModule extends CoreTool {
    public String TAG = getClass().getSimpleName();

    public enum StageEnum {
        MODULE_LOADED,
        PACKAGE_LOADED,
        PACKAGE_READY,
        SYSTEM_SERVER_STARTING
    }

    protected boolean isEnabled() {
        return true;
    }

    protected abstract void onLoaded(@NonNull StageEnum stage);

    protected void onLoaded(@NonNull ClassLoader classLoader) {
    }

    protected void onThrowable(@NonNull Throwable e) {
    }

    final public void handleLoaded(@NonNull StageEnum stage) {
        try {
            if (!isEnabled()) return;

            Objects.requireNonNull(stage);
            onLoaded(stage);
        } catch (Throwable e) {
            onThrowable(e);
            logE(TAG, "[handleLoaded]: Will stop hook process!!", e);
        }
    }

    final public void handleLoaded(@NonNull ClassLoader classLoader) {
        try {
            if (!isEnabled()) return;
            onLoaded(classLoader);
        } catch (Throwable e) {
            onThrowable(e);
            logE(TAG, "[handleLoaded/classLoader]: Will stop hook process!!", e);
        }
    }
}
