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
package com.hchen.hooktool.utils;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Pair;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.ModuleData;
import com.hchen.hooktool.core.CoreTool;
import com.hchen.hooktool.exception.InjectResourcesException;
import com.hchen.hooktool.hook.AbsHook;
import com.hchen.hooktool.log.XposedLog;

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

import io.github.libxposed.api.XposedInterface;

/**
 * 资源注入与运行时替换工具类。
 * <p>
 * 提供在 Xposed 环境下将模块 APK 资源注入目标应用资源加载链的能力，同时支持按资源 ID、密度值或任意对象
 * 在运行时替换目标应用的资源访问结果。此外还提供基于 FNV-1a 哈希算法的伪资源 ID 生成功能。
 * <p>
 * 使用前需在模块的 {@code build.gradle} 中配置自定义 package-id（推荐范围 0x30 至 0x6F），
 * 以避免与目标应用的资源 ID 发生冲突。
 *
 * @author 焕晨HChen
 */
public final class ResInjectTool {
    private static final String TAG = "ResInjectTool";
    private static volatile ResourcesLoader resourcesLoader = null;
    private static final ConcurrentHashMap<String, Pair<ReplacementType, Object>> replacements = new ConcurrentHashMap<>();
    private static final AtomicBoolean isInjected = new AtomicBoolean(false);
    private static final AtomicBoolean isHooked = new AtomicBoolean(false);

    /**
     * 资源替换类型枚举。
     */
    private enum ReplacementType {
        /** 按资源 ID 替换 */
        ID,
        /** 按密度值替换 */
        DENSITY,
        /** 按任意对象替换 */
        OBJECT
    }

    private ResInjectTool() {
    }

    /**
     * 将模块的 APK 资源注入到目标应用的资源加载流程中。
     * <p>
     * 在 Android R（API 30）及以上版本中，通过创建 {@link ResourcesLoader} 并 hook
     * {@code android.content.res.ResourcesKey} 构造函数，将模块资源注入资源搜索链；
     * 在更低版本中则通过修改 {@code splitResDirs} 参数实现注入。此方法仅首次调用生效，后续调用将被忽略。
     * <p>
     * 使用前务必在模块的 {@code build.gradle} 中添加如下配置：
     * <pre> {@code
     * Kotlin Gradle DSL:
     *
     * androidResources.additionalParameters("--allow-reserved-package-id", "--package-id", "0x64")
     *
     * Groovy:
     *
     * aaptOptions.additionalParameters '--allow-reserved-package-id', '--package-id', '0x64'
     *
     * }<br/>
     * Tip: `0x64` 为资源 ID 前缀，可自行修改（推荐范围 0x30 至 0x6F）。
     *
     * @throws InjectResourcesException 创建 {@link ResourcesLoader} 或 {@link ParcelFileDescriptor} 失败时抛出
     */
    public static void injectModuleRes() {
        if (!isInjected.compareAndSet(false, true)) return;

        String sourceDir = ModuleData.getModulePath();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (resourcesLoader == null) {
                try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(new File(sourceDir), ParcelFileDescriptor.MODE_READ_ONLY)) {
                    ResourcesProvider provider = ResourcesProvider.loadFromApk(pfd);
                    ResourcesLoader loader = new ResourcesLoader();
                    loader.addProvider(provider);
                    resourcesLoader = loader;
                } catch (IOException e) {
                    throw new InjectResourcesException("Failed to create res loader.", e);
                }
            }

