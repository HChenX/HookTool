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
 * Copyright (C) 2023–2025 HChenX
 */
package com.hchen.hooktool.callback;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Prefs 实现接口
 *
 * @author 焕晨HChen
 */
public interface IPrefsApply {
    /**
     * 根据 key，获取 String
     */
    @Nullable
    String getString(String key, @Nullable String def);

    /**
     * 根据 key，获取 Set
     */
    @Nullable
    Set<String> getStringSet(String key, @Nullable Set<String> def);

    /**
     * 根据 key，获取 boolean
     */
    boolean getBoolean(String key, boolean def);

    /**
     * 根据 key，获取 int
     */
    int getInt(String key, int def);

    /**
     * 根据 key，获取 float
     */
    float getFloat(String key, float def);

    /**
     * 根据 key，获取 long
     */
    long getLong(String key, long def);

    /**
     * 根据 key 和 def 类型，获取值
     */
    Object get(String key, Object def);

    /**
     * 是否包含指定 key
     */
    boolean contains(String key);

    /**
     * 获取共享首选项 Map
     */
    Map<String, ?> getAll();

    /**
     * 获取共享首选项编辑器
     */
    @NonNull
    SharedPreferences.Editor editor();
}
