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

 * Copyright (C) 2023-2025 HChenX
 */
package com.hchen.hooktool.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Pair;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.hchen.hooktool.HCData;
import com.hchen.hooktool.core.CoreTool;
import com.hchen.hooktool.exception.InjectResourcesException;
import com.hchen.hooktool.hook.IHook;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
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
    private static String modulePath = null;
    private static Handler handler = null;
    private static final CopyOnWriteArraySet<Resources> resourceSets = new CopyOnWriteArraySet<>();
    private static final ConcurrentHashMap<String, Pair<ReplacementType, Object>> replacements = new ConcurrentHashMap<>();
    private static final CopyOnWriteArraySet<Integer> waitSet = new CopyOnWriteArraySet<>();
    private static boolean isHooked = false;

    private enum ReplacementType {
        ID,
        DENSITY,
        OBJECT
    }

    private ResInjectTool() {
    }

    @NonNull
    public static Resources injectModuleRes(@NonNull Context context) {
        return injectModuleRes(context.getResources());
    }

    @NonNull
    public static Resources injectModuleRes(@NonNull Context context, boolean mainLooper) {
        return injectModuleRes(context.getResources(), mainLooper);
    }

    @NonNull
    public static Resources injectModuleRes(@NonNull Resources resources) {
        return injectModuleRes(resources, false);
    }

    /**
     * 把本项目资源注入目标作用域上下文。一般调用本方法即可<br/>
     * 请在项目 app 下的 build.gradle 中添加如下代码：
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
    @NonNull
    public static Resources injectModuleRes(@NonNull Resources resources, boolean mainLooper) {
        if (modulePath == null) {
            modulePath = HCData.getModulePath();
            if (modulePath == null)
                throw new NullPointerException("[ResInjectTool]: Module path is null, Please set module path!");
        }

        if (resourceSets.contains(resources))
            return resources;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            injectResAboveApi30(resources, mainLooper);
        else injectResBelowApi30(resources);

        resourceSets.add(resources);

        return resources;
    }

    /**
     * 来自 QA 的方法
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    private static void injectResAboveApi30(@NonNull Resources resources, boolean mainLooper) {
        if (resourcesLoader == null) {
            try (ParcelFileDescriptor pfd =
                     ParcelFileDescriptor.open(new File(modulePath), ParcelFileDescriptor.MODE_READ_ONLY)
            ) {
                ResourcesProvider provider = ResourcesProvider.loadFromApk(pfd);
                ResourcesLoader loader = new ResourcesLoader();
                loader.addProvider(provider);
                resourcesLoader = loader;
            } catch (IOException e) {
                throw new InjectResourcesException("[ResInjectTool/injectResAboveApi30]: Failed to inject res!", e);
            }
        }
        if (mainLooper)
            if (Looper.myLooper() == Looper.getMainLooper()) {
                addLoaders(resources);
            } else {
                if (handler == null) {
                    handler = new Handler(Looper.getMainLooper());
                }
                handler.post(() -> addLoaders(resources));
            }
        else
            addLoaders(resources);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private static void addLoaders(@NonNull Resources resources) {
        try {
            resources.addLoaders(resourcesLoader);
        } catch (IllegalArgumentException e) {
            String expected = "Cannot modify resource loaders of ResourcesImpl not registered with ResourcesManager";
            if (expected.equals(e.getMessage())) {
                // fallback to below API 30
                injectResBelowApi30(resources);
            } else {
                throw new InjectResourcesException("[ResInjectTool/addLoaders]: Failed to inject res!!", e);
            }
        }
    }

    /**
     * @noinspection JavaReflectionMemberAccess
     */
    @SuppressLint("DiscouragedPrivateApi")
    private static void injectResBelowApi30(@NonNull Resources resources) {
        try {
            AssetManager assets = resources.getAssets();
            Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            Integer cookie = (Integer) addAssetPath.invoke(assets, modulePath);
            if (cookie == null || cookie == 0) {
                throw new InjectResourcesException("[ResInjectTool/injectResBelowApi30]: Method 'addAssetPath' result 0, maybe inject res failed!");
            }
        } catch (Throwable e) {
            throw new InjectResourcesException("[ResInjectTool/injectResBelowApi30]: Failed to inject res!", e);
        }
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

                injectModuleRes((Resources) thisObject()); // 注入资源
                String methodName = getMethod().getName();
                Object value = getResourceReplacement(methodName, (Resources) thisObject(), getArgs());
                if (value != null) {
                    if ("getDimensionPixelOffset".equals(methodName) || "getDimensionPixelSize".equals(methodName)) {
                        if (value instanceof Float) value = ((Float) value).intValue();
                    }
                    setResult(value);
                }
            } catch (Throwable ignore) {
                // 忽略报错
            }
        }
    };

    private static final IHook hookTypedBefore = new IHook() {
        @Override
        public void before() {
            int index = (int) getArg(0);
            index *= STYLE_NUM_ENTRIES;
            int[] data = (int[]) getThisField("mData");

            try {
                int type = data[index + STYLE_TYPE];
                int id = data[index + STYLE_RESOURCE_ID];
                if (type != TypedValue.TYPE_NULL /* 不为空数据 */ && id != 0 /* 储存的是资源 */) {
                    String methodName = getMethod().getName();
                    Resources resources = (Resources) getThisField("mResources");
                    Resources.Theme theme = (Resources.Theme) getThisField("mTheme");
                    injectModuleRes(resources); // 注入资源
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
            } catch (Throwable ignore) {
                // 忽略报错
            }
        }
    };

    @Nullable
    private static Object getResourceReplacement(@NonNull String methodName, @NonNull Resources res, @NonNull Object[] params) {
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
                    waitSet.add(resId);
                    params[0] = resId;
                    Object result = CoreTool.callMethod(res, methodName, params);
                    waitSet.remove(resId);
                    return result;
                }
            }
        }
        return null;
    }

    @Nullable
    private static Object getTypedArrayReplacement(@NonNull Resources res, @NonNull Resources.Theme theme, int id, @NonNull String methodName, Object[] params) {
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
                    waitSet.add(resId);
                    Object result = null;
                    switch (methodName) {
                        case "getBoolean", "getFloat", "getInteger", "getString", "getText",
                             "getFont", "getDimension", "getDimensionPixelOffset",
                             "getDimensionPixelSize" -> {
                            result = CoreTool.callMethod(res, methodName, resId);
                        }
                        case "getColor", "getColorStateList" -> {
                            result = CoreTool.callMethod(res, methodName, resId, theme);
                        }
                        case "getDrawableForDensity" -> {
                            result = CoreTool.callMethod(res, methodName, resId, 0, theme);
                        }
                        case "getLayoutDimension" -> {
                            result = CoreTool.callMethod(res, "getDimensionPixelSize", resId);
                        }
                        case "getFraction" -> {
                            result = CoreTool.callMethod(res, methodName, resId, params[1], params[2]);
                        }
                        case "getInt" -> {
                            result = CoreTool.callMethod(res, "getInteger", resId);
                        }
                    }
                    waitSet.remove(resId);
                    return result;
                }
            }
        }
        return null;
    }

    // 下面注入方法存在风险，可能导致资源混乱。抛弃。
    // public static Context getModuleContext(Context context)
    //         throws PackageManager.NameNotFoundException {
    //     return getModuleContext(context, null);
    // }
    //
    // public static Context getModuleContext(Context context, Configuration config)
    //         throws PackageManager.NameNotFoundException {
    //     Context mModuleContext;
    //     mModuleContext = context.createPackageContext(mProjectPkg, Context.CONTEXT_IGNORE_SECURITY).createDeviceProtectedStorageContext();
    //     return config == null ? mModuleContext : mModuleContext.createConfigurationContext(config);
    // }
    //
    // public static Resources getModuleRes(Context context)
    //         throws PackageManager.NameNotFoundException {
    //     Configuration config = context.getResources().getConfiguration();
    //     Context moduleContext = getModuleContext(context);
    //     return (config == null ? moduleContext.getResources() : moduleContext.createConfigurationContext(config).getResources());
    // }
}
