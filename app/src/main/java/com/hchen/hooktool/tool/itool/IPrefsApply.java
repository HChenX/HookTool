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
package com.hchen.hooktool.tool.itool;

import com.hchen.hooktool.tool.PrefsTool;

import java.util.Map;
import java.util.Set;

/**
 * prefs 工具接口，方法具体介绍请看实现类 >>
 * {@link com.hchen.hooktool.tool.PrefsTool}
 * 
 * @author 焕晨HChen
 */
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
