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
package com.hchen.hooktool.tool;

import static com.hchen.hooktool.data.ChainData.TYPE_ANY_CONSTRUCTOR;
import static com.hchen.hooktool.data.ChainData.TYPE_ANY_METHOD;
import static com.hchen.hooktool.data.ChainData.TYPE_CONSTRUCTOR;
import static com.hchen.hooktool.data.ChainData.TYPE_METHOD;
import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.LogExpand.getTag;
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
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

/**
 * 创建链式调用
 *
 * @author 焕晨HChen
 */
public final class ChainTool {
    private final ChainHook mChainHook; // 创建 hook
    private ChainData mCacheData;
    private final List<ChainData> mChainDataList = new ArrayList<>(); // 链式数据
    private final List<ChainData> mCacheDataList = new ArrayList<>(); // 暂时的缓存数据
    private final HashSet<Member> mExistingMembers = new HashSet<>();

    public ChainTool() {
        mChainHook = new ChainHook(this);
    }

    public static void chain(String clazz, ChainTool chain) {
        chain.doFind(findClass(clazz));
    }

    public static void chain(String clazz, ClassLoader classLoader, ChainTool chain) {
        chain.doFind(findClass(clazz, classLoader));
    }

    public static void chain(Class<?> clazz, ChainTool chain) {
        if (clazz == null) {
            logW(getTag(), "Class is null, can't create chain hook!", getStackTrace());
            return;
        }
        chain.doFind(clazz);
    }

    /**
     * 查找方法。
     */
    public ChainHook method(String name, Object... params) {
        mCacheData = new ChainData(name, params);
        return mChainHook;
    }

    public ChainHook methodIfExist(String name, Object... params) {
        mCacheData = new ChainData(name, params);
        mCacheData.setCheckExist(true);
        return mChainHook;
    }

    public ChainHook anyMethod(String name) {
        mCacheData = new ChainData(name);
        return mChainHook;
    }

    /**
     * 查找构造函数。
     */
    public ChainHook constructor(Object... params) {
        mCacheData = new ChainData(params);
        return mChainHook;
    }

    public ChainHook constructorIfExist(Object... params) {
        mCacheData = new ChainData(params);
        mCacheData.setCheckExist(true);
        return mChainHook;
    }

    public ChainHook anyConstructor() {
        mCacheData = new ChainData();
        return mChainHook;
    }

    // 各种奇奇怪怪的添加 >.<
    private void doFind(Class<?> clazz) {
        if (clazz == null) {
            mCacheDataList.clear();
            return;
        }
        if (mCacheDataList.isEmpty()) {
            logW(getTag(), "cache data list is empty, can't find or hook anything!", getStackTrace());
            return;
        }

        List<ChainData> members = new ArrayList<>();
        for (ChainData cacheData : mCacheDataList) {
            String UUID = cacheData.mType + "#" + clazz.getName() + "#" + cacheData.mName + "#" + Arrays.toString(cacheData.mParams);
            switch (cacheData.mType) {
                case TYPE_METHOD -> {
                    if (cacheData.mCheckExist)
                        if (!existsMethod(clazz, cacheData.mName, cacheData.mParams)) continue;

                    members.add(new ChainData(findMethod(clazz, cacheData.mName, cacheData.mParams)));
                }
                case TYPE_CONSTRUCTOR -> {
                    if (cacheData.mCheckExist)
                        if (!existsConstructor(clazz, cacheData.mParams)) continue;

                    members.add(new ChainData(findConstructor(clazz, cacheData.mParams)));
                }
                case TYPE_ANY_METHOD ->
                    members.addAll(Arrays.stream(CoreTool.findAllMethod(clazz, cacheData.mName)).map(
                        ChainData::new).collect(Collectors.toCollection(ArrayList::new)));
                case TYPE_ANY_CONSTRUCTOR ->
                    members.addAll(Arrays.stream(CoreTool.findAllConstructor(clazz)).map(
                        ChainData::new).collect(Collectors.toCollection(ArrayList::new)));
                default -> {
                    logW(getTag(), "Unknown type: " + cacheData.mType, getStackTrace());
                    members.clear();
                    continue;
                }
            }
            if (members.isEmpty()) continue;
            if (mChainDataList.stream().noneMatch(c -> c.UUID.equals(UUID))) {
                Iterator<ChainData> iterator = members.iterator();
                while (iterator.hasNext()) {
                    ChainData memberData = iterator.next();
                    if (memberData.mMember == null || mExistingMembers.contains(memberData.mMember)) {
                        iterator.remove();
                        logW(getTag(), "This member maybe repeated or maybe is null, will remove it! " + "\ndebug: " + UUID + "#member: " + memberData.mMember);
                        continue;
                    }
                    mExistingMembers.add(memberData.mMember);
                }
                if (members.isEmpty()) continue;
                mChainDataList.add(new ChainData(new ArrayList<>(members), cacheData.mIHook, HookState.NONE, UUID));
            } else
                logW(getTag(), "This member maybe repeated, will skip add it! \ndebug: " + UUID);
            members.clear();
        }
        mCacheDataList.clear();
        doChainHook();
    }

    // 太复杂啦，我也迷糊了 >.<
    public void doChainHook() {
        ListIterator<ChainData> iterator = mChainDataList.listIterator();
        while (iterator.hasNext()) {
            ChainData chainData = iterator.next();
            if (chainData.mHookState == HookState.HOOKED) continue;
            if (chainData.mIHook == null) {
                logW(getTag(), "Action is null, can't hook! will remove this! \ndebug: " + chainData.UUID);
                iterator.remove();
                continue;
            }
            for (ChainData memberData : chainData.mMembers) {
                CoreBase.baseHookAll(new Member[]{memberData.mMember}, chainData.mIHook);
            }
            chainData.mHookState = HookState.HOOKED;
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
            chain.mCacheData.mIHook = iHook;
            chain.mCacheDataList.add(chain.mCacheData);
            return chain;
        }

        /**
         * 直接返回指定值。
         */
        public ChainTool returnResult(final Object result) {
            chain.mCacheData.mIHook = CoreTool.returnResult(result);
            chain.mCacheDataList.add(chain.mCacheData);
            return chain;
        }

        /**
         * 拦截方法执行。
         */
        public ChainTool doNothing() {
            chain.mCacheData.mIHook = CoreTool.doNothing();
            chain.mCacheDataList.add(chain.mCacheData);
            return chain;
        }
    }
}
