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
import static com.hchen.hooktool.utils.LogExpand.getStackTrace;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.data.ChainData;
import com.hchen.hooktool.data.StateEnum;
import com.hchen.hooktool.utils.ToolData;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * Hook 执行
 * <p>
 * Hook execution
 */
public class ActionTool {
    private final ToolData data;

    public ActionTool(ToolData data) {
        this.data = data;
    }

    protected void doAction(ChainTool chain) {
        ArrayList<ChainData> chainDataList = chain.chainDataList;

        ListIterator<ChainData> iterator = chainDataList.listIterator();
        while (iterator.hasNext()) {
            ChainData chainData = iterator.next();
            switch (chainData.stateEnum) {
                case StateEnum.NONE -> {
                    try {
                        if (chainData.members.isEmpty()) {
                            logW(data.tag(), "ChainData: members is empty! debug: [class: " + chainData.clazz +
                                    " , method: " + chainData.mName + " , type: " + chainData.mType + " ]" + getStackTrace());
                            chainData.stateEnum = StateEnum.FAILED;
                        } else if (chainData.members.stream().allMatch(Objects::isNull)) {
                            logW(data.tag(), "ChainData: all member is null! debug: [class: " + chainData.clazz +
                                    " , method: " + chainData.mName + " , type: " + chainData.mType + " ]" + getStackTrace());
                            chainData.stateEnum = StateEnum.FAILED;
                        } else if (chainData.iAction == null) {
                            logW(data.tag(), "ChainData: action is null! debug: [class: " + chainData.clazz +
                                    " , method: " + chainData.mName + " , type: " + chainData.mType + " ]" + getStackTrace());
                            chainData.stateEnum = StateEnum.FAILED;
                        } else {
                            for (Member m : chainData.members) {
                                XposedBridge.hookMethod(m, createHook(chainData.iAction));
                                logD(data.tag(), "ChainData: Success Hook: " + m);
                            }
                            chainData.stateEnum = StateEnum.HOOKED;
                        }
                    } catch (Throwable e) {
                        chainData.stateEnum = StateEnum.FAILED;
                        logE(data.tag(), e);
                    }
                    iterator.set(chainData);
                }
                case StateEnum.HOOKED -> {
                    logD(data.tag(), "ChainData: members hooked: " + chainData.members);
                }
                case StateEnum.FAILED -> {
                    logD(data.tag(), "ChainData: members hook failed: " + chainData.members);
                }
            }
        }
    }

    protected Action createHook(IAction iAction) {
        iAction.putUtils(data);
        return new Action(data.tag()) {
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
        private final String TAG;

        protected void before(MethodHookParam param) throws Throwable {
        }

        protected void after(MethodHookParam param) throws Throwable {
        }

        abstract void putThis(XC_MethodHook xcMethodHook);

        public Action(String tag) {
            super();
            TAG = tag;
            putThis(this);
        }

        public Action(String tag, int priority) {
            super(priority);
            TAG = tag;
            putThis(this);
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            try {
                before(param);
            } catch (Throwable e) {
                logE(TAG + ":before", e);
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            try {
                after(param);
            } catch (Throwable e) {
                logE(TAG + ":after", e);
            }
        }
    }
}
