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
import com.hchen.hooktool.tool.ClassTool;
import com.hchen.hooktool.tool.DexkitTool;
import com.hchen.hooktool.tool.ExpandTool;
import com.hchen.hooktool.tool.FieldTool;
import com.hchen.hooktool.tool.MethodTool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 对需要使用工具的类继承本类，可快速使用工具。
 */
public abstract class BaseHC implements IMember, IDynamic, IStatic {
    public String TAG = getClass().getSimpleName();
    public static XC_LoadPackage.LoadPackageParam lpparam;
    public static HCHook hcHook;
    public static ClassTool classTool;
    public static MethodTool methodTool;
    public static FieldTool fieldTool;
    public static DexkitTool dexkitTool;
    public static ExpandTool expandTool;

    public abstract void init();

    public BaseHC() {
        BaseHC.hcHook = new HCHook();
        BaseHC.classTool = hcHook.classTool();
        BaseHC.methodTool = hcHook.methodTool();
        BaseHC.fieldTool = hcHook.fieldTool();
        BaseHC.dexkitTool = hcHook.dexkitTool();
        BaseHC.expandTool = hcHook.expandTool();
        BaseHC.hcHook.setThisTag(TAG);
        lpparam = hcHook.getLpparam();
        try {
            init();
        } catch (Throwable e) {
            logE(TAG, e);
        }
    }

    @Override
    public <T, C> C callMethod(Object instance, String name, T ts) {
        return expandTool.callMethod(instance, name, ts);
    }

    @Override
    public <C> C callMethod(Object instance, String name) {
        return expandTool.callMethod(instance, name);
    }

    @Override
    public <T> T getField(Object instance, String name) {
        return expandTool.getField(instance, name);
    }

    @Override
    public <T> T getField(Object instance, Field field) {
        return expandTool.getField(instance, field);
    }

    @Override
    public boolean setField(Object instance, String name, Object value) {
        return expandTool.setField(instance, name, value);
    }

    @Override
    public boolean setField(Object instance, Field field, Object value) {
        return expandTool.setField(instance, field, value);
    }

    @Override
    public boolean setAdditionalInstanceField(Object instance, String key, Object value) {
        return expandTool.setAdditionalInstanceField(instance, key, value);
    }

    @Override
    public <T> T getAdditionalInstanceField(Object instance, String key) {
        return expandTool.getAdditionalInstanceField(instance, key);
    }

    @Override
    public boolean removeAdditionalInstanceField(Object instance, String key) {
        return expandTool.removeAdditionalInstanceField(instance, key);
    }

    @Override
    public boolean findClassIfExists(String clazz) {
        return expandTool.findClassIfExists(clazz);
    }

    @Override
    public boolean findClassIfExists(String clazz, ClassLoader classLoader) {
        return expandTool.findClassIfExists(clazz, classLoader);
    }

    @Override
    public Class<?> findClass(String name) {
        return expandTool.findClass(name);
    }

    @Override
    public Class<?> findClass(String name, ClassLoader classLoader) {
        return expandTool.findClass(name, classLoader);
    }

    @Override
    public boolean findMethodIfExists(String clazz, String name, Object... ojbs) {
        return expandTool.findMethodIfExists(clazz, name, ojbs);
    }

    @Override
    public boolean findMethodIfExists(String clazz, ClassLoader classLoader, String name, Object... ojbs) {
        return expandTool.findMethodIfExists(clazz, classLoader, name, ojbs);
    }

    @Override
    public boolean findAnyMethodIfExists(String clazz, String name) {
        return expandTool.findAnyMethodIfExists(clazz, name);
    }

    @Override
    public boolean findAnyMethodIfExists(String clazz, ClassLoader classLoader, String name) {
        return expandTool.findAnyMethodIfExists(clazz, classLoader, name);
    }

    @Override
    public Method findMethod(String clazz, String name, Object... objects) {
        return expandTool.findMethod(clazz, name, objects);
    }

    @Override
    public Method findMethod(String clazz, ClassLoader classLoader, String name, Object... objects) {
        return expandTool.findMethod(clazz, classLoader, name, objects);
    }

    @Override
    public Method findMethod(Class<?> clazz, String name, Object... objects) {
        return expandTool.findMethod(clazz, name, objects);
    }

    @Override
    public ArrayList<Method> findAnyMethod(String clazz, String name) {
        return expandTool.findAnyMethod(clazz, name);
    }

