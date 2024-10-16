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
package com.hchen.hooktool.hook;

import static com.hchen.hooktool.log.XposedLog.logE;

import de.robv.android.xposed.XC_MethodHook;

/**
 * Hook 创建工厂
 *
 * @author 焕晨HChen
 */
public class HookFactory {
    public static XposedCallBack createHook(String tag, IAction iAction) {
        iAction.PRIVATETAG = tag;
        return new XposedCallBack(iAction);
    }

    public static class XposedCallBack extends XC_MethodHook {
        private final IAction iAction;

        public XposedCallBack(IAction iAction) {
            super(iAction.PRIORITY);
            this.iAction = iAction;
            iAction.XCMethodHook(this);
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                iAction.MethodHookParam(param);
                iAction.before();
            } catch (Throwable e) {
                logE(iAction.PRIVATETAG + ":before", "Waring! will stop hook process!!", e);
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                iAction.MethodHookParam(param);
                iAction.after();
            } catch (Throwable e) {
                logE(iAction.PRIVATETAG + ":after", "Waring! will stop hook process!!", e);
            }
        }
    }
}

