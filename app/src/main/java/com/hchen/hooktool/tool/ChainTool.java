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
import static com.hchen.hooktool.helper.ConvertHelper.arrayToClass;
import static com.hchen.hooktool.hook.HookFactory.createHook;
import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.XposedLog.logD;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;
import static com.hchen.hooktool.tool.CoreTool.findAnyConstructor;
import static com.hchen.hooktool.tool.CoreTool.findAnyMethod;
import static com.hchen.hooktool.tool.CoreTool.findClass;
import static com.hchen.hooktool.tool.CoreTool.findConstructor;
import static com.hchen.hooktool.tool.CoreTool.findMethod;

import com.hchen.hooktool.data.ChainData;
import com.hchen.hooktool.data.HookState;
import com.hchen.hooktool.hook.IAction;
import com.hchen.hooktool.log.LogExpand;

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
public class ChainTool {
    private final ChainHook chainHook;
    private ChainData chainData;
    private ClassLoader classLoader = null;
    private final ArrayList<ChainData> chainDataList = new ArrayList<>();
    private final ArrayList<ChainData> cacheDataList = new ArrayList<>();

    public ChainTool() {
        chainHook = new ChainHook(this);
    }

    public static void chain(String clazz, ChainTool chain) {
        chain.classLoader = null;
        chain(findClass(clazz), chain);
    }

    public static void chain(String clazz, ClassLoader classLoader, ChainTool chain) {
        chain.classLoader = classLoader;
        chain(findClass(clazz, classLoader), chain);
    }

    public static void chain(Class<?> clazz, ChainTool chain) {
        if (clazz == null) {
            logW(tag(), "Class is null!" + getStackTrace());
            return;
        }
        chain.doFind(clazz);
        chain.doChainHook();
    }

    /**
     * 查找方法。
     */
    public ChainHook method(String name, Object... params) {
        chainData = new ChainData(name, params);
        return chainHook;
    }

    public ChainHook anyMethod(String name) {
        chainData = new ChainData(name);
        return chainHook;
    }

    /**
     * 查找构造函数。
     */
    public ChainHook constructor(Object... params) {
        chainData = new ChainData(params);
        return chainHook;
    }

    public ChainHook anyConstructor() {
        chainData = new ChainData();
        return chainHook;
    }

    // 各种奇奇怪怪的添加 >.<
    private void doFind(Class<?> clazz) {
        ArrayList<ChainData> memberWithState = new ArrayList<>();
        for (ChainData chainData : cacheDataList) {
            String UUID = "";
            switch (chainData.mType) {
                case TYPE_METHOD -> {
                    UUID = chainData.mType + "#" + clazz.getName() + "#" + chainData.mName + "#" + Arrays.toString(chainData.mParams);
                    if (classLoader == null)
                        memberWithState.add(new ChainData(
                                findMethod(clazz, chainData.mName, arrayToClass(chainData.mParams)),
                                HookState.NONE));
                    else
                        memberWithState.add(new ChainData(
                                findMethod(clazz, chainData.mName, arrayToClass(classLoader, chainData.mParams)),
                                HookState.NONE));
                }
                case TYPE_CONSTRUCTOR -> {
                    UUID = chainData.mType + "#" + clazz.getName() + "#" + Arrays.toString(chainData.mParams);
                    if (classLoader == null)
                        memberWithState.add(new ChainData(
                                findConstructor(clazz, arrayToClass(chainData.mParams)),
                                HookState.NONE));
                    else
                        memberWithState.add(new ChainData(
                                findConstructor(clazz, arrayToClass(classLoader, chainData.mParams)),
                                HookState.NONE));
                }
                case TYPE_ANY_METHOD -> {
                    UUID = chainData.mType + "#" + clazz.getName() + "#" + chainData.mName;
                    memberWithState.addAll(findAnyMethod(clazz, chainData.mName).stream().map(
                            method -> new ChainData(method, HookState.NONE)).collect(Collectors.toCollection(ArrayList::new)));
                }
                case TYPE_ANY_CONSTRUCTOR -> {
                    UUID = chainData.mType + "#" + clazz.getName();
                    memberWithState.addAll(findAnyConstructor(clazz).stream().map(
                            constructor -> new ChainData(constructor, HookState.NONE)).collect(Collectors.toCollection(ArrayList::new)));
                }
                default -> memberWithState = new ArrayList<>();
            }
            ArrayList<ChainData> cache = new ArrayList<>(memberWithState);
            String finalUUID = UUID;
            if (chainDataList.stream().noneMatch(c -> c.UUID.equals(finalUUID))) {
                if (chainDataList.stream().noneMatch(chainData1 -> {
                    // 使用HashSet来存储已有的成员，避免嵌套遍历
                    HashSet<Object> existingMembers = new HashSet<>();
                    chainData1.memberWithState.forEach(memberData -> {
                        if (memberData.member != null) {
                            existingMembers.add(memberData.member);
                        }
                    });

                    // 检查cache中是否有重复的成员
                    boolean repeat = false;
                    for (Iterator<ChainData> it = cache.iterator(); it.hasNext(); ) {
                        ChainData c = it.next();
                        if (c.member != null && existingMembers.contains(c.member)) {
                            it.remove(); // 从cache中移除重复成员
                            logW(tag(), "This member maybe repeated, will remove it! \ndebug: " + finalUUID);
                            repeat = true;
                        }
                    }
                    return cache.isEmpty() && repeat; // cache 列表处理后为空则排除，如果不为空则执行 hook。
                }))
                    chainDataList.add(new ChainData(clazz.getSimpleName(),
                            chainData.mName, cache, chainData.iAction, UUID));
            } else
                logW(tag(), "This member maybe repeated, will skip add it! \ndebug: " + UUID);
            memberWithState.clear();
        }
        cacheDataList.clear();
    }

