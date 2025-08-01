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
 * Copyright (C) 2023–2025 HChenX
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

import com.hchen.hooktool.HCData;
import com.hchen.hooktool.core.CoreTool;
import com.hchen.hooktool.exception.InjectResourcesException;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.log.XposedLog;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 资源注入工具
 *
 * @author 焕晨HChen
 */
public class ResInjectTool {
    private static final String TAG = "ResInjectTool";
    private static ResourcesLoader resourcesLoader = null;
    private static final ConcurrentHashMap<String, Pair<ReplacementType, Object>> replacements = new ConcurrentHashMap<>();
    private static final CopyOnWriteArraySet<Integer> waitSet = new CopyOnWriteArraySet<>();
    private static boolean isInjected = false;
    private static boolean isHooked = false;

    private enum ReplacementType {
        ID,
        DENSITY,
        OBJECT
    }

    private ResInjectTool() {
    }

    /**
     * 把模块资源注入至目标作用域<br/>
     * 同时请务必请在项目 app 下的 build.gradle 中添加如下代码：
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
     * Tip: `0x64` is the resource id, you can change it to any value you want.(recommended [0x30 to 0x6F])
     */
    public static void injectModuleRes() {
        if (isInjected) return;
        if (HCData.getModulePath() == null)
            throw new NullPointerException("[ResInjectTool]: Module path must not be null!!");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (resourcesLoader == null) {
                assert HCData.getModulePath() != null;
                try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(new File(HCData.getModulePath()), ParcelFileDescriptor.MODE_READ_ONLY)) {
                    ResourcesProvider provider = ResourcesProvider.loadFromApk(pfd);
                    ResourcesLoader loader = new ResourcesLoader();
                    loader.addProvider(provider);
                    resourcesLoader = loader;
                } catch (IOException e) {
                    throw new InjectResourcesException("[ResInjectTool]: Failed to create res loader!!", e);
                }
            }

