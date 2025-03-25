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
package com.hchen.hooktool.tool.additional;

import static com.hchen.hooktool.log.LogExpand.createRuntimeExceptionMsg;
import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.LogExpand.getTag;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;
import static com.hchen.hooktool.tool.additional.ContextTool.FLAG_ALL;

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
import androidx.annotation.RequiresApi;

import com.hchen.hooktool.HCData;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.tool.CoreTool;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;

/**
 * 资源注入工具
 *
 * @author 焕晨HChen
 */
public final class ResInjectTool {
    private static ResourcesLoader mResourcesLoader = null;
    private static String mModulePath = null;
    private static Handler mHandler = null;

    /**
     * 请在 {@link  com.hchen.hooktool.HCInit#initStartupParam(IXposedHookZygoteInit.StartupParam)} 处调用。
     *
     * @param modulePath startupParam.modulePath 即可
     */
    public static void init(String modulePath) {
        mModulePath = modulePath;
    }

    /**
     * 把本项目资源注入目标作用域上下文。一般调用本方法即可。<br/>
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
    public static Resources injectModuleRes(Resources resources, boolean doOnMainLooper) {
        if (resources == null)
            throw new RuntimeException(createRuntimeExceptionMsg("Resources can't is null! inject res failed!"));

        if (mModulePath == null) {
            mModulePath = HCData.getModulePath() != null ? HCData.getModulePath() : null;
            if (mModulePath == null)
                throw new RuntimeException(createRuntimeExceptionMsg("Module path is null, Please set module path!"));
        }

        if (mResourcesArrayList.contains(resources))
            return resources;

        boolean load;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            load = injectResAboveApi30(resources, doOnMainLooper);
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
        if (!mResourcesArrayList.contains(resources))
            mResourcesArrayList.add(resources);
        return resources;
    }

    @NonNull
    public static Resources injectModuleRes(Resources resources) {
        return injectModuleRes(resources, false);
    }

    @NonNull
    public static Resources injectModuleRes(Context context) {
        return injectModuleRes(context, false);
    }

    @NonNull
    public static Resources injectModuleRes(Context context, boolean doOnMainLooper) {
        return injectModuleRes(context.getResources(), doOnMainLooper);
    }

    /**
     * 来自 QA 的方法
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    private static boolean injectResAboveApi30(Resources resources, boolean doOnMainLooper) {
        if (mResourcesLoader == null) {
            try (ParcelFileDescriptor pfd =
                     ParcelFileDescriptor.open(new File(mModulePath), ParcelFileDescriptor.MODE_READ_ONLY)
            ) {
                ResourcesProvider provider = ResourcesProvider.loadFromApk(pfd);
                ResourcesLoader loader = new ResourcesLoader();
                loader.addProvider(provider);
                mResourcesLoader = loader;
            } catch (IOException e) {
                logE(getTag(), "Failed to inject res! debug: above api 30.", e);
                return false;
            }
        }
        if (doOnMainLooper)
            if (Looper.myLooper() == Looper.getMainLooper()) {
                return addLoaders(resources);
            } else {
                if (mHandler == null) {
                    mHandler = new Handler(Looper.getMainLooper());
                }
                mHandler.post(() -> addLoaders(resources));
                return true; // 此状态下保持返回 true，请观察日志是否有报错来判断是否成功。
            }
        else
            return addLoaders(resources);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private static boolean addLoaders(Resources resources) {
        try {
            resources.addLoaders(mResourcesLoader);
        } catch (IllegalArgumentException e) {
            String expected = "Cannot modify resource loaders of ResourcesImpl not registered with ResourcesManager";
            if (expected.equals(e.getMessage())) {
                // fallback to below API 30
                return injectResBelowApi30(resources);
            } else {
                logE(getTag(), "Failed to add loaders!", e);
                return false;
            }
        }
        return true;
    }

    /**
     * @noinspection JavaReflectionMemberAccess
     */
    @SuppressLint("DiscouragedPrivateApi")
    private static boolean injectResBelowApi30(Resources resources) {
        try {
            AssetManager assets = resources.getAssets();
            Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            Integer cookie = (Integer) addAssetPath.invoke(assets, mModulePath);
            if (cookie == null || cookie == 0) {
                logW(getTag(), "Method 'addAssetPath' result 0, maybe inject res failed!", getStackTrace());
                return false;
            }
        } catch (Throwable e) {
            logE(getTag(), "Failed to inject res! debug: below api 30.", e);
            return false;
        }
        return true;
    }