    @Override
    public ArrayList<Method> findAnyMethod(String clazz, ClassLoader classLoader, String name) {
        return expandTool.findAnyMethod(clazz, classLoader, name);
    }

    @Override
    public ArrayList<Method> findAnyMethod(Class<?> clazz, String name) {
        return expandTool.findAnyMethod(clazz, name);
    }

    @Override
    public Constructor<?> findConstructor(String clazz, Object... objects) {
        return expandTool.findConstructor(clazz, objects);
    }

    @Override
    public Constructor<?> findConstructor(String clazz, ClassLoader classLoader, Object... objects) {
        return expandTool.findConstructor(clazz, classLoader, objects);
    }

    @Override
    public Constructor<?> findConstructor(Class<?> clazz, Object... objects) {
        return expandTool.findConstructor(clazz, objects);
    }

    @Override
    public ArrayList<Constructor<?>> findAnyConstructor(String clazz) {
        return expandTool.findAnyConstructor(clazz);
    }

    @Override
    public ArrayList<Constructor<?>> findAnyConstructor(String clazz, ClassLoader classLoader) {
        return expandTool.findAnyConstructor(clazz, classLoader);
    }

    @Override
    public ArrayList<Constructor<?>> findAnyConstructor(Class<?> clazz) {
        return expandTool.findAnyConstructor(clazz);
    }

    @Override
    public boolean findFieldIfExists(String clazz, String name) {
        return expandTool.findFieldIfExists(clazz, name);
    }

    @Override
    public boolean findFieldIfExists(String clazz, ClassLoader classLoader, String name) {
        return expandTool.findFieldIfExists(clazz, classLoader, name);
    }

    @Override
    public Field findField(String clazz, String name) {
        return expandTool.findField(clazz, name);
    }

    @Override
    public Field findField(String clazz, ClassLoader classLoader, String name) {
        return expandTool.findField(clazz, classLoader, name);
    }

    @Override
    public Field findField(Class<?> clazz, String name) {
        return expandTool.findField(clazz, name);
    }

    @Override
    public void hook(Member member, IAction iAction) {
        expandTool.hook(member, iAction);
    }

    @Override
    public void hook(ArrayList<?> members, IAction iAction) {
        expandTool.hook(members, iAction);
    }

    @Override
    public IAction returnResult(Object result) {
        return expandTool.returnResult(result);
    }

    @Override
    public IAction doNothing() {
        return expandTool.doNothing();
    }

    @Override
    public ArrayList<Method> filterMethod(Class<?> clazz, ExpandTool.IFindMethod iFindMethod) {
        return expandTool.filterMethod(clazz, iFindMethod);
    }

    @Override
    public ArrayList<Constructor<?>> filterMethod(Class<?> clazz, ExpandTool.IFindConstructor iFindConstructor) {
        return expandTool.filterMethod(clazz, iFindConstructor);
    }

    @Override
    public <T, C> C newInstance(Class<?> clz, T objects) {
        return expandTool.newInstance(clz, objects);
    }

    @Override
    public <C> C newInstance(Class<?> clz) {
        return expandTool.newInstance(clz);
    }

    @Override
    public <T, C> C callStaticMethod(Class<?> clz, String name, T objs) {
        return expandTool.callStaticMethod(clz, name, objs);
    }

    @Override
    public <C> C callStaticMethod(Class<?> clz, String name) {
        return expandTool.callStaticMethod(clz, name);
    }

    @Override
    public <T> T getStaticField(Class<?> clz, String name) {
        return expandTool.getStaticField(clz, name);
    }

    @Override
    public <T> T getStaticField(Field field) {
        return expandTool.getStaticField(field);
    }

    @Override
    public boolean setStaticField(Class<?> clz, String name, Object value) {
        return expandTool.setStaticField(clz, name, value);
    }

    @Override
    public boolean setStaticField(Field field, Object value) {
        return expandTool.setStaticField(field, value);
    }

    @Override
    public boolean setAdditionalStaticField(Class<?> clz, String key, Object value) {
        return expandTool.setAdditionalStaticField(clz, key, value);
    }

    @Override
    public <T> T getAdditionalStaticField(Class<?> clz, String key) {
        return expandTool.getAdditionalStaticField(clz, key);
    }

    @Override
    public boolean removeAdditionalStaticField(Class<?> clz, String key) {
        return expandTool.removeAdditionalStaticField(clz, key);
    }
}
