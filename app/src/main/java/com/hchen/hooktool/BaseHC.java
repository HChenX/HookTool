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

import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.itool.IDynamic;
import com.hchen.hooktool.itool.IMember;
import com.hchen.hooktool.itool.IPrefs;
import com.hchen.hooktool.itool.IStatic;
import com.hchen.hooktool.tool.ChainTool;
import com.hchen.hooktool.tool.CoreTool;
import com.hchen.hooktool.tool.PrefsTool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 对需要使用工具的类继承本类，可快速使用工具。
 */
public abstract class BaseHC implements IMember, IDynamic, IStatic {
    public String TAG = getClass().getSimpleName();
    public XC_LoadPackage.LoadPackageParam lpparam;
    public ClassLoader classLoader;
    public ApplicationInfo appInfo;
    public String packageName;
    public boolean isFirstApplication;
    public String processName;
    public HCHook hcHook;
    public CoreTool core;
    public PrefsTool prefs;

    // 工具为了保持日志易读性，无法全部静态化，但您仍然可以直接调用此静态字段使用本工具。
    // 唯一区别是工具日志 tag 始终为 ”StaticHC“（不影响手动设置的 log tag。
    // 当然你也可以在自己类内手动存储静态本类，实现日志正常。
    public static HCHook sHc;
    public static CoreTool sCore;
    public static PrefsTool sPrefs;

    static {
        sHc = new HCHook().setThisTag("StaticHC");
        sCore = sHc.core();
        sPrefs = sHc.prefs();
    }

    /**
     * 正常阶段。
     */
    public abstract void init();

