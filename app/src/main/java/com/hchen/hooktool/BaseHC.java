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
import com.hchen.hooktool.itool.IFilter;
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
 * 对需要使用工具的类继承本类，可快速使用工具
 * <p>
 * This class inherits from the class that requires the use of the tool, so that you can quickly use the tool
 * 
 * @author 焕晨HChen
 */
public abstract class BaseHC implements IMember, IDynamic, IStatic, IChain {
    public String TAG = getClass().getSimpleName();

    // onZygote 阶段以下均为 null 或 false
    public XC_LoadPackage.LoadPackageParam lpparam;
    public ClassLoader classLoader;
    public ApplicationInfo appInfo;
    public String packageName;
    public boolean isFirstApplication;
    public String processName;
    // END

    // 在外处使用可以传递本参数。
    public BaseHC baseHC;

    private boolean isZygote = false;
    private HCHook hcHook;
    private PrefsTool prefs;
    private IDynamic iDynamic;
    private IMember iMember;
    private IStatic iStatic;
    private IChain iChain;

    /**
     * 正常阶段。
     * <p>
     * Normal stage.
     */
    public abstract void init();

    /**
     * zygote 阶段。
     * <p>
     * 如果 startupParam 为 null，请检查是否为工具初始化。
     * <p>
     * Zygote stages.
     * <p>
     * If startupParam is null, check if it is initialized for the tool.
     */
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
    }

    final public void onCreate() {
        isZygote = false;
        initTool();
        try {
            init();
        } catch (Throwable e) {
            logE(TAG, e);
        }
    }

    final public void onZygote() {
        isZygote = true;
        initTool();
        try {
            initZygote(ToolData.startupParam);
        } catch (Throwable e) {
            logE(TAG, e);
        }
    }

    private void initTool() {
        if (hcHook == null) {
            hcHook = new HCHook();
            hcHook.setThisTag(TAG);
            iDynamic = hcHook.core();
            iStatic = hcHook.core();
            iMember = hcHook.core();
            iChain = hcHook.chain();
            prefs = hcHook.prefs();
        }
        hcHook.setStateChange(isZygote);
        if (!isZygote) {
            lpparam = hcHook.lpparam();
            classLoader = lpparam.classLoader;
            appInfo = lpparam.appInfo;
            packageName = lpparam.packageName;
            isFirstApplication = lpparam.isFirstApplication;
            processName = lpparam.processName;
        }
        baseHC = this;
    }

    // ---------- prefs -----------
    final public IPrefs prefs() {
        return prefs.prefs();
    }

    final public IPrefs prefs(String prefsName) {
        return prefs.prefs(prefsName);
    }

    final public IPrefs prefs(Context context) {
        return PrefsTool.prefs(context);
    }

    final public IPrefs prefs(Context context, String prefsName) {
        return PrefsTool.prefs(context, prefsName);
    }

    final public void asyncPrefs(PrefsTool.IAsyncPrefs asyncPrefs) {
        prefs.asyncPrefs(asyncPrefs);
    }

    // ---------- 链式调用 ----------
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

    // --------- 是否存在类 ------------
    @Override
    final public boolean existsClass(String clazz) {
        return iMember.existsClass(clazz);
    }

    @Override
    final public boolean existsClass(String clazz, ClassLoader classLoader) {
        return iMember.existsClass(clazz, classLoader);
    }

    // --------- 查找类 --------------
    @Override
    final public Class<?> findClass(String name) {
        return iMember.findClass(name);
    }

    @Override
    final public Class<?> findClass(String name, ClassLoader classLoader) {
        return iMember.findClass(name, classLoader);
    }

    // --------- 是否存在指定方法 ------------
    @Override
    final public boolean existsMethod(String clazz, String name, Object... objs) {
        return iMember.existsMethod(clazz, name, objs);
    }

    @Override
    final public boolean existsMethod(String clazz, ClassLoader classLoader, String name, Object... objs) {
        return iMember.existsMethod(clazz, classLoader, name, objs);
    }

    // ---------- 是否存在指定方法名 -----------
    @Override
    final public boolean existsAnyMethod(String clazz, String name) {
        return iMember.existsAnyMethod(clazz, name);
    }

    @Override
    final public boolean existsAnyMethod(String clazz, ClassLoader classLoader, String name) {
        return iMember.existsAnyMethod(clazz, classLoader, name);
    }

    // ----------- 查找方法 -------------
    @Override
    final public Method findMethod(String clazz, String name, Object... objects) {
        return iMember.findMethod(clazz, name, objects);
    }

    @Override
    final public Method findMethod(String clazz, ClassLoader classLoader, String name, Object... objects) {
        return iMember.findMethod(clazz, classLoader, name, objects);
    }

    @Override
    final public Method findMethod(Class<?> clazz, ClassLoader classLoader, String name, Object... objects) {
        return iMember.findMethod(clazz, classLoader, name, objects);
    }

    @Override
    final public Method findMethod(Class<?> clazz, String name, Class<?>... objects) {
        return iMember.findMethod(clazz, name, objects);
    }

    // ------------ 查找匹配的方法名 ----------
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

    // --------- 查找构造函数 ------------
    @Override
    final public Constructor<?> findConstructor(String clazz, Object... objects) {
        return iMember.findConstructor(clazz, objects);
    }

    @Override
    final public Constructor<?> findConstructor(String clazz, ClassLoader classLoader, Object... objects) {
        return iMember.findConstructor(clazz, classLoader, objects);
    }

    @Override
    final public Constructor<?> findConstructor(Class<?> clazz, ClassLoader classLoader, Object... objects) {
        return iMember.findConstructor(clazz, classLoader, objects);
    }

    @Override
    final public Constructor<?> findConstructor(Class<?> clazz, Class<?>... objects) {
        return iMember.findConstructor(clazz, objects);
    }

    // ----------- 查找匹配的构造函数 ------------
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

    // ------------ 是否存在指定字段 ----------
    @Override
    final public boolean existsField(String clazz, String name) {
        return iMember.existsField(clazz, name);
    }

    @Override
    final public boolean existsField(String clazz, ClassLoader classLoader, String name) {
        return iMember.existsField(clazz, classLoader, name);
    }

    // ---------- 查找字段 ---------------
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

    // ---------- hook 一般方法 ----------
    @Override
    final public CoreTool.UnHook hook(String clazz, String method, Object... params) {
        return iMember.hook(clazz, method, params);
    }

    @Override
    final public CoreTool.UnHook hook(String clazz, ClassLoader classLoader, String method, Object... params) {
        return iMember.hook(clazz, classLoader, method, params);
    }

    @Override
    final public CoreTool.UnHook hook(Class<?> clazz, ClassLoader classLoader, String method, Object... params) {
        return iMember.hook(clazz, classLoader, method, params);
    }

    @Override
    final public CoreTool.UnHook hook(Class<?> clazz, String method, Object... params) {
        return iMember.hook(clazz, method, params);
    }

    @Override
    final public CoreTool.UnHookList hookAll(String clazz, String method, IAction iAction) {
        return iMember.hookAll(clazz, method, iAction);
    }

    @Override
    final public CoreTool.UnHookList hookAll(String clazz, ClassLoader classLoader, String method, IAction iAction) {
        return iMember.hookAll(clazz, classLoader, method, iAction);
    }

    @Override
    final public CoreTool.UnHookList hookAll(Class<?> clazz, String method, IAction iAction) {
        return iMember.hookAll(clazz, method, iAction);
    }

    // ---------- hook 构造函数 ------------
    @Override
    final public CoreTool.UnHook hook(String clazz, Object... params) {
        return iMember.hook(clazz, params);
    }

    @Override
    final public CoreTool.UnHook hook(String clazz, ClassLoader classLoader, Object... params) {
        return iMember.hook(clazz, classLoader, params);
    }

    @Override
    final public CoreTool.UnHook hook(Class<?> clazz, ClassLoader classLoader, Object... params) {
        return iMember.hook(clazz, classLoader, params);
    }

    @Override
    final public CoreTool.UnHook hook(Class<?> clazz, Object... params) {
        return iMember.hook(clazz, params);
    }

    @Override
    final public CoreTool.UnHookList hookAll(String clazz, IAction iAction) {
        return iMember.hookAll(clazz, iAction);
    }

    @Override
    final public CoreTool.UnHookList hookAll(String clazz, ClassLoader classLoader, IAction iAction) {
        return iMember.hookAll(clazz, classLoader, iAction);
    }

    @Override
    final public CoreTool.UnHookList hookAll(Class<?> clazz, IAction iAction) {
        return iMember.hookAll(clazz, iAction);
    }

    // ----------- 核心方法 ------------
    @Override
    final public CoreTool.UnHook hook(Member member, IAction iAction) {
        return iMember.hook(member, iAction);
    }

    @Override
    final public CoreTool.UnHookList hookAll(ArrayList<?> members, IAction iAction) {
        return iMember.hookAll(members, iAction);
    }

    // ----------- 快捷方法 -------------
    @Override
    final public IAction returnResult(Object result) {
        return iMember.returnResult(result);
    }

    @Override
    final public IAction doNothing() {
        return iMember.doNothing();
    }

    // --------- 解除 hook ---------
    @Override
    final public boolean unHook(Member hookMember, XC_MethodHook xcMethodHook) {
        return iMember.unHook(hookMember, xcMethodHook);
    }

    // ----------- 过滤方法 -------------
    @Override
    final public ArrayList<Method> filterMethod(String clazz, IFilter iFilter) {
        return iMember.filterMethod(clazz, iFilter);
    }

    @Override
    final public ArrayList<Method> filterMethod(String clazz, ClassLoader classLoader, IFilter iFilter) {
        return iMember.filterMethod(clazz, classLoader, iFilter);
    }

    @Override
    final public ArrayList<Method> filterMethod(Class<?> clazz, IFilter iFilter) {
        return iMember.filterMethod(clazz, iFilter);
    }

    @Override
    final public ArrayList<Constructor<?>> filterConstructor(String clazz, IFilter iFilter) {
        return iMember.filterConstructor(clazz, iFilter);
    }

    @Override
    final public ArrayList<Constructor<?>> filterConstructor(String clazz, ClassLoader classLoader, IFilter iFilter) {
        return iMember.filterConstructor(clazz, classLoader, iFilter);
    }

    @Override
    final public ArrayList<Constructor<?>> filterConstructor(Class<?> clazz, IFilter iFilter) {
        return iMember.filterConstructor(clazz, iFilter);
    }

    // ---------- 打印堆栈 --------------
    @Override
    final public String getStackTrace(boolean autoLog) {
        return iMember.getStackTrace(autoLog);
    }

    @Override
    final public String getStackTrace() {
        return iMember.getStackTrace();
    }

    // --------- 耗时检查 -----------
    @Override
    final public long timeConsumption(Runnable runnable) {
        return iMember.timeConsumption(runnable);
    }

    // ---------- 非静态 ---------------
    @Override
    final public <T> T callMethod(Object instance, String name, Object... objs) {
        return iDynamic.callMethod(instance, name, objs);
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

    // --------- 静态 --------------
    @Override
    final public <T> T newInstance(String clz, Object... objects) {
        return iStatic.newInstance(clz, objects);
    }

    @Override
    final public <T> T newInstance(String clz, ClassLoader classLoader, Object... objects) {
        return iStatic.newInstance(clz, classLoader, objects);
    }

    @Override
    final public <T> T newInstance(Class<?> clz, Object... objects) {
        return iStatic.newInstance(clz, objects);
    }

    @Override
    final public <T> T callStaticMethod(String clz, String name, Object... objs) {
        return iStatic.callStaticMethod(clz, name, objs);
    }

    @Override
    final public <T> T callStaticMethod(String clz, ClassLoader classLoader, String name, Object... objs) {
        return iStatic.callStaticMethod(clz, classLoader, name, objs);
    }

    @Override
    final public <T> T callStaticMethod(Class<?> clz, String name, Object... objs) {
        return iStatic.callStaticMethod(clz, name, objs);
    }

    @Override
    final public <T> T callStaticMethod(Method method, Object... objs) {
        return iStatic.callStaticMethod(method, objs);
    }

    @Override
    final public <T> T getStaticField(String clz, String name) {
        return iStatic.getStaticField(clz, name);
    }

    @Override
    final public <T> T getStaticField(String clz, ClassLoader classLoader, String name) {
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
    final public boolean setStaticField(String clz, String name, Object value) {
        return iStatic.setStaticField(clz, name, value);
    }

    @Override
    final public boolean setStaticField(String clz, ClassLoader classLoader, String name, Object value) {
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
    final public boolean setAdditionalStaticField(String clz, String key, Object value) {
        return iStatic.setAdditionalStaticField(clz, key, value);
    }

    @Override
    final public boolean setAdditionalStaticField(String clz, ClassLoader classLoader, String key, Object value) {
        return iStatic.setAdditionalStaticField(clz, classLoader, key, value);
    }

    @Override
    final public boolean setAdditionalStaticField(Class<?> clz, String key, Object value) {
        return iStatic.setAdditionalStaticField(clz, key, value);
    }

    @Override
    final public <T> T getAdditionalStaticField(String clz, String key) {
        return iStatic.getAdditionalStaticField(clz, key);
    }

    @Override
    final public <T> T getAdditionalStaticField(String clz, ClassLoader classLoader, String key) {
        return iStatic.getAdditionalStaticField(clz, classLoader, key);
    }

    @Override
    final public <T> T getAdditionalStaticField(Class<?> clz, String key) {
        return iStatic.getAdditionalStaticField(clz, key);
    }

    @Override
    final public boolean removeAdditionalStaticField(String clz, String key) {
        return iStatic.removeAdditionalStaticField(clz, key);
    }

    @Override
    final public boolean removeAdditionalStaticField(String clz, ClassLoader classLoader, String key) {
        return iStatic.removeAdditionalStaticField(clz, classLoader, key);
    }

    @Override
    final public boolean removeAdditionalStaticField(Class<?> clz, String key) {
        return iStatic.removeAdditionalStaticField(clz, key);
    }
}
