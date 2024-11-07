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

import static com.hchen.hooktool.helper.ConvertHelper.arrayToClass;
import static com.hchen.hooktool.helper.TryHelper.run;
import static com.hchen.hooktool.helper.TryHelper.runDump;
import static com.hchen.hooktool.hook.HookFactory.createHook;
import static com.hchen.hooktool.log.LogExpand.getTag;
import static com.hchen.hooktool.log.XposedLog.logD;
import static com.hchen.hooktool.log.XposedLog.logI;
import static com.hchen.hooktool.log.XposedLog.logW;
import static com.hchen.hooktool.tool.CoreBase.baseCallStaticMethod;
import static com.hchen.hooktool.tool.CoreBase.baseFilterConstructor;
import static com.hchen.hooktool.tool.CoreBase.baseFilterMethod;
import static com.hchen.hooktool.tool.CoreBase.baseFindAllConstructor;
import static com.hchen.hooktool.tool.CoreBase.baseFindAllMethod;
import static com.hchen.hooktool.tool.CoreBase.baseFindConstructor;
import static com.hchen.hooktool.tool.CoreBase.baseFindField;
import static com.hchen.hooktool.tool.CoreBase.baseFindMethod;
import static com.hchen.hooktool.tool.CoreBase.baseGetAdditionalStaticField;
import static com.hchen.hooktool.tool.CoreBase.baseGetStaticField;
import static com.hchen.hooktool.tool.CoreBase.baseHook;
import static com.hchen.hooktool.tool.CoreBase.baseNewInstance;
import static com.hchen.hooktool.tool.CoreBase.baseRemoveAdditionalStaticField;
import static com.hchen.hooktool.tool.CoreBase.baseSetAdditionalStaticField;
import static com.hchen.hooktool.tool.CoreBase.baseSetStaticField;

import com.hchen.hooktool.data.ToolData;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.log.LogExpand;
import com.hchen.hooktool.tool.itool.IMemberFilter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 核心工具
 *
 * @author 焕晨HChen
 * @noinspection unused
 */
public class CoreTool {
    public static ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

    //------------ 检查指定类是否存在 --------------
    public static boolean existsClass(String clazz) {
        return existsClass(clazz, ToolData.classLoader);
    }

    public static boolean existsClass(String clazz, ClassLoader classLoader) {
        return findClass(clazz, classLoader).isSuccess();
    }

    // --------- 查找类 -----------
    public static MemberData<Class<?>> findClass(String name) {
        return findClass(name, ToolData.classLoader);
    }

    public static MemberData<Class<?>> findClass(String name, ClassLoader classLoader) {
        return (MemberData<Class<?>>) (MemberData<?>) runDump(() -> XposedHelpers.findClass(name, classLoader))
                .setErrMsg("Failed to find class!");
    }

    //------------ 检查指定方法是否存在 --------------
    public static boolean existsMethod(String clazz, String name, Object... objs) {
        return existsMethod(findClass(clazz).getIfExists(), name, arrayToClass(objs));
    }

    public static boolean existsMethod(String clazz, ClassLoader classLoader, String name, Object... objs) {
        return existsMethod(findClass(clazz, classLoader).getIfExists(), name, arrayToClass(classLoader, objs));
    }

    public static boolean existsMethod(Class<?> clazz, ClassLoader classLoader, String name, Object... objs) {
        return existsMethod(clazz, name, arrayToClass(classLoader, objs));
    }

    public static boolean existsMethod(Class<?> clazz, String name, Class<?>... classes) {
        if (clazz == null || classes == null) return false;
        return findMethod(clazz, name, classes).isSuccess();
    }

    public static boolean existsAnyMethod(String clazz, String name) {
        return existsAnyMethod(findClass(clazz).getIfExists(), name);
    }

    public static boolean existsAnyMethod(String clazz, ClassLoader classLoader, String name) {
        return existsAnyMethod(findClass(clazz, classLoader).getIfExists(), name);
    }

    public static boolean existsAnyMethod(Class<?> clazz, String name) {
        if (clazz == null) return false;
        return Arrays.stream(clazz.getDeclaredMethods()).anyMatch(method -> name.equals(method.getName()));
    }

