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

import static com.hchen.hooktool.utils.ContextTool.FLAG_ALL;

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
    private static String modulePath = null;
    private static Handler handler = null;
    private static final CopyOnWriteArraySet<Resources> resourceSets = new CopyOnWriteArraySet<>();
    private static final ConcurrentHashMap<Integer, Boolean> resMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Pair<ReplacementType, Object>> replacements = new ConcurrentHashMap<>();
    private static boolean isHooked = false;

    private enum ReplacementType {
        ID,
        DENSITY,
        OBJECT
    }

    private ResInjectTool() {
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

        boolean load;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            load = injectResAboveApi30(resources, mainLooper);
        } else {
            load = injectResBelowApi30(resources);
        }
        // if (!load) {
        //     try {
        //         return getModuleRes(context);
        //     } catch (PackageManager.NameNotFoundException e) {
        //         logE(tag(), "failed to load resource! critical error!! scope may crash!!", e);
        //     }
        // }
        resourceSets.add(resources);
        return resources;
    }

    @NonNull
    public static Resources injectModuleRes(@NonNull Resources resources) {
        return injectModuleRes(resources, false);
    }

    @NonNull
    public static Resources injectModuleRes(@NonNull Context context) {
        return injectModuleRes(context, false);
    }

    @NonNull
    public static Resources injectModuleRes(@NonNull Context context, boolean mainLooper) {
        return injectModuleRes(context.getResources(), mainLooper);
    }

    /**
     * 来自 QA 的方法
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    private static boolean injectResAboveApi30(@NonNull Resources resources, boolean mainLooper) {
        if (resourcesLoader == null) {
            try (ParcelFileDescriptor pfd =
                     ParcelFileDescriptor.open(new File(modulePath), ParcelFileDescriptor.MODE_READ_ONLY)
            ) {
                ResourcesProvider provider = ResourcesProvider.loadFromApk(pfd);
                ResourcesLoader loader = new ResourcesLoader();
                loader.addProvider(provider);
                resourcesLoader = loader;
            } catch (IOException e) {
                throw new InjectResourcesException("[ResInjectTool]: Failed to inject res! debug: above api 30.", e);
            }
        }
        if (mainLooper)
            if (Looper.myLooper() == Looper.getMainLooper()) {
                return addLoaders(resources);
            } else {
                if (handler == null) {
                    handler = new Handler(Looper.getMainLooper());
                }
                handler.post(() -> addLoaders(resources));
                return true; // 此状态下保持返回 true，请观察日志是否有报错来判断是否成功。
            }
        else
            return addLoaders(resources);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private static boolean addLoaders(@NonNull Resources resources) {
        try {
            resources.addLoaders(resourcesLoader);
        } catch (IllegalArgumentException e) {
            String expected = "Cannot modify resource loaders of ResourcesImpl not registered with ResourcesManager";
            if (expected.equals(e.getMessage())) {
                // fallback to below API 30
                return injectResBelowApi30(resources);
            } else {
                throw new InjectResourcesException("[ResInjectTool]: Failed to add loaders!", e);
            }
        }
        return true;
    }

    /**
     * @noinspection JavaReflectionMemberAccess
     */
    @SuppressLint("DiscouragedPrivateApi")
    private static boolean injectResBelowApi30(@NonNull Resources resources) {
        try {
            AssetManager assets = resources.getAssets();
            Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            Integer cookie = (Integer) addAssetPath.invoke(assets, modulePath);
            if (cookie == null || cookie == 0) {
                throw new InjectResourcesException("[ResInjectTool]: Method 'addAssetPath' result 0, maybe inject res failed!");
            }
        } catch (Throwable e) {
            throw new InjectResourcesException("[ResInjectTool]: Failed to inject res! debug: above api 30.", e);
        }
        return true;
    }

    public static int createFakeResId(@NonNull String resName) {
        return 0x7e000000 | (fnv1a32Hash(resName) & 0x00ffffff);
    }

    public static int createFakeResId(@NonNull Resources res, int id) {
        return createFakeResId(res.getResourceName(id));
    }

    private static int fnv1a32Hash(String str) {
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

    private static void applyHooks() {
        if (isHooked) return;

        Method[] resMethods = Resources.class.getDeclaredMethods();
        for (Method method : resMethods) {
            String name = method.getName();
            switch (name) {
                case "getInteger", "getLayout", "getBoolean", "getDimension",
                     "getDimensionPixelOffset", "getDimensionPixelSize", "getText", "getFloat",
                     "getIntArray", "getStringArray", "getTextArray", "getAnimation" -> {
                    if (method.getParameterCount() == 1 && Objects.equals(method.getParameterTypes()[0], int.class)) {
                        hookResMethod(method.getName(), int.class, hookResBefore);
                    }
                }
                case "getColor" -> {
                    if (method.getParameterCount() == 2) {
                        hookResMethod(method.getName(), int.class, Resources.Theme.class, hookResBefore);
                    }
                }
                case "getFraction" -> {
                    if (method.getParameterCount() == 3) {
                        hookResMethod(method.getName(), int.class, int.class, int.class, hookResBefore);
                    }
                }
                case "getDrawableForDensity" -> {
                    if (method.getParameterCount() == 3) {
                        hookResMethod(method.getName(), int.class, int.class, Resources.Theme.class, hookResBefore);
                    }
                }
            }
        }

        Method[] typedMethod = TypedArray.class.getDeclaredMethods();
        for (Method method : typedMethod) {
            if (Objects.equals(method.getName(), "getColor")) {
                hookTypedMethod(method.getName(), int.class, int.class, hookTypedBefore);
            }
        }
        isHooked = true;
    }

    private static void hookResMethod(String methodName, Object... params) {
        CoreTool.hookMethod(Resources.class, methodName, params);
    }

    private static void hookTypedMethod(String methodName, Object... params) {
        CoreTool.hookMethod(TypedArray.class, methodName, params);
    }

    private static final IHook hookTypedBefore = new IHook() {
        @Override
        public void before() {
            int[] mData = (int[]) getThisField("mData");
            if (mData == null) return;

            int index = (int) getArg(0);
            if (index < 0 || index >= mData.length - 3) return;

            int type = mData[index];
            int id = mData[index + 3];

            if (id != 0 && (type != TypedValue.TYPE_NULL)) {
                Resources resources = (Resources) getThisField("mResources");
                Object value = getTypedArrayReplacement(resources, id);
                if (value != null) setResult(value);
            }
        }
    };

    private static final IHook hookResBefore = new IHook() {
        @Override
        public void before() {
            int id = (int) getArg(0);
            if (Boolean.TRUE.equals(resMap.get(id))) return;

            if (resourceSets.isEmpty())
                resourceSets.add(injectModuleRes(ContextTool.getContext(FLAG_ALL)));
            for (Resources resources : resourceSets) {
                if (resources == null) return;
                String methodName = getMember().getName();
                Object value;
                try {
                    value = getResourceReplacement(resources, (Resources) thisObject(), methodName, getArgs());
                } catch (Resources.NotFoundException ignore) {
                    continue;
                }
                if (value != null) {
                    if ("getDimensionPixelOffset".equals(methodName) || "getDimensionPixelSize".equals(methodName)) {
                        if (value instanceof Float) value = ((Float) value).intValue();
                    }
                    setResult(value);
                    break;
                }
            }
        }
    };

    @Nullable
    private static Object getResourceReplacement(@NonNull Resources appResources, @NonNull Resources thisRes,
                                                 @NonNull String methodName, @NonNull Object[] params) throws Resources.NotFoundException {
        String pkgName = null;
        String resType = null;
        String resName = null;
        try {
            pkgName = thisRes.getResourcePackageName((int) params[0]);
            resType = thisRes.getResourceTypeName((int) params[0]);
            resName = thisRes.getResourceEntryName((int) params[0]);
        } catch (Throwable ignore) {
        }
        if (pkgName == null || resType == null || resName == null) return null;

        String resFullName = pkgName + ":" + resType + "/" + resName;
        String resAnyPkgName = "*:" + resType + "/" + resName;

        Object value;
        Integer modResId;
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
                    return (Float) replacement.second * thisRes.getDisplayMetrics().density;
                }
                case ID -> {
                    modResId = (Integer) replacement.second;
                    if (modResId == 0) return null;

                    try {
                        appResources.getResourceName(modResId);
                    } catch (Resources.NotFoundException ignore) {
                        injectModuleRes(appResources);
                        appResources.getResourceName(modResId);
                    }

                    resMap.put(modResId, true);
                    if ("getDrawable".equals(methodName))
                        value = CoreTool.callMethod(appResources, methodName, modResId, params[1]);
                    else if ("getDrawableForDensity".equals(methodName) || "getFraction".equals(methodName))
                        value = CoreTool.callMethod(appResources, methodName, modResId, params[1], params[2]);
                    else
                        value = CoreTool.callMethod(appResources, methodName, modResId);
                    resMap.remove(modResId);
                    return value;
                }
            }
        }
        return null;
    }

    @Nullable
    private static Object getTypedArrayReplacement(@NonNull Resources resources, int id) {
        if (id != 0) {
            String pkgName = null;
            String resType = null;
            String resName = null;
            try {
                pkgName = resources.getResourcePackageName(id);
                resType = resources.getResourceTypeName(id);
                resName = resources.getResourceEntryName(id);
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
            if (replacement != null && (replacement.first == ReplacementType.OBJECT)) {
                return replacement.second;
            }
        }
        return null;
    }

    // 下面注入方法存在风险，可能导致资源混乱，抛弃。
    /*public static Context getModuleContext(Context context)
            throws PackageManager.NameNotFoundException {
        return getModuleContext(context, null);
    }

    public static Context getModuleContext(Context context, Configuration config)
            throws PackageManager.NameNotFoundException {
        Context mModuleContext;
        mModuleContext = context.createPackageContext(mProjectPkg, Context.CONTEXT_IGNORE_SECURITY).createDeviceProtectedStorageContext();
        return config == null ? mModuleContext : mModuleContext.createConfigurationContext(config);
    }

    public static Resources getModuleRes(Context context)
            throws PackageManager.NameNotFoundException {
        Configuration config = context.getResources().getConfiguration();
        Context moduleContext = getModuleContext(context);
        return (config == null ? moduleContext.getResources() : moduleContext.createConfigurationContext(config).getResources());
    }*/
}