    private static final List<Resources> mResourcesArrayList = new ArrayList<>();
    private static final ConcurrentHashMap<Integer, Boolean> mResMap = new ConcurrentHashMap<>();
    private static final List<XC_MethodHook.Unhook> mUnhooks = new ArrayList<>();
    private static final ConcurrentHashMap<String, Pair<ReplacementType, Object>> mReplacements = new ConcurrentHashMap<>();
    private static boolean isHooked;

    private ResInjectTool() {
        isHooked = false;
        mResourcesArrayList.clear();
        mResMap.clear();
        mUnhooks.clear();
        mReplacements.clear();
    }

    private enum ReplacementType {
        ID,
        DENSITY,
        OBJECT
    }

    public static int createFakeResId(String resName) {
        return 0x7e000000 | (resName.hashCode() & 0x00ffffff);
    }

    public static int createFakeResId(Resources res, int id) {
        return createFakeResId(res.getResourceName(id));
    }

    /**
     * 设置资源 ID 类型的替换
     */
    public static void setResReplacement(String pkg, String type, String name, int replacementResId) {
        try {
            applyHooks();
            mReplacements.put(pkg + ":" + type + "/" + name, new Pair<>(ReplacementType.ID, replacementResId));
        } catch (Throwable t) {
            logE(getTag(), "Failed to set res replacement!", t);
        }
    }

    /**
     * 设置密度类型的资源
     */
    public static void setDensityReplacement(String pkg, String type, String name, float replacementResValue) {
        try {
            applyHooks();
            mReplacements.put(pkg + ":" + type + "/" + name, new Pair<>(ReplacementType.DENSITY, replacementResValue));
        } catch (Throwable t) {
            logE(getTag(), "Failed to set density res replacement!", t);
        }
    }