    // ------------ 查找方法 --------------
    public static MemberData<Method> findMethod(String clazz, String name, Object... objects) {
        return baseFindMethod(findClass(clazz), name, arrayToClass(objects));
    }

    public static MemberData<Method> findMethod(String clazz, ClassLoader classLoader, String name, Object... objects) {
        return baseFindMethod(findClass(clazz, classLoader), name, arrayToClass(classLoader, objects));
    }

    public static MemberData<Method> findMethod(Class<?> clazz, ClassLoader classLoader, String name, Object... objects) {
        return baseFindMethod(new MemberData<>(clazz, null), name, arrayToClass(classLoader, objects));
    }

    public static MemberData<Method> findMethod(Class<?> clazz, String name, Class<?>... classes) {
        return baseFindMethod(new MemberData<>(clazz, null), name, classes);
    }

    public static ArrayList<Method> findAllMethod(String clazz, String name) {
        return baseFindAllMethod(findClass(clazz), name);
    }

    public static ArrayList<Method> findAllMethod(String clazz, ClassLoader classLoader, String name) {
        return baseFindAllMethod(findClass(clazz, classLoader), name);
    }

    public static ArrayList<Method> findAllMethod(Class<?> clazz, String name) {
        return baseFindAllMethod(new MemberData<>(clazz, null), name);
    }

    //------------ 检查指定构造函数是否存在 --------------
    public static boolean existsConstructor(String clazz, Object... objs) {
        return existsConstructor(findClass(clazz).getIfExists(), arrayToClass(objs));
    }

    public static boolean existsConstructor(String clazz, ClassLoader classLoader, Object... objs) {
        return existsConstructor(findClass(clazz, classLoader).getIfExists(), arrayToClass(classLoader, objs));
    }

    public static boolean existsConstructor(Class<?> clazz, ClassLoader classLoader, Object... objs) {
        return existsConstructor(clazz, arrayToClass(classLoader, objs));
    }

    public static boolean existsConstructor(Class<?> clazz, Class<?>... classes) {
        if (clazz == null || classes == null) return false;
        return findConstructor(clazz, classes).isSuccess();
    }

    // --------- 查找构造函数 -----------
    public static MemberData<Constructor<?>> findConstructor(String clazz, Object... objects) {
        return baseFindConstructor(findClass(clazz), arrayToClass(objects));
    }

    public static MemberData<Constructor<?>> findConstructor(String clazz, ClassLoader classLoader, Object... objects) {
        return baseFindConstructor(findClass(clazz, classLoader), arrayToClass(classLoader, objects));
    }

    public static MemberData<Constructor<?>> findConstructor(Class<?> clazz, ClassLoader classLoader, Object... objects) {
        return baseFindConstructor(new MemberData<>(clazz, null), arrayToClass(classLoader, objects));
    }

    public static MemberData<Constructor<?>> findConstructor(Class<?> clazz, Class<?>... classes) {
        return baseFindConstructor(new MemberData<>(clazz, null), classes);
    }

    public static ArrayList<Constructor<?>> findAllConstructor(String clazz) {
        return baseFindAllConstructor(findClass(clazz));
    }

    public static ArrayList<Constructor<?>> findAllConstructor(String clazz, ClassLoader classLoader) {
        return baseFindAllConstructor(findClass(clazz, classLoader));
    }

    public static ArrayList<Constructor<?>> findAllConstructor(Class<?> clazz) {
        return baseFindAllConstructor(new MemberData<>(clazz, null));
    }

    //------------ 检查指定字段是否存在 --------------
    public static boolean existsField(String clazz, String name) {
        return existsField(findClass(clazz).getIfExists(), name);
    }

    public static boolean existsField(String clazz, ClassLoader classLoader, String name) {
        return existsField(findClass(clazz, classLoader).getIfExists(), name);
    }

    public static boolean existsField(Class<?> clazz, String name) {
        if (clazz == null) return false;
        return findField(clazz, name).isSuccess();
    }

    // --------- 查找字段 -----------
    public static MemberData<Field> findField(String clazz, String name) {
        return baseFindField(findClass(clazz), name);
    }

    public static MemberData<Field> findField(String clazz, ClassLoader classLoader, String name) {
        return baseFindField(findClass(clazz, classLoader), name);
    }

