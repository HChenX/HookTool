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
import com.hchen.hooktool.itool.IChain;
import com.hchen.hooktool.itool.IDynamic;
import com.hchen.hooktool.itool.IMember;
import com.hchen.hooktool.itool.IPrefs;
import com.hchen.hooktool.itool.IStatic;
import com.hchen.hooktool.tool.ChainTool;
import com.hchen.hooktool.tool.CoreTool;
import com.hchen.hooktool.tool.PrefsTool;
import com.hchen.hooktool.utils.ToolData;

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
public abstract class BaseHC implements IMember, IDynamic, IStatic, IChain {
    public String TAG = getClass().getSimpleName();
    public XC_LoadPackage.LoadPackageParam lpparam;
    public ClassLoader classLoader;
    public ApplicationInfo appInfo;
    public String packageName;
    public boolean isFirstApplication;
    public String processName;
    private PrefsTool prefs;
    private IDynamic iDynamic;
    private IMember iMember;
    private IStatic iStatic;
    private IChain iChain;

    // 工具为了保持日志易读性，无法全部静态化，但您仍然可以直接调用此静态字段使用本工具。
    // 唯一区别是工具日志 tag 始终为 ”StaticHC“（不影响手动设置的 log tag。
    // 当然你也可以在自己类内手动存储静态本类，实现日志正常。
    public static HCHook sHc;
    public static CoreTool sCore;
    public static ChainTool sChain;
    public static PrefsTool sPrefs;

