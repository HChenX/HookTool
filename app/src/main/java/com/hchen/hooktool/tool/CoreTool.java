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
import static com.hchen.hooktool.helper.Try.run;
import static com.hchen.hooktool.helper.Try.runDump;
import static com.hchen.hooktool.hook.HookFactory.createHook;
import static com.hchen.hooktool.log.LogExpand.tag;
import static com.hchen.hooktool.log.XposedLog.logD;
import static com.hchen.hooktool.log.XposedLog.logI;
import static com.hchen.hooktool.log.XposedLog.logW;

import com.hchen.hooktool.data.ToolData;
import com.hchen.hooktool.hook.IAction;
import com.hchen.hooktool.log.LogExpand;
import com.hchen.hooktool.tool.itool.IFilter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
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
        return (MemberData<Class<?>>) (MemberData<?>) runDump(() -> XposedHelpers.findClass(name, classLoader)).setErrMsg("Failed to find class!");
    }

    //------------ 检查指定方法是否存在 --------------
    public static boolean existsMethod(String clazz, String name, Object... objs) {
        return existsMethod(clazz, ToolData.classLoader, name, objs);
    }

    public static boolean existsMethod(String clazz, ClassLoader classLoader, String name, Object... objs) {
        return existsMethod(findClass(clazz, classLoader).getIfExists(), classLoader, name, objs);
    }

    public static boolean existsMethod(Class<?> clazz, ClassLoader classLoader, String name, Object... objs) {
        return existsMethod(clazz, name, arrayToClass(objs, classLoader));
    }

    public static boolean existsMethod(Class<?> clazz, String name, Class<?>... classes) {
        return findMethod(clazz, name, classes).isSuccess();
    }

    public static boolean existsAnyMethod(String clazz, String name) {
        return existsAnyMethod(clazz, ToolData.classLoader, name);
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
        return findMethod(clazz, name, arrayToClass(classLoader, objects));
    }

    public static MemberData<Method> findMethod(Class<?> clazz, String name, Class<?>... classes) {
        return baseFindMethod(new MemberData<>(clazz, null), name, classes);
    }

    private static MemberData<Method> baseFindMethod(MemberData<Class<?>> clazz, String name, Class<?>... classes) {
        return runDump(() -> XposedHelpers.findMethodExact(clazz.getIfExists(), name, classes))
                .setErrMsg("Failed to find method!")
                .spiltThrowableMsg(new Throwable[]{clazz.getThrowable()});
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

    private static ArrayList<Method> baseFindAllMethod(MemberData<Class<?>> clazz, String name) {
        return runDump(() -> Arrays.stream(clazz.getIfExists().getDeclaredMethods())
                .filter(method -> name.equals(method.getName()))
                .collect(Collectors.toCollection(ArrayList::new)))
                .setErrMsg("Failed to find all method!")
                .spiltThrowableMsg(new Throwable[]{clazz.getThrowable()})
                .or(new ArrayList<>());
    }

    //------------ 检查指定构造函数是否存在 --------------
    public static boolean existsConstructor(String clazz, Object... objs) {
        return existsConstructor(clazz, ToolData.classLoader, objs);
    }

    public static boolean existsConstructor(String clazz, ClassLoader classLoader, Object... objs) {
        return existsConstructor(findClass(clazz, classLoader).getIfExists(), arrayToClass(classLoader, objs));
    }

    public static boolean existsConstructor(Class<?> clazz, ClassLoader classLoader, Object... objs) {
        return existsConstructor(clazz, arrayToClass(classLoader, objs));
    }

    public static boolean existsConstructor(Class<?> clazz, Class<?>... classes) {
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
        return findConstructor(clazz, arrayToClass(classLoader, objects));
    }

    public static MemberData<Constructor<?>> findConstructor(Class<?> clazz, Class<?>... classes) {
        return baseFindConstructor(new MemberData<>(clazz, null), classes);
    }

    private static MemberData<Constructor<?>> baseFindConstructor(MemberData<Class<?>> clazz, Class<?>... classes) {
        return (MemberData<Constructor<?>>) (MemberData<?>) runDump(() -> XposedHelpers.findConstructorExact(clazz.getIfExists(), classes))
                .setErrMsg("Failed to find constructor!")
                .spiltThrowableMsg(new Throwable[]{clazz.getThrowable()});
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

    private static ArrayList<Constructor<?>> baseFindAllConstructor(MemberData<Class<?>> clazz) {
        return runDump(() -> new ArrayList<>(Arrays.asList(clazz.getIfExists().getDeclaredConstructors())))
                .setErrMsg("Failed to find constructor!")
                .spiltThrowableMsg(new Throwable[]{clazz.getThrowable()})
                .or(new ArrayList<>());
    }

    //------------ 检查指定字段是否存在 --------------
    public static boolean existsField(String clazz, String name) {
        return existsField(clazz, ToolData.classLoader, name);
    }

    public static boolean existsField(String clazz, ClassLoader classLoader, String name) {
        return existsField(findClass(clazz, classLoader).getIfExists(), name);
    }

    public static boolean existsField(Class<?> clazz, String name) {
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

    private static MemberData<Field> baseFindField(MemberData<Class<?>> clazz, String name) {
        return runDump(() -> XposedHelpers.findField(clazz.getIfExists(), name))
                .setErrMsg("Failed to find field!")
                .spiltThrowableMsg(new Throwable[]{clazz.getThrowable()});
    }

    // --------- 执行 hook -----------
    // --------- 普通方法 -------------
    public static UnHook hookMethod(String clazz, String method, Object... params) {
        return hookMethod(clazz, ToolData.classLoader, method, params);
    }

    public static UnHook hookMethod(String clazz, ClassLoader classLoader, String method, Object... params) {
        return baseHook(findClass(clazz, classLoader), method, classLoader, params);
    }

    public static UnHook hookMethod(Class<?> clazz, ClassLoader classLoader, String method, Object... params) {
        return hookMethod(clazz, method, classLoader, params);
    }

    public static UnHook hookMethod(Class<?> clazz, String method, Object... params) {
        return baseHook(new MemberData<>(clazz, null), method, ToolData.classLoader, params);
    }

    public static UnHookList hookAllMethod(String clazz, String method, IAction iAction) {
        return hookAll(findAllMethod(clazz, method), iAction);
    }

    public static UnHookList hookAllMethod(String clazz, ClassLoader classLoader, String method, IAction iAction) {
        return hookAll(findAllMethod(clazz, classLoader, method), iAction);
    }

    public static UnHookList hookAllMethod(Class<?> clazz, String method, IAction iAction) {
        return hookAll(findAllMethod(clazz, method), iAction);
    }

    // --------- 构造函数 ------------
    public static UnHook hookConstructor(String clazz, Object... params) {
        return hookConstructor(clazz, ToolData.classLoader, params);
    }

    public static UnHook hookConstructor(String clazz, ClassLoader classLoader, Object... params) {
        return baseHook(findClass(clazz, classLoader), null, classLoader, params);
    }

    public static UnHook hookConstructor(Class<?> clazz, Object... params) {
        return baseHook(new MemberData<>(clazz, null), null, ToolData.classLoader, params);
    }

    public static UnHookList hookAllConstructor(String clazz, IAction iAction) {
        return hookAll(findAllConstructor(clazz), iAction);
    }

    public static UnHookList hookAllConstructor(String clazz, ClassLoader classLoader, IAction iAction) {
        return hookAll(findAllConstructor(clazz, classLoader), iAction);
    }

    public static UnHookList hookAllConstructor(Class<?> clazz, IAction iAction) {
        return hookAll(findAllConstructor(clazz), iAction);
    }

    // ----------- 核心实现 ---------------

    private static UnHook baseHook(MemberData<Class<?>> clazz, String method, ClassLoader classLoader, Object... params) {
        String debug = (method != null ? "METHOD" : "CONSTRUCTOR") + "#" + (clazz.getIfExists() == null ? "null" : clazz.getIfExists().getName())
                + "#" + method + "#" + Arrays.toString(params);
        String tag = tag();
        if (params == null || params.length == 0 || !(params[params.length - 1] instanceof IAction iAction)) {
            logW(tag, "Hook params is null or length is 0 or last param not is IAction! \ndebug: " + debug + getStackTrace());
            return new UnHook(null);
        }

        final MemberData<?>[] member = new MemberData[1];
        run(() -> {
            Class<?>[] classes = Arrays.stream(params)
                    .limit(params.length - 1)
                    .map(o -> {
                        if (o instanceof String s) {
                            MemberData<Class<?>> classMemberData = findClass(s);
                            if (classMemberData.getThrowable() != null)
                                throw new RuntimeException(classMemberData.getThrowable());
                            return classMemberData.getIfExists();
                        } else if (o instanceof Class<?> c) return c;
                        else throw new RuntimeException("Unknown type: " + o);
                    }).toArray(Class<?>[]::new);

            if (method != null)
                member[0] = findMethod(clazz.getIfExists(), method, classes);
            else
                member[0] = findConstructor(clazz.getIfExists(), classes);
            return null;
        }).orErrMag(null, "Failed to hook! \ndebug: " + debug);

        if (member[0] == null) return new UnHook(null); // 上方必抛错

        return runDump(() -> {
            UnHook unHook = new UnHook(XposedBridge.hookMethod(((MemberData<Member>) member[0]).getIfExists(), createHook(tag, iAction)));
            logD(tag, "Success to hook: " + member[0].getIfExists());
            return unHook;
        })
                .setErrMsg("Failed to hook: " + member[0].getIfExists())
                .spiltThrowableMsg(new Throwable[]{member[0].getThrowable(), clazz.getThrowable()})
                .or(new UnHook(null));
    }

    public static UnHook hook(Member member, IAction iAction) {
        String tag = tag();
        return run(() -> {
            UnHook unhook = new UnHook(XposedBridge.hookMethod(member, createHook(tag, iAction)));
            logD(tag, "Success to hook: " + member);
            return unhook;
        }).orErrMag(new UnHook(null), "Failed to hook: " + member);
    }

    public static UnHookList hookAll(ArrayList<?> members, IAction iAction) {
        String tag = tag();
        if (members.isEmpty()) {
            logW(tag, "Member list is empty, will hook nothing!");
            return new UnHookList();
        }
        return members.stream().map((Function<Object, UnHook>) member -> run(() -> {
            UnHook unHook = new UnHook(XposedBridge.hookMethod((Member) member, createHook(tag, iAction)));
            logD(tag, "Success to hook: " + member);
            return unHook;
        }).orErrMag(new UnHook(null), "Failed to hook: " + member)).collect(Collectors.toCollection(UnHookList::new));
    }

    // --------- 快捷方法 -----------
    public static IAction returnResult(final Object result) {
        return new IAction() {
            @Override
            public void before() {
                setResult(result);
            }
        };
    }

    public static IAction doNothing() {
        return new IAction() {
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

        private UnHook(XC_MethodHook.Unhook unhook) {
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
    public static ArrayList<Method> filterMethod(String clazz, IFilter<Method> iFilter) {
        return filterMethod(findClass(clazz).getIfExists(), iFilter);
    }

    public static ArrayList<Method> filterMethod(String clazz, ClassLoader classLoader, IFilter<Method> iFilter) {
        return filterMethod(findClass(clazz, classLoader).getIfExists(), iFilter);
    }

    public static ArrayList<Method> filterMethod(Class<?> clazz, IFilter<Method> iFilter) {
        return run(() -> Arrays.stream(clazz.getDeclaredMethods()).filter(iFilter::test)
                .collect(Collectors.toCollection(ArrayList::new)))
                .orErrMag(new ArrayList<>(), "Failed to filter method!");
    }

    public static ArrayList<Constructor<?>> filterConstructor(String clazz, IFilter<Constructor<?>> iFilter) {
        return filterConstructor(findClass(clazz).getIfExists(), iFilter);
    }

    public static ArrayList<Constructor<?>> filterConstructor(String clazz, ClassLoader classLoader, IFilter<Constructor<?>> iFilter) {
        return filterConstructor(findClass(clazz, classLoader).getIfExists(), iFilter);
    }

    public static ArrayList<Constructor<?>> filterConstructor(Class<?> clazz, IFilter<Constructor<?>> iFilter) {
        return run(() -> Arrays.stream(clazz.getDeclaredConstructors()).filter(iFilter::test)
                .collect(Collectors.toCollection(ArrayList::new)))
                .orErrMag(new ArrayList<>(), "Failed to filter constructor!");
    }

    // --------- 打印堆栈 ----------
    public static String getStackTrace(boolean autoLog) {
        String task = getStackTrace();
        if (autoLog) logI(tag(), task);
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
    public static <T> T newInstance(Class<?> clz, Object... objects) {
        return baseNewInstance(new MemberData<>(clz, null), objects);
    }

    public static <T> T newInstance(String clz, Object... objects) {
        return baseNewInstance(findClass(clz), objects);
    }

    public static <T> T newInstance(String clz, ClassLoader classLoader, Object... objects) {
        return baseNewInstance(findClass(clz, classLoader), objects);
    }

    private static <T> T baseNewInstance(MemberData<Class<?>> clz, Object... objects) {
        return runDump(() -> (T) XposedHelpers.newInstance(clz.getIfExists(), objects))
                .setErrMsg("Failed to create new instance!")
                .spiltThrowableMsg(new Throwable[]{clz.getThrowable()})
                .or(null);
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

    private static <T> T baseCallStaticMethod(MemberData<Class<?>> clz, String name, Object... objs) {
        return runDump(() -> (T) XposedHelpers.callStaticMethod(clz.getIfExists(), name, objs))
                .setErrMsg("Failed to call static method!")
                .spiltThrowableMsg(new Throwable[]{clz.getThrowable()})
                .or(null);
    }

    public static <T> T getStaticField(Class<?> clz, String name) {
        return baseGetStaticField(new MemberData<>(clz, null), name);
    }

    public static <T> T baseGetStaticField(MemberData<Class<?>> clz, String name) {
        return runDump(() -> (T) XposedHelpers.getStaticObjectField(clz.getIfExists(), name))
                .setErrMsg("Failed to get static field!")
                .spiltThrowableMsg(new Throwable[]{clz.getThrowable()})
                .or(null);
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

    public static boolean baseSetStaticField(MemberData<Class<?>> clz, String name, Object value) {
        return runDump(() -> {
            XposedHelpers.setStaticObjectField(clz.getIfExists(), name, value);
            return true;
        })
                .setErrMsg("Failed to set static field!")
                .spiltThrowableMsg(new Throwable[]{clz.getThrowable()})
                .or(false);
    }

    public static boolean setStaticField(Field field, Object value) {
        return run(() -> {
            field.setAccessible(true);
            field.set(null, value);
            return true;
        }).orErrMag(false, "Failed to set static field!");
    }

    public static boolean setStaticField(String clz, String name, Object value) {
        return baseSetStaticField(findClass(clz), name, value);
    }

    public static boolean setStaticField(String clz, ClassLoader classLoader, String name, Object value) {
        return baseSetStaticField(findClass(clz, classLoader), name, value);
    }

    public static <T> T setAdditionalStaticField(Class<?> clz, String key, Object value) {
        return baseSetAdditionalStaticField(new MemberData<>(clz, null), key, value);
    }

    public static <T> T baseSetAdditionalStaticField(MemberData<Class<?>> clz, String key, Object value) {
        return runDump(() -> (T) XposedHelpers.setAdditionalStaticField(clz.getIfExists(), key, value))
                .setErrMsg("Failed to set static additional instance!")
                .spiltThrowableMsg(new Throwable[]{clz.getThrowable()})
                .or(null);
    }

    public static <T> T getAdditionalStaticField(Class<?> clz, String key) {
        return baseGetAdditionalStaticField(new MemberData<>(clz, null), key);
    }

    public static <T> T baseGetAdditionalStaticField(MemberData<Class<?>> clz, String key) {
        return runDump(() -> (T) XposedHelpers.getAdditionalStaticField(clz.getIfExists(), key))
                .setErrMsg("Failed to get static additional instance!")
                .spiltThrowableMsg(new Throwable[]{clz.getThrowable()})
                .or(null);
    }

    public static <T> T removeAdditionalStaticField(Class<?> clz, String key) {
        return baseRemoveAdditionalStaticField(new MemberData<>(clz, null), key);
    }

    public static <T> T baseRemoveAdditionalStaticField(MemberData<Class<?>> clz, String key) {
        return runDump(() -> (T) XposedHelpers.removeAdditionalStaticField(clz.getIfExists(), key))
                .setErrMsg("Failed to remove static additional instance!")
                .spiltThrowableMsg(new Throwable[]{clz.getThrowable()})
                .or(null);
    }

    public static <T> T setAdditionalStaticField(String clz, String key, Object value) {
        return baseSetAdditionalStaticField(findClass(clz), key, value);
    }

    public static <T> T setAdditionalStaticField(String clz, ClassLoader classLoader, String key, Object value) {
        return baseSetAdditionalStaticField(findClass(clz, classLoader), key, value);
    }

    public static <T> T getAdditionalStaticField(String clz, String key) {
        return baseGetAdditionalStaticField(findClass(clz), key);
    }

    public static <T> T getAdditionalStaticField(String clz, ClassLoader classLoader, String key) {
        return baseGetAdditionalStaticField(findClass(key, classLoader), key);
    }

    public static <T> T removeAdditionalStaticField(String clz, String key) {
        return baseRemoveAdditionalStaticField(findClass(clz), key);
    }

    public static <T> T removeAdditionalStaticField(String clz, ClassLoader classLoader, String key) {
        return baseRemoveAdditionalStaticField(findClass(clz, classLoader), key);
    }
}