    public static MemberData<Field> findField(Class<?> clazz, String name) {
        return baseFindField(new MemberData<>(clazz, null), name);
    }

    // --------- 执行 hook -----------
    // --------- 普通方法 -------------
    public static UnHook hookMethod(String clazz, String method, Object... params) {
        return baseHook(findClass(clazz), ToolData.classLoader, method, params);
    }

    public static UnHook hookMethod(String clazz, ClassLoader classLoader, String method, Object... params) {
        return baseHook(findClass(clazz, classLoader), classLoader, method, params);
    }

    public static UnHook hookMethod(Class<?> clazz, ClassLoader classLoader, String method, Object... params) {
        return baseHook(new MemberData<>(clazz, null), classLoader, method, params);
    }

    public static UnHook hookMethod(Class<?> clazz, String method, Object... params) {
        return baseHook(new MemberData<>(clazz, null), ToolData.classLoader, method, params);
    }

    public static UnHookList hookAllMethod(String clazz, String method, IHook iHook) {
        return hookAll((ArrayList<Member>) (ArrayList<?>) findAllMethod(clazz, method), iHook);
    }

    public static UnHookList hookAllMethod(String clazz, ClassLoader classLoader, String method, IHook iHook) {
        return hookAll((ArrayList<Member>) (ArrayList<?>) findAllMethod(clazz, classLoader, method), iHook);
    }

    public static UnHookList hookAllMethod(Class<?> clazz, String method, IHook iHook) {
        return hookAll((ArrayList<Member>) (ArrayList<?>) findAllMethod(clazz, method), iHook);
    }

    // --------- 构造函数 ------------
    public static UnHook hookConstructor(String clazz, Object... params) {
        return baseHook(findClass(clazz), ToolData.classLoader, null, params);
    }

    public static UnHook hookConstructor(String clazz, ClassLoader classLoader, Object... params) {
        return baseHook(findClass(clazz, classLoader), classLoader, null, params);
    }

    public static UnHook hookConstructor(Class<?> clazz, Object... params) {
        return baseHook(new MemberData<>(clazz, null), ToolData.classLoader, null, params);
    }

    public static UnHookList hookAllConstructor(String clazz, IHook iHook) {
        return hookAll((ArrayList<Member>) (ArrayList<?>) findAllConstructor(clazz), iHook);
    }

    public static UnHookList hookAllConstructor(String clazz, ClassLoader classLoader, IHook iHook) {
        return hookAll((ArrayList<Member>) (ArrayList<?>) findAllConstructor(clazz, classLoader), iHook);
    }

    public static UnHookList hookAllConstructor(Class<?> clazz, IHook iHook) {
        return hookAll((ArrayList<Member>) (ArrayList<?>) findAllConstructor(clazz), iHook);
    }

    // ----------- 核心实现 ---------------
    public static UnHook hook(Member member, IHook iHook) {
        String tag = getTag();
        return run(() -> {
            UnHook unhook = new UnHook(XposedBridge.hookMethod(member, createHook(tag, iHook)));
            logD(tag, "Success to hook: " + member);
            return unhook;
        }).orErrMag(new UnHook(null), "Failed to hook: " + member);
    }

    public static UnHookList hookAll(ArrayList<Member> members, IHook iHook) {
        String tag = getTag();
        if (members.isEmpty()) {
            logW(tag, "Member list is empty, will hook nothing!");
            return new UnHookList();
        }
        return members.stream().map(member -> run(() -> {
            UnHook unHook = new UnHook(XposedBridge.hookMethod(member, createHook(tag, iHook)));
            logD(tag, "Success to hook: " + member);
            return unHook;
        }).orErrMag(new UnHook(null), "Failed to hook: " + member)).collect(Collectors.toCollection(UnHookList::new));
    }

    // --------- 快捷方法 -----------
    public static IHook returnResult(final Object result) {
        return new IHook() {
            @Override
            public void before() {
                setResult(result);
            }
        };
    }

    public static IHook doNothing() {
        return new IHook() {
            @Override
            public void before() {
                setResult(null);
            }
        };
    }