    /**
     * 设置 Object 类型的资源
     */
    public static void setObjectReplacement(String pkg, String type, String name, Object replacementResValue) {
        try {
            applyHooks();
            mReplacements.put(pkg + ":" + type + "/" + name, new Pair<>(ReplacementType.OBJECT, replacementResValue));
        } catch (Throwable t) {
            logE(getTag(), "Failed to set object res replacement!", t);
        }
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
                    if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(int.class)) {
                        hookResMethod(method.getName(), int.class, hookResBefore);
                    }
                }
                case "getColor" -> {
                    if (method.getParameterTypes().length == 2) {
                        hookResMethod(method.getName(), int.class, Resources.Theme.class, hookResBefore);
                    }
                }
                case "getFraction" -> {
                    if (method.getParameterTypes().length == 3) {
                        hookResMethod(method.getName(), int.class, int.class, int.class, hookResBefore);
                    }
                }
                case "getDrawableForDensity" -> {
                    if (method.getParameterTypes().length == 3) {
                        hookResMethod(method.getName(), int.class, int.class, Resources.Theme.class, hookResBefore);
                    }
                }
            }
        }

        Method[] typedMethod = TypedArray.class.getDeclaredMethods();
        for (Method method : typedMethod) {
            if (method.getName().equals("getColor")) {
                hookTypedMethod(method.getName(), int.class, int.class, hookTypedBefore);
            }
        }
        isHooked = true;
    }

    private static void hookResMethod(String name, Object... args) {
        mUnhooks.add(CoreTool.hookMethod(Resources.class, name, args));
    }

    private static void hookTypedMethod(String name, Object... args) {
        mUnhooks.add(CoreTool.hookMethod(TypedArray.class, name, args));
    }

    public static void unHookRes() {
        if (mUnhooks.isEmpty()) {
            isHooked = false;
            return;
        }
        for (XC_MethodHook.Unhook unhook : mUnhooks) {
            unhook.unhook();
        }
        mUnhooks.clear();
        isHooked = false;
    }

    private static final IHook hookTypedBefore = new IHook() {
        @Override
        public void before() {
            Integer index = (Integer) getArgs(0);
            if (index == null) return;

            int[] mData = (int[]) CoreTool.getField(thisObject(), "mData");
            assert mData != null;
            int type = mData[index];
            int id = mData[index + 3];

            if (id != 0 && (type != TypedValue.TYPE_NULL)) {
                Resources mResources = (Resources) CoreTool.getField(thisObject(), "mResources");
                Object value = getTypedArrayReplacement(mResources, id);
                if (value != null) {
                    setResult(value);
                }
            }
        }
    };

    private static final IHook hookResBefore = new IHook() {
        @Override
        public void before() {
            if (mResourcesArrayList.isEmpty())
                mResourcesArrayList.add(injectModuleRes(ContextTool.getContext(FLAG_ALL)));

            Integer key = (Integer) getArgs(0);
            if (key == null) return;
            if (Boolean.TRUE.equals(mResMap.get(key))) return;

            for (Resources resources : mResourcesArrayList) {
                if (resources == null) return;
                String method = mMember.getName();
                Object value;
                try {
                    value = getResourceReplacement(resources, (Resources) thisObject(), method, mArgs);
                } catch (Resources.NotFoundException ignore) {
                    continue;
                }
                if (value != null) {
                    if ("getDimensionPixelOffset".equals(method) || "getDimensionPixelSize".equals(method)) {
                        if (value instanceof Float) value = ((Float) value).intValue();
                    }
                    setResult(value);
                    break;
                }
            }
        }
    };

    private static Object getResourceReplacement(Resources resources, Resources res, String method, Object[] args) throws Resources.NotFoundException {
        if (resources == null) return null;

        String pkgName = null;
        String resType = null;
        String resName = null;
        try {
            pkgName = res.getResourcePackageName((int) args[0]);
            resType = res.getResourceTypeName((int) args[0]);
            resName = res.getResourceEntryName((int) args[0]);
        } catch (Throwable ignore) {
        }
        if (pkgName == null || resType == null || resName == null) return null;

        String resFullName = pkgName + ":" + resType + "/" + resName;
        String resAnyPkgName = "*:" + resType + "/" + resName;

        Object value;
        Integer modResId;
        Pair<ReplacementType, Object> replacement = null;
        if (mReplacements.containsKey(resFullName)) {
            replacement = mReplacements.get(resFullName);
        } else if (mReplacements.containsKey(resAnyPkgName)) {
            replacement = mReplacements.get(resAnyPkgName);
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
                    modResId = (Integer) replacement.second;
                    if (modResId == 0) return null;
                    try {
                        resources.getResourceName(modResId);
                    } catch (Resources.NotFoundException ignore) {
                        injectModuleRes(resources);
                        resources.getResourceName(modResId);
                    }
                    if (method == null) return null;

                    mResMap.put(modResId, true);
                    if ("getDrawable".equals(method))
                        value = CoreTool.callMethod(resources, method, modResId, args[1]);
                    else if ("getDrawableForDensity".equals(method) || "getFraction".equals(method))
                        value = CoreTool.callMethod(resources, method, modResId, args[1], args[2]);
                    else
                        value = CoreTool.callMethod(resources, method, modResId);
                    mResMap.remove(modResId);
                    return value;
                }
            }
        }
        return null;
    }

    private static Object getTypedArrayReplacement(Resources resources, int id) {
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
            if (mReplacements.containsKey(resFullName)) {
                replacement = mReplacements.get(resFullName);
            } else if (mReplacements.containsKey(resAnyPkgName)) {
                replacement = mReplacements.get(resAnyPkgName);
            }
            if (replacement != null && (Objects.requireNonNull(replacement.first) == ReplacementType.OBJECT)) {
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
