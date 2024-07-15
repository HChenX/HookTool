package com.hchen.hooktool.itool;

import com.hchen.hooktool.tool.PrefsTool;

import java.util.Map;
import java.util.Set;

public interface IPrefs {
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
