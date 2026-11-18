/*
 * This file is part of HookTool.
 *
 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * HookTool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with HookTool. If not, see <https://www.gnu.org/licenses/lgpl-2.1>.
 *
 * Copyright (C) 2024–2026 HChenX
 */
package com.hchen.hooktool.res;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.ParcelFileDescriptor;
import android.util.Pair;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.hook.AbsHook;
import com.hchen.hooktool.hook.Hook;
import com.hchen.hooktool.hook.HookInterop;
import com.hchen.hooktool.host.Host;
import com.hchen.hooktool.log.AndroidLog;
import com.hchen.hooktool.reflect.Reflect;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.api.XposedInterface;

/**
 * 资源注入与运行时替换工具类。
 *
 * @author 焕晨HChen
 */
public final class ResInject {
    private static final String TAG = "ResInjectTool";
    private static volatile ResourcesLoader sResourcesLoader = null;
    private static final ConcurrentHashMap<String, Pair<ReplacementType, Object>> sReplacements = new ConcurrentHashMap<>();
    private static final AtomicBoolean sIsInjected = new AtomicBoolean(false);
    private static final AtomicBoolean sIsHooked = new AtomicBoolean(false);

    private static volatile int sStyleNumEntries;
    private static volatile int sStyleType;
    private static volatile int sStyleResourceId;
    /**
     * 已注册替换规则数，用于 hook 回调热路径的空表短路：非零才进入完整的资源名解析与查找。
     */
    private static final AtomicInteger sReplacementCount = new AtomicInteger();

    private static final AbsHook sHookResBefore = new AbsHook() {
        @Override
        public void before() {
            String methodName = null;
            try {
                methodName = getExecutable().getName();
                Object value = getResourceReplacement((Method) getExecutable(), (Resources) getThisObject(), getArgs());
                if (value != null) {
                    if ("getDimensionPixelOffset".equals(methodName) || "getDimensionPixelSize".equals(methodName)) {
                        if (value instanceof Float) value = Math.round((Float) value);
                    }
                    setResult(value);
                }
            } catch (Throwable t) {
                AndroidLog.logW(TAG, "Failed to replace resource (method: " + methodName + ").", t);
            }
        }
    };

    private static final AbsHook sHookTypedBefore = new AbsHook() {
        @Override
        public void before() {
            String methodName = null;
            try {
                int index = (int) getArg(0);
                index *= sStyleNumEntries;
                int[] data = (int[]) Reflect.getObjectField(getThisObject(), "mData");
                if (data == null) return;
                // 越界校验：部分 ROM 的 TypedArray 实现或索引异常时避免中断替换逻辑。
                if (index < 0 || index >= data.length - sStyleResourceId - 1) return;

                int type = data[index + sStyleType];
                int id = data[index + sStyleResourceId];
                if (type != TypedValue.TYPE_NULL /* 不为空数据 */ && id != 0 /* 储存的是资源 */) {
                    methodName = getExecutable().getName();
                    Resources resources = (Resources) Reflect.getObjectField(getThisObject(), "mResources");
                    if (resources == null) return;
                    Resources.Theme theme = (Resources.Theme) Reflect.getObjectField(getThisObject(), "mTheme");

                    Object value = getTypedArrayReplacement(resources, theme, id, methodName, getArgs());
                    if (value != null) {
                        if (
                            "getDimensionPixelOffset".equals(methodName) ||
                                "getDimensionPixelSize".equals(methodName) ||
                                "getLayoutDimension".equals(methodName)
                        ) {
                            if (value instanceof Float) value = Math.round((Float) value);
                        }
                        setResult(value);
                    }
                }
            } catch (Throwable t) {
                AndroidLog.logW(TAG, "Failed to replace typed array resource (method: " + methodName + ").", t);
            }
        }
    };

    /**
     * 资源替换类型枚举。
     */
    private enum ReplacementType {
        ID,
        DENSITY,
        OBJECT
    }

    private ResInject() {
    }

