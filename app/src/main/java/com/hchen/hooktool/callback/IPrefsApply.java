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
package com.hchen.hooktool.callback;

import com.hchen.hooktool.utils.PrefsTool;

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
    String getString(String key, String def);

    /**
     * 根据 key，获取 Set
     */
    Set<String> getStringSet(String key, Set<String> def);

    /**
     * 根据 key，获取 Boolean
     */
    boolean getBoolean(String key, boolean def);

    /**
     * 根据 key，获取 Int
     */
    int getInt(String key, int def);

    /**
     * 根据 key，获取 Float
     */
    float getFloat(String key, float def);

    /**
     * 根据 key，获取 Long
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
    PrefsTool.Editor editor();
}
