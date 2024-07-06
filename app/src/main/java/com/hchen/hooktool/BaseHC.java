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
package com.hchen.hooktool;

import static com.hchen.hooktool.log.XposedLog.logE;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.itool.IDynamic;
import com.hchen.hooktool.itool.IMember;
import com.hchen.hooktool.itool.IStatic;
import com.hchen.hooktool.tool.ChainTool;
import com.hchen.hooktool.tool.CoreTool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 对需要使用工具的类继承本类，可快速使用工具。
 */
public abstract class BaseHC implements IMember, IDynamic, IStatic {
    public String TAG = getClass().getSimpleName();
    public XC_LoadPackage.LoadPackageParam lpparam;
    public HCHook hcHook;
    public CoreTool coreTool;

    public abstract void init();

    public void onCreate() {
        hcHook = new HCHook();
        coreTool = hcHook.coreTool();
        hcHook.setThisTag(TAG);
        lpparam = hcHook.getLpparam();
        try {
            init();
        } catch (Throwable e) {
            logE(TAG, e);
        }
    }

    public void chain(String clazz, ChainTool chain) {
        hcHook.actionTool().chain(clazz, chain);
    }

    public void chain(String clazz, ClassLoader classLoader, ChainTool chain) {
        hcHook.actionTool().chain(clazz, classLoader, chain);
    }

    public ChainTool.ChainHook method(String name, Object... params) {
        return hcHook.chainTool().method(name, params);
    }

    public ChainTool.ChainHook constructor(Object... params) {
        return hcHook.chainTool().constructor(params);
    }

    @Override
    public <T, C> C callMethod(Object instance, String name, T ts) {
        return coreTool.callMethod(instance, name, ts);
    }

    @Override
    public <C> C callMethod(Object instance, String name) {
        return coreTool.callMethod(instance, name);
    }

    @Override
    public <T> T getField(Object instance, String name) {
        return coreTool.getField(instance, name);
    }

    @Override
    public <T> T getField(Object instance, Field field) {
        return coreTool.getField(instance, field);
    }

    @Override
    public boolean setField(Object instance, String name, Object value) {
        return coreTool.setField(instance, name, value);
    }

    @Override
    public boolean setField(Object instance, Field field, Object value) {
        return coreTool.setField(instance, field, value);
    }

    @Override
    public boolean setAdditionalInstanceField(Object instance, String key, Object value) {
        return coreTool.setAdditionalInstanceField(instance, key, value);
    }

    @Override
    public <T> T getAdditionalInstanceField(Object instance, String key) {
        return coreTool.getAdditionalInstanceField(instance, key);
    }

    @Override
    public boolean removeAdditionalInstanceField(Object instance, String key) {
        return coreTool.removeAdditionalInstanceField(instance, key);
    }

    @Override
    public boolean ifExistsClass(String clazz) {
        return coreTool.ifExistsClass(clazz);
    }

    @Override
    public boolean ifExistsClass(String clazz, ClassLoader classLoader) {
        return coreTool.ifExistsClass(clazz, classLoader);
    }

    @Override
    public Class<?> findClass(String name) {
        return coreTool.findClass(name);
    }

    @Override
    public Class<?> findClass(String name, ClassLoader classLoader) {
        return coreTool.findClass(name, classLoader);
    }

    @Override
    public boolean ifExistsMethod(String clazz, String name, Object... ojbs) {
        return coreTool.ifExistsMethod(clazz, name, ojbs);
    }

    @Override
    public boolean ifExistsMethod(String clazz, ClassLoader classLoader, String name, Object... ojbs) {
        return coreTool.ifExistsMethod(clazz, classLoader, name, ojbs);
    }

    @Override
    public boolean ifExistsAnyMethod(String clazz, String name) {
        return coreTool.ifExistsAnyMethod(clazz, name);
    }

    @Override
    public boolean ifExistsAnyMethod(String clazz, ClassLoader classLoader, String name) {
        return coreTool.ifExistsAnyMethod(clazz, classLoader, name);
    }

    @Override
    public Method findMethod(String clazz, String name, Object... objects) {
        return coreTool.findMethod(clazz, name, objects);
    }

    @Override
    public Method findMethod(String clazz, ClassLoader classLoader, String name, Object... objects) {
        return coreTool.findMethod(clazz, classLoader, name, objects);
    }

    @Override
    public Method findMethod(Class<?> clazz, String name, Object... objects) {
        return coreTool.findMethod(clazz, name, objects);
    }

