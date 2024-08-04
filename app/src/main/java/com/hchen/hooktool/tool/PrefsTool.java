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

 * Copyright (C) 2023-2024 HookTool Contributions
 */
package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.hchen.hooktool.additional.ContextUtils;
import com.hchen.hooktool.data.ToolData;
import com.hchen.hooktool.log.AndroidLog;
import com.hchen.hooktool.tool.itool.IPrefs;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;

/**
 * prefs 工具
 * <p>
 * prefs tool
 * 
 * @author 焕晨HChen
 */
public class PrefsTool {
    private ToolData data;
    private final static String TAG = "PrefsTool";
    private static String prefsName;
    private static PrefsTool xposedPrefs = null;
    private static PrefsTool modulePrefs = null;
    private final static HashMap<String, XSharedPreferences> xPrefs = new HashMap<>();
    private final static HashMap<String, SharedPreferences> sPrefs = new HashMap<>();

    // ------------ 模块使用 ----------------
    // ---------- 寄生应用使用则存自身私有路径内 -----------
    private PrefsTool() {
        // 默认值，即设置的 tag 值后加 _prefs
        prefsName = ToolData.spareTag.replace(" ", "").toLowerCase() + "_prefs";
    }

    /**
     * 寄生实例入口，无需手动实例。
     * <p>
     * Parasitic instance entry, no manual instance is required.
     */
    public PrefsTool(ToolData data) {
        // 默认值，即设置的 tag 值后加 _prefs
        prefsName = ToolData.spareTag.replace(" ", "").toLowerCase() + "_prefs";
        this.data = data;
        xposedPrefs = this;
    }

    /**
     * 模块使用。
     * <p>
     * 寄生应用使用则存其私有目录内，读取也从其私有目录读取。
     * <p>
     * Module use. <p>
     * If parasitic applications are stored in their private directory and reads are read from their private directory.
     */
    public static IPrefs prefs(Context context) {
        return prefs(context, prefsName);
    }

    /**
     * 模块使用。
     * <p>
     * 寄生应用使用则存其私有目录内，读取也从其私有目录读取。
     * <p>
     * Module use. <p>
     * If parasitic applications are stored in their private directory and reads are read from their private directory.
     */
    public static IPrefs prefs(Context context, String prefsName) {
        initModulePrefs();
        return new Sprefs(modulePrefs.currentSp(context, prefsName));
    }

    // ---------------- 寄生应用使用 -----------------

    private static void initModulePrefs() {
        if (modulePrefs == null) {
            modulePrefs = new PrefsTool();
        }
    }

    public static PrefsTool xposedPrefs() {
        return xposedPrefs;
    }

    /**
     * 寄生应用读取配置一般使用。
     * <p>
     * Parasitic application read prefs is generally used.
     */
    public IPrefs prefs() {
        return prefs(prefsName);
    }

    /**
     * 寄生应用读取配置一般使用。
     * <p>
     * Parasitic application read prefs is generally used.
     */
    public IPrefs prefs(String prefsName) {
        prefsName = prefsName.replace(" ", "").toLowerCase();
        return new Xprefs(currentXsp(prefsName), data);
    }

    /**
     * 异步设置配置。
     * <p>
     * 仅限寄生应用内调用，适用于不方便获取 context 的情况。
     * <p>
     * Asynchronous setup prefs.
     * <p>
     * Parasitic in-app calls only, for situations where it's inconvenient to get context.
     */
    public void asyncPrefs(IAsyncPrefs asyncPrefs) {
        ContextUtils.getAsyncContext(new ContextUtils.IContext() {
            @Override
            public void find(Context context) {
                if (context == null) {
                    throw new RuntimeException(ToolData.mInitTag +
                            "[" + data.tag() + "][E]: PrefsTool: async prefs context is null!!" + getStackTrace());
                }
                asyncPrefs.async(context);
            }
        }, false);
    }

    public interface IAsyncPrefs {
        void async(Context context);
    }

    private XSharedPreferences currentXsp(String prefsName) {
        if (xPrefs.get(prefsName) == null) {
            if (ToolData.modulePackageName == null) {
                throw new RuntimeException(ToolData.mInitTag +
                        "[" + data.tag() + "][E]: PrefsTool: module package name is null!!" + getStackTrace());
            }
            XSharedPreferences x = new XSharedPreferences(ToolData.modulePackageName, prefsName);
            x.makeWorldReadable();
            x.reload();
            xPrefs.put(prefsName, x);
            return x;
        } else {
            return xPrefs.get(prefsName);
        }
    }

    /** @noinspection deprecation */
    @SuppressLint("WorldReadableFiles")
    private SharedPreferences currentSp(Context context, String prefsName) {
        if (context == null) {
            throw new RuntimeException(ToolData.mInitTag + "[E]: PrefsTool: context is null!! can't create sprefs!" + getStackTrace());
        }
        if (sPrefs.get(context + prefsName) == null) {
            SharedPreferences s;
            try {
                s = context.getSharedPreferences(prefsName, Context.MODE_WORLD_READABLE);
            } catch (Throwable ignored) {
                s = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
                AndroidLog.logW(TAG, "PrefsTool: maybe can't use xSharedPreferences!" + getStackTrace());
            }
            sPrefs.put(context + prefsName, s);
            return s;
        } else {
            return sPrefs.get(prefsName);
        }
    }

    public static class Xprefs implements IPrefs {
        private final ToolData data;
        private final XSharedPreferences xSharedPreferences;

        private Xprefs(XSharedPreferences xSharedPreferences, ToolData data) {
            this.data = data;
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
            reload();
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
                logE(data.tag(), "PrefsTool: unknown error!", e);
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
         * <p>
         * Xprefs doesn't support editor!
         */
        @Override
        @Nullable
        public Editor editor() {
            logW(data.tag(), "PrefsTool: xposed can't edit prefs!" + getStackTrace());
            return null;
        }

        private void reload() {
            if (ToolData.autoReload) {
                if (xSharedPreferences.hasFileChanged()) {
                    xSharedPreferences.reload();
                }
            }
        }
    }

    public static class Sprefs implements IPrefs {
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
                AndroidLog.logE(TAG, "PrefsTool: unknown error!", e);
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
                AndroidLog.logE(TAG, "PrefsTool: unknown error!", e);
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
