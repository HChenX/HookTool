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

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.data.Priority;
import com.hchen.hooktool.data.ToolData;

import de.robv.android.xposed.XC_MethodHook;

/**
 * Hook 创建工厂
 * <p>
 * Hook to create a factory
 * 
 * @author 焕晨HChen
 */
public class HookFactory {
    private final ToolData data;

    public HookFactory(ToolData data) {
        this.data = data;
    }

    public XposedCallBack createHook(IAction iAction) {
        iAction.ToolData(data);
        return new XposedCallBack(data.tag(), switch (iAction.priority) {
            case Priority.DEFAULT -> 50;
            case Priority.LOWEST -> -10000;
            case Priority.HIGHEST -> 10000;
        }) {
            @Override
            public void before(MethodHookParam param) {
                iAction.before();
            }

            @Override
            public void after(MethodHookParam param) {
                iAction.after();
            }

            @Override
            void XC_MethodHook(XC_MethodHook xcMethodHook) {
                iAction.XCMethodHook(xcMethodHook);
            }
        };
    }
}
