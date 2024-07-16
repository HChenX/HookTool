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
 */
public class ChainTool implements IChain {
    private final ToolData data;
    private final ChainHook chainHook;
    protected ChainData chainData;
    protected final ArrayList<ChainData> chainDataList = new ArrayList<>();
    private final ArrayList<ChainData> cacheData = new ArrayList<>();

    public ChainTool(ToolData data) {
        this.data = data;
        chainHook = new ChainHook(this);
    }

    @Override
    public void chain(String clazz, ChainTool chain) {
        chain(data.getCoreTool().findClass(clazz), chain);
    }

    @Override
    public void chain(String clazz, ClassLoader classLoader, ChainTool chain) {
        chain(data.getCoreTool().findClass(clazz, classLoader), chain);
    }

    @Override
    public void chain(Class<?> clazz, ChainTool chain) {
        if (clazz == null) {
            logW(data.getTAG(), "class is null! can't use chain!!");
            return;
        }
        chain.doFind(clazz);
        data.getActionTool().doAction(chain);
    }

    /**
     * 查找方法。
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
                    mMembers.add(this.data.getCoreTool().findMethod(clazz, data.mName, data.mParams));
                }
                case TYPE_CONSTRUCTOR -> {
                    mMembers.add(this.data.getCoreTool().findConstructor(clazz, data.mParams));
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
            ArrayList<Member> finalMMembers = mMembers;
            if (chainDataList.stream().noneMatch(chainData -> chainData.members.equals(finalMMembers)))
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
         * hook 动作。
         */
        public ChainTool hook(IAction iAction) {
            chain.chainData.iAction = iAction;
            chain.cacheData.add(chain.chainData);
            return chain;
        }
    }
}