    // 太复杂啦，我也迷糊了 >.<
    public void doChainHook() {
        ArrayList<ChainData> chainDataList = this.chainDataList;

        ListIterator<ChainData> iterator = chainDataList.listIterator();
        while (iterator.hasNext()) {
            ChainData chainData = iterator.next();
            String UUID = chainData.UUID;
            if (chainData.iAction == null) {
                logW(tag(), "Action is null, can't hook! will remove this! \ndebug: " + UUID);
                iterator.remove();
                continue;
            }
            if (chainData.memberWithState.isEmpty()) {
                logW(tag(), "Members is empty, will remove this! \ndebug: " + UUID);
                iterator.remove();
                continue;
            }
            ListIterator<ChainData> iteratorMember = chainData.memberWithState.listIterator();
            while (iteratorMember.hasNext()) {
                ChainData memberData = iteratorMember.next();
                switch (memberData.hookState) {
                    case NONE -> {
                        if (memberData.member == null) {
                            logW(tag(), "Member is null, can't hook! will remove this! \ndebug: " + UUID);
                            memberData.hookState = HookState.FAILED;
                            iteratorMember.remove();
                        } else {
                            try {
                                XposedBridge.hookMethod(memberData.member, createHook(tag(), chainData.iAction));
                                memberData.hookState = HookState.HOOKED;
                                logD(tag(), "Success to hook: " + memberData.member);
                            } catch (Throwable e) {
                                memberData.hookState = HookState.FAILED;
                                logE(tag(), "Failed to hook: " + memberData.member, e);
                            }
                            iteratorMember.set(memberData);
                        }
                    }
                    case FAILED -> {
                        logD(tag(), "Hooked members: " + memberData.member);
                    }
                    case HOOKED -> {
                        logD(tag(), "Failed to hook members: " + memberData.member);
                    }
                }
            }
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
        public ChainTool hook(IAction iAction) {
            chain.chainData.iAction = iAction;
            chain.cacheDataList.add(chain.chainData);
            return chain;
        }

        /**
         * 直接返回指定值。
         */
        public ChainTool returnResult(final Object result) {
            chain.chainData.iAction = CoreTool.returnResult(result);
            chain.cacheDataList.add(chain.chainData);
            return chain;
        }

        /**
         * 拦截方法执行。
         */
        public ChainTool doNothing() {
            chain.chainData.iAction = CoreTool.doNothing();
            chain.cacheDataList.add(chain.chainData);
            return chain;
        }
    }

    private static String tag() {
        String tag = LogExpand.tag();
        if (tag == null) return "ChainTool";
        return tag;
    }
}
