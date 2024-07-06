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

import static com.hchen.hooktool.log.XposedLog.logD;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.data.ChainData;
import com.hchen.hooktool.data.StateEnum;
import com.hchen.hooktool.utils.DataUtils;
import com.hchen.hooktool.utils.LogExpand;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.ListIterator;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * Hook 执行类
 */
public class ActionTool {
    private final DataUtils utils;
    private ChainTool chain;

    public ActionTool(DataUtils data) {
        this.utils = data;
    }

    public void chain(String clazz, ChainTool chain) {
        chain(utils.getCoreTool().findClass(clazz), chain);
    }

    public void chain(String clazz, ClassLoader classLoader, ChainTool chain) {
        chain(utils.getCoreTool().findClass(clazz, classLoader), chain);
    }

    public void chain(Class<?> clazz, ChainTool chain) {
        this.chain = chain;
        chain.doFind(clazz);
        doAction();
    }

    private void doAction() {
        ArrayList<ChainData> chainData = chain.getChainData();
        if (chainData == null) {
            logW(utils.getTAG(), "chain data is null!!");
            return;
        }

        ListIterator<ChainData> iterator = chainData.listIterator();
        while (iterator.hasNext()) {
            ChainData data = iterator.next();
            switch (data.stateEnum) {
                case StateEnum.NONE -> {
                    try {
                        XposedBridge.hookMethod(data.member, createHook(data.member, data.iAction));
                        data.stateEnum = StateEnum.HOOKED;
                        logD(utils.getTAG(), "success to hook: " + data.member);
                    } catch (Throwable e) {
                        data.stateEnum = StateEnum.FAILED;
                        logE(utils.getTAG(), e);
                    }
                    iterator.set(data);
                }
                case StateEnum.HOOKED -> {
                    logD(utils.getTAG(), "this method is hooked: " + data.member);
                }
                case StateEnum.FAILED -> {
                    logD(utils.getTAG(), "this method is hook failed: " + data.member);
                }
            }
        }
    }

    protected Action createHook(Member member, IAction iAction) {
        iAction.putUtils(utils);
        return new Action(member, utils.getTAG()) {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                iAction.putMethodHookParam(param);
                iAction.before();
            }

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                iAction.putMethodHookParam(param);
                iAction.after();
            }

            @Override
            void putThis(XC_MethodHook xcMethodHook) {
                iAction.putXCMethodHook(xcMethodHook);
            }
        };
    }

    protected abstract static class Action extends XC_MethodHook {
        private String TAG = null;
        private LogExpand logExpand = null;
        private boolean useLogExpand = false;

        protected void before(MethodHookParam param) throws Throwable {
        }

        protected void after(MethodHookParam param) throws Throwable {
        }

        abstract void putThis(XC_MethodHook xcMethodHook);

        public Action(Member member, String tag) {
            super();
            TAG = tag;
            putThis(this);
            this.useLogExpand = DataUtils.useLogExpand;
            if (useLogExpand) this.logExpand = new LogExpand(member, TAG);
        }

        public Action(Member member, String tag, int priority) {
            super(priority);
            TAG = tag;
            putThis(this);
            this.useLogExpand = DataUtils.useLogExpand;
            if (useLogExpand) this.logExpand = new LogExpand(member, TAG);
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            try {
                before(param);
                if (useLogExpand) {
                    if (logExpand != null) {
                        logExpand.setParam(param);
                        logExpand.detailedLogs();
                    }
                }
            } catch (Throwable e) {
                logE(TAG + ":" + "before", e);
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            try {
                after(param);
            } catch (Throwable e) {
                logE(TAG + ":" + "after", e);
            }
        }
    }
}