            CoreTool.hookConstructor(
                "android.content.res.ResourcesKey",
                String.class /* resDir */, String[].class /* splitResDirs */, String[].class /* overlayPaths */,
                String[].class,/* libDirs */ int.class /* overrideDisplayId */, Configuration.class /* overrideConfig */,
                "android.content.res.CompatibilityInfo" /* compatInfo */, ResourcesLoader[].class /* loader */,
                new AbsHook() {
                    @Override
                    public void before() {
                        ResourcesLoader[] loader = (ResourcesLoader[]) getArg(7);
                        if (loader != null) {
                            List<ResourcesLoader> loaders = new ArrayList<>(Arrays.asList(loader));
                            if (!loaders.contains(resourcesLoader)) {
                                loaders.add(resourcesLoader);
                                setArg(7, loaders.toArray(new ResourcesLoader[0]));
                            }
                        } else {
                            setArg(7, new ResourcesLoader[]{resourcesLoader});
                        }
                    }
                }
            );
        } else {
            CoreTool.hookConstructor(
                "android.content.res.ResourcesKey",
                String.class /* resDir */, String[].class /* splitResDirs */, String[].class /* overlayDirs */,
                String[].class,/* libDirs */ int.class /* displayId */, Configuration.class /* overrideConfig */,
                "android.content.res.CompatibilityInfo" /* compatInfo */,
                new AbsHook() {
                    @Override
                    public void before() {
                        String[] splitResDirs = (String[]) getArg(1);
                        if (splitResDirs != null) {
                            List<String> loaders = new ArrayList<>(Arrays.asList(splitResDirs));
                            if (!loaders.contains(sourceDir)) {
                                loaders.add(sourceDir);
                                setArg(1, loaders.toArray(new String[0]));
                            }
                        } else {
                            setArg(1, new String[]{sourceDir});
                        }
                    }
                }
            );
        }

    }

    /**
     * 根据资源名称字符串生成一个伪资源 ID。
     * <p>
     * 使用 FNV-1a 32 位哈希算法对资源名称进行哈希运算，然后与 {@code 0x7e000000} 取或，生成以
     * {@code 0x7e} 为包 ID 前缀的伪资源 ID。
     *
     * @param resName 资源名称字符串
     * @return 生成的伪资源 ID
     */
    public static int createFakeResId(@NonNull String resName) {
        return 0x7e000000 | (fnv1a32Hash(resName) & 0x00ffffff);
    }

    /**
     * 根据已有资源 ID 对应的资源名称生成伪资源 ID。
     * <p>
     * 先通过 {@link Resources#getResourceName(int)} 获取原始资源的全限定名称，再调用
     * {@link #createFakeResId(String)} 生成伪 ID。
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
     * <p>
     * 对输入字符串的 UTF-8 字节序列逐字节进行异或和乘法运算，返回 32 位哈希值。
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
     * 当目标应用访问指定资源时，将其重定向到模块提供的替换资源 ID。首次调用时会自动触发 hook 注入。
     *
     * @param packageName      目标应用包名；使用 {@code "*"} 可匹配所有应用
     * @param type             资源类型（如 {@code "drawable"}、{@code "string"}、{@code "color"} 等）
     * @param resName          资源名称
     * @param replacementResId 用作替换的模块资源 ID
     */
    public static void setResReplacement(@NonNull String packageName, @NonNull String type, @NonNull String resName, int replacementResId) {
        applyHooks();
        replacements.put(packageName + ":" + type + "/" + resName, new Pair<>(ReplacementType.ID, replacementResId));
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
        replacements.put(packageName + ":" + type + "/" + resName, new Pair<>(ReplacementType.DENSITY, replacementResValue));
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
        replacements.put(packageName + ":" + type + "/" + resName, new Pair<>(ReplacementType.OBJECT, replacementResValue));
    }

    private static int STYLE_NUM_ENTRIES;
    private static int STYLE_TYPE;
    private static int STYLE_RESOURCE_ID;

    /**
     * 对 {@link Resources} 和 {@link TypedArray} 的各类资源获取方法进行 hook，使其支持运行时替换。
     * <p>
     * 仅首次调用生效。hook 范围覆盖以下资源类型：
     * <ul>
     *     <li>XmlResourceParser</li>
     *     <li>尺寸（getDimension、getDimensionPixelOffset、getDimensionPixelSize）</li>
     *     <li>布尔值、整型、浮点数</li>
     *     <li>字符串、文本</li>
     *     <li>颜色、ColorStateList</li>
     *     <li>字体、Drawable、Movie</li>
     *     <li>整型数组、字符串数组、文本数组</li>
     *     <li>百分比（Fraction）</li>
     * </ul>
     *
     * @throws InjectResourcesException 未先调用 {@link #injectModuleRes()} 或读取 {@link TypedArray} 内部常量失败时抛出
     */
    @SuppressWarnings("DataFlowIssue")
    private static void applyHooks() {
        if (!isHooked.compareAndSet(false, true)) return;
        if (!isInjected.get()) {
            isHooked.set(false);
            throw new InjectResourcesException("Should inject module res first.");
        }

        CoreTool.hookMethod(Resources.class, "loadXmlResourceParser", int.class, String.class, hookResBefore); // XmlResourceParser
        CoreTool.hookMethod(Resources.class, "getDimension", int.class, hookResBefore); // float
        CoreTool.hookMethod(Resources.class, "getDimensionPixelOffset", int.class, hookResBefore); // int
        CoreTool.hookMethod(Resources.class, "getDimensionPixelSize", int.class, hookResBefore); // int
        CoreTool.hookMethod(Resources.class, "getBoolean", int.class, hookResBefore); // boolean
        CoreTool.hookMethod(Resources.class, "getInteger", int.class, hookResBefore); // int
        CoreTool.hookMethod(Resources.class, "getFloat", int.class, hookResBefore); // float
        CoreTool.hookMethod(Resources.class, "getText", int.class, hookResBefore); // CharSequence
        CoreTool.hookMethod(Resources.class, "getText", int.class, CharSequence.class, hookResBefore); // CharSequence
        CoreTool.hookMethod(Resources.class, "getQuantityText", int.class, int.class, hookResBefore); // CharSequence
        CoreTool.hookMethod(Resources.class, "getIntArray", int.class, hookResBefore); // int[]
        CoreTool.hookMethod(Resources.class, "getStringArray", int.class, hookResBefore); // String[]
        CoreTool.hookMethod(Resources.class, "getTextArray", int.class, hookResBefore); // CharSequence[]
        CoreTool.hookMethod(Resources.class, "getFont", int.class, hookResBefore); // Typeface
        CoreTool.hookMethod(Resources.class, "getMovie", int.class, hookResBefore); // Movie
        CoreTool.hookMethod(Resources.class, "getColor", int.class, Resources.Theme.class, hookResBefore); // int
        CoreTool.hookMethod(Resources.class, "getColorStateList", int.class, Resources.Theme.class, hookResBefore); // ColorStateList
        CoreTool.hookMethod(Resources.class, "getFraction", int.class, int.class, int.class, hookResBefore); // float
        CoreTool.hookMethod(Resources.class, "getDrawableForDensity", int.class, int.class, Resources.Theme.class, hookResBefore); // Drawable

        STYLE_NUM_ENTRIES = (int) CoreTool.getStaticField(TypedArray.class, "STYLE_NUM_ENTRIES");
        STYLE_TYPE = (int) CoreTool.getStaticField(TypedArray.class, "STYLE_TYPE");
        STYLE_RESOURCE_ID = (int) CoreTool.getStaticField(TypedArray.class, "STYLE_RESOURCE_ID");
        if (STYLE_NUM_ENTRIES <= 0) {
            throw new InjectResourcesException("Failed to read STYLE_NUM_ENTRIES from TypedArray.");
        }
        CoreTool.hookMethod(TypedArray.class, "getColor", int.class, int.class, hookTypedBefore); // int
        CoreTool.hookMethod(TypedArray.class, "getColorStateList", int.class, hookTypedBefore); // ColorStateList
        CoreTool.hookMethod(TypedArray.class, "getBoolean", int.class, boolean.class, hookTypedBefore); // boolean
        CoreTool.hookMethod(TypedArray.class, "getFloat", int.class, float.class, hookTypedBefore); // float
        CoreTool.hookMethod(TypedArray.class, "getInt", int.class, int.class, hookTypedBefore); // int
        CoreTool.hookMethod(TypedArray.class, "getInteger", int.class, int.class, hookTypedBefore); // int
        CoreTool.hookMethod(TypedArray.class, "getString", int.class, hookTypedBefore); // String
        CoreTool.hookMethod(TypedArray.class, "getText", int.class, hookTypedBefore); // CharSequence
        CoreTool.hookMethod(TypedArray.class, "getFont", int.class, hookTypedBefore); // Typeface
        CoreTool.hookMethod(TypedArray.class, "getDimension", int.class, float.class, hookTypedBefore); // float
        CoreTool.hookMethod(TypedArray.class, "getDimensionPixelOffset", int.class, int.class, hookTypedBefore); // int
        CoreTool.hookMethod(TypedArray.class, "getDimensionPixelSize", int.class, int.class, hookTypedBefore); // int
        CoreTool.hookMethod(TypedArray.class, "getLayoutDimension", int.class, int.class, hookTypedBefore); // int
        CoreTool.hookMethod(TypedArray.class, "getLayoutDimension", int.class, String.class, hookTypedBefore); // int
        CoreTool.hookMethod(TypedArray.class, "getDrawableForDensity", int.class, int.class, hookTypedBefore); // Drawable
        CoreTool.hookMethod(TypedArray.class, "getFraction", int.class, int.class, int.class, float.class, hookTypedBefore); // float

    }

    private static final AbsHook hookResBefore = new AbsHook() {
        @Override
        public void before() {
            try {
                String methodName = getExecutable().getName();
                Object value = getResourceReplacement((Method) getExecutable(), (Resources) getThisObject(), getArgs());
                if (value != null) {
                    if ("getDimensionPixelOffset".equals(methodName) || "getDimensionPixelSize".equals(methodName)) {
                        if (value instanceof Float) value = ((Float) value).intValue();
                    }
                    setResult(value);
                }
            } catch (Throwable t) {
                XposedLog.logD(TAG, "Failed to replacement res.", t);
            }
        }
    };

    private static final AbsHook hookTypedBefore = new AbsHook() {
        @Override
        public void before() {
            try {
                int index = (int) getArg(0);
                index *= STYLE_NUM_ENTRIES;
                int[] data = (int[]) CoreTool.getField(getThisObject(), "mData");
                if (data == null) return;

                assert data != null;
                int type = data[index + STYLE_TYPE];
                int id = data[index + STYLE_RESOURCE_ID];
                if (type != TypedValue.TYPE_NULL /* 不为空数据 */ && id != 0 /* 储存的是资源 */) {
                    String methodName = getExecutable().getName();
                    Resources resources = (Resources) CoreTool.getField(getThisObject(), "mResources");
                    if (resources == null) return;
                    Resources.Theme theme = (Resources.Theme) CoreTool.getField(getThisObject(), "mTheme");

                    Object value = getTypedArrayReplacement(resources, theme, id, methodName, getArgs());
                    if (value != null) {
                        if (
                            "getDimensionPixelOffset".equals(methodName) ||
                                "getDimensionPixelSize".equals(methodName) ||
                                "getLayoutDimension".equals(methodName)
                        ) {
                            if (value instanceof Float) value = ((Float) value).intValue();
                        }
                        setResult(value);
                    }
                }
            } catch (Throwable t) {
                XposedLog.logD(TAG, "Failed to replacement typed array.", t);
            }
        }
    };

    /**
     * 在已注册的替换规则中查找与指定资源 ID 匹配的规则。
     * <p>
     * 查找策略：先通过 {@link Resources} API 解析资源的包名、类型和名称，构建全限定资源名进行精确匹配；
     * 若未命中，再尝试使用通配包名（{@code "*"}）进行匹配。
     *
     * @param res   {@link Resources} 实例
     * @param resId 资源 ID
     * @return 匹配到的替换规则（类型 + 值），未找到返回 {@code null}
     */
    @Nullable
    private static Pair<ReplacementType, Object> findReplacement(@NonNull Resources res, int resId) {
        String pkgName = null;
        String resType = null;
        String resName = null;
        try {
            pkgName = res.getResourcePackageName(resId);
            resType = res.getResourceTypeName(resId);
            resName = res.getResourceEntryName(resId);
        } catch (Throwable ignore) {
        }

        if (pkgName == null || resType == null || resName == null) return null;

        String resFullName = pkgName + ":" + resType + "/" + resName;
        Pair<ReplacementType, Object> replacement = replacements.get(resFullName);
        if (replacement == null) {
            replacement = replacements.get("*:" + resType + "/" + resName);
        }
        return replacement;
    }

    /**
     * 执行 {@link Resources} 级别的资源替换逻辑。
     * <p>
     * 根据替换规则的类型进行不同的处理：
     * <ul>
     *     <li>{@code OBJECT} —— 直接返回对象值</li>
     *     <li>{@code DENSITY} —— 返回经密度换算后的值</li>
     *     <li>{@code ID} —— 以替换后的资源 ID 调用原始方法并返回结果</li>
     * </ul>
     *
     * @param method 被 hook 的原始方法
     * @param res    {@link Resources} 实例
     * @param params 方法参数；第一个元素为资源 ID
     * @return 替换后的值；无匹配规则时返回 {@code null}
     */
    @Nullable
    private static Object getResourceReplacement(@NonNull Method method, @NonNull Resources res, @NonNull Object[] params) {
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
                    return CoreTool.getMethodInvoker(method)
                        .setType(XposedInterface.Invoker.Type.ORIGIN)
                        .invoke(res, params);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new InjectResourcesException(e);
                }
            }
        }
        return null;
    }

    /**
     * 执行 {@link TypedArray} 级别的资源替换逻辑。
     * <p>
     * 根据替换规则类型和被 hook 的方法名称，选择对应的 {@link Resources} API 进行调用，确保返回类型
     * 与原始方法一致。对于 {@code ID} 类型替换，会根据不同方法名（如 {@code getColor}、
     * {@code getDrawableForDensity} 等）分发到对应的 Resources 方法。
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
                            result = CoreTool.getMethodInvoker(resClass, methodName, int.class)
                                .setType(XposedInterface.Invoker.Type.ORIGIN)
                                .invoke(res, resId);
                        }
                        case "getColor", "getColorStateList" -> {
                            result = CoreTool.getMethodInvoker(resClass, methodName, int.class, Resources.Theme.class)
                                .setType(XposedInterface.Invoker.Type.ORIGIN)
                                .invoke(res, resId, theme);
                        }
                        case "getDrawableForDensity" -> {
                            result = CoreTool.getMethodInvoker(resClass, methodName, int.class, int.class, Resources.Theme.class)
                                .setType(XposedInterface.Invoker.Type.ORIGIN)
                                .invoke(res, resId, params[1], theme);
                        }
                        case "getLayoutDimension" -> {
                            result = CoreTool.getMethodInvoker(resClass, "getDimensionPixelSize", int.class)
                                .setType(XposedInterface.Invoker.Type.ORIGIN)
                                .invoke(res, resId);
                        }
                        case "getFraction" -> {
                            result = CoreTool.getMethodInvoker(resClass, methodName, int.class, int.class, int.class)
                                .setType(XposedInterface.Invoker.Type.ORIGIN)
                                .invoke(res, resId, params[1], params[2]);
                        }
                        case "getInt" -> {
                            result = CoreTool.getMethodInvoker(resClass, "getInteger", int.class)
                                .setType(XposedInterface.Invoker.Type.ORIGIN)
                                .invoke(res, resId);
                        }
                        default -> result = null;
                    }
                    return result;
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new InjectResourcesException(e);
                }
            }
        }
        return null;
    }
}