    @Override
    public ArrayList<Method> findAnyMethod(String clazz, String name) {
        return coreTool.findAnyMethod(clazz, name);
    }

    @Override
    public ArrayList<Method> findAnyMethod(String clazz, ClassLoader classLoader, String name) {
        return coreTool.findAnyMethod(clazz, classLoader, name);
    }

    @Override
    public ArrayList<Method> findAnyMethod(Class<?> clazz, String name) {
        return coreTool.findAnyMethod(clazz, name);
    }

    @Override
    public Constructor<?> findConstructor(String clazz, Object... objects) {
        return coreTool.findConstructor(clazz, objects);
    }

    @Override
    public Constructor<?> findConstructor(String clazz, ClassLoader classLoader, Object... objects) {
        return coreTool.findConstructor(clazz, classLoader, objects);
    }

    @Override
    public Constructor<?> findConstructor(Class<?> clazz, Object... objects) {
        return coreTool.findConstructor(clazz, objects);
    }

    @Override
    public ArrayList<Constructor<?>> findAnyConstructor(String clazz) {
        return coreTool.findAnyConstructor(clazz);
    }

    @Override
    public ArrayList<Constructor<?>> findAnyConstructor(String clazz, ClassLoader classLoader) {
        return coreTool.findAnyConstructor(clazz, classLoader);
    }

    @Override
    public ArrayList<Constructor<?>> findAnyConstructor(Class<?> clazz) {
        return coreTool.findAnyConstructor(clazz);
    }

    @Override
    public boolean ifExistsField(String clazz, String name) {
        return coreTool.ifExistsField(clazz, name);
    }

    @Override
    public boolean ifExistsField(String clazz, ClassLoader classLoader, String name) {
        return coreTool.ifExistsField(clazz, classLoader, name);
    }

    @Override
    public Field findField(String clazz, String name) {
        return coreTool.findField(clazz, name);
    }

    @Override
    public Field findField(String clazz, ClassLoader classLoader, String name) {
        return coreTool.findField(clazz, classLoader, name);
    }

    @Override
    public Field findField(Class<?> clazz, String name) {
        return coreTool.findField(clazz, name);
    }

    @Override
    public XC_MethodHook.Unhook hook(Member member, IAction iAction) {
        return coreTool.hook(member, iAction);
    }

    @Override
    public ArrayList<XC_MethodHook.Unhook> hook(ArrayList<?> members, IAction iAction) {
        return coreTool.hook(members, iAction);
    }

    @Override
    public IAction returnResult(Object result) {
        return coreTool.returnResult(result);
    }

    @Override
    public IAction doNothing() {
        return coreTool.doNothing();
    }

    @Override
    public ArrayList<Method> filterMethod(Class<?> clazz, CoreTool.IFindMethod iFindMethod) {
        return coreTool.filterMethod(clazz, iFindMethod);
    }

    @Override
    public ArrayList<Constructor<?>> filterMethod(Class<?> clazz, CoreTool.IFindConstructor iFindConstructor) {
        return coreTool.filterMethod(clazz, iFindConstructor);
    }

    @Override
    public <T, C> C newInstance(Class<?> clz, T objects) {
        return coreTool.newInstance(clz, objects);
    }

    @Override
    public <C> C newInstance(Class<?> clz) {
        return coreTool.newInstance(clz);
    }

    @Override
    public <T, C> C callStaticMethod(Class<?> clz, String name, T objs) {
        return coreTool.callStaticMethod(clz, name, objs);
    }

    @Override
    public <C> C callStaticMethod(Class<?> clz, String name) {
        return coreTool.callStaticMethod(clz, name);
    }

    @Override
    public <T> T getStaticField(Class<?> clz, String name) {
        return coreTool.getStaticField(clz, name);
    }

    @Override
    public <T> T getStaticField(Field field) {
        return coreTool.getStaticField(field);
    }

    @Override
    public boolean setStaticField(Class<?> clz, String name, Object value) {
        return coreTool.setStaticField(clz, name, value);
    }

    @Override
    public boolean setStaticField(Field field, Object value) {
        return coreTool.setStaticField(field, value);
    }

    @Override
    public boolean setAdditionalStaticField(Class<?> clz, String key, Object value) {
        return coreTool.setAdditionalStaticField(clz, key, value);
    }

    @Override
    public <T> T getAdditionalStaticField(Class<?> clz, String key) {
        return coreTool.getAdditionalStaticField(clz, key);
    }

    @Override
    public boolean removeAdditionalStaticField(Class<?> clz, String key) {
        return coreTool.removeAdditionalStaticField(clz, key);
    }
}
