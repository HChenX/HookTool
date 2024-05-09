package com.hchen.hooktool.utils;

import com.hchen.hooktool.log.XposedLog;

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

    public MapUtils<V> put(Integer key, V value) {
        if (count == -1) {
            hashMap.put(key, value);
        } else {
            if (!hashMap.isEmpty()) {
                clear();
                count = -1;
                put(key, value);
                XposedLog.logE("MapUtils", "count is changed, will clear map, " +
                        "and writ new key and value.");
            }
        }
        return this;
    }

    public V get(Integer key) {
        return hashMap.get(key);
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
