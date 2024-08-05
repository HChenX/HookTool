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
import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.XposedLog.logD;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.data.ChainData;
import com.hchen.hooktool.data.HookState;
import com.hchen.hooktool.data.ToolData;
import com.hchen.hooktool.tool.itool.IChain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.stream.Collectors;

import de.robv.android.xposed.XposedBridge;

/**
 * 创建链式调用
 * <p>
 * Create a chain call
 *
 * @author 焕晨HChen
 */
public class ChainTool implements IChain {
    private final ToolData data;
    private final ChainHook chainHook;
    private ClassLoader classLoader = null;
    private final ArrayList<ChainData> chainDataList = new ArrayList<>();
    private ChainData chainData;
    private final ArrayList<ChainData> cacheDataList = new ArrayList<>();

    public ChainTool(ToolData data) {
        this.data = data;
        chainHook = new ChainHook(this);
    }

    public void chain(String clazz, ChainTool chain) {
        classLoader = null;
        chain(data.core.findClass(clazz), chain);
    }

    public void chain(String clazz, ClassLoader classLoader, ChainTool chain) {
        this.classLoader = classLoader;
        chain(data.core.findClass(clazz, classLoader), chain);
    }

    public void chain(Class<?> clazz, ChainTool chain) {
        if (clazz == null) {
            logW(data.tag(), "ChainTool: class is null!" + getStackTrace());
            return;
        }
        chain.doFind(clazz);
        doChainHook(chain);
    }

    /**
     * 查找方法。
     * <p>
     * Find method.
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
     * <p>
     * Find constructor.
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
                                data.core.findMethod(clazz, chainData.mName, data.convert.arrayToClass(chainData.mParams)),
                                HookState.NONE));
                    else
                        memberWithState.add(new ChainData(
                                data.core.findMethod(clazz, chainData.mName, data.convert.arrayToClass(classLoader, chainData.mParams)),
                                HookState.NONE));
                }
                case TYPE_CONSTRUCTOR -> {
                    UUID = chainData.mType + "#" + clazz.getName() + "#" + Arrays.toString(chainData.mParams);
                    if (classLoader == null)
                        memberWithState.add(new ChainData(
                                data.core.findConstructor(clazz, data.convert.arrayToClass(chainData.mParams)),
                                HookState.NONE));
                    else
                        memberWithState.add(new ChainData(
                                data.core.findConstructor(clazz, data.convert.arrayToClass(classLoader, chainData.mParams)),
                                HookState.NONE));
                }
                case TYPE_ANY_METHOD -> {
                    UUID = chainData.mType + "#" + clazz.getName() + "#" + chainData.mName;
                    memberWithState.addAll(data.core.findAnyMethod(clazz, chainData.mName).stream().map(
                            method -> new ChainData(method, HookState.NONE)).collect(Collectors.toCollection(ArrayList::new)));
                }
                case TYPE_ANY_CONSTRUCTOR -> {
                    UUID = chainData.mType + "#" + clazz.getName();
                    memberWithState.addAll(data.core.findAnyConstructor(clazz).stream().map(
                            constructor -> new ChainData(constructor, HookState.NONE)).collect(Collectors.toCollection(ArrayList::new)));
                }
                default -> memberWithState = new ArrayList<>();
            }
            ArrayList<ChainData> cache = new ArrayList<>(memberWithState);
            String finalUUID = UUID;
            if (chainDataList.stream().noneMatch(c -> c.UUID.equals(finalUUID))) {
                chainDataList.add(new ChainData(clazz.getSimpleName(),
                        chainData.mName, cache, chainData.iAction, UUID));
            } else
                logW(data.tag(), "ChainTool: this member maybe repeated! debug: [uuid: " + UUID + " ]");
            memberWithState.clear();
        }
        cacheDataList.clear();
    }

    // 太复杂啦，我也迷糊了 >.<
    public void doChainHook(ChainTool chain) {
        ArrayList<ChainData> chainDataList = chain.chainDataList;

        ListIterator<ChainData> iterator = chainDataList.listIterator();
        while (iterator.hasNext()) {
            ChainData chainData = iterator.next();
            String UUID = chainData.UUID;
            if (chainData.iAction == null) {
                logW(data.tag(), "ChainTool: action is null, can't hook! will remove this! debug: [uuid: " + UUID + " ]");
                iterator.remove();
                continue;
            }
            if (chainData.memberWithState.isEmpty()) {
                logW(data.tag(), "ChainTool: members is empty! debug: [uuid: " + UUID + " ]");
                continue;
            }
            ListIterator<ChainData> iteratorMember = chainData.memberWithState.listIterator();
            while (iteratorMember.hasNext()) {
                ChainData memberData = iteratorMember.next();
                switch (memberData.hookState) {
                    case NONE -> {
                        if (memberData.member == null) {
                            logW(data.tag(), "ChainTool: member is null, can't hook! will remove this! debug: [uuid: " + UUID + " ]");
                            memberData.hookState = HookState.FAILED;
                            iteratorMember.remove();
                        } else {
                            try {
                                XposedBridge.hookMethod(memberData.member, data.hook.createHook(chainData.iAction));
                                memberData.hookState = HookState.HOOKED;
                                logD(data.tag(), "ChainTool: success hook: " + memberData.member);
                            } catch (Throwable e) {
                                memberData.hookState = HookState.FAILED;
                                logE(data.tag(), e);
                            }
                            iteratorMember.set(memberData);
                        }
                    }
                    case FAILED -> {
                        logD(data.tag(), "ChainTool: members hooked: " + memberData.member);
                    }
                    case HOOKED -> {
                        logD(data.tag(), "ChainTool: members hook failed: " + memberData.member);
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
         * <p>
         * Hook action.
         */
        public ChainTool hook(IAction iAction) {
            chain.chainData.iAction = iAction;
            chain.cacheDataList.add(chain.chainData);
            return chain;
        }

        /**
         * 直接返回指定值。
         * <p>
         * Returns the specified value directly.
         */
        public ChainTool returnResult(final Object result) {
            chain.chainData.iAction = chain.data.core.returnResult(result);
            chain.cacheDataList.add(chain.chainData);
            return chain;
        }

        /**
         * 拦截方法执行。
         * <p>
         * Intercept method execution.
         */
        public ChainTool doNothing() {
            chain.chainData.iAction = chain.data.core.doNothing();
            chain.cacheDataList.add(chain.chainData);
            return chain;
        }
    }
}
