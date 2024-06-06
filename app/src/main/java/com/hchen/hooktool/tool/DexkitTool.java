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
package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logD;
import static com.hchen.hooktool.log.XposedLog.logE;

import com.hchen.hooktool.action.Action;
import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.utils.ConvertHelper;
import com.hchen.hooktool.utils.DataUtils;

import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;

public class DexkitTool extends ConvertHelper {
    public DexkitTool(DataUtils utils) {
        super(utils);
    }

    public DexkitTool hookMethod(Member member, IAction iAction) {
        try {
            hook(member, iAction);
        } catch (Throwable e) {
            logE(utils.getTAG(), "dexkit hook member failed!", e);
        }
        return this;
    }

    public DexkitTool hookMethod(MethodData methodData, IAction iAction) {
        try {
            Method method = methodData.getMethodInstance(utils.getClassLoader());
            hook(method, iAction);
        } catch (Throwable e) {
            logE(utils.getTAG(), "dexkit instance method failed!", e);
        }
        return this;
    }

    public DexkitTool hookMethod(ClassData classData, IAction iAction, Object... objs) {
        try {
            Class<?> clzz = classData.getInstance(utils.getClassLoader());
            Constructor<?> constructor = clzz.getConstructor(objectArrayToClassArray(objs));
            hook(constructor, iAction);
        } catch (Throwable e) {
            logE(utils.getTAG(), "dexkit instance constructor failed!", e);
        }
        return this;
    }

    private void hook(Member member, IAction iAction) throws Throwable {
        XposedBridge.hookMethod(member, hookTool(member, iAction));
        logD(utils.getTAG(), "success to hook: " + member);
    }

    private Action hookTool(Member member, IAction iAction) {
        ParamTool paramTool = new ParamTool(utils);
        return new Action(member, utils.getTAG()) {
            @Override
            protected void before(MethodHookParam param) {
                paramTool.setParam(param);
                iAction.before(paramTool);
            }

            @Override
            protected void after(MethodHookParam param) {
                paramTool.setParam(param);
                iAction.after(paramTool);
            }
        };
    }
}
