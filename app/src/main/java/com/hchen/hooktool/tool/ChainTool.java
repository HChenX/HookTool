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

import static com.hchen.hooktool.data.ChainData.TYPE_ANY_CONSTRUCTOR;
import static com.hchen.hooktool.data.ChainData.TYPE_ANY_METHOD;
import static com.hchen.hooktool.data.ChainData.TYPE_CONSTRUCTOR;
import static com.hchen.hooktool.data.ChainData.TYPE_METHOD;
import static com.hchen.hooktool.hook.HookFactory.createHook;
import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.LogExpand.getTag;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logI;
import static com.hchen.hooktool.log.XposedLog.logW;
import static com.hchen.hooktool.tool.CoreTool.existsConstructor;
import static com.hchen.hooktool.tool.CoreTool.existsMethod;
import static com.hchen.hooktool.tool.CoreTool.findClass;
import static com.hchen.hooktool.tool.CoreTool.findConstructor;
import static com.hchen.hooktool.tool.CoreTool.findMethod;

import com.hchen.hooktool.data.ChainData;
import com.hchen.hooktool.data.HookState;
import com.hchen.hooktool.hook.IHook;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.stream.Collectors;

import de.robv.android.xposed.XposedBridge;

/**
 * 创建链式调用
 *
 * @author 焕晨HChen
 */
public final class ChainTool {
    private final ChainHook chainHook; // 创建 hook
    private ChainData cacheData;
    private final ArrayList<ChainData> chainDataList = new ArrayList<>(); // 链式数据
    private final ArrayList<ChainData> cacheDataList = new ArrayList<>(); // 暂时的缓存数据
    private final HashSet<Member> existingMembers = new HashSet<>();

    public ChainTool() {
        chainHook = new ChainHook(this);
    }

    public static void chain(String clazz, ChainTool chain) {
        chain.doFind(findClass(clazz).get());
    }

    public static void chain(String clazz, ClassLoader classLoader, ChainTool chain) {
        chain.doFind(findClass(clazz, classLoader).get());
    }

    public static void chain(Class<?> clazz, ChainTool chain) {
        if (clazz == null) {
            logW(getTag(), "Class is null, can't create chain hook!" + getStackTrace());
            return;
        }
        chain.doFind(clazz);
    }

    /**
     * 查找方法。
     */
    public ChainHook method(String name, Object... params) {
        cacheData = new ChainData(name, params);
        return chainHook;
    }

    public ChainHook methodIfExist(String name, Object... params) {
        cacheData = new ChainData(name, params);
        cacheData.setCheckExist(true);
        return chainHook;
    }

    public ChainHook anyMethod(String name) {
        cacheData = new ChainData(name);
        return chainHook;
    }

    /**
     * 查找构造函数。
     */
    public ChainHook constructor(Object... params) {
        cacheData = new ChainData(params);
        return chainHook;
    }

    public ChainHook constructorIfExist(Object... params) {
        cacheData = new ChainData(params);
        cacheData.setCheckExist(true);
        return chainHook;
    }

    public ChainHook anyConstructor() {
        cacheData = new ChainData();
        return chainHook;
    }

    // 各种奇奇怪怪的添加 >.<
    private void doFind(Class<?> clazz) {
        if (clazz == null) {
            cacheDataList.clear();
            return;
        }
        if (cacheDataList.isEmpty()) {
            logW(getTag(), "cache data list is empty, can't find or hook anything!" + getStackTrace());
            return;
        }

        ArrayList<ChainData> members = new ArrayList<>();
        for (ChainData cacheData : cacheDataList) {
            String UUID = cacheData.mType + "#" + clazz.getName() + "#" + cacheData.mName + "#" + Arrays.toString(cacheData.mParams);
            switch (cacheData.mType) {
                case TYPE_METHOD -> {
                    if (cacheData.mCheckExist)
                        if (!existsMethod(clazz, cacheData.mName, cacheData.mParams)) continue;
                    members.add(new ChainData(findMethod(clazz, cacheData.mName, cacheData.mParams).get()));
                }
                case TYPE_CONSTRUCTOR -> {
                    if (cacheData.mCheckExist)
                        if (!existsConstructor(clazz, cacheData.mParams)) continue;
                    members.add(new ChainData(findConstructor(clazz, cacheData.mParams).get()));
                }
                case TYPE_ANY_METHOD ->
                        members.addAll(CoreTool.findAllMethod(clazz, cacheData.mName).stream().map(
                                ChainData::new).collect(Collectors.toCollection(ArrayList::new)));
                case TYPE_ANY_CONSTRUCTOR ->
                        members.addAll(CoreTool.findAllConstructor(clazz).stream().map(
                                ChainData::new).collect(Collectors.toCollection(ArrayList::new)));
                default -> {
                    logW(getTag(), "Unknown type: " + cacheData.mType + getStackTrace());
                    members.clear();
                    continue;
                }
            }
            if (members.isEmpty()) continue;
            if (chainDataList.stream().noneMatch(c -> c.UUID.equals(UUID))) {
                Iterator<ChainData> iterator = members.iterator();
                while (iterator.hasNext()) {
                    ChainData memberData = iterator.next();
                    if (memberData.member == null || existingMembers.contains(memberData.member)) {
                        iterator.remove();
                        logW(getTag(), "This member maybe repeated or maybe is null, will remove it! " +
                                "\ndebug: " + UUID + "#member: " + memberData.member);
                        continue;
                    }
                    existingMembers.add(memberData.member);
                }
                if (members.isEmpty()) continue;
                chainDataList.add(new ChainData(new ArrayList<>(members), cacheData.iHook, HookState.NONE, UUID));
            } else
                logW(getTag(), "This member maybe repeated, will skip add it! \ndebug: " + UUID);
            members.clear();
        }
        cacheDataList.clear();
        doChainHook();
    }

    // 太复杂啦，我也迷糊了 >.<
    public void doChainHook() {
        ArrayList<ChainData> chainDataList = this.chainDataList;

        ListIterator<ChainData> iterator = chainDataList.listIterator();
        while (iterator.hasNext()) {
            ChainData chainData = iterator.next();
            if (chainData.hookState == HookState.HOOKED) continue;
            if (chainData.iHook == null) {
                logW(getTag(), "Action is null, can't hook! will remove this! \ndebug: " + chainData.UUID);
                iterator.remove();
                continue;
            }
            for (ChainData memberData : chainData.members) {
                try {
                    XposedBridge.hookMethod(memberData.member, createHook(getTag(), chainData.iHook));
                    logI(getTag(), "Success to hook: " + memberData.member);
                } catch (Throwable e) {
                    logE(getTag(), "Failed to hook: " + memberData.member, e);
                }
            }
            chainData.hookState = HookState.HOOKED;
            iterator.set(chainData);
        }
    }

    public static class ChainHook {
        private final ChainTool chain;

        public ChainHook(ChainTool chain) {
            this.chain = chain;
        }

        /**
         * Hook 动作。
         */
        public ChainTool hook(IHook iHook) {
            chain.cacheData.iHook = iHook;
            chain.cacheDataList.add(chain.cacheData);
            return chain;
        }

        /**
         * 直接返回指定值。
         */
        public ChainTool returnResult(final Object result) {
            chain.cacheData.iHook = CoreTool.returnResult(result);
            chain.cacheDataList.add(chain.cacheData);
            return chain;
        }

        /**
         * 拦截方法执行。
         */
        public ChainTool doNothing() {
            chain.cacheData.iHook = CoreTool.doNothing();
            chain.cacheDataList.add(chain.cacheData);
            return chain;
        }
    }
}
