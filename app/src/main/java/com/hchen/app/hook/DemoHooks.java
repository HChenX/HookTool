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

import com.hchen.hooktool.log.AndroidLog;

import java.util.List;

/**
 * 示例 Hook 的注册表。
 *
 * @author 焕晨HChen
 */
public final class DemoHooks {
    private static final String TAG = "DemoHooks";
    private static final List<DemoHook> HOOKS = List.of(
        new BasicHooks(),
        new ChainHooks(),
        new DslHooks()
    );

    private DemoHooks() {
    }

    /**
     * 安装全部示例 Hook，单个失败不影响其余。
     */
    public static void installAll() {
        for (DemoHook hook : HOOKS) {
            try {
                hook.install();
            } catch (Throwable throwable) {
                AndroidLog.logE(TAG, "安装失败: " + hook.getClass().getSimpleName(), throwable);
            }
        }
    }

    /**
     * 卸载全部示例 Hook。
     */
    public static void uninstallAll() {
        for (DemoHook hook : HOOKS) {
            try {
                hook.uninstall();
            } catch (Throwable throwable) {
                AndroidLog.logW(TAG, "卸载失败: " + hook.getClass().getSimpleName(), throwable);
            }
        }
    }
}
