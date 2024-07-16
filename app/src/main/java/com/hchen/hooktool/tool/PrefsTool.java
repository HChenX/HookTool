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

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.hchen.hooktool.itool.IPrefs;
import com.hchen.hooktool.log.AndroidLog;
import com.hchen.hooktool.utils.ContextUtils;
import com.hchen.hooktool.utils.ToolData;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;

/**
 * prefs 工具
 */
public class PrefsTool {
    private ToolData data;
    private final static String TAG = "PrefsTool";
    private static String prefsName;
    private boolean isUsingNativeStorage = false;
    private boolean isXposedEnvironment = false;
    private boolean isUsingNewXSharedPreferences = false;
    private static PrefsTool xposedPrefs = null;
    private final static PrefsTool modulePrefs = new PrefsTool();
    private final static HashMap<String, XSharedPreferences> xPrefs = new HashMap<>();
    private final static HashMap<String, SharedPreferences> sPrefs = new HashMap<>();

    /**
     * 模块实例入口。
     */
    public PrefsTool() {
        isXposedEnvironment = false;
        isUsingNativeStorage = true;
        // 默认值，即设置的 tag 值后加 _prefs
        prefsName = ToolData.spareTag.replace(" ", "").toLowerCase() + "_prefs";
    }

    /**
     * 寄生实例入口，无需手动实例。
     */
    public PrefsTool(ToolData data) {
        this.data = data;
        isXposedEnvironment = true;
        isUsingNativeStorage = false;
        // 默认值，即设置的 tag 值后加 _prefs
        prefsName = ToolData.spareTag.replace(" ", "").toLowerCase() + "_prefs";
        xposedPrefs = this;
    }

    public static PrefsTool getXposedPrefs() {
        return xposedPrefs;
    }

    public static PrefsTool modulePrefs() {
        return modulePrefs;
    }

    /**
     * 寄生应用读取配置一般使用。
     */
    public IPrefs prefs() {
        return prefs(prefsName);
    }

    /**
     * 寄生应用读取配置一般使用。
     */
    public IPrefs prefs(String prefsName) {
        if (!isXposedEnvironment) {
            throw new RuntimeException(ToolData.mInitTag +
                    "[E]: not is xposed can't call this method! please use context method!");
        }
        prefsName = prefsName.replace(" ", "").toLowerCase();
        if (isUsingNativeStorage) {
            return new Sprefs(currentSp(ContextUtils.getContext(ContextUtils.FLAG_CURRENT_APP), prefsName));
        }
        if (isUsingNewXSharedPreferences)
            return new Xprefs(currentXsp(prefsName), data);
        else
            throw new RuntimeException(ToolData.mInitTag +
                    "[E]: not supported new xshared prefs! can't use!");
    }

    /**
     * 模块应用读取配置一般使用。
     */
    public IPrefs prefs(Context context) {
        return prefs(context, prefsName);
    }

    /**
     * 模块应用读取配置一般使用。
     */
    public IPrefs prefs(Context context, String prefsName) {
        prefsName = prefsName.replace(" ", "").toLowerCase();
        if (isXposedEnvironment && !isUsingNativeStorage) {
            if (isUsingNewXSharedPreferences)
                return new Xprefs(currentXsp(prefsName), data);
            else
                throw new RuntimeException(ToolData.mInitTag +
                        "[E]: not supported new xshared prefs! can't use!");
        } else {
            return new Sprefs(currentSp(context, prefsName));
        }
    }

    /**
     * 异步设置配置。
     * <p>
     * 仅限寄生应用内调用，适用于不方便获取 context 的情况。
     */
    public void asynPrefs(IAsynPrefs asynPrefs) {
        if (!isXposedEnvironment) {
            throw new RuntimeException(ToolData.mInitTag +
                    "[E]: not is xposed can't call this method! please use context method!");
        }
        isUsingNativeStorage = true;
        ContextUtils.getWaitContext(new ContextUtils.IContext() {
            @Override
            public void findContext(Context context) {
                if (context == null) {
                    throw new RuntimeException(ToolData.mInitTag +
                            "[" + data.getTAG() + "][E]: asyn prefs context is null!!");
                }
                asynPrefs.asyn(context);
            }
        }, false);
    }

    public interface IAsynPrefs {
        void asyn(Context context);
    }

    /**
     * 使寄生应用独立存储配置。
     */
    public PrefsTool nativePrefs() {
        isUsingNativeStorage = true;
        return this;
    }

    /**
     * 使寄生应用使用模块的配置。
     */
    public PrefsTool xposedPrefs() {
        isUsingNativeStorage = false;
        return this;
    }

    private XSharedPreferences currentXsp(String prefsName) {
        if (xPrefs.get(prefsName) == null) {
            if (ToolData.modulePackageName == null) {
                throw new RuntimeException(ToolData.mInitTag +
                        "[" + data.getTAG() + "][E]: module package name is null!!");
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
            throw new RuntimeException(ToolData.mInitTag + "[E]: context is null!! can't create sprefs!");
        }
        if (sPrefs.get(context + prefsName) == null) {
            SharedPreferences s;
            try {
                s = context.getSharedPreferences(prefsName, Context.MODE_WORLD_READABLE);
                isUsingNewXSharedPreferences = true;
            } catch (Throwable ignored) {
                s = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
                isUsingNewXSharedPreferences = false;
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

        public Xprefs(XSharedPreferences xSharedPreferences, ToolData data) {
            this.data = data;
            this.xSharedPreferences = xSharedPreferences;
        }

        @Override
        public String getString(String key, String def) {
            return xSharedPreferences.getString(key, def);
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> def) {
            return xSharedPreferences.getStringSet(key, def);
        }

        @Override
        public boolean getBoolean(String key, boolean def) {
            return xSharedPreferences.getBoolean(key, def);
        }

        @Override
        public int getInt(String key, int def) {
            return xSharedPreferences.getInt(key, def);
        }

        @Override
        public float getFloat(String key, float def) {
            return xSharedPreferences.getFloat(key, def);
        }

        @Override
        public long getLong(String key, long def) {
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
                logE(data.getTAG(), "unknown error!", e);
            }
            return null;
        }

        @Override
        public boolean contains(String key) {
            return xSharedPreferences.contains(key);
        }

        @Override
        public Map<String, ?> getAll() {
            return xSharedPreferences.getAll();
        }

        /**
         * Xprefs 不支持修改！
         */
        @Override
        @Nullable
        public Editor editor() {
            logW(data.getTAG(), "xposed can't edit prefs!");
            return null;
        }
    }

    public static class Sprefs implements IPrefs {
        private final SharedPreferences preferences;

        public Sprefs(SharedPreferences preferences) {
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
                AndroidLog.logE(TAG, "unknown error!", e);
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
                AndroidLog.logE(TAG, "unknown error!", e);
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
