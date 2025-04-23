package com.hchen.hooktool.core;

import static com.hchen.hooktool.helper.TryHelper.doTry;
import static com.hchen.hooktool.log.LogExpand.getTag;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.HCData;
import com.hchen.hooktool.callback.IMemberFilter;
import com.hchen.hooktool.exception.MissingParameterException;
import com.hchen.hooktool.exception.UnexpectedException;
import com.hchen.hooktool.helper.ConstructorHelper;
import com.hchen.hooktool.helper.MethodHelper;
import com.hchen.hooktool.hook.HookFactory;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.log.AndroidLog;
import com.hchen.hooktool.log.LogExpand;
import com.hchen.hooktool.log.XposedLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * @noinspection DataFlowIssue
 */
public class CoreTool {
    // -------------------------- Class ------------------------------
    public static boolean existsClass(String classPath) {
        return existsClass(classPath, HCData.getClassLoader());
    }

    public static boolean existsClass(String classPath, ClassLoader classLoader) {
        return doTry(
            () -> !Objects.isNull(XposedHelpers.findClassIfExists(classPath, classLoader))
        ).orElse(false);
    }

    public static Class<?> findClass(String classPath) {
        return findClass(classPath, HCData.getClassLoader());
    }

    public static Class<?> findClass(String classPath, ClassLoader classLoader) {
        return XposedHelpers.findClass(classPath, classLoader);
    }

    @Nullable
    public static Class<?> findClassIfExists(String classPath) {
        return findClassIfExists(classPath, HCData.getClassLoader());
    }

    @Nullable
    public static Class<?> findClassIfExists(String classPath, ClassLoader classLoader) {
        return doTry(
            () -> XposedHelpers.findClassIfExists(classPath, classLoader)
        ).get();
    }

    // -------------------------- Method ------------------------------
    public static boolean existsMethod(String classPath, String methodName, @NonNull Object... params) {
        return existsMethod(classPath, HCData.getClassLoader(), methodName, params);
    }

    public static boolean existsMethod(String classPath, ClassLoader classLoader, String methodName, @NonNull Object... params) {
        return doTry(
            () -> !Objects.isNull(XposedHelpers.findMethodExactIfExists(classPath, classLoader, methodName, params))
        ).orElse(false);
    }

    public static boolean existsMethod(@NonNull Class<?> clazz, String methodName, @NonNull Object... params) {
        return doTry(
            () -> !Objects.isNull(XposedHelpers.findMethodExactIfExists(clazz, methodName, params))
        ).orElse(false);
    }

    public static boolean existsAnyMethod(String classPath, String methodName) {
        return existsAnyMethod(classPath, HCData.getClassLoader(), methodName);
    }

    public static boolean existsAnyMethod(String classPath, ClassLoader classLoader, String methodName) {
        return existsAnyMethod(findClass(classPath, classLoader), methodName);
    }

    public static boolean existsAnyMethod(@NonNull Class<?> clazz, String methodName) {
        return doTry(() ->
            Arrays.stream(clazz.getDeclaredMethods())
                .anyMatch(method -> Objects.equals(method.getName(), methodName))
        ).orElse(false);
    }

    public static Method findMethod(String classPath, String methodName, @NonNull Object... params) {
        return findMethod(classPath, HCData.getClassLoader(), methodName, params);
    }

    public static Method findMethod(String classPath, ClassLoader classLoader, String methodName, @NonNull Object... params) {
        return XposedHelpers.findMethodExact(classPath, classLoader, methodName, params);
    }

    public static Method findMethod(@NonNull Class<?> clazz, String methodName, @NonNull Object... params) {
        return XposedHelpers.findMethodExact(clazz, methodName, params);
    }

    public static MethodHelper findMethodPro(String classPath) {
        return findMethodPro(classPath, HCData.getClassLoader());
    }

    public static MethodHelper findMethodPro(String classPath, ClassLoader classLoader) {
        return findMethodPro(findClass(classPath, classLoader));
    }

