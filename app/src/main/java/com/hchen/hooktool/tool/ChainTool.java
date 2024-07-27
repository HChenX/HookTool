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
    private final ArrayList<ChainData> cacheData = new ArrayList<>();

    public ChainTool(ToolData data) {
        this.data = data;
        chainHook = new ChainHook(this);
    }

    @Override
    public void chain(String clazz, ChainTool chain) {
        classLoader = null;
        chain(data.getCoreTool().findClass(clazz), chain);
    }

    @Override
    public void chain(String clazz, ClassLoader classLoader, ChainTool chain) {
        this.classLoader = classLoader;
        chain(data.getCoreTool().findClass(clazz, classLoader), chain);
    }

    @Override
    public void chain(Class<?> clazz, ChainTool chain) {
        if (clazz == null) {
            logW(data.getTag(), "class is null! can't use chain!!");
            return;
        }
        chain.doFind(clazz);
        data.getActionTool().doAction(chain);
    }

    /**
     * 查找方法。
     * <p>
     * Find method.
     *
     * @param name   方法名
     * @param params 方法参数
     */
    @Override
    public ChainHook method(String name, Object... params) {
        chainData = new ChainData(name, params);
        return chainHook;
    }

    @Override
    public ChainHook anyMethod(String name) {
        chainData = new ChainData(name);
        return chainHook;
    }

    /**
     * 查找构造函数。
     * <p>
     * Find constructor.
     *
     * @param params 参数
     */
    @Override
    public ChainHook constructor(Object... params) {
        chainData = new ChainData(params);
        return chainHook;
    }

    @Override
    public ChainHook anyConstructor() {
        chainData = new ChainData();
        return chainHook;
    }

    protected void doFind(Class<?> clazz) {
        ArrayList<Member> mMembers = new ArrayList<>();
        for (ChainData data : cacheData) {
            switch (data.mType) {
                case TYPE_METHOD -> {
                    if (classLoader == null)
                        mMembers.add(this.data.getCoreTool().findMethod(clazz, data.mName, data.mParams));
                    else
                        mMembers.add(this.data.getCoreTool().findMethod(clazz, data.mName,
                                (Object) this.data.getConvertHelper().arrayToClass(classLoader, data.mParams)));
                }
                case TYPE_CONSTRUCTOR -> {
                    if (classLoader == null)
                        mMembers.add(this.data.getCoreTool().findConstructor(clazz, data.mParams));
                    else
                        mMembers.add(this.data.getCoreTool().findConstructor(clazz,
                                (Object) this.data.getConvertHelper().arrayToClass(classLoader, data.mParams)));
                }
                case TYPE_ANY_METHOD -> {
                    mMembers.addAll(this.data.getCoreTool().findAnyMethod(clazz, data.mName));
                }
                case TYPE_ANY_CONSTRUCTOR -> {
                    mMembers.addAll(this.data.getCoreTool().findAnyConstructor(clazz));
                }
                default -> mMembers = new ArrayList<>();
            }
            ArrayList<Member> cache = new ArrayList<>(mMembers);
            ArrayList<Member> finalMembers = mMembers;
            if (chainDataList.stream().noneMatch(chainData -> chainData.members.equals(finalMembers)))
                chainDataList.add(new ChainData(clazz.getSimpleName(),
                        data.mName, data.mType, cache, data.iAction, StateEnum.NONE));
            mMembers.clear();
        }
        cacheData.clear();
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
            chain.cacheData.add(chain.chainData);
            return chain;
        }
    }
}
