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

import static com.hchen.hooktool.log.LogExpand.getStackTrace;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.HCData;
import com.hchen.hooktool.callback.IAsyncPrefs;
import com.hchen.hooktool.callback.IPrefsApply;
import com.hchen.hooktool.exception.NonXposedException;
import com.hchen.hooktool.exception.UnexpectedException;
import com.hchen.hooktool.log.AndroidLog;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;

/**
 * 共享首选项工具
 *
 * @author 焕晨HChen
 */
public class PrefsTool {
    private static final String TAG = "PrefsTool";
    private final static HashMap<String, Xprefs> xPrefsMap = new HashMap<>();
    private final static HashMap<String, Sprefs> sPrefsMap = new HashMap<>();

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
        return createSpIfNeed(context, prefsName);
    }

    /**
     * Xposed 环境中读取模块的共享首选项，非 Xposed 环境中使用会引发异常
     */
    @NonNull
    public static IPrefsApply prefs() {
        if (!HCData.isXposed())
            throw new NonXposedException("[PrefsTool]: Not xposed environment!!");
        return prefs("");
    }

    /**
     * Xposed 环境中读取模块的共享首选项，非 Xposed 环境中使用会引发异常
     */
    @NonNull
    public static IPrefsApply prefs(@NonNull String prefsName) {
        if (!HCData.isXposed())
            throw new NonXposedException("[PrefsTool]: Not xposed environment!!");
        return createXspIfNeed(prefsName);
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
        if (!HCData.isXposed())
            throw new NonXposedException("[PrefsTool]: Not xposed environment!!");

        ContextTool.getAsyncContext(context ->
                asyncPrefs.async(createSpIfNeed(Objects.requireNonNull(context), prefsName)),
            ContextTool.FLAG_CURRENT_APP
        );
    }

    /**
     * @hide
     */
    @Deprecated
    public static void clear() {
        sPrefsMap.clear();
        xPrefsMap.clear();
    }

    private static IPrefsApply createXspIfNeed(@NonNull String prefsName) {
        if (HCData.getModulePackageName().isEmpty())
            throw new UnexpectedException("[PrefsTool]: Module package name is empty, Please set module package name!!");

        prefsName = initPrefsName(prefsName);
        if (xPrefsMap.get(HCData.getModulePackageName() + prefsName) == null) {
            XSharedPreferences x = new XSharedPreferences(HCData.getModulePackageName(), prefsName);
            x.makeWorldReadable();
            x.reload();

            Xprefs xprefs = new Xprefs(x);
            xPrefsMap.put(HCData.getModulePackageName() + prefsName, xprefs);
            return xprefs;
        } else {
            return xPrefsMap.get(HCData.getModulePackageName() + prefsName);
        }
    }

    /**
     * @noinspection deprecation
     */
    @SuppressLint("WorldReadableFiles")
    private static IPrefsApply createSpIfNeed(@NonNull Context context, @NonNull String prefsName) {
        prefsName = initPrefsName(prefsName);
        if (sPrefsMap.get(context.getPackageName() + prefsName) == null) {
            SharedPreferences s;
            try {
                s = context.getSharedPreferences(prefsName, Context.MODE_WORLD_READABLE);
            } catch (Throwable ignored) {
                s = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
                AndroidLog.logW(TAG, "Maybe unsupported xSharedPreferences!!", getStackTrace());
            }

            Sprefs sprefs = new Sprefs(s);
            sPrefsMap.put(context.getPackageName() + prefsName, sprefs);
            return sprefs;
        } else {
            return sPrefsMap.get(context.getPackageName() + prefsName);
        }
    }

    @NonNull
    private static String initPrefsName(@NonNull String name) {
        Objects.requireNonNull(name, "[PrefsTool]: prefs name must not be null!!");

        if (name.isEmpty()) {
            if (HCData.getPrefsName().isEmpty()) {
                if (HCData.getModulePackageName().isEmpty())
                    throw new UnexpectedException("[PrefsTool]: What prefs name you want use?");

                return HCData.getModulePackageName() + "_prefs";
            }
            return HCData.getPrefsName();
        } else return name;
    }

    /**
     * @noinspection unchecked, DataFlowIssue
     */
    private record Xprefs(@NonNull XSharedPreferences xSharedPreferences) implements IPrefsApply {
        @Override
        @Nullable
        public String getString(String key, @Nullable String def) {
            reload();
            return xSharedPreferences.getString(key, def);
        }

        @Override
        @Nullable
        public Set<String> getStringSet(String key, @Nullable Set<String> def) {
            reload();
            return xSharedPreferences.getStringSet(key, def);
        }

        @Override
        public boolean getBoolean(String key, boolean def) {
            reload();
            return xSharedPreferences.getBoolean(key, def);
        }

        @Override
        public int getInt(String key, int def) {
            reload();
            return xSharedPreferences.getInt(key, def);
        }

        @Override
        public float getFloat(String key, float def) {
            reload();
            return xSharedPreferences.getFloat(key, def);
        }

        @Override
        public long getLong(String key, long def) {
            reload();
            return xSharedPreferences.getLong(key, def);
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
            throw new UnexpectedException("[PrefsTool]: Unknown type value: " + def);
        }

        @Override
        public boolean contains(String key) {
            reload();
            return xSharedPreferences.contains(key);
        }

        @Override
        public Map<String, ?> getAll() {
            reload();
            return xSharedPreferences.getAll();
        }

        /**
         * Xprefs 不支持修改！
         */
        @Override
        @NonNull
        public SharedPreferences.Editor editor() {
            throw new UnsupportedOperationException("[PrefsTool]: Xposed unsupported edit prefs!!");
        }

        private void reload() {
            if (HCData.isAutoReload()) {
                if (xSharedPreferences.hasFileChanged()) {
                    xSharedPreferences.reload();
                }
            }
        }
    }

    /**
     * @noinspection unchecked
     */
    private record Sprefs(@NonNull SharedPreferences preferences) implements IPrefsApply {
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
            throw new UnexpectedException("[PrefsTool]: Unknown type value: " + def);
        }

        @Override
        public boolean contains(String key) {
            return preferences.contains(key);
        }

        @Override
        public Map<String, ?> getAll() {
            return preferences.getAll();
        }

        @Override
        @NonNull
        public SharedPreferences.Editor editor() {
            return preferences.edit();
        }
    }
}