    // --------- 解除 hook ---------
    public static void unHook(Member hookMember, XC_MethodHook xcMethodHook) {
        XposedBridge.unhookMethod(hookMember, xcMethodHook);
    }

    public static class UnHook {
        private XC_MethodHook.Unhook unhook;

        protected UnHook(XC_MethodHook.Unhook unhook) {
            this.unhook = unhook;
        }

        public void unHook() {
            if (unhook != null) unhook.unhook();
            unhook = null;
        }
    }

    public static class UnHookList extends ArrayList<UnHook> {
        public void unHookAll() {
            forEach(UnHook::unHook);
            clear();
        }
    }

    // --------- 过滤方法 -----------
    public static ArrayList<Method> filterMethod(String clazz, IMemberFilter<Method> iMemberFilter) {
        return baseFilterMethod(findClass(clazz), iMemberFilter);
    }

    public static ArrayList<Method> filterMethod(String clazz, ClassLoader classLoader, IMemberFilter<Method> iMemberFilter) {
        return baseFilterMethod(findClass(clazz, classLoader), iMemberFilter);
    }

    public static ArrayList<Method> filterMethod(Class<?> clazz, IMemberFilter<Method> iMemberFilter) {
        return baseFilterMethod(new MemberData<>(clazz, null), iMemberFilter);
    }

    public static ArrayList<Constructor<?>> filterConstructor(String clazz, IMemberFilter<Constructor<?>> iMemberFilter) {
        return baseFilterConstructor(findClass(clazz), iMemberFilter);
    }

    public static ArrayList<Constructor<?>> filterConstructor(String clazz, ClassLoader classLoader, IMemberFilter<Constructor<?>> iMemberFilter) {
        return baseFilterConstructor(findClass(clazz, classLoader), iMemberFilter);
    }

    public static ArrayList<Constructor<?>> filterConstructor(Class<?> clazz, IMemberFilter<Constructor<?>> iMemberFilter) {
        return baseFilterConstructor(new MemberData<>(clazz, null), iMemberFilter);
    }

    // --------- 打印堆栈 ----------
    public static String getStackTrace(boolean autoLog) {
        String task = getStackTrace();
        if (autoLog) logI(getTag(), task);
        return task;
    }

    public static String getStackTrace() {
        return LogExpand.getStackTrace();
    }

    // --------- 耗时检查 -----------
    public static long timeConsumption(Runnable runnable) {
        return run(() -> {
            Instant start = Instant.now();
            runnable.run();
            Instant end = Instant.now();
            return Duration.between(start, end).toMillis();
        }).orErrMag(-1L, "Failed to check code time consumption!");
    }

    // ---------- 非静态 -----------
    public static <T> T callMethod(Object instance, String name, Object... objs) {
        return run(() -> (T) XposedHelpers.callMethod(instance, name, objs))
                .orErrMag(null, "Failed to call method!");
    }

    public static <T> T getField(Object instance, String name) {
        return run(() -> (T) XposedHelpers.getObjectField(instance, name))
                .orErrMag(null, "Failed to get field!");
    }

    public static <T> T getField(Object instance, Field field) {
        return run(() -> {
            field.setAccessible(true);
            return (T) field.get(instance);
        }).orErrMag(null, "Failed to get field!");
    }

    public static boolean setField(Object instance, String name, Object value) {
        return run(() -> {
            XposedHelpers.setObjectField(instance, name, value);
            return true;
        }).orErrMag(false, "Failed to set field!");
    }

    public static boolean setField(Object instance, Field field, Object value) {
        return run(() -> {
            field.setAccessible(true);
            field.set(instance, value);
            return true;
        }).orErrMag(false, "Failed to set field!");
    }

    public static <T> T setAdditionalInstanceField(Object instance, String key, Object value) {
        return run(() -> (T) XposedHelpers.setAdditionalInstanceField(instance, key, value))
                .orErrMag(null, "Failed to set additional instance!");
    }

    public static <T> T getAdditionalInstanceField(Object instance, String key) {
        return run(() -> (T) XposedHelpers.getAdditionalInstanceField(instance, key))
                .orErrMag(null, "Failed to get additional instance!");
    }

