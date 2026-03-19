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

import static com.hchen.hooktool.log.LogExpand.getStackTrace;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.ModuleConfig;
import com.hchen.hooktool.ModuleData;
import com.hchen.hooktool.callback.IAsyncPrefs;
import com.hchen.hooktool.callback.IContextGetter;
import com.hchen.hooktool.callback.IPrefsApply;
import com.hchen.hooktool.exception.NonXposedException;
import com.hchen.hooktool.exception.UnexpectedException;
import com.hchen.hooktool.log.AndroidLog;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 共享首选项工具
 *
 * @author 焕晨HChen
 */
public class PrefsTool {
    private static final String TAG = "PrefsTool";
    private final static ConcurrentHashMap<String, SPrefs> xSPrefsMap = new ConcurrentHashMap<>(); // 宿主端
    private final static ConcurrentHashMap<String, SPrefs> sPrefsMap = new ConcurrentHashMap<>(); // 模块端

    private PrefsTool() {
    }

    /**
     * 从应用私有目录读取/写入共享首选项数据
     */
    @NonNull
    public static IPrefsApply prefs(@NonNull Context context) {
        return prefs(context, "");
    }

    /**
     * 从应用私有目录读取/写入共享首选项数据
     */
    @NonNull
    public static IPrefsApply prefs(@NonNull Context context, @NonNull String prefsName) {
        return createSPrefsIfNeed(context, prefsName);
    }

    /**
     * Xposed 环境中读取模块的共享首选项，非 Xposed 环境中使用会引发异常
     */
    @NonNull
    public static IPrefsApply prefs() {
        if (!ModuleConfig.isXposedEnvironment())
            throw new NonXposedException("Must be in the xposed environment.");
        return prefs("");
    }

    /**
     * Xposed 环境中读取模块的共享首选项，非 Xposed 环境中使用会引发异常
     */
    @NonNull
    public static IPrefsApply prefs(@NonNull String prefsName) {
        if (!ModuleConfig.isXposedEnvironment())
            throw new NonXposedException("Must be in the xposed environment.");
        return createXSPrefsIfNeed(prefsName);
    }

    /**
     * Xposed 环境中异步获取寄生应用的共享首选项，非 Xposed 环境中使用会引发异常
     */
    public static void asyncPrefs(@NonNull IAsyncPrefs asyncPrefs) {
        asyncPrefs("", asyncPrefs);
    }

    /**
     * Xposed 环境中异步获取寄生应用的共享首选项，非 Xposed 环境中使用会引发异常
     */
    public static void asyncPrefs(@NonNull String prefsName, @NonNull IAsyncPrefs asyncPrefs) {
        if (!ModuleConfig.isXposedEnvironment())
            throw new NonXposedException("Must be in the xposed environment.");

        ContextTool.getAsyncContext(
            new IContextGetter() {
                @Override
                public void onContext(@Nullable Context context) {
                    asyncPrefs.async(createSPrefsIfNeed(Objects.requireNonNull(context), prefsName));
                }
            },
            ContextTool.FLAG_CURRENT_APP
        );
    }

    private static IPrefsApply createXSPrefsIfNeed(@NonNull String prefsName) {
        prefsName = initPrefsName(prefsName);
        if (xSPrefsMap.get(ModuleConfig.getModulePackageName() + prefsName) == null) {
            Objects.requireNonNull(ModuleData.getWrapper());

            SPrefs sPrefs = new SPrefs(ModuleData.getRemotePreferences(prefsName));
            xSPrefsMap.put(ModuleConfig.getModulePackageName() + prefsName, sPrefs);
            return sPrefs;
        } else {
            return xSPrefsMap.get(ModuleConfig.getModulePackageName() + prefsName);
        }
    }

    /**
     * @noinspection deprecation
     */
    @SuppressLint("WorldReadableFiles")
    private static IPrefsApply createSPrefsIfNeed(@NonNull Context context, @NonNull String prefsName) {
        prefsName = initPrefsName(prefsName);
        if (sPrefsMap.get(context.getPackageName() + prefsName) == null) {
            SharedPreferences s;
            try {
                s = context.getSharedPreferences(prefsName, Context.MODE_WORLD_READABLE);
            } catch (Throwable ignored) {
                s = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
                AndroidLog.logW(TAG, "maybe unsupported prefs.", getStackTrace());
            }

            SPrefs sprefs = new SPrefs(s);
            sPrefsMap.put(context.getPackageName() + prefsName, sprefs);
            return sprefs;
        } else {
            return sPrefsMap.get(context.getPackageName() + prefsName);
        }
    }

    @NonNull
    private static String initPrefsName(@NonNull String name) {
        Objects.requireNonNull(name, "Prefs name must not be null.");

        if (name.isEmpty()) {
            if (ModuleConfig.getPrefsName().isEmpty()) {
                if (ModuleConfig.getModulePackageName().isEmpty())
                    throw new UnexpectedException("What prefs name you want use?");

                return ModuleConfig.getModulePackageName().toLowerCase() + "_prefs";
            }
            return ModuleConfig.getPrefsName();
        } else return name;
    }

    /**
     * @noinspection unchecked
     */
    private record SPrefs(@NonNull SharedPreferences preferences) implements IPrefsApply {
        @Override
        @Nullable
        public String getString(String key, @Nullable String def) {
            return preferences.getString(key, def);
        }

        @Override
        @Nullable
        public Set<String> getStringSet(String key, @Nullable Set<String> def) {
            return preferences.getStringSet(key, def);
        }

        @Override
        public boolean getBoolean(String key, boolean def) {
            return preferences.getBoolean(key, def);
        }

        @Override
        public int getInt(String key, int def) {
            return preferences.getInt(key, def);
        }

        @Override
        public float getFloat(String key, float def) {
            return preferences.getFloat(key, def);
        }

        @Override
        public long getLong(String key, long def) {
            return preferences.getLong(key, def);
        }

        @Override
        public Object get(String key, Object def) {
            if (def instanceof String s) {
                return getString(key, s);
            } else if (def instanceof Set<?> set) {
                return getStringSet(key, (Set<String>) set);
            } else if (def instanceof Integer i) {
                return getInt(key, i);
            } else if (def instanceof Boolean b) {
                return getBoolean(key, b);
            } else if (def instanceof Float f) {
                return getFloat(key, f);
            } else if (def instanceof Long l) {
                return getLong(key, l);
            }
            throw new UnexpectedException("Unknown type value: " + def);
        }

        @Override
        public boolean contains(String key) {
            return preferences.contains(key);
        }

        @Override
        public Map<String, ?> getAll() {
            return preferences.getAll();
        }

        @NonNull
        @Override
        public SharedPreferences.Editor editor() {
            return preferences.edit();
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
            preferences.registerOnSharedPreferenceChangeListener(listener);
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
            preferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }
}
