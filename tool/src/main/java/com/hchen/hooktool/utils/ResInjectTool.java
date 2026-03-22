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

import io.github.libxposed.api.XposedInterface;

/**
 * 资源注入工具
 *
 * @author 焕晨HChen
 */
public final class ResInjectTool {
    private static final String TAG = "ResInjectTool";
    private static ResourcesLoader resourcesLoader = null;
    private static final ConcurrentHashMap<String, Pair<ReplacementType, Object>> replacements = new ConcurrentHashMap<>();
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

        String sourceDir = ModuleData.getModuleApplicationInfo().sourceDir;
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

            CoreTool.hook(
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
            CoreTool.hook(
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

    @SuppressWarnings("DataFlowIssue")
    private static void applyHooks() {
        if (isHooked) return;
        if (!isInjected) {
            throw new InjectResourcesException("Should inject module res first.");
        }

        CoreTool.hook(Resources.class, "loadXmlResourceParser", int.class, String.class, hookResBefore); // XmlResourceParser
        CoreTool.hook(Resources.class, "getDimension", int.class, hookResBefore); // float
        CoreTool.hook(Resources.class, "getDimensionPixelOffset", int.class, hookResBefore); // int
        CoreTool.hook(Resources.class, "getDimensionPixelSize", int.class, hookResBefore); // int
        CoreTool.hook(Resources.class, "getBoolean", int.class, hookResBefore); // boolean
        CoreTool.hook(Resources.class, "getInteger", int.class, hookResBefore); // int
        CoreTool.hook(Resources.class, "getFloat", int.class, hookResBefore); // float
        CoreTool.hook(Resources.class, "getText", int.class, hookResBefore); // CharSequence
        CoreTool.hook(Resources.class, "getText", int.class, CharSequence.class, hookResBefore); // CharSequence
        CoreTool.hook(Resources.class, "getQuantityText", int.class, int.class, hookResBefore); // CharSequence
        CoreTool.hook(Resources.class, "getIntArray", int.class, hookResBefore); // int[]
        CoreTool.hook(Resources.class, "getStringArray", int.class, hookResBefore); // String[]
        CoreTool.hook(Resources.class, "getTextArray", int.class, hookResBefore); // CharSequence[]
        CoreTool.hook(Resources.class, "getFont", int.class, hookResBefore); // Typeface
        CoreTool.hook(Resources.class, "getMovie", int.class, hookResBefore); // Movie
        CoreTool.hook(Resources.class, "getColor", int.class, Resources.Theme.class, hookResBefore); // int
        CoreTool.hook(Resources.class, "getColorStateList", int.class, Resources.Theme.class, hookResBefore); // ColorStateList
        CoreTool.hook(Resources.class, "getFraction", int.class, int.class, int.class, hookResBefore); // float
        CoreTool.hook(Resources.class, "getDrawableForDensity", int.class, int.class, Resources.Theme.class, hookResBefore); // Drawable

        STYLE_NUM_ENTRIES = (int) CoreTool.getStaticField(TypedArray.class, "STYLE_NUM_ENTRIES");
        STYLE_TYPE = (int) CoreTool.getStaticField(TypedArray.class, "STYLE_TYPE");
        STYLE_RESOURCE_ID = (int) CoreTool.getStaticField(TypedArray.class, "STYLE_RESOURCE_ID");
        CoreTool.hook(TypedArray.class, "getColor", int.class, int.class, hookTypedBefore); // int
        CoreTool.hook(TypedArray.class, "getColorStateList", int.class, hookTypedBefore); // ColorStateList
        CoreTool.hook(TypedArray.class, "getBoolean", int.class, boolean.class, hookTypedBefore); // boolean
        CoreTool.hook(TypedArray.class, "getFloat", int.class, float.class, hookTypedBefore); // float
        CoreTool.hook(TypedArray.class, "getInt", int.class, int.class, hookTypedBefore); // int
        CoreTool.hook(TypedArray.class, "getInteger", int.class, int.class, hookTypedBefore); // int
        CoreTool.hook(TypedArray.class, "getString", int.class, hookTypedBefore); // String
        CoreTool.hook(TypedArray.class, "getText", int.class, hookTypedBefore); // CharSequence
        CoreTool.hook(TypedArray.class, "getFont", int.class, hookTypedBefore); // Typeface
        CoreTool.hook(TypedArray.class, "getDimension", int.class, float.class, hookTypedBefore); // float
        CoreTool.hook(TypedArray.class, "getDimensionPixelOffset", int.class, int.class, hookTypedBefore); // int
        CoreTool.hook(TypedArray.class, "getDimensionPixelSize", int.class, int.class, hookTypedBefore); // int
        CoreTool.hook(TypedArray.class, "getLayoutDimension", int.class, int.class, hookTypedBefore); // int
        CoreTool.hook(TypedArray.class, "getLayoutDimension", int.class, String.class, hookTypedBefore); // int
        CoreTool.hook(TypedArray.class, "getDrawableForDensity", int.class, int.class, hookTypedBefore); // Drawable
        CoreTool.hook(TypedArray.class, "getFraction", int.class, int.class, int.class, float.class, hookTypedBefore); // float

        isHooked = true;
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
                XposedLog.logD(TAG, "failed to replacement res.", t);
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

                assert data != null;
                int type = data[index + STYLE_TYPE];
                int id = data[index + STYLE_RESOURCE_ID];
                if (type != TypedValue.TYPE_NULL /* 不为空数据 */ && id != 0 /* 储存的是资源 */) {
                    String methodName = getExecutable().getName();
                    Resources resources = (Resources) CoreTool.getField(getThisObject(), "mResources");
                    Resources.Theme theme = (Resources.Theme) CoreTool.getField(getThisObject(), "mTheme");

                    assert resources != null;
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
                XposedLog.logD(TAG, "failed to replacement typed array.", t);
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
                    try {
                        params[0] = replacement.second;
                        return CoreTool.getInvoker(method)
                            .setType(XposedInterface.Invoker.Type.ORIGIN)
                            .invoke(res, params);
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        throw new InjectResourcesException(e);
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
                    try {
                        Object result;
                        Class<?> resClass = res.getClass();
                        int resId = (int) replacement.second;
                        switch (methodName) {
                            case "getBoolean", "getFloat", "getInteger", "getString", "getText",
                                 "getFont", "getDimension", "getDimensionPixelOffset",
                                 "getDimensionPixelSize" -> {
                                result = CoreTool.getInvoker(resClass, methodName, int.class)
                                    .setType(XposedInterface.Invoker.Type.ORIGIN)
                                    .invoke(res, resId);
                            }
                            case "getColor", "getColorStateList" -> {
                                result = CoreTool.getInvoker(resClass, methodName, int.class, Resources.Theme.class)
                                    .setType(XposedInterface.Invoker.Type.ORIGIN)
                                    .invoke(res, resId, theme);
                            }
                            case "getDrawableForDensity" -> {
                                result = CoreTool.getInvoker(resClass, methodName, int.class, int.class, Resources.Theme.class)
                                    .setType(XposedInterface.Invoker.Type.ORIGIN)
                                    .invoke(res, resId, params[1], theme);
                            }
                            case "getLayoutDimension" -> {
                                result = CoreTool.getInvoker(resClass, "getDimensionPixelSize", int.class)
                                    .setType(XposedInterface.Invoker.Type.ORIGIN)
                                    .invoke(res, resId);
                            }
                            case "getFraction" -> {
                                result = CoreTool.getInvoker(resClass, methodName, int.class, int.class, int.class)
                                    .setType(XposedInterface.Invoker.Type.ORIGIN)
                                    .invoke(res, resId, params[1], params[2]);
                            }
                            case "getInt" -> {
                                result = CoreTool.getInvoker(resClass, "getInteger", int.class)
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
        }
        return null;
    }
}