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

 * Copyright (C) 2023-2024 HChenX
 */
package com.hchen.hooktool.hook;

import static com.hchen.hooktool.log.XposedLog.logE;

import de.robv.android.xposed.XC_MethodHook;

/**
 * Hook 创建工厂
 *
 * @author 焕晨HChen
 */
public final class HookFactory {
    public static XposedCallBack createHook(String tag, IHook iHook) {
        iHook.PRIVATETAG = tag;
        return new XposedCallBack(iHook);
    }

    public final static class XposedCallBack extends XC_MethodHook {
        private final IHook iHook;

        public XposedCallBack(IHook iHook) {
            super(iHook.PRIORITY);
            this.iHook = iHook;
            iHook.XCMethodHook(this);
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                iHook.MethodHookParam(param);
                iHook.before();
            } catch (Throwable e) {
                logE(iHook.PRIVATETAG + ":before", e);
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                iHook.MethodHookParam(param);
                iHook.after();
            } catch (Throwable e) {
                logE(iHook.PRIVATETAG + ":after", e);
            }
        }
    }
}

