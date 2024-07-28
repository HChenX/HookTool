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
import static com.hchen.hooktool.log.XposedLog.logW;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.data.ChainData;
import com.hchen.hooktool.data.StateEnum;
import com.hchen.hooktool.itool.IChain;
import com.hchen.hooktool.utils.ToolData;

import java.lang.reflect.Member;
import java.util.ArrayList;

/**
 * 创建链式调用
 * <p>
 * Create a chain call
 */
public class ChainTool implements IChain {
    private final ToolData data;
    private final ChainHook chainHook;
    private ClassLoader classLoader = null;
    protected ChainData chainData;
    protected final ArrayList<ChainData> chainDataList = new ArrayList<>();
    private final ArrayList<ChainData> cacheDataList = new ArrayList<>();

    public ChainTool(ToolData data) {
        this.data = data;
        chainHook = new ChainHook(this);
    }

    public void chain(String clazz, ChainTool chain) {
        classLoader = null;
        chain(data.coreTool().findClass(clazz), chain);
    }

    public void chain(String clazz, ClassLoader classLoader, ChainTool chain) {
        this.classLoader = classLoader;
        chain(data.coreTool().findClass(clazz, classLoader), chain);
    }

    public void chain(Class<?> clazz, ChainTool chain) {
        if (clazz == null) {
            logW(data.tag(), "class is null! can't use chain!!");
            return;
        }
        chain.doFind(clazz);
        data.actionTool().doAction(chain);
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

    protected void doFind(Class<?> clazz) {
        ArrayList<Member> mMembers = new ArrayList<>();
        for (ChainData chainData : cacheDataList) {
            switch (chainData.mType) {
                case TYPE_METHOD -> {
                    if (classLoader == null)
                        mMembers.add(data.coreTool().findMethod(clazz, chainData.mName, chainData.mParams));
                    else
                        mMembers.add(data.coreTool().findMethod(clazz, chainData.mName,
                                (Object) data.convertHelper().arrayToClass(classLoader, chainData.mParams)));
                }
                case TYPE_CONSTRUCTOR -> {
                    if (classLoader == null)
                        mMembers.add(data.coreTool().findConstructor(clazz, chainData.mParams));
                    else
                        mMembers.add(data.coreTool().findConstructor(clazz,
                                (Object) data.convertHelper().arrayToClass(classLoader, chainData.mParams)));
                }
                case TYPE_ANY_METHOD -> {
                    mMembers.addAll(data.coreTool().findAnyMethod(clazz, chainData.mName));
                }
                case TYPE_ANY_CONSTRUCTOR -> {
                    mMembers.addAll(data.coreTool().findAnyConstructor(clazz));
                }
                default -> mMembers = new ArrayList<>();
            }
            ArrayList<Member> cache = new ArrayList<>(mMembers);
            ArrayList<Member> finalMembers = mMembers;
            if (chainDataList.stream().noneMatch(d -> d.members.equals(finalMembers)))
                chainDataList.add(new ChainData(clazz.getSimpleName(),
                        chainData.mName, chainData.mType, cache, chainData.iAction, StateEnum.NONE));
            mMembers.clear();
        }
        cacheDataList.clear();
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
            chain.chainData.iAction = chain.data.coreTool().returnResult(result);
            chain.cacheDataList.add(chain.chainData);
            return chain;
        }

        /**
         * 拦截方法执行。
         * <p>
         * Intercept method execution.
         */
        public ChainTool doNothing() {
            chain.chainData.iAction = chain.data.coreTool().doNothing();
            chain.cacheDataList.add(chain.chainData);
            return chain;
        }
    }
}
