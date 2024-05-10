package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;
import static de.robv.android.xposed.callbacks.XCallback.PRIORITY_HIGHEST;

import com.hchen.hooktool.action.Action;
import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.callback.IAllAction;
import com.hchen.hooktool.utils.DataUtils;
import com.hchen.hooktool.utils.SafeUtils;

import java.lang.reflect.Member;
import java.util.ArrayList;

import de.robv.android.xposed.XposedBridge;

public class ActionTool {
    private final DataUtils utils;
    private final SafeUtils safe;
    protected ArrayList<Member> members = null;

    public ActionTool(DataUtils data) {
        this.utils = data;
        this.safe = data.safeUtils;
    }

    public MethodTool allAction(IAllAction iAllAction) {
        return allAction(-1, iAllAction);
    }

    public MethodTool allAction(int index, IAllAction iAllAction) {
        if (safe.actionSafe("iAllAction", iAllAction)) {
            hookTool("allAction", index, new IActionTool() {
                @Override
                public Action action(Member member) {
                    return allActionTool(member, iAllAction);
                }
            });
        }
        return utils.getMethodTool();
    }

    public MethodTool after(IAction iAction) {
        return after(-1, iAction);
    }

    public MethodTool after(int index, IAction iAction) {
        if (safe.actionSafe("iAction after", iAction)) {
            hookTool("after", index, new IActionTool() {
                @Override
                public Action action(Member member) {
                    return afterTool(member, iAction);
                }
            });
        }
        return utils.getMethodTool();
    }

    public MethodTool before(IAction iAction) {
        return before(-1, iAction);
    }

    public MethodTool before(int index, IAction iAction) {
        if (safe.actionSafe("iAction before", iAction)) {
            hookTool("before", index, new IActionTool() {
                @Override
                public Action action(Member member) {
                    return beforeTool(member, iAction);
                }
            });
        }
        return utils.getMethodTool();
    }

    public void returnResult(final Object result) {
        returnResult(-1, result);
    }

    public void returnResult(int index, final Object result) {
        hookTool("returnResult", index, new IActionTool() {
            @Override
            public Action action(Member member) {
                return new Action(utils.getTAG()) {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(result);
                    }
                };
            }
        });
    }

    public void doNothing() {
        doNothing(-1);
    }

    public void doNothing(int index) {
        hookTool("doNothing", index, new IActionTool() {
            @Override
            public Action action(Member member) {
                return new Action(utils.getTAG(), PRIORITY_HIGHEST * 2) {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(null);
                    }
                };
            }
        });
    }

    private void hookTool(String name, int index, IActionTool tool) {
        if (index == -1) {
            if (members != null) {
                if (!members.isEmpty()) {
                    for (Member member : members) {
                        try {
                            XposedBridge.hookMethod(member, tool.action(member));
                        } catch (Throwable e) {
                            logE(utils.getTAG(), name + " hook method: " + member + " e: " + e);
                        }
                    }
                } else {
                    logW(utils.getTAG(), name + " methods is empty, hook nothing.");
                }
            } else {
                logW(utils.getTAG(), name + " methods is null, cant use this action.");
            }
            members = null;
            return;
        }
        if (utils.methods.size() > index) {
            ArrayList<Member> methods = utils.methods.get(index);
            if (!methods.isEmpty()) {
                for (Member member : methods) {
                    try {
                        XposedBridge.hookMethod(member, tool.action(member));
                    } catch (Throwable e) {
                        logE(utils.getTAG(), name + " hook method: " + member + " e: " + e);
                    }
                }
            } else {
                logW(utils.getTAG(), name + " methods is empty!");
            }
        } else {
            logW(utils.getTAG(), name + " methods size < index!");
        }
    }

    private Action allActionTool(Member member, IAllAction iAllAction) {
        ParamTool paramTool = new ParamTool(utils.getTAG());
        return new Action(utils.getTAG()) {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                paramTool.setParam(param);
                iAllAction.before(paramTool);
            }

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                paramTool.setParam(param);
                iAllAction.after(paramTool);
            }
        };
    }

    private Action afterTool(Member member, IAction iAction) {
        ParamTool paramTool = new ParamTool(utils.getTAG());
        return new Action(utils.getTAG()) {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                paramTool.setParam(param);
                iAction.action(paramTool);
            }
        };
    }

    private Action beforeTool(Member member, IAction iAction) {
        ParamTool paramTool = new ParamTool(utils.getTAG());
        return new Action(utils.getTAG()) {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                paramTool.setParam(param);
                iAction.action(paramTool);
            }
        };
    }

    interface IActionTool {
        Action action(Member member);
    }

    public MethodTool methodTool() {
        return utils.getMethodTool();
    }
}