            CoreTool.hookConstructor("android.content.res.ResourcesKey",
                String.class /* resDir */, String[].class /* splitResDirs */, String[].class /* overlayPaths */,
                String[].class,/* libDirs */ int.class /* overrideDisplayId */, Configuration.class /* overrideConfig */,
                "android.content.res.CompatibilityInfo" /* compatInfo */, ResourcesLoader[].class /* loader */,
                new IHook() {
                    @Override
                    public void before() {
                        ResourcesLoader[] loader = (ResourcesLoader[]) getArg(7);
                        if (loader != null) {
                            if (Arrays.stream(loader).noneMatch(l -> Objects.equals(l, resourcesLoader))) {
                                List<ResourcesLoader> loaders = new ArrayList<>(Arrays.asList(loader));
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
            CoreTool.hookConstructor("android.content.res.ResourcesKey",
                String.class /* resDir */, String[].class /* splitResDirs */, String[].class /* overlayDirs */,
                String[].class,/* libDirs */ int.class /* displayId */, Configuration.class /* overrideConfig */,
                "android.content.res.CompatibilityInfo" /* compatInfo */,
                new IHook() {
                    @Override
                    public void before() {
                        String[] splitResDirs = (String[]) getArg(1);
                        if (splitResDirs != null) {
                            if (Arrays.stream(splitResDirs).noneMatch(s -> Objects.equals(s, HCData.getModulePath()))) {
                                List<String> loaders = new ArrayList<>(Arrays.asList(splitResDirs));
                                loaders.add(HCData.getModulePath());
                                setArg(1, loaders.toArray(new String[0]));
                            }
                        } else {
                            setArg(1, new String[]{HCData.getModulePath()});
                        }
                    }
                }
            );
        }

        isInjected = true;
    }

    public static int createFakeResId(@NonNull String resName) {
        return 0x7e000000 | (fnv1a32Hash(resName) & 0x00ffffff);
    }

    public static int createFakeResId(@NonNull Resources res, int id) {
        return createFakeResId(res.getResourceName(id));
    }

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
     * 设置资源 ID 类型的替换
     */
    public static void setResReplacement(@NonNull String packageName, @NonNull String type, @NonNull String resName, int replacementResId) {
        applyHooks();
        replacements.put(packageName + ":" + type + "/" + resName, new Pair<>(ReplacementType.ID, replacementResId));
    }

    /**
     * 设置密度类型的资源
     */
    public static void setDensityReplacement(@NonNull String packageName, @NonNull String type, @NonNull String resName, float replacementResValue) {
        applyHooks();
        replacements.put(packageName + ":" + type + "/" + resName, new Pair<>(ReplacementType.DENSITY, replacementResValue));
    }

    /**
     * 设置 Object 类型的资源
     */
    public static void setObjectReplacement(@NonNull String packageName, @NonNull String type, @NonNull String resName, Object replacementResValue) {
        applyHooks();
        replacements.put(packageName + ":" + type + "/" + resName, new Pair<>(ReplacementType.OBJECT, replacementResValue));
    }

    private static int STYLE_NUM_ENTRIES;
    private static int STYLE_TYPE;
    private static int STYLE_RESOURCE_ID;

    private static void applyHooks() {
        if (isHooked) return;
        if (!isInjected) {
            throw new InjectResourcesException("[ResInjectTool]: You should inject module res first!!");
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

        isHooked = true;
    }

    private static final IHook hookResBefore = new IHook() {
        @Override
        public void before() {
            try {
                Integer resId = (Integer) getArg(0);
                if (waitSet.contains(resId)) return;

                String methodName = getMethod().getName();
                Object value = getResourceReplacement((Method) getMethod(), (Resources) thisObject(), getArgs());
                if (value != null) {
                    if ("getDimensionPixelOffset".equals(methodName) || "getDimensionPixelSize".equals(methodName)) {
                        if (value instanceof Float) value = ((Float) value).intValue();
                    }
                    setResult(value);
                }
            } catch (Throwable t) {
                XposedLog.logD(TAG, "Failed to replacement res!!", t);
            }
        }
    };

    private static final IHook hookTypedBefore = new IHook() {
        @Override
        public void before() {
            try {
                int index = (int) getArg(0);
                index *= STYLE_NUM_ENTRIES;
                int[] data = (int[]) getThisField("mData");

                int type = data[index + STYLE_TYPE];
                int id = data[index + STYLE_RESOURCE_ID];
                if (type != TypedValue.TYPE_NULL /* 不为空数据 */ && id != 0 /* 储存的是资源 */) {
                    String methodName = getMethod().getName();
                    Resources resources = (Resources) getThisField("mResources");
                    Resources.Theme theme = (Resources.Theme) getThisField("mTheme");
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
                XposedLog.logD(TAG, "Failed to replacement typed array!!", t);
            }
        }
    };

    @Nullable
    private static Object getResourceReplacement(@NonNull Method method, @NonNull Resources res, @NonNull Object[] params) {
        String pkgName = null;
        String resType = null;
        String resName = null;
        try {
            pkgName = res.getResourcePackageName((int) params[0]);
            resType = res.getResourceTypeName((int) params[0]);
            resName = res.getResourceEntryName((int) params[0]);
        } catch (Throwable ignore) {
        }

        if (pkgName == null || resType == null || resName == null) return null;

        String resFullName = pkgName + ":" + resType + "/" + resName;
        String resAnyPkgName = "*:" + resType + "/" + resName;

        Pair<ReplacementType, Object> replacement = null;
        if (replacements.containsKey(resFullName)) {
            replacement = replacements.get(resFullName);
        } else if (replacements.containsKey(resAnyPkgName)) {
            replacement = replacements.get(resAnyPkgName);
        }
        if (replacement != null) {
            switch (replacement.first) {
                case OBJECT -> {
                    return replacement.second;
                }
                case DENSITY -> {
                    return (Float) replacement.second * res.getDisplayMetrics().density;
                }
                case ID -> {
                    int resId = (int) replacement.second;
                    try {
                        waitSet.add(resId);
                        params[0] = resId;
                        return CoreTool.callMethod(res, method, params);
                    } finally {
                        waitSet.remove(resId);
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private static Object getTypedArrayReplacement(@NonNull Resources res, @Nullable Resources.Theme theme, int id, @NonNull String methodName, @NonNull Object[] params) {
        String pkgName = null;
        String resType = null;
        String resName = null;
        try {
            pkgName = res.getResourcePackageName(id);
            resType = res.getResourceTypeName(id);
            resName = res.getResourceEntryName(id);
        } catch (Throwable ignore) {
        }

        if (pkgName == null || resType == null || resName == null) return null;

        String resFullName = pkgName + ":" + resType + "/" + resName;
        String resAnyPkgName = "*:" + resType + "/" + resName;

        Pair<ReplacementType, Object> replacement = null;
        if (replacements.containsKey(resFullName)) {
            replacement = replacements.get(resFullName);
        } else if (replacements.containsKey(resAnyPkgName)) {
            replacement = replacements.get(resAnyPkgName);
        }
        if (replacement != null) {
            switch (replacement.first) {
                case OBJECT -> {
                    return replacement.second;
                }
                case DENSITY -> {
                    return (Float) replacement.second * res.getDisplayMetrics().density;
                }
                case ID -> {
                    int resId = (int) replacement.second;
                    try {
                        Object result;
                        waitSet.add(resId);
                        switch (methodName) {
                            case "getBoolean", "getFloat", "getInteger", "getString", "getText",
                                 "getFont", "getDimension", "getDimensionPixelOffset",
                                 "getDimensionPixelSize" -> {
                                result = CoreTool.callMethod(res, methodName, new Class<?>[]{int.class}, resId);
                            }
                            case "getColor", "getColorStateList" -> {
                                result = CoreTool.callMethod(res, methodName, new Class<?>[]{int.class, Resources.Theme.class}, resId, theme);
                            }
                            case "getDrawableForDensity" -> {
                                result = CoreTool.callMethod(res, methodName, new Class<?>[]{int.class, int.class, Resources.Theme.class}, resId, params[1], theme);
                            }
                            case "getLayoutDimension" -> {
                                result = CoreTool.callMethod(res, "getDimensionPixelSize", new Class<?>[]{int.class}, resId);
                            }
                            case "getFraction" -> {
                                result = CoreTool.callMethod(res, methodName, new Class<?>[]{int.class, int.class, int.class}, resId, params[1], params[2]);
                            }
                            case "getInt" -> {
                                result = CoreTool.callMethod(res, "getInteger", new Class<?>[]{int.class}, resId);
                            }
                            default -> result = null;
                        }
                        return result;
                    } finally {
                        waitSet.remove(resId);
                    }
                }
            }
        }
        return null;
    }
}