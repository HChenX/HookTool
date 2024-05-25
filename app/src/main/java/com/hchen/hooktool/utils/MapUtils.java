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
package com.hchen.hooktool.utils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 本类提供多线程的 Map 储存与获取。
 *
 * @param <V>
 */
public class MapUtils<V> {
    private final ConcurrentHashMap<Integer, V> hashMap;
    private int count = -1;

    public MapUtils() {
        hashMap = new ConcurrentHashMap<>();
    }

    public MapUtils<V> put(V value) {
        count = count + 1;
        hashMap.put(count, value);
        return this;
    }

    public V get(Integer key) {
        return hashMap.get(key);
    }

    public void remove(Integer key) {
        hashMap.remove(key);
    }

    public boolean isEmpty() {
        return hashMap.isEmpty();
    }

    public int size() {
        return hashMap.size();
    }

    public ConcurrentHashMap<Integer, V> getHashMap() {
        return hashMap;
    }

    public void clear() {
        hashMap.clear();
        count = -1;
    }
}