    /**
     * 将模块的 APK 资源注入到目标应用的资源加载流程中。
     * <p>
     * 仅首次调用生效，调用前须在模块的 {@code build.gradle} 中指定资源包前缀。
     * <pre>{@code
     * androidResources.additionalParameters("--allow-reserved-package-id", "--package-id", "0x64")
     * }</pre>
     *
     * @throws IllegalStateException 创建 {@link ResourcesLoader} 或 {@link ParcelFileDescriptor} 失败时抛出；其余任意 {@link Throwable}（如 hook 失败）将原样上抛
     */
    public static void injectModuleRes() {
        if (!sIsInjected.compareAndSet(false, true)) return;

        String sourceDir = Host.get().modulePath();
        try {
            if (sResourcesLoader == null) {
                try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(new File(sourceDir), ParcelFileDescriptor.MODE_READ_ONLY)) {
                    ResourcesProvider provider = ResourcesProvider.loadFromApk(pfd);
                    ResourcesLoader loader = new ResourcesLoader();
                    loader.addProvider(provider);
                    sResourcesLoader = loader;
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to create res loader.", e);
                }
            }

            Hook.hookConstructor(
                Reflect.findClass("android.content.res.ResourcesKey"),
                String.class /* resDir */, String[].class /* splitResDirs */, String[].class /* overlayPaths */,
                String[].class,/* libDirs */ int.class /* overrideDisplayId */, Configuration.class /* overrideConfig */,
                "android.content.res.CompatibilityInfo" /* compatInfo */, ResourcesLoader[].class /* loader */,
                new AbsHook() {
                    @Override
                    public void before() {
                        ResourcesLoader[] loader = (ResourcesLoader[]) getArg(7);
                        if (loader != null) {
                            List<ResourcesLoader> loaders = new ArrayList<>(Arrays.asList(loader));
                            if (!loaders.contains(sResourcesLoader)) {
                                loaders.add(sResourcesLoader);
                                setArg(7, loaders.toArray(new ResourcesLoader[0]));
                            }
                        } else {
                            setArg(7, new ResourcesLoader[]{sResourcesLoader});
                        }
                    }
                }
            );
        } catch (Throwable t) {
            // 失败回滚标志，允许后续重试，避免注入永久失效。
            sIsInjected.set(false);
            throw t;
        }
    }

    /**
     * 根据资源名称字符串生成一个伪资源 ID。
     *
     * @param resName 资源名称字符串
     * @return 生成的伪资源 ID
     */
    public static int createFakeResId(@NonNull String resName) {
        return 0x7e000000 | (fnv1a32Hash(resName) & 0x00ffffff);
    }

    /**
     * 根据已有资源 ID 对应的资源名称生成伪资源 ID。
     *
     * @param res {@link Resources} 实例，用于解析资源名称
     * @param id  原始资源 ID
     * @return 生成的伪资源 ID
     */
    public static int createFakeResId(@NonNull Resources res, int id) {
        return createFakeResId(res.getResourceName(id));
    }

    /**
     * FNV-1a 32 位哈希算法实现。
     *
     * @param str 待计算哈希的字符串
     * @return 32 位哈希值
     */
    private static int fnv1a32Hash(@NonNull String str) {
        final int FNV_32_PRIME = 0x01000193;
        int hash = 0x811c9dc5;
        for (byte b : str.getBytes(StandardCharsets.UTF_8)) {
            hash ^= (b & 0xff);
            hash *= FNV_32_PRIME;
        }
        return hash;
    }

    /**
     * 注册一条资源 ID 替换规则。
     * <p>
     * 当目标应用访问指定资源时，将其重定向到模块提供的替换资源 ID；调用前必须先执行 {@link #injectModuleRes()}。
     *
     * @param packageName      目标应用包名；使用 {@code "*"} 可匹配所有应用
     * @param type             资源类型（如 {@code "drawable"}、{@code "string"}、{@code "color"} 等）
     * @param resName          资源名称
     * @param replacementResId 用作替换的模块资源 ID
     */
    public static void setResReplacement(@NonNull String packageName, @NonNull String type, @NonNull String resName, int replacementResId) {
        applyHooks();
        sReplacements.put(packageName + ":" + type + "/" + resName, new Pair<>(ReplacementType.ID, replacementResId));
        sReplacementCount.incrementAndGet();
    }

    /**
     * 注册一条密度值替换规则。
     * <p>
     * 当目标应用访问指定的尺寸类资源时，返回值将为 {@code replacementResValue * displayMetrics.density}。
     *
     * @param packageName         目标应用包名
     * @param type                资源类型
     * @param resName             资源名称
     * @param replacementResValue 替换用的密度基准值
     */
    public static void setDensityReplacement(@NonNull String packageName, @NonNull String type, @NonNull String resName, float replacementResValue) {
        applyHooks();
        sReplacements.put(packageName + ":" + type + "/" + resName, new Pair<>(ReplacementType.DENSITY, replacementResValue));
        sReplacementCount.incrementAndGet();
    }

    /**
     * 注册一条对象替换规则。
     * <p>
     * 当目标应用访问指定资源时，直接返回给定的对象值，不进行任何类型转换。
     *
     * @param packageName         目标应用包名
     * @param type                资源类型
     * @param resName             资源名称
     * @param replacementResValue 用作替换的任意对象
     */
    public static void setObjectReplacement(@NonNull String packageName, @NonNull String type, @NonNull String resName, Object replacementResValue) {
        applyHooks();
        sReplacements.put(packageName + ":" + type + "/" + resName, new Pair<>(ReplacementType.OBJECT, replacementResValue));
        sReplacementCount.incrementAndGet();
    }

    /**
     * 对 {@link Resources} 和 {@link TypedArray} 的各类资源获取方法进行 hook，使其支持运行时替换。
     * <p>
     * 仅首次调用生效，未先注入模块资源时会直接抛出异常。
     *
     * @throws IllegalStateException 未先调用 {@link #injectModuleRes()} 或读取 {@link TypedArray} 内部常量失败时抛出
     */
    private static void applyHooks() {
        if (!sIsHooked.compareAndSet(false, true)) return;
        if (!sIsInjected.get()) {
            sIsHooked.set(false);
            throw new IllegalStateException("Should inject module res first.");
        }
        try {
            // 先解析 TypedArray 内部常量，失败时尚未注册任何 hook。
            sStyleNumEntries = (int) Reflect.getStaticObjectField(TypedArray.class, "STYLE_NUM_ENTRIES");
            sStyleType = (int) Reflect.getStaticObjectField(TypedArray.class, "STYLE_TYPE");
            sStyleResourceId = (int) Reflect.getStaticObjectField(TypedArray.class, "STYLE_RESOURCE_ID");
            if (sStyleNumEntries <= 0) {
                throw new IllegalStateException("Failed to read STYLE_NUM_ENTRIES from TypedArray.");
            }

            registerResHooks();
            registerTypedArrayHooks();
        } catch (Throwable t) {
            // 失败回滚标志，允许调用方重试，而非永久静默失效。
            sIsHooked.set(false);
            throw t;
        }
    }

    private static void registerResHooks() {
        Hook.hookMethod(Resources.class, "loadXmlResourceParser", int.class, String.class, sHookResBefore); // XmlResourceParser
        Hook.hookMethod(Resources.class, "getDimension", int.class, sHookResBefore); // float
        Hook.hookMethod(Resources.class, "getDimensionPixelOffset", int.class, sHookResBefore); // int
        Hook.hookMethod(Resources.class, "getDimensionPixelSize", int.class, sHookResBefore); // int
        Hook.hookMethod(Resources.class, "getBoolean", int.class, sHookResBefore); // boolean
        Hook.hookMethod(Resources.class, "getInteger", int.class, sHookResBefore); // int
        Hook.hookMethod(Resources.class, "getFloat", int.class, sHookResBefore); // float
        Hook.hookMethod(Resources.class, "getText", int.class, sHookResBefore); // CharSequence
        Hook.hookMethod(Resources.class, "getText", int.class, CharSequence.class, sHookResBefore); // CharSequence
        Hook.hookMethod(Resources.class, "getQuantityText", int.class, int.class, sHookResBefore); // CharSequence
        Hook.hookMethod(Resources.class, "getIntArray", int.class, sHookResBefore); // int[]
        Hook.hookMethod(Resources.class, "getStringArray", int.class, sHookResBefore); // String[]
        Hook.hookMethod(Resources.class, "getTextArray", int.class, sHookResBefore); // CharSequence[]
        Hook.hookMethod(Resources.class, "getFont", int.class, sHookResBefore); // Typeface
        Hook.hookMethod(Resources.class, "getMovie", int.class, sHookResBefore); // Movie
        Hook.hookMethod(Resources.class, "getColor", int.class, Resources.Theme.class, sHookResBefore); // int
        Hook.hookMethod(Resources.class, "getColorStateList", int.class, Resources.Theme.class, sHookResBefore); // ColorStateList
        Hook.hookMethod(Resources.class, "getFraction", int.class, int.class, int.class, sHookResBefore); // float
        Hook.hookMethod(Resources.class, "getDrawableForDensity", int.class, int.class, Resources.Theme.class, sHookResBefore); // Drawable
    }

    private static void registerTypedArrayHooks() {
        Hook.hookMethod(TypedArray.class, "getColor", int.class, int.class, sHookTypedBefore); // int
        Hook.hookMethod(TypedArray.class, "getColorStateList", int.class, sHookTypedBefore); // ColorStateList
        Hook.hookMethod(TypedArray.class, "getBoolean", int.class, boolean.class, sHookTypedBefore); // boolean
        Hook.hookMethod(TypedArray.class, "getFloat", int.class, float.class, sHookTypedBefore); // float
        Hook.hookMethod(TypedArray.class, "getInt", int.class, int.class, sHookTypedBefore); // int
        Hook.hookMethod(TypedArray.class, "getInteger", int.class, int.class, sHookTypedBefore); // int
        Hook.hookMethod(TypedArray.class, "getString", int.class, sHookTypedBefore); // String
        Hook.hookMethod(TypedArray.class, "getText", int.class, sHookTypedBefore); // CharSequence
        Hook.hookMethod(TypedArray.class, "getFont", int.class, sHookTypedBefore); // Typeface
        Hook.hookMethod(TypedArray.class, "getDimension", int.class, float.class, sHookTypedBefore); // float
        Hook.hookMethod(TypedArray.class, "getDimensionPixelOffset", int.class, int.class, sHookTypedBefore); // int
        Hook.hookMethod(TypedArray.class, "getDimensionPixelSize", int.class, int.class, sHookTypedBefore); // int
        Hook.hookMethod(TypedArray.class, "getLayoutDimension", int.class, int.class, sHookTypedBefore); // int
        Hook.hookMethod(TypedArray.class, "getLayoutDimension", int.class, String.class, sHookTypedBefore); // int
        Hook.hookMethod(TypedArray.class, "getDrawableForDensity", int.class, int.class, sHookTypedBefore); // Drawable
        Hook.hookMethod(TypedArray.class, "getFraction", int.class, int.class, int.class, float.class, sHookTypedBefore); // float
    }

    /**
     * 在已注册的替换规则中查找与指定资源 ID 匹配的规则。
     * <p>
     * 精确匹配未命中时会回退到通配包名（{@code "*"}）规则。
     *
     * @param res   {@link Resources} 实例
     * @param resId 资源 ID
     * @return 匹配到的替换规则（类型 + 值），未找到返回 {@code null}
     */
    @Nullable
    private static Pair<ReplacementType, Object> findReplacement(@NonNull Resources res, int resId) {
        String packageName = null;
        String resType = null;
        String resName = null;
        try {
            packageName = res.getResourcePackageName(resId);
            resType = res.getResourceTypeName(resId);
            resName = res.getResourceEntryName(resId);
        } catch (Throwable e) {
            // 无效 resId 属预期路径，D 级记录便于排查替换未生效。
            AndroidLog.logD(TAG, "Failed to resolve resource name for id: " + resId + ".", e);
        }

        if (packageName == null || resType == null || resName == null) return null;

        String resFullName = packageName + ":" + resType + "/" + resName;
        Pair<ReplacementType, Object> replacement = sReplacements.get(resFullName);
        if (replacement == null) {
            replacement = sReplacements.get("*:" + resType + "/" + resName);
        }
        return replacement;
    }

    /**
     * 执行 {@link Resources} 级别的资源替换逻辑。
     * <p>
     * 按规则类型分别返回对象值、密度换算值，或以替换后的资源 ID 调用原方法的结果。
     *
     * @param method 被 hook 的原始方法
     * @param res    {@link Resources} 实例
     * @param params 方法参数；第一个元素为资源 ID
     * @return 替换后的值；无匹配规则时返回 {@code null}
     */
    @Nullable
    private static Object getResourceReplacement(@NonNull Method method, @NonNull Resources res, @NonNull Object[] params) {
        if (sReplacementCount.get() == 0) return null;
        Pair<ReplacementType, Object> replacement = findReplacement(res, (int) params[0]);
        if (replacement == null) return null;

        switch (replacement.first) {
            case OBJECT -> {
                return replacement.second;
            }
            case DENSITY -> {
                return (Float) replacement.second * res.getDisplayMetrics().density;
            }
            case ID -> {
                try {
                    params[0] = replacement.second;
                    return HookInterop.getMethodInvoker(method)
                        .setType(XposedInterface.Invoker.Type.ORIGIN)
                        .invoke(res, params);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return null;
    }

    /**
     * 执行 {@link TypedArray} 级别的资源替换逻辑。
     * <p>
     * {@code ID} 类型替换会按被 hook 的方法名分发到对应的 {@link Resources} API，以保证返回类型一致。
     *
     * @param res        {@link Resources} 实例
     * @param theme      当前主题，可能为 {@code null}
     * @param id         从 {@link TypedArray} 的 {@code mData} 数组中提取的原始资源 ID
     * @param methodName 被 hook 的方法名称
     * @param params     原始方法参数
     * @return 替换后的值；无匹配规则时返回 {@code null}
     */
    @Nullable
    private static Object getTypedArrayReplacement(@NonNull Resources res, @Nullable Resources.Theme theme, int id, @NonNull String methodName, @NonNull Object[] params) {
        if (sReplacementCount.get() == 0) return null;
        Pair<ReplacementType, Object> replacement = findReplacement(res, id);
        if (replacement == null) return null;

        switch (replacement.first) {
            case OBJECT -> {
                return replacement.second;
            }
            case DENSITY -> {
                return (Float) replacement.second * res.getDisplayMetrics().density;
            }
            case ID -> {
                try {
                    Object result;
                    Class<?> resClass = res.getClass();
                    int resId = (int) replacement.second;
                    switch (methodName) {
                        case "getBoolean", "getFloat", "getInteger", "getString", "getText",
                             "getFont", "getDimension", "getDimensionPixelOffset",
                             "getDimensionPixelSize" -> {
                            result = HookInterop.getMethodInvoker(resClass, methodName, int.class)
                                .setType(XposedInterface.Invoker.Type.ORIGIN)
                                .invoke(res, resId);
                        }
                        case "getColor", "getColorStateList" -> {
                            result = HookInterop.getMethodInvoker(resClass, methodName, int.class, Resources.Theme.class)
                                .setType(XposedInterface.Invoker.Type.ORIGIN)
                                .invoke(res, resId, theme);
                        }
                        case "getDrawableForDensity" -> {
                            result = HookInterop.getMethodInvoker(resClass, methodName, int.class, int.class, Resources.Theme.class)
                                .setType(XposedInterface.Invoker.Type.ORIGIN)
                                .invoke(res, resId, params[1], theme);
                        }
                        case "getLayoutDimension" -> {
                            result = HookInterop.getMethodInvoker(resClass, "getDimensionPixelSize", int.class)
                                .setType(XposedInterface.Invoker.Type.ORIGIN)
                                .invoke(res, resId);
                        }
                        case "getFraction" -> {
                            result = HookInterop.getMethodInvoker(resClass, methodName, int.class, int.class, int.class)
                                .setType(XposedInterface.Invoker.Type.ORIGIN)
                                .invoke(res, resId, params[1], params[2]);
                        }
                        case "getInt" -> {
                            result = HookInterop.getMethodInvoker(resClass, "getInteger", int.class)
                                .setType(XposedInterface.Invoker.Type.ORIGIN)
                                .invoke(res, resId);
                        }
                        default -> result = null;
                    }
                    return result;
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return null;
    }
}