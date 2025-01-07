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

 * Copyright (C) 2023-2024 HChenX
 */
package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.LogExpand.createRuntimeExceptionLog;
import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.LogExpand.getTag;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.hchen.hooktool.HCData;
import com.hchen.hooktool.log.AndroidLog;
import com.hchen.hooktool.tool.additional.ContextTool;
import com.hchen.hooktool.tool.itool.IAsyncPrefs;
import com.hchen.hooktool.tool.itool.IContextGetter;
import com.hchen.hooktool.tool.itool.IPrefsApply;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;

/**
 * prefs 工具
 *
 * @author 焕晨HChen
 */
public final class PrefsTool {
    private final static HashMap<String, XSharedPreferences> xPrefs = new HashMap<>();
    private final static HashMap<String, SharedPreferences> sPrefs = new HashMap<>();

    /**
     * 共享首选项储存至应用私有目录内/从私有目录读取，模块如果设置 xposedsharedprefs 为 true 则由 xposed 统一管理。
     */
    public static IPrefsApply prefs(Context context) {
        return prefs(context, "");
    }

    /**
     * 共享首选项储存至应用私有目录内/从私有目录读取，并使用指定的 prefsName 命名文件，模块如果设置 xposedsharedprefs 为 true 则由 xposed 统一管理。
     */
    public static IPrefsApply prefs(Context context, @NonNull String prefsName) {
        return new Sprefs(currentSp(context, prefsName));
    }

    /**
     * 模块内不可使用，否则触发崩溃！
     * <p>
     * 将读取模块的共享首选项并供寄生应用使用。此状态下仅可读取，不可修改。
     */
    public static IPrefsApply prefs() {
        if (!HCData.isXposed())
            throw new RuntimeException(createRuntimeExceptionLog("Not xposed environment!"));
        return prefs("");
    }

    /**
     * 模块内不可使用，否则触发崩溃！
     * <p>
     * 将读取指定 prefsName 名的模块共享首选项文件并供寄生应用使用。此状态下仅可读取，不可修改。
     */
    public static IPrefsApply prefs(@NonNull String prefsName) {
        if (!HCData.isXposed())
            throw new RuntimeException(createRuntimeExceptionLog("Not xposed environment!"));
        return new Xprefs(currentXsp(prefsName));
    }

    /**
     * 异步设置配置。
     * <p>
     * 仅限寄生应用内调用，适用于不方便获取 context 的情况。
     */
    public static void asyncPrefs(IAsyncPrefs asyncPrefs) {
        if (!HCData.isXposed())
            throw new RuntimeException(createRuntimeExceptionLog("Not xposed environment!"));

        ContextTool.getAsyncContext(new IContextGetter() {
            @Override
            public void tryToFindContext(@androidx.annotation.Nullable Context context) {
                if (context == null)
                    throw new RuntimeException(createRuntimeExceptionLog("Async prefs context is null!"));

                asyncPrefs.async(context);
            }
        }, ContextTool.FLAG_CURRENT_APP);
    }

    private static XSharedPreferences currentXsp(String prefsName) {
        prefsName = initPrefsName(prefsName);
        if (xPrefs.get(prefsName) == null) {
            if (HCData.getModulePackageName() == null || HCData.getModulePackageName().isEmpty())
                throw new RuntimeException(createRuntimeExceptionLog("Module package name is null, Please set module package name!"));

            XSharedPreferences x = new XSharedPreferences(HCData.getModulePackageName(), prefsName);
            x.makeWorldReadable();
            x.reload();
            xPrefs.put(prefsName, x);
            return x;
        } else {
            return xPrefs.get(prefsName);
        }
    }

