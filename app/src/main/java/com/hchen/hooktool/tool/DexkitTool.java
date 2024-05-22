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

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import androidx.annotation.Nullable;

import com.hchen.hooktool.action.Action;
import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.utils.DataUtils;

import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class DexkitTool {
    private final DataUtils utils;

    public DexkitTool(DataUtils utils) {
        this.utils = utils;
    }

    @Nullable
    public Method getMethod(Class<?> clz, String name, Object... obs) {
        try {
            return clz.getMethod(name, objsToClist(obs));
        } catch (NoSuchMethodException e) {
            logE(utils.getTAG(), "get method: " + e);
        }
        return null;
    }

    @Nullable
    public Constructor<?> getConstructor(Class<?> clz, Object... obs) {
        try {
            return clz.getConstructor(objsToClist(obs));
        } catch (NoSuchMethodException e) {
            logE(utils.getTAG(), "get constructor: " + e);
        }
        return null;
    }

    public void hookMethod(Member member, IAction iAction) {
        try {
            hook(member, iAction);
        } catch (Throwable e) {
            logE(utils.getTAG(), "dexkit hook member: " + e);
        }
    }

    public void hookMethod(MethodData methodData, IAction iAction) {
        try {
            Method method = methodData.getMethodInstance(utils.getClassLoader());
            hook(method, iAction);
        } catch (Throwable e) {
            logE(utils.getTAG(), "dexkit instance method: " + e);
        }
    }

    public void hookMethod(ClassData classData, IAction iAction, Object... objs) {
        try {
            Class<?> clzz = classData.getInstance(utils.getClassLoader());
            Constructor<?> constructor = clzz.getConstructor(objsToClist(objs));
            hook(constructor, iAction);
        } catch (Throwable e) {
            logE(utils.getTAG(), "dexkit instance constructor: " + e);
        }
    }

    private void hook(Member member, IAction iAction) throws Throwable {
        XposedBridge.hookMethod(member, hookTool(member, iAction));
    }

    private Action hookTool(Member member, IAction iAction) {
        ParamTool paramTool = new ParamTool(member, utils.getTAG());
        StaticTool staticTool = new StaticTool(utils.getClassLoader(), utils.getTAG());
        return new Action(utils.getTAG()) {
            @Override
            protected void before(MethodHookParam param) {
                paramTool.setParam(param);
                iAction.before(paramTool, staticTool);
            }

            @Override
            protected void after(MethodHookParam param) {
                paramTool.setParam(param);
                iAction.after(paramTool, staticTool);
            }
        };
    }

    @Nullable
    private Class<?> findClass(String name) {
        try {
            return XposedHelpers.findClass(name,
                    utils.getClassLoader());
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(utils.getTAG(), "The specified class could not be found: " + name + " e: " + e);
        }
        return null;
    }

    private Class<?>[] objsToClist(Object... objs) {
        ArrayList<Class<?>> classes = new ArrayList<>();
        for (Object o : objs) {
            if (o instanceof Class<?> c) {
                classes.add(c);
            } else if (o instanceof String s) {
                Class<?> ct = findClass(s);
                if (ct == null) {
                    logW(utils.getTAG(), "this string to class is null: " + s);
                    return null;
                }
                classes.add(ct);
            } else {
                logW(utils.getTAG(), "unknown type: " + o);
                return null;
            }
        }
        return classes.toArray(new Class<?>[classes.size()]);
    }
}