    public static MethodHelper findMethodPro(@NonNull Class<?> clazz) {
        return new MethodHelper(clazz);
    }

    @Nullable
    public static Method findMethodIfExists(String classPath, String methodName, @NonNull Object... params) {
        return findMethodIfExists(classPath, HCData.getClassLoader(), methodName, params);
    }

    @Nullable
    public static Method findMethodIfExists(String classPath, ClassLoader classLoader, String methodName, @NonNull Object... params) {
        return doTry(
            () -> XposedHelpers.findMethodExactIfExists(classPath, classLoader, methodName, params)
        ).get();
    }

    @Nullable
    public static Method findMethodIfExists(@NonNull Class<?> clazz, String methodName, @NonNull Object... params) {
        return doTry(
            () -> XposedHelpers.findMethodExactIfExists(clazz, methodName, params)
        ).get();
    }

    public static Method[] findAllMethod(String classPath, String methodName) {
        return findAllMethod(classPath, HCData.getClassLoader(), methodName);
    }

    public static Method[] findAllMethod(String classPath, ClassLoader classLoader, String methodName) {
        return findAllMethod(findClass(classPath, classLoader), methodName);
    }

    public static Method[] findAllMethod(@NonNull Class<?> clazz, String methodName) {
        return Arrays.stream(clazz.getDeclaredMethods())
            .filter(method -> Objects.equals(method.getName(), methodName))
            .toArray(Method[]::new);
    }

    // -------------------------- Constructor ------------------------------

    public static boolean existsConstructor(String classPath, @NonNull Object... params) {
        return existsConstructor(classPath, HCData.getClassLoader(), params);
    }

    public static boolean existsConstructor(String classPath, ClassLoader classLoader, @NonNull Object... params) {
        return doTry(
            () -> !Objects.isNull(XposedHelpers.findConstructorExactIfExists(classPath, classLoader, params))
        ).orElse(false);
    }

    public static boolean existsConstructor(@NonNull Class<?> clazz, @NonNull Object... params) {
        return doTry(
            () -> !Objects.isNull(XposedHelpers.findConstructorExactIfExists(clazz, params))
        ).orElse(false);
    }

    public static Constructor<?> findConstructor(String classPath, @NonNull Object... params) {
        return findConstructor(classPath, HCData.getClassLoader(), params);
    }

    public static Constructor<?> findConstructor(String classPath, ClassLoader classLoader, @NonNull Object... params) {
        return XposedHelpers.findConstructorExact(classPath, classLoader, params);
    }

    public static Constructor<?> findConstructor(@NonNull Class<?> clazz, @NonNull Object... params) {
        return XposedHelpers.findConstructorExact(clazz, params);
    }

    public static ConstructorHelper findConstructorPro(String classPath) {
        return findConstructorPro(classPath, HCData.getClassLoader());
    }

    public static ConstructorHelper findConstructorPro(String classPath, ClassLoader classLoader) {
        return findConstructorPro(findClass(classPath, classLoader));
    }

    public static ConstructorHelper findConstructorPro(@NonNull Class<?> clazz) {
        return new ConstructorHelper(clazz);
    }

    @Nullable
    public static Constructor<?> findConstructorIfExists(String classPath, @NonNull Object... params) {
        return findConstructorIfExists(classPath, HCData.getClassLoader(), params);
    }

    @Nullable
    public static Constructor<?> findConstructorIfExists(String classPath, ClassLoader classLoader, @NonNull Object... params) {
        return doTry(
            () -> XposedHelpers.findConstructorExactIfExists(classPath, classLoader, params)
        ).get();
    }

    @Nullable
    public static Constructor<?> findConstructorIfExists(@NonNull Class<?> clazz, @NonNull Object... params) {
        return doTry(
            () -> XposedHelpers.findConstructorExactIfExists(clazz, params)
        ).get();
    }