    /**
     * zygote 阶段。
     * <p>
     * 如果 startupParam 为 null，请检查是否为工具初始化。
     */
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
    }

    final public void onCreate() {
        hcHook = new HCHook();
        hcHook.setThisTag(TAG);
        core = hcHook.core();
        prefs = hcHook.prefs();
        lpparam = hcHook.lpparam();
        classLoader = lpparam.classLoader;
        appInfo = lpparam.appInfo;
        packageName = lpparam.packageName;
        isFirstApplication = lpparam.isFirstApplication;
        processName = lpparam.processName;
        try {
            init();
        } catch (Throwable e) {
            logE(TAG, e);
        }
        try {
            initZygote(HCInit.getStartupParam());
        } catch (Throwable e) {
            logE(TAG, e);
        }
    }

    final public IPrefs prefs() {
        return prefs.prefs();
    }

    final public IPrefs prefs(String prefsName) {
        return prefs.prefs(prefsName);
    }

    final public IPrefs prefs(Context context) {
        return prefs.prefs(context);
    }

    final public IPrefs prefs(Context context, String prefsName) {
        return prefs.prefs(context, prefsName);
    }

    final public PrefsTool nativePrefs() {
        return prefs.nativePrefs();
    }

    final public void chain(String clazz, ChainTool chain) {
        hcHook.action().chain(clazz, chain);
    }

    final public void chain(String clazz, ClassLoader classLoader, ChainTool chain) {
        hcHook.action().chain(clazz, classLoader, chain);
    }

    final public ChainTool.ChainHook method(String name, Object... params) {
        return hcHook.chain().method(name, params);
    }

    final public ChainTool.ChainHook anyMethod(String name) {
        return hcHook.chain().anyMethod(name);
    }

    final public ChainTool.ChainHook constructor(Object... params) {
        return hcHook.chain().constructor(params);
    }

    final public ChainTool.ChainHook anyConstructor() {
        return hcHook.chain().anyConstructor();
    }

    @Override
    final public <T, C> C callMethod(Object instance, String name, T ts) {
        return core.callMethod(instance, name, ts);
    }

    @Override
    final public <C> C callMethod(Object instance, String name) {
        return core.callMethod(instance, name);
    }

    @Override
    final public <T> T getField(Object instance, String name) {
        return core.getField(instance, name);
    }

    @Override
    final public <T> T getField(Object instance, Field field) {
        return core.getField(instance, field);
    }

    @Override
    final public boolean setField(Object instance, String name, Object value) {
        return core.setField(instance, name, value);
    }

    @Override
    final public boolean setField(Object instance, Field field, Object value) {
        return core.setField(instance, field, value);
    }

    @Override
    final public boolean setAdditionalInstanceField(Object instance, String key, Object value) {
        return core.setAdditionalInstanceField(instance, key, value);
    }

    @Override
    final public <T> T getAdditionalInstanceField(Object instance, String key) {
        return core.getAdditionalInstanceField(instance, key);
    }

    @Override
    final public boolean removeAdditionalInstanceField(Object instance, String key) {
        return core.removeAdditionalInstanceField(instance, key);
    }

    @Override
    final public boolean existsClass(String clazz) {
        return core.existsClass(clazz);
    }

    @Override
    final public boolean existsClass(String clazz, ClassLoader classLoader) {
        return core.existsClass(clazz, classLoader);
    }

    @Override
    final public Class<?> findClass(String name) {
        return core.findClass(name);
    }

    @Override
    final public Class<?> findClass(String name, ClassLoader classLoader) {
        return core.findClass(name, classLoader);
    }

    @Override
    final public boolean existsMethod(String clazz, String name, Object... ojbs) {
        return core.existsMethod(clazz, name, ojbs);
    }

    @Override
    final public boolean existsMethod(String clazz, ClassLoader classLoader, String name, Object... ojbs) {
        return core.existsMethod(clazz, classLoader, name, ojbs);
    }

    @Override
    final public boolean existsAnyMethod(String clazz, String name) {
        return core.existsAnyMethod(clazz, name);
    }

    @Override
    final public boolean existsAnyMethod(String clazz, ClassLoader classLoader, String name) {
        return core.existsAnyMethod(clazz, classLoader, name);
    }

    @Override
    final public Method findMethod(String clazz, String name, Object... objects) {
        return core.findMethod(clazz, name, objects);
    }

    @Override
    final public Method findMethod(String clazz, ClassLoader classLoader, String name, Object... objects) {
        return core.findMethod(clazz, classLoader, name, objects);
    }

    @Override
    final public Method findMethod(Class<?> clazz, String name, Object... objects) {
        return core.findMethod(clazz, name, objects);
    }

    @Override
    final public ArrayList<Method> findAnyMethod(String clazz, String name) {
        return core.findAnyMethod(clazz, name);
    }

    @Override
    final public ArrayList<Method> findAnyMethod(String clazz, ClassLoader classLoader, String name) {
        return core.findAnyMethod(clazz, classLoader, name);
    }

    @Override
    final public ArrayList<Method> findAnyMethod(Class<?> clazz, String name) {
        return core.findAnyMethod(clazz, name);
    }

    @Override
    final public Constructor<?> findConstructor(String clazz, Object... objects) {
        return core.findConstructor(clazz, objects);
    }

    @Override
    final public Constructor<?> findConstructor(String clazz, ClassLoader classLoader, Object... objects) {
        return core.findConstructor(clazz, classLoader, objects);
    }

    @Override
    final public Constructor<?> findConstructor(Class<?> clazz, Object... objects) {
        return core.findConstructor(clazz, objects);
    }

    @Override
    final public ArrayList<Constructor<?>> findAnyConstructor(String clazz) {
        return core.findAnyConstructor(clazz);
    }

    @Override
    final public ArrayList<Constructor<?>> findAnyConstructor(String clazz, ClassLoader classLoader) {
        return core.findAnyConstructor(clazz, classLoader);
    }

    @Override
    final public ArrayList<Constructor<?>> findAnyConstructor(Class<?> clazz) {
        return core.findAnyConstructor(clazz);
    }

    @Override
    final public boolean existsField(String clazz, String name) {
        return core.existsField(clazz, name);
    }

    @Override
    final public boolean existsField(String clazz, ClassLoader classLoader, String name) {
        return core.existsField(clazz, classLoader, name);
    }

    @Override
    final public Field findField(String clazz, String name) {
        return core.findField(clazz, name);
    }

    @Override
    final public Field findField(String clazz, ClassLoader classLoader, String name) {
        return core.findField(clazz, classLoader, name);
    }

    @Override
    final public Field findField(Class<?> clazz, String name) {
        return core.findField(clazz, name);
    }

    @Override
    final public XC_MethodHook.Unhook hook(String clazz, String method, Object... params) {
        return core.hook(clazz, method, params);
    }

    @Override
    final public XC_MethodHook.Unhook hook(String clazz, ClassLoader classLoader, String method, Object... params) {
        return core.hook(clazz, classLoader, method, params);
    }

    @Override
    final public XC_MethodHook.Unhook hook(Class<?> clazz, String method, Object... params) {
        return core.hook(clazz, method, params);
    }

    @Override
    final public ArrayList<XC_MethodHook.Unhook> hook(String clazz, IAction iAction) {
        return core.hook(clazz, iAction);
    }

    @Override
    final public XC_MethodHook.Unhook hook(String clazz, Object... params) {
        return core.hook(clazz, params);
    }

    @Override
    final public XC_MethodHook.Unhook hook(Member member, IAction iAction) {
        return core.hook(member, iAction);
    }

    @Override
    final public ArrayList<XC_MethodHook.Unhook> hook(ArrayList<?> members, IAction iAction) {
        return core.hook(members, iAction);
    }

    @Override
    final public boolean unHook(XC_MethodHook.Unhook unhook) {
        return core.unHook(unhook);
    }

    @Override
    final public boolean unHook(Member hookMember, XC_MethodHook xcMethodHook) {
        return core.unHook(hookMember, xcMethodHook);
    }

    @Override
    final public IAction returnResult(Object result) {
        return core.returnResult(result);
    }

    @Override
    final public IAction doNothing() {
        return core.doNothing();
    }

    @Override
    final public ArrayList<Method> filterMethod(Class<?> clazz, CoreTool.IFindMethod iFindMethod) {
        return core.filterMethod(clazz, iFindMethod);
    }

    @Override
    final public ArrayList<Constructor<?>> filterMethod(Class<?> clazz, CoreTool.IFindConstructor iFindConstructor) {
        return core.filterMethod(clazz, iFindConstructor);
    }

    @Override
    final public <T, C> C newInstance(Class<?> clz, T objects) {
        return core.newInstance(clz, objects);
    }

    @Override
    final public <C> C newInstance(Class<?> clz) {
        return core.newInstance(clz);
    }

    @Override
    final public <T, C> C callStaticMethod(Class<?> clz, String name, T objs) {
        return core.callStaticMethod(clz, name, objs);
    }

    @Override
    final public <C> C callStaticMethod(Class<?> clz, String name) {
        return core.callStaticMethod(clz, name);
    }

    @Override
    final public <T> T getStaticField(Class<?> clz, String name) {
        return core.getStaticField(clz, name);
    }

    @Override
    final public <T> T getStaticField(Field field) {
        return core.getStaticField(field);
    }

    @Override
    final public boolean setStaticField(Class<?> clz, String name, Object value) {
        return core.setStaticField(clz, name, value);
    }

    @Override
    final public boolean setStaticField(Field field, Object value) {
        return core.setStaticField(field, value);
    }

    @Override
    final public boolean setAdditionalStaticField(Class<?> clz, String key, Object value) {
        return core.setAdditionalStaticField(clz, key, value);
    }

    @Override
    final public <T> T getAdditionalStaticField(Class<?> clz, String key) {
        return core.getAdditionalStaticField(clz, key);
    }

    @Override
    final public boolean removeAdditionalStaticField(Class<?> clz, String key) {
        return core.removeAdditionalStaticField(clz, key);
    }
}
