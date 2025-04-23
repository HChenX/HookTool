package com.hchen.hooktool.callback;

import com.hchen.hooktool.utils.PrefsTool;

import java.util.Map;
import java.util.Set;

public interface IPrefsApply {
    String getString(String key, String def);

    Set<String> getStringSet(String key, Set<String> def);

    boolean getBoolean(String key, boolean def);

    int getInt(String key, int def);

    float getFloat(String key, float def);

    long getLong(String key, long def);

    Object get(String key, Object def);

    boolean contains(String key);

    Map<String, ?> getAll();

    PrefsTool.Editor editor();
}