    static {
        sHc = new HCHook().setThisTag("StaticHC");
        sCore = sHc.core();
        sChain = sHc.chain();
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
        HCHook hcHook = new HCHook();
        hcHook.setThisTag(TAG);
        lpparam = hcHook.lpparam();
        iDynamic = hcHook.core();
        iStatic = hcHook.core();
        iMember = hcHook.core();
        iChain = hcHook.chain();
        prefs = hcHook.prefs();
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
            initZygote(ToolData.startupParam);
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

    final public void asynPrefs(PrefsTool.IAsynPrefs asynPrefs) {
        prefs.asynPrefs(asynPrefs);
    }

    final public PrefsTool nativePrefs() {
        return prefs.nativePrefs();
    }

    final public PrefsTool xposedPrefs() {
        return prefs.xposedPrefs();
    }

    @Override
    final public void chain(String clazz, ChainTool chain) {
        iChain.chain(clazz, chain);
    }

    @Override
    final public void chain(String clazz, ClassLoader classLoader, ChainTool chain) {
        iChain.chain(clazz, classLoader, chain);
    }

    @Override
    final public void chain(Class<?> clazz, ChainTool chain) {
        iChain.chain(clazz, chain);
    }

    @Override
    final public ChainTool.ChainHook method(String name, Object... params) {
        return iChain.method(name, params);
    }

    @Override
    final public ChainTool.ChainHook anyMethod(String name) {
        return iChain.anyMethod(name);
    }

    @Override
    final public ChainTool.ChainHook constructor(Object... params) {
        return iChain.constructor(params);
    }

    @Override
    final public ChainTool.ChainHook anyConstructor() {
        return iChain.anyConstructor();
    }

    @Override
    final public <T, C> C callMethod(Object instance, String name, T ts) {
        return iDynamic.callMethod(instance, name, ts);
    }

    @Override
    final public <C> C callMethod(Object instance, String name) {
        return iDynamic.callMethod(instance, name);
    }

    @Override
    final public <T> T getField(Object instance, String name) {
        return iDynamic.getField(instance, name);
    }

    @Override
    final public <T> T getField(Object instance, Field field) {
        return iDynamic.getField(instance, field);
    }

    @Override
    final public boolean setField(Object instance, String name, Object value) {
        return iDynamic.setField(instance, name, value);
    }

    @Override
    final public boolean setField(Object instance, Field field, Object value) {
        return iDynamic.setField(instance, field, value);
    }

    @Override
    final public boolean setAdditionalInstanceField(Object instance, String key, Object value) {
        return iDynamic.setAdditionalInstanceField(instance, key, value);
    }

    @Override
    final public <T> T getAdditionalInstanceField(Object instance, String key) {
        return iDynamic.getAdditionalInstanceField(instance, key);
    }

    @Override
    final public boolean removeAdditionalInstanceField(Object instance, String key) {
        return iDynamic.removeAdditionalInstanceField(instance, key);
    }

    @Override
    final public boolean existsClass(String clazz) {
        return iMember.existsClass(clazz);
    }

    @Override
    final public boolean existsClass(String clazz, ClassLoader classLoader) {
        return iMember.existsClass(clazz, classLoader);
    }

    @Override
    final public Class<?> findClass(String name) {
        return iMember.findClass(name);
    }

    @Override
    final public Class<?> findClass(String name, ClassLoader classLoader) {
        return iMember.findClass(name, classLoader);
    }

    @Override
    final public boolean existsMethod(String clazz, String name, Object... ojbs) {
        return iMember.existsMethod(clazz, name, ojbs);
    }

    @Override
    final public boolean existsMethod(String clazz, ClassLoader classLoader, String name, Object... ojbs) {
        return iMember.existsMethod(clazz, classLoader, name, ojbs);
    }

    @Override
    final public boolean existsAnyMethod(String clazz, String name) {
        return iMember.existsAnyMethod(clazz, name);
    }

    @Override
    final public boolean existsAnyMethod(String clazz, ClassLoader classLoader, String name) {
        return iMember.existsAnyMethod(clazz, classLoader, name);
    }

    @Override
    final public Method findMethod(String clazz, String name, Object... objects) {
        return iMember.findMethod(clazz, name, objects);
    }

    @Override
    final public Method findMethod(String clazz, ClassLoader classLoader, String name, Object... objects) {
        return iMember.findMethod(clazz, classLoader, name, objects);
    }

    @Override
    final public Method findMethod(Class<?> clazz, String name, Object... objects) {
        return iMember.findMethod(clazz, name, objects);
    }

    @Override
    final public ArrayList<Method> findAnyMethod(String clazz, String name) {
        return iMember.findAnyMethod(clazz, name);
    }

    @Override
    final public ArrayList<Method> findAnyMethod(String clazz, ClassLoader classLoader, String name) {
        return iMember.findAnyMethod(clazz, classLoader, name);
    }

    @Override
    final public ArrayList<Method> findAnyMethod(Class<?> clazz, String name) {
        return iMember.findAnyMethod(clazz, name);
    }

    @Override
    final public Constructor<?> findConstructor(String clazz, Object... objects) {
        return iMember.findConstructor(clazz, objects);
    }

    @Override
    final public Constructor<?> findConstructor(String clazz, ClassLoader classLoader, Object... objects) {
        return iMember.findConstructor(clazz, classLoader, objects);
    }

    @Override
    final public Constructor<?> findConstructor(Class<?> clazz, Object... objects) {
        return iMember.findConstructor(clazz, objects);
    }

    @Override
    final public ArrayList<Constructor<?>> findAnyConstructor(String clazz) {
        return iMember.findAnyConstructor(clazz);
    }

    @Override
    final public ArrayList<Constructor<?>> findAnyConstructor(String clazz, ClassLoader classLoader) {
        return iMember.findAnyConstructor(clazz, classLoader);
    }

    @Override
    final public ArrayList<Constructor<?>> findAnyConstructor(Class<?> clazz) {
        return iMember.findAnyConstructor(clazz);
    }

    @Override
    final public boolean existsField(String clazz, String name) {
        return iMember.existsField(clazz, name);
    }

    @Override
    final public boolean existsField(String clazz, ClassLoader classLoader, String name) {
        return iMember.existsField(clazz, classLoader, name);
    }

    @Override
    final public Field findField(String clazz, String name) {
        return iMember.findField(clazz, name);
    }

    @Override
    final public Field findField(String clazz, ClassLoader classLoader, String name) {
        return iMember.findField(clazz, classLoader, name);
    }

    @Override
    final public Field findField(Class<?> clazz, String name) {
        return iMember.findField(clazz, name);
    }

    @Override
    final public XC_MethodHook.Unhook hook(String clazz, String method, Object... params) {
        return iMember.hook(clazz, method, params);
    }

    @Override
    final public XC_MethodHook.Unhook hook(String clazz, ClassLoader classLoader, String method, Object... params) {
        return iMember.hook(clazz, classLoader, method, params);
    }

    @Override
    final public XC_MethodHook.Unhook hook(Class<?> clazz, String method, Object... params) {
        return iMember.hook(clazz, method, params);
    }

    @Override
    final public ArrayList<XC_MethodHook.Unhook> hook(String clazz, IAction iAction) {
        return iMember.hook(clazz, iAction);
    }

    @Override
    final public XC_MethodHook.Unhook hook(String clazz, Object... params) {
        return iMember.hook(clazz, params);
    }

    @Override
    final public XC_MethodHook.Unhook hook(Member member, IAction iAction) {
        return iMember.hook(member, iAction);
    }

    @Override
    final public ArrayList<XC_MethodHook.Unhook> hook(ArrayList<?> members, IAction iAction) {
        return iMember.hook(members, iAction);
    }

    @Override
    final public boolean unHook(XC_MethodHook.Unhook unhook) {
        return iMember.unHook(unhook);
    }

    @Override
    final public boolean unHook(Member hookMember, XC_MethodHook xcMethodHook) {
        return iMember.unHook(hookMember, xcMethodHook);
    }

    @Override
    final public IAction returnResult(Object result) {
        return iMember.returnResult(result);
    }

    @Override
    final public IAction doNothing() {
        return iMember.doNothing();
    }

    @Override
    final public ArrayList<Method> filterMethod(Class<?> clazz, CoreTool.IFindMethod iFindMethod) {
        return iMember.filterMethod(clazz, iFindMethod);
    }

    @Override
    final public ArrayList<Constructor<?>> filterMethod(Class<?> clazz, CoreTool.IFindConstructor iFindConstructor) {
        return iMember.filterMethod(clazz, iFindConstructor);
    }

    @Override
    final public String getStackTrace() {
        return iMember.getStackTrace();
    }

    @Override
    public <T, R> R newInstance(String clz, T objects) {
        return iStatic.newInstance(clz, objects);
    }

    @Override
    public <T, R> R newInstance(String clz, ClassLoader classLoader, T objects) {
        return iStatic.newInstance(clz, classLoader, objects);
    }

    @Override
    final public <T, C> C newInstance(Class<?> clz, T objects) {
        return iStatic.newInstance(clz, objects);
    }

    @Override
    public <R> R newInstance(String clz) {
        return iStatic.newInstance(clz);
    }

    @Override
    public <R> R newInstance(String clz, ClassLoader classLoader) {
        return iStatic.newInstance(clz, classLoader);
    }

    @Override
    final public <C> C newInstance(Class<?> clz) {
        return iStatic.newInstance(clz);
    }

    @Override
    public <T, R> R callStaticMethod(String clz, String name, T objs) {
        return iStatic.callStaticMethod(clz, name, objs);
    }

    @Override
    public <T, R> R callStaticMethod(String clz, ClassLoader classLoader, String name, T objs) {
        return iStatic.callStaticMethod(clz, classLoader, name, objs);
    }

    @Override
    final public <T, C> C callStaticMethod(Class<?> clz, String name, T objs) {
        return iStatic.callStaticMethod(clz, name, objs);
    }

    @Override
    public <R> R callStaticMethod(String clz, String name) {
        return iStatic.callStaticMethod(clz, name);
    }

    @Override
    public <R> R callStaticMethod(String clz, ClassLoader classLoader, String name) {
        return iStatic.callStaticMethod(clz, classLoader, name);
    }

    @Override
    final public <C> C callStaticMethod(Class<?> clz, String name) {
        return iStatic.callStaticMethod(clz, name);
    }

    @Override
    public <T> T getStaticField(String clz, String name) {
        return iStatic.getStaticField(clz, name);
    }

    @Override
    public <T> T getStaticField(String clz, ClassLoader classLoader, String name) {
        return iStatic.getStaticField(clz, classLoader, name);
    }

    @Override
    final public <T> T getStaticField(Class<?> clz, String name) {
        return iStatic.getStaticField(clz, name);
    }

    @Override
    final public <T> T getStaticField(Field field) {
        return iStatic.getStaticField(field);
    }

    @Override
    public boolean setStaticField(String clz, String name, Object value) {
        return iStatic.setStaticField(clz, name, value);
    }

    @Override
    public boolean setStaticField(String clz, ClassLoader classLoader, String name, Object value) {
        return iStatic.setStaticField(clz, classLoader, name, value);
    }

    @Override
    final public boolean setStaticField(Class<?> clz, String name, Object value) {
        return iStatic.setStaticField(clz, name, value);
    }

    @Override
    final public boolean setStaticField(Field field, Object value) {
        return iStatic.setStaticField(field, value);
    }

    @Override
    public boolean setAdditionalStaticField(String clz, String key, Object value) {
        return iStatic.setAdditionalStaticField(clz, key, value);
    }

    @Override
    public boolean setAdditionalStaticField(String clz, ClassLoader classLoader, String key, Object value) {
        return iStatic.setAdditionalStaticField(clz, classLoader, key, value);
    }

    @Override
    final public boolean setAdditionalStaticField(Class<?> clz, String key, Object value) {
        return iStatic.setAdditionalStaticField(clz, key, value);
    }

    @Override
    public <T> T getAdditionalStaticField(String clz, String key) {
        return iStatic.getAdditionalStaticField(clz, key);
    }

    @Override
    public <T> T getAdditionalStaticField(String clz, ClassLoader classLoader, String key) {
        return iStatic.getAdditionalStaticField(clz, classLoader, key);
    }

    @Override
    final public <T> T getAdditionalStaticField(Class<?> clz, String key) {
        return iStatic.getAdditionalStaticField(clz, key);
    }

    @Override
    public boolean removeAdditionalStaticField(String clz, String key) {
        return iStatic.removeAdditionalStaticField(clz, key);
    }

    @Override
    public boolean removeAdditionalStaticField(String clz, ClassLoader classLoader, String key) {
        return iStatic.removeAdditionalStaticField(clz, classLoader, key);
    }

    @Override
    final public boolean removeAdditionalStaticField(Class<?> clz, String key) {
        return iStatic.removeAdditionalStaticField(clz, key);
    }
}
