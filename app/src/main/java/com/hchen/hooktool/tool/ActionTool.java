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
import static de.robv.android.xposed.callbacks.XCallback.PRIORITY_HIGHEST;

import androidx.annotation.NonNull;

import com.hchen.hooktool.action.Action;
import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.data.MemberData;
import com.hchen.hooktool.data.StateEnum;
import com.hchen.hooktool.utils.DataUtils;
import com.hchen.hooktool.utils.MethodOpt;

import java.lang.reflect.Member;
import java.util.ArrayList;

import de.robv.android.xposed.XposedBridge;

public class ActionTool extends MethodOpt {
    private final DataUtils utils;
    // private int classIndex = -1;
    // protected ArrayList<Member> members = null;

    public ActionTool(DataUtils data) {
        super(data);
        this.utils = data;
    }

    /**
     * 指定类标签
     */
    public ActionTool to(@NonNull Object label) {
        utils.getClassTool().to(label);
        return utils.getActionTool();
    }

    /**
     * 执行 hook
     */
    public MethodTool hook(IAction iAction) {
        if (actionSafe("hook", iAction)) {
            hookTool("hook", new IActionTool() {
                @Override
                public Action action() {
                    return hookTool(iAction);
                }
            });
        }
        return utils.getMethodTool();
    }

    /**
     * 直接 Hook 指定方法
     */
    public MethodTool hook(Member member, IAction iAction) {
        if (member == null || iAction == null) {
            logW(utils.getTAG(), "member or iAction is null, cant hook!");
            return utils.getMethodTool();
        }
        try {
            XposedBridge.hookMethod(member, hookTool(iAction));
        } catch (Throwable e) {
            logE(utils.getTAG(), "hook: [" + member + "] failed!", e);
        }
        return utils.getMethodTool();
    }

    /**
     * 直接返回指定值
     */
    public MethodTool returnResult(final Object result) {
        hookTool("returnResult", new IActionTool() {
            @Override
            public Action action() {
                return new Action(utils.getTAG()) {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(result);
                    }
                };
            }
        });
        return utils.getMethodTool();
    }

    /**
     * 使 hook 方法失效
     */
    public MethodTool doNothing() {
        hookTool("doNothing", new IActionTool() {
            @Override
            public Action action() {
                return new Action(utils.getTAG(), PRIORITY_HIGHEST * 2) {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(null);
                    }
                };
            }
        });
        return utils.getMethodTool();
    }

    private void hookTool(String name, IActionTool tool) {
        Object label = utils.getLabel();
        MemberData data = utils.members.get(label);
        if (data != null) {
            if (data.mClass == null) {
                logW(utils.getTAG(), "label: [" + label + "] this data class is null!");
            }
            int count = data.memberMap.size();
            if (count == 0) {
                logW(utils.getTAG(), name + " member map is empty! class: " + data.mClass);
            } else {
                ArrayList<Member> members = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    members = data.memberMap.get(i);
                    if (!isHooked(name, data, members)) {
                        logD(utils.getTAG(), "now try to hook member: " + members);
                        break;
                    }
                }
                if (members == null) {
                    logW(utils.getTAG(), name + " don't have anything can hook. class is: " + data.mClass);
                } else {
                    if (members.isEmpty()) {
                        logW(utils.getTAG(), "this members is empty! cant hook anything, will skip! class: ["
                                + data.mClass + "] label: [" + label + "] count: " + count);
                    } else {
                        for (Member member : members) {
                            try {
                                XposedBridge.hookMethod(member, tool.action());
                                logD(utils.getTAG(), "success hook: [" + member + "] class: [" + data.mClass
                                        + "] label: [" + label + "] count: " + count);
                                setState(name, data, members, true);
                            } catch (Throwable e) {
                                logE(utils.getTAG(), name + " hook method: " + member, e);
                                setState(name, data, members, false);
                            }
                        }

                    }
                }
                utils.members.put(label, data);
            }
        } else {
            logW(utils.getTAG(), name + " member data is null!");
        }
    }

    private boolean isHooked(String name, MemberData data, ArrayList<Member> members) {
        switch (name) {
            case "hook", "doNothing", "returnResult" -> {
                return data.stateMap.get(members) == StateEnum.HOOK
                        || data.stateMap.get(members) == StateEnum.Failed;
            }
            default -> {
                return false;
            }
        }
    }

    private void setState(String name, MemberData data, ArrayList<Member> members, boolean isSuccess) {
        switch (name) {
            case "hook", "doNothing", "returnResult" -> {
                data.stateMap.put(members, isSuccess ? StateEnum.HOOK : StateEnum.Failed);
            }
        }
    }

    private Action hookTool(IAction iAction) {
        ParamTool paramTool = new ParamTool(utils);
        return new Action(utils.getTAG()) {
            @Override
            protected void before(MethodHookParam param) {
                paramTool.setParam(param);
                iAction.before(paramTool);
            }

            @Override
            protected void after(MethodHookParam param) {
                paramTool.setParam(param);
                iAction.after(paramTool);
            }
        };
    }

    private boolean actionSafe(String name, Object iAction) {
        if (iAction == null) {
            logW(utils.getTAG(), name + " is null!");
            return false;
        }
        return true;
    }

    private interface IActionTool {
        Action action();
    }

    public MethodTool methodTool() {
        return utils.getMethodTool();
    }
}