    public static Constructor<?>[] findAllConstructor(String classPath) {
        return findAllConstructor(classPath, HCData.getClassLoader());
    }

    public static Constructor<?>[] findAllConstructor(String classPath, ClassLoader classLoader) {
        return findAllConstructor(findClass(classPath, classLoader));
    }

    public static Constructor<?>[] findAllConstructor(@NonNull Class<?> clazz) {
        return clazz.getDeclaredConstructors();
    }

    // -------------------------- Field ------------------------------

    public static boolean existsField(String classPath, String fieldName) {
        return existsField(classPath, HCData.getClassLoader(), fieldName);
    }

    public static boolean existsField(String classPath, ClassLoader classLoader, String fieldName) {
        return doTry(
            () -> existsField(findClass(classPath, classLoader), fieldName)
        ).orElse(false);
    }

    public static boolean existsField(@NonNull Class<?> clazz, String fieldName) {
        return doTry(
            () -> !Objects.isNull(XposedHelpers.findFieldIfExists(clazz, fieldName))
        ).orElse(false);
    }

    public static Field findField(String classPath, String fieldName) {
        return findField(classPath, HCData.getClassLoader(), fieldName);
    }

    public static Field findField(String classPath, ClassLoader classLoader, String fieldName) {
        return findField(findClass(classPath, classLoader), fieldName);
    }

    public static Field findField(@NonNull Class<?> clazz, String fieldName) {
        return XposedHelpers.findField(clazz, fieldName);
    }

    @Nullable
    public static Field findFieldIfExists(String classPath, String fieldName) {
        return findFieldIfExists(classPath, HCData.getClassLoader(), fieldName);
    }

    @Nullable
    public static Field findFieldIfExists(String classPath, ClassLoader classLoader, String fieldName) {
        return doTry(
            () -> findFieldIfExists(findClass(classPath, classLoader), fieldName)
        ).get();
    }

    @Nullable
    public static Field findFieldIfExists(@NonNull Class<?> clazz, String fieldName) {
        return doTry(
            () -> XposedHelpers.findFieldIfExists(clazz, fieldName)
        ).get();
    }

    // -------------------------- Hook ------------------------------
    @Nullable
    public static XC_MethodHook.Unhook hookMethod(String classPath, String methodName, @NonNull Object... params) {
        return hookMethod(classPath, HCData.getClassLoader(), methodName, params);
    }

    @Nullable
    public static XC_MethodHook.Unhook hookMethod(String classPath, ClassLoader classLoader, String methodName, @NonNull Object... params) {
        return hookMethod(findClass(classPath, classLoader), methodName, params);
    }

    @Nullable
    public static XC_MethodHook.Unhook hookMethod(@NonNull Class<?> clazz, String methodName, @NonNull Object... params) {
        return hook(findMethod(clazz, methodName, filterParams(params)), filterIHook(params));
    }

    @Nullable
    public static XC_MethodHook.Unhook hookMethodIfExists(String classPath, String methodName, @NonNull Object... params) {
        return hookMethod(classPath, HCData.getClassLoader(), methodName, params);
    }

    @Nullable
    public static XC_MethodHook.Unhook hookMethodIfExists(String classPath, ClassLoader classLoader, String methodName, @NonNull Object... params) {
        return doTry(
            () -> hookMethod(findClass(classPath, classLoader), methodName, params)
        ).get();
    }

    @Nullable
    public static XC_MethodHook.Unhook hookMethodIfExists(@NonNull Class<?> clazz, String methodName, @NonNull Object... params) {
        return doTry(
            () -> hook(findMethod(clazz, methodName, filterParams(params)), filterIHook(params))
        ).get();
    }

    public static XC_MethodHook.Unhook[] hookAllMethod(String classPath, String methodName, @NonNull IHook iHook) {
        return hookAllMethod(classPath, HCData.getClassLoader(), methodName, iHook);
    }