    /**
     * @noinspection deprecation
     */
    @SuppressLint("WorldReadableFiles")
    private static SharedPreferences currentSp(Context context, String prefsName) {
        prefsName = initPrefsName(prefsName);
        if (context == null)
            throw new RuntimeException(createRuntimeExceptionLog("Context is null, can't create sprefs!"));

        if (sPrefs.get(context.getPackageName() + prefsName) == null) {
            SharedPreferences s;
            try {
                s = context.getSharedPreferences(prefsName, Context.MODE_WORLD_READABLE);
            } catch (Throwable ignored) {
                s = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
                AndroidLog.logW(getTag(), "Maybe can't use xSharedPreferences!" + getStackTrace());
            }
            sPrefs.put(context.getPackageName() + prefsName, s);
            return s;
        } else {
            return sPrefs.get(prefsName);
        }
    }

    private static String initPrefsName(String name) {
        if (name == null)
            throw new RuntimeException(createRuntimeExceptionLog("prefs name can't is null!!"));

        if (name.isEmpty()) {
            if (HCData.getPrefsName() == null || HCData.getPrefsName().isEmpty()) {
                if (HCData.getModulePackageName() == null || HCData.getModulePackageName().isEmpty())
                    throw new RuntimeException(createRuntimeExceptionLog("What prefs name you want use??"));

                return HCData.getModulePackageName() + "_preferences";
            }
            return HCData.getPrefsName();
        } else return name;
    }

    public static class Xprefs implements IPrefsApply {
        private final XSharedPreferences xSharedPreferences;

        private Xprefs(XSharedPreferences xSharedPreferences) {
            this.xSharedPreferences = xSharedPreferences;
        }

        @Override
        public String getString(String key, String def) {
            reload();
            return xSharedPreferences.getString(key, def);
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> def) {
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
            try {
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
            } catch (Throwable e) {
                logE(getTag(), "Unknown error!", e);
            }
            return null;
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
        @Nullable
        public Editor editor() {
            logW(getTag(), "Xposed can't edit prefs!" + getStackTrace());
            return null;
        }

        private void reload() {
            if (HCData.isAutoReload()) {
                if (xSharedPreferences.hasFileChanged()) {
                    xSharedPreferences.reload();
                }
            }
        }
    }

    public static class Sprefs implements IPrefsApply {
        private final SharedPreferences preferences;

        private Sprefs(SharedPreferences preferences) {
            this.preferences = preferences;
        }

        @Override
        public String getString(String key, String def) {
            return preferences.getString(key, def);
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> def) {
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
            try {
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
            } catch (Throwable e) {
                AndroidLog.logE(getTag(), "Unknown error!", e);
            }
            return null;
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
        public PrefsTool.Editor editor() {
            return new Editor(preferences.edit());
        }
    }

    public static class Editor {
        private final SharedPreferences.Editor editor;

        private Editor(SharedPreferences.Editor editor) {
            this.editor = editor;
        }

        public Editor putString(String key, String value) {
            editor.putString(key, value);
            return this;
        }

        public Editor putStringSet(String key, Set<String> value) {
            editor.putStringSet(key, value);
            return this;
        }

        public Editor putBoolean(String key, boolean value) {
            editor.putBoolean(key, value);
            return this;
        }

        public Editor putInt(String key, int value) {
            editor.putInt(key, value);
            return this;
        }

        public Editor putFloat(String key, float value) {
            editor.putFloat(key, value);
            return this;
        }

        public Editor putLong(String key, long value) {
            editor.putLong(key, value);
            return this;
        }

        public Editor put(String key, Object value) {
            try {
                if (value instanceof String s) {
                    return putString(key, s);
                } else if (value instanceof Set<?> set) {
                    return putStringSet(key, (Set<String>) set);
                } else if (value instanceof Integer i) {
                    return putInt(key, i);
                } else if (value instanceof Boolean b) {
                    return putBoolean(key, b);
                } else if (value instanceof Float f) {
                    return putFloat(key, f);
                } else if (value instanceof Long l) {
                    return putLong(key, l);
                }
            } catch (Throwable e) {
                AndroidLog.logE(getTag(), "Unknown error!", e);
            }
            return this;
        }

        public Editor remove(String key) {
            editor.remove(key);
            return this;
        }

        public Editor clear() {
            editor.clear();
            return this;
        }

        public boolean commit() {
            return editor.commit();
        }

        public void apply() {
            editor.apply();
        }
    }
}