    public static <T> T removeAdditionalInstanceField(Object instance, String key) {
        return run(() -> (T) XposedHelpers.removeAdditionalInstanceField(instance, key))
                .orErrMag(null, "Failed to remove additional instance!");
    }

    // ---------- 静态 ------------
    public static <T> T newInstance(Class<?> clz, Object... objs) {
        return baseNewInstance(new MemberData<>(clz, null), objs);
    }

    public static <T> T newInstance(String clz, Object... objs) {
        return baseNewInstance(findClass(clz), objs);
    }

    public static <T> T newInstance(String clz, ClassLoader classLoader, Object... objs) {
        return baseNewInstance(findClass(clz, classLoader), objs);
    }

    public static <T> T callStaticMethod(Class<?> clz, String name, Object... objs) {
        return baseCallStaticMethod(new MemberData<>(clz, null), name, objs);
    }

    public static <T> T callStaticMethod(String clz, String name, Object... objs) {
        return baseCallStaticMethod(findClass(clz), name, objs);
    }

    public static <T> T callStaticMethod(String clz, ClassLoader classLoader, String name, Object... objs) {
        return baseCallStaticMethod(findClass(clz, classLoader), name, objs);
    }

    public static <T> T callStaticMethod(Method method, Object... objs) {
        return run(() -> {
            method.setAccessible(true);
            return (T) method.invoke(null, objs);
        }).orErrMag(null, "Failed to call static method!");
    }

    public static <T> T getStaticField(Class<?> clz, String name) {
        return baseGetStaticField(new MemberData<>(clz, null), name);
    }

    public static <T> T getStaticField(String clz, String name) {
        return baseGetStaticField(findClass(clz), name);
    }

    public static <T> T getStaticField(String clz, ClassLoader classLoader, String name) {
        return baseGetStaticField(findClass(clz, classLoader), name);
    }

    public static <T> T getStaticField(Field field) {
        return run(() -> {
            field.setAccessible(true);
            return (T) field.get(null);
        }).orErrMag(null, "Failed to get static field!");
    }

    public static boolean setStaticField(Class<?> clz, String name, Object value) {
        return baseSetStaticField(new MemberData<>(clz, null), name, value);
    }

    public static boolean setStaticField(String clz, String name, Object value) {
        return baseSetStaticField(findClass(clz), name, value);
    }

    public static boolean setStaticField(String clz, ClassLoader classLoader, String name, Object value) {
        return baseSetStaticField(findClass(clz, classLoader), name, value);
    }

    public static boolean setStaticField(Field field, Object value) {
        return run(() -> {
            field.setAccessible(true);
            field.set(null, value);
            return true;
        }).orErrMag(false, "Failed to set static field!");
    }

    public static <T> T setAdditionalStaticField(Class<?> clz, String key, Object value) {
        return baseSetAdditionalStaticField(new MemberData<>(clz, null), key, value);
    }

    public static <T> T setAdditionalStaticField(String clz, String key, Object value) {
        return baseSetAdditionalStaticField(findClass(clz), key, value);
    }

    public static <T> T setAdditionalStaticField(String clz, ClassLoader classLoader, String key, Object value) {
        return baseSetAdditionalStaticField(findClass(clz, classLoader), key, value);
    }

    public static <T> T getAdditionalStaticField(Class<?> clz, String key) {
        return baseGetAdditionalStaticField(new MemberData<>(clz, null), key);
    }

    public static <T> T getAdditionalStaticField(String clz, String key) {
        return baseGetAdditionalStaticField(findClass(clz), key);
    }

    public static <T> T getAdditionalStaticField(String clz, ClassLoader classLoader, String key) {
        return baseGetAdditionalStaticField(findClass(key, classLoader), key);
    }

    public static <T> T removeAdditionalStaticField(Class<?> clz, String key) {
        return baseRemoveAdditionalStaticField(new MemberData<>(clz, null), key);
    }

    public static <T> T removeAdditionalStaticField(String clz, String key) {
        return baseRemoveAdditionalStaticField(findClass(clz), key);
    }

    public static <T> T removeAdditionalStaticField(String clz, ClassLoader classLoader, String key) {
        return baseRemoveAdditionalStaticField(findClass(clz, classLoader), key);
    }
}