    public static XC_MethodHook.Unhook[] hookAllMethod(String classPath, ClassLoader classLoader, String methodName, @NonNull IHook iHook) {
        return hookAllMethod(findClass(classPath, classLoader), methodName, iHook);
    }

    public static XC_MethodHook.Unhook[] hookAllMethod(@NonNull Class<?> clazz, String methodName, @NonNull IHook iHook) {
        return hookAll(findAllMethod(clazz, methodName), iHook);
    }

    @Nullable
    public static XC_MethodHook.Unhook hookConstructor(String classPath, @NonNull Object... params) {
        return hookConstructor(classPath, HCData.getClassLoader(), params);
    }

    @Nullable
    public static XC_MethodHook.Unhook hookConstructor(String classPath, ClassLoader classLoader, @NonNull Object... params) {
        return hookConstructor(findClass(classPath, classLoader), params);
    }

    @Nullable
    public static XC_MethodHook.Unhook hookConstructor(@NonNull Class<?> clazz, @NonNull Object... params) {
        return hook(findConstructor(clazz, filterParams(params)), filterIHook(params));
    }

    @Nullable
    public static XC_MethodHook.Unhook hookConstructorIfExists(String classPath, @NonNull Object... params) {
        return hookConstructorIfExists(classPath, HCData.getClassLoader(), params);
    }

    @Nullable
    public static XC_MethodHook.Unhook hookConstructorIfExists(String classPath, ClassLoader classLoader, @NonNull Object... params) {
        return doTry(
            () -> hookConstructorIfExists(findClass(classPath, classLoader), params)
        ).get();
    }

    @Nullable
    public static XC_MethodHook.Unhook hookConstructorIfExists(@NonNull Class<?> clazz, @NonNull Object... params) {
        return doTry(
            () -> hook(findConstructor(clazz, filterParams(params)), filterIHook(params))
        ).get();
    }

    public static XC_MethodHook.Unhook[] hookAllConstructor(String classPath, @NonNull IHook iHook) {
        return hookAllConstructor(classPath, HCData.getClassLoader(), iHook);
    }

    public static XC_MethodHook.Unhook[] hookAllConstructor(String classPath, ClassLoader classLoader, @NonNull IHook iHook) {
        return hookAllConstructor(findClass(classPath, classLoader), iHook);
    }

    public static XC_MethodHook.Unhook[] hookAllConstructor(@NonNull Class<?> clazz, @NonNull IHook iHook) {
        return hookAll(findAllConstructor(clazz), iHook);
    }

    @Nullable
    public static XC_MethodHook.Unhook hook(@NonNull Member member, @NonNull IHook iHook) {
        return hookAll(new Member[]{member}, iHook)[0];
    }

    public static XC_MethodHook.Unhook[] hookAll(@NonNull Member[] members, @NonNull IHook iHook) {
        return Arrays.stream(members).map(new Function<Member, XC_MethodHook.Unhook>() {
            @Override
            public XC_MethodHook.Unhook apply(Member member) {
                try {
                    XC_MethodHook.Unhook unhook = XposedBridge.hookMethod(member, HookFactory.createHook(iHook));
                    XposedLog.logI("Hook", "Success to hook: " + member);
                    return unhook;
                } catch (Throwable e) {
                    XposedLog.logE("Hook", "Failed to hook: " + member, e);
                    return null;
                }
            }
        }).toArray(XC_MethodHook.Unhook[]::new);
    }

