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

 * Copyright (C) 2023-2024 HookTool Contributions
 */
package com.hchen.hooktool.action;

import static com.hchen.hooktool.log.XposedLog.logE;

import de.robv.android.xposed.XC_MethodHook;

public class Action extends XC_MethodHook {
    private String TAG = null;

    protected void before(MethodHookParam param) {
    }

    protected void after(MethodHookParam param) {
    }

    public Action(String tag) {
        super();
        TAG = tag;
    }

    public Action(String tag, int priority) {
        super(priority);
        TAG = tag;
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        try {
            before(param);
        } catch (Throwable e) {
            logE(TAG + ":" + "before", e);
        }
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) {
        try {
            after(param);
        } catch (Throwable e) {
            logE(TAG + ":" + "after", e);
        }
    }
}
