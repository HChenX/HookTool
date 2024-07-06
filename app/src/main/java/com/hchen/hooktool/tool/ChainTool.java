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

import static com.hchen.hooktool.data.ChainData.TYPE_CONSTRUCTOR;
import static com.hchen.hooktool.data.ChainData.TYPE_METHOD;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.data.ChainData;
import com.hchen.hooktool.data.StateEnum;
import com.hchen.hooktool.utils.DataUtils;

import java.lang.reflect.Member;
import java.util.ArrayList;

/**
 * 创建链式调用
 */
public class ChainTool {
    protected Member mMember = null;
    private final DataUtils utils;
    private final ChainHook chainHook;
    protected final ArrayList<ChainData> chainData = new ArrayList<>();
    private final ArrayList<ChainData> cacheData = new ArrayList<>();

    public ChainTool(DataUtils utils) {
        this.utils = utils;
        chainHook = new ChainHook(this);
    }

    public ChainHook method(String name, Object... params) {
        cacheData.add(new ChainData(name, params));
        return chainHook;
    }

    public ChainHook constructor(Object... params) {
        cacheData.add(new ChainData(params));
        return chainHook;
    }

    protected void doFind(Class<?> clazz) {
        for (ChainData data : cacheData) {
            switch (data.mType) {
                case TYPE_METHOD -> {
                    mMember = utils.getCoreTool().findMethod(clazz, data.mName, data.mParams);
                }
                case TYPE_CONSTRUCTOR -> {
                    mMember = utils.getCoreTool().findConstructor(clazz, data.mParams);
                }
                default -> mMember = null;
            }
            chainData.add(new ChainData(mMember, chainHook.iAction, StateEnum.NONE));
        }
        cacheData.clear();
    }

    protected ArrayList<ChainData> getChainData() {
        return chainData;
    }

    public static class ChainHook {
        private final ChainTool chain;
        protected IAction iAction;

        public ChainHook(ChainTool chain) {
            this.chain = chain;
        }

        public ChainTool hook(IAction iAction) {
            this.iAction = iAction;
            return chain;
        }
    }
}