    public static void unHook(@NonNull Member member, @NonNull XC_MethodHook xcMethodHook) {
        XposedBridge.unhookMethod(member, xcMethodHook);
    }

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
                returnNull();
            }
        };
    }

    public static IHook setArg(int index, Object value) {
        return new IHook() {
            @Override
            public void before() {
                setArg(index, value);
            }
        };
    }

    private static Object[] filterParams(@NonNull Object... params) {
        if (params.length == 0 || !(params[params.length - 1] instanceof IHook))
            throw new MissingParameterException("[CoreTool]: Missing IHook parameter!");

        return Arrays.copyOf(params, params.length - 1);
    }

    private static IHook filterIHook(@NonNull Object... params) {
        if (params.length == 0 || !(params[params.length - 1] instanceof IHook iHook))
            throw new MissingParameterException("[CoreTool]: Missing IHook parameter!");

        return iHook;
    }

    // -------------------------- Non static ------------------------------

    public static Object callMethod(@NonNull Object instance, String methodName, @NonNull Object... params) {
        return XposedHelpers.callMethod(instance, methodName, params);
    }

    public static Object callMethodIfExists(@NonNull Object instance, String methodName, @NonNull Object... params) {
        return doTry(
            () -> XposedHelpers.callMethod(instance, methodName, params)
        ).get();
    }

    public static Object callMethod(@NonNull Object instance, @NonNull Method method, @NonNull Object... params) {
        try {
            method.setAccessible(true);
            return method.invoke(instance, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new UnexpectedException(e);
        }
    }

    public static Object getField(@NonNull Object instance, String fieldName) {
        return XposedHelpers.getObjectField(instance, fieldName);
    }

    public static Object getFieldIfExists(@NonNull Object instance, String fieldName) {
        return doTry(
            () -> XposedHelpers.getObjectField(instance, fieldName)
        ).get();
    }

    public static Object getField(@NonNull Object instance, @NonNull Field field) {
        try {
            field.setAccessible(true);
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new UnexpectedException(e);
        }
    }

    public static void setField(@NonNull Object instance, String fieldName, Object value) {
        XposedHelpers.setObjectField(instance, fieldName, value);
    }

    public static void setFieldIfExists(@NonNull Object instance, String fieldName, Object value) {
        doTry(() -> {
            XposedHelpers.setObjectField(instance, fieldName, value);
            return null;
        });
    }

    public static void setField(@NonNull Object instance, @NonNull Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new UnexpectedException(e);
        }
    }

    public static Object setAdditionalInstanceField(@NonNull Object instance, String key, Object value) {
        return XposedHelpers.setAdditionalInstanceField(instance, key, value);
    }

    public static Object getAdditionalInstanceField(@NonNull Object instance, String key) {
        return XposedHelpers.getAdditionalInstanceField(instance, key);
    }

    public static Object removeAdditionalInstanceField(@NonNull Object instance, String key) {
        return XposedHelpers.removeAdditionalInstanceField(instance, key);
    }

    // -------------------------- Static ------------------------------

    public static Object newInstance(String classPath, @NonNull Object... params) {
        return newInstance(classPath, HCData.getClassLoader(), params);
    }

    public static Object newInstance(String classPath, ClassLoader classLoader, @NonNull Object... params) {
        return newInstance(findClass(classPath, classLoader), params);
    }

    public static Object newInstance(@NonNull Class<?> clazz, @NonNull Object... params) {
        return XposedHelpers.newInstance(clazz, params);
    }

    public static Object newInstanceIfExists(String classPath, @NonNull Object... params) {
        return newInstance(classPath, HCData.getClassLoader(), params);
    }

    public static Object newInstanceIfExists(String classPath, ClassLoader classLoader, @NonNull Object... params) {
        return doTry(
            () -> newInstance(findClass(classPath, classLoader), params)
        ).get();
    }

    public static Object newInstanceIfExists(@NonNull Class<?> clazz, @NonNull Object... params) {
        return doTry(
            () -> XposedHelpers.newInstance(clazz, params)
        ).get();
    }

    public static Object callStaticMethod(String classPath, String methodName, @NonNull Object... params) {
        return callStaticMethod(classPath, HCData.getClassLoader(), methodName, params);
    }

    public static Object callStaticMethod(String classPath, ClassLoader classLoader, String methodName, @NonNull Object... params) {
        return callStaticMethod(findClass(classPath, classLoader), methodName, params);
    }

    public static Object callStaticMethod(@NonNull Class<?> clazz, String methodName, @NonNull Object... params) {
        return XposedHelpers.callStaticMethod(clazz, methodName, params);
    }

    public static Object callStaticMethodIfExists(String classPath, String methodName, @NonNull Object... params) {
        return callStaticMethod(classPath, HCData.getClassLoader(), methodName, params);
    }

    public static Object callStaticMethodIfExists(String classPath, ClassLoader classLoader, String methodName, @NonNull Object... params) {
        return doTry(
            () -> callStaticMethod(findClass(classPath, classLoader), methodName, params)
        ).get();
    }

    public static Object callStaticMethodIfExists(@NonNull Class<?> clazz, String methodName, @NonNull Object... params) {
        return doTry(
            () -> XposedHelpers.callStaticMethod(clazz, methodName, params)
        ).get();
    }

    public static Object callStaticMethod(@NonNull Method method, @NonNull Object... params) {
        try {
            method.setAccessible(true);
            return method.invoke(null, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new UnexpectedException(e);
        }
    }

    public static Object getStaticField(String classPath, String fieldName) {
        return getStaticField(classPath, HCData.getClassLoader(), fieldName);
    }

    public static Object getStaticField(String classPath, ClassLoader classLoader, String fieldName) {
        return getStaticField(findClass(classPath, classLoader), fieldName);
    }

    public static Object getStaticField(@NonNull Class<?> clazz, String fieldName) {
        return XposedHelpers.getStaticObjectField(clazz, fieldName);
    }

    public static Object getStaticFieldIfExists(String classPath, String fieldName) {
        return getStaticField(classPath, HCData.getClassLoader(), fieldName);
    }

    public static Object getStaticFieldIfExists(String classPath, ClassLoader classLoader, String fieldName) {
        return doTry(
            () -> getStaticField(findClass(classPath, classLoader), fieldName)
        ).get();
    }

    public static Object getStaticFieldIfExists(@NonNull Class<?> clazz, String fieldName) {
        return doTry(
            () -> XposedHelpers.getStaticObjectField(clazz, fieldName)
        ).get();
    }

    public static Object getStaticField(@NonNull Field field) {
        try {
            field.setAccessible(true);
            return field.get(null);
        } catch (IllegalAccessException e) {
            throw new UnexpectedException(e);
        }
    }

    public static void setStaticField(String classPath, String fieldName, Object value) {
        setStaticField(classPath, HCData.getClassLoader(), fieldName, value);
    }

    public static void setStaticField(String classPath, ClassLoader classLoader, String fieldName, Object value) {
        setStaticField(findClass(classPath, classLoader), fieldName, value);
    }

    public static void setStaticField(@NonNull Class<?> clazz, String fieldName, Object value) {
        XposedHelpers.setStaticObjectField(clazz, fieldName, value);
    }

    public static void setStaticFieldIfExists(String classPath, String fieldName, Object value) {
        setStaticField(classPath, HCData.getClassLoader(), fieldName, value);
    }

    public static void setStaticFieldIfExists(String classPath, ClassLoader classLoader, String fieldName, Object value) {
        doTry(() -> {
            setStaticField(findClass(classPath, classLoader), fieldName, value);
            return null;
        });
    }

    public static void setStaticFieldIfExists(@NonNull Class<?> clazz, String fieldName, Object value) {
        doTry(() -> {
            XposedHelpers.setStaticObjectField(clazz, fieldName, value);
            return null;
        });
    }

    public static void setStaticField(@NonNull Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(null, value);
        } catch (IllegalAccessException e) {
            throw new UnexpectedException(e);
        }
    }

    public static Object setAdditionalStaticField(String classPath, String key, Object value) {
        return setAdditionalStaticField(classPath, HCData.getClassLoader(), key, value);
    }

    public static Object setAdditionalStaticField(String classPath, ClassLoader classLoader, String key, Object value) {
        return setAdditionalStaticField(findClass(classPath, classLoader), key, value);
    }

    public static Object setAdditionalStaticField(@NonNull Class<?> clazz, String key, Object value) {
        return XposedHelpers.setAdditionalStaticField(clazz, key, value);
    }

    public static Object getAdditionalStaticField(String classPath, String key) {
        return getAdditionalStaticField(classPath, HCData.getClassLoader(), key);
    }

    public static Object getAdditionalStaticField(String classPath, ClassLoader classLoader, String key) {
        return getAdditionalStaticField(findClass(classPath, classLoader), key);
    }

    public static Object getAdditionalStaticField(@NonNull Class<?> clazz, String key) {
        return XposedHelpers.getAdditionalStaticField(clazz, key);
    }

    public static Object removeAdditionalStaticField(String classPath, String key) {
        return removeAdditionalStaticField(classPath, HCData.getClassLoader(), key);
    }

    public static Object removeAdditionalStaticField(String classPath, ClassLoader classLoader, String key) {
        return removeAdditionalStaticField(findClass(classPath, classLoader), key);
    }

    public static Object removeAdditionalStaticField(@NonNull Class<?> clazz, String key) {
        return XposedHelpers.removeAdditionalStaticField(clazz, key);
    }

    // -------------------------- Invoke ------------------------------
    public static Object invokeOriginalMethod(Member method, Object thisObject, Object[] args) throws InvocationTargetException, IllegalAccessException {
        return XposedBridge.invokeOriginalMethod(method, thisObject, args);
    }

    // -------------------------- Chain ------------------------------

    public static ChainTool buildChain(String classPath) {
        return ChainTool.buildChain(classPath);
    }

    public static ChainTool buildChain(String classPath, ClassLoader classLoader) {
        return ChainTool.buildChain(classPath, classLoader);
    }

    public static ChainTool buildChain(@NonNull Class<?> clazz) {
        return ChainTool.buildChain(clazz);
    }

    // -------------------------- Filter ------------------------------

    public static Method[] filterMethod(String classPath, @NonNull IMemberFilter<Method> iMemberFilter) {
        return filterMethod(classPath, HCData.getClassLoader(), iMemberFilter);
    }

    public static Method[] filterMethod(String classPath, ClassLoader classLoader, @NonNull IMemberFilter<Method> iMemberFilter) {
        return filterMethod(findClass(classPath, classLoader), iMemberFilter);
    }

    public static Method[] filterMethod(@NonNull Class<?> clazz, @NonNull IMemberFilter<Method> iMemberFilter) {
        return Arrays.stream(clazz.getDeclaredMethods())
            .filter(iMemberFilter::test)
            .toArray(Method[]::new);
    }

    public static Constructor<?>[] filterConstructor(String classPath, @NonNull IMemberFilter<Constructor<?>> iMemberFilter) {
        return filterConstructor(classPath, HCData.getClassLoader(), iMemberFilter);
    }

    public static Constructor<?>[] filterConstructor(String classPath, ClassLoader classLoader, @NonNull IMemberFilter<Constructor<?>> iMemberFilter) {
        return filterConstructor(findClass(classPath, classLoader), iMemberFilter);
    }

    public static Constructor<?>[] filterConstructor(@NonNull Class<?> clazz, @NonNull IMemberFilter<Constructor<?>> iMemberFilter) {
        return Arrays.stream(clazz.getDeclaredConstructors())
            .filter(iMemberFilter::test)
            .toArray(Constructor[]::new);
    }

    // -------------------------- Other ------------------------------

    public static String getStackTrace(boolean autoLog) {
        String task = getStackTrace();
        if (autoLog) AndroidLog.logD(getTag(), task);
        return task;
    }

    public static String getStackTrace() {
        return LogExpand.getStackTrace();
    }

    public static long timeConsumption(@NonNull Runnable runnable) {
        return doTry(() -> {
            Instant start = Instant.now();
            runnable.run();
            Instant end = Instant.now();
            return Duration.between(start, end).toMillis();
        }).orElse(-1L);
    }
}
