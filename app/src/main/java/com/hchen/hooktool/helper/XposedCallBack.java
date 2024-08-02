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
package com.hchen.hooktool.helper;

import static com.hchen.hooktool.log.XposedLog.logE;

import de.robv.android.xposed.XC_MethodHook;

/**
 * Hook 回调
 * <p>
 * Hook CallBack
 * 
 * @author 焕晨HChen
 */
public abstract class XposedCallBack extends XC_MethodHook {
    private final String TAG;

    public XposedCallBack(String tag, int priority) {
        super(priority);
        TAG = tag;
        XC_MethodHook(this);
    }

    public void before(MethodHookParam param) {
    }

    public void after(MethodHookParam param) {
    }

    abstract void XC_MethodHook(XC_MethodHook xcMethodHook);

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        try {
            before(param);
        } catch (Throwable e) {
            logE(TAG + ":before", e);
        }
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        try {
            after(param);
        } catch (Throwable e) {
            logE(TAG + ":after", e);
        }
    }
}
