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

 * Copyright (C) 2023-2025 HChenX
 */
package com.hchen.hooktool.hook;

import com.hchen.hooktool.core.CoreTool;
import com.hchen.hooktool.log.XposedLog;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XC_MethodHook;

/**
 * Hook 构建工厂
 *
 * @author 焕晨HChen
 */
public class HookFactory {
    private HookFactory() {
    }

    public static XC_MethodHook createHook(String tag, IHook iHook) {
        iHook.INNER_TAG = tag;
        XC_MethodHook xcMethodHook = new XC_MethodHook(iHook.PRIORITY) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    iHook.param = param;
                    iHook.before();
                } catch (Throwable e) {
                    if (!iHook.onThrow(IHook.BEFORE, e)) {
                        try {
                            param.setResult(CoreTool.invokeOriginalMethod(param.method, param.thisObject, param.args));
                        } catch (InvocationTargetException exception) {
                            param.setThrowable(exception.getCause());
                        } catch (Throwable ignore) {
                        }
                        XposedLog.logE(tag, "Before throw!", e);
                    }
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object lastResult = param.getResult();
                Throwable lastThrowable = param.getThrowable();

                try {
                    iHook.param = param;
                    iHook.after();
                } catch (Throwable e) {
                    if (!iHook.onThrow(IHook.AFTER, e)) {
                        if (lastThrowable == null) param.setResult(lastResult);
                        else param.setThrowable(lastThrowable);

                        XposedLog.logE(tag, "After throw!", e);
                    }
                }
            }
        };
        iHook.xcMethodHook = xcMethodHook;
        return xcMethodHook;
    }
}
