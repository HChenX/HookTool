package com.hchen.hooktool.tool.hook;

import static com.hchen.hooktool.log.XposedLog.logD;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;
import static de.robv.android.xposed.callbacks.XCallback.PRIORITY_HIGHEST;

import com.hchen.hooktool.action.Action;
import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.data.MemberData;
import com.hchen.hooktool.data.StateEnum;
import com.hchen.hooktool.tool.MethodTool;
import com.hchen.hooktool.utils.DataUtils;
import com.hchen.hooktool.utils.MethodOpt;

import java.lang.reflect.Member;
import java.util.ArrayList;

import de.robv.android.xposed.XposedBridge;

public class ActionTool extends MethodOpt {
    private final DataUtils utils;
    private int count = 0;
    private Object lastLabel = null;
    // private int classIndex = -1;
    // protected ArrayList<Member> members = null;

    public ActionTool(DataUtils data) {
        super(data);
        this.utils = data;
    }

    /**
     * 指定类 TAG
     */
    public ActionTool to(Object label) {
        utils.getClassTool().to(label);
        return utils.getActionTool();
    }

    /**
     * {@link ActionTool#hook(int, IAction)}
     */
    public MethodTool hook(IAction iAction) {
        return hook(-1, iAction);
    }

    /**
     * 执行 hook
     */
    public MethodTool hook(int methodIndex, IAction iAction) {
        if (actionSafe("hook", iAction)) {
            hookTool("hook", methodIndex, new IActionTool() {
                @Override
                public Action action(Member member) {
                    return hookTool(member, iAction);
                }
            });
        }
        return utils.getMethodTool();
    }

    /**
     * {@link ActionTool#returnResult(int, Object)}
     */
    public void returnResult(final Object result) {
        returnResult(-1, result);
    }

    /**
     * 直接返回指定值
     */
    public void returnResult(int methodIndex, final Object result) {
        hookTool("returnResult", methodIndex, new IActionTool() {
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

    /**
     * {@link ActionTool#doNothing(int)}
     */
    public void doNothing() {
        doNothing(-1);
    }

    /**
     * 使 hook 方法失效
     */
    public void doNothing(int methodIndex) {
        hookTool("doNothing", methodIndex, new IActionTool() {
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

    private void hookTool(String name, int methodIndex, IActionTool tool) {
        boolean useMethodIndex = methodIndex != -1;
        Object label = utils.getLabel();
        MemberData data = utils.members.get(label);
        if (data != null) {
            if (data.mClass == null) {
                logW(utils.getTAG(), "label: " + label + " this data class is null!");
            }
            if (lastLabel == null) lastLabel = label;
            else if (lastLabel != label) {
                lastLabel = label;
                count = 0;
            }
            int size = data.memberMap.size();
            ArrayList<Member> members = null;
            if (size >= 2) {
                if (count + 1 > size) {
                    logW(utils.getTAG(), name + " count > index, cant get memberMap! calss: " + data.mClass + "label: "
                            + label + " size: " + size + " count: " + count);
                    return;
                }
                count = data.count;
                // utils.setMethodCount(data.count);
                members = data.memberMap.get(useMethodIndex ? methodIndex : count);
                // count = count + 1;
            } else {
                members = data.memberMap.get(0);
                data.count = 0;
                // count = 1;
            }
            if (members == null) {
                logW(utils.getTAG(), name + " don't have anything can hook. class is: " + data.mClass);
            } else {
                if (isHooked(name, data, members)) {
                    logW(utils.getTAG(), "this method or constructor is hooked [" + name + "]! members: " + members);
                } else {
                    if (members.isEmpty()) {
                        logW(utils.getTAG(), "this members is empty! cant hook anything, will skip! class: "
                                + data.mClass + " label: " + label + " count: " + count);
                    } else {
                        for (Member member : members) {
                            try {
                                XposedBridge.hookMethod(member, tool.action(member));
                                logD(utils.getTAG(), "success hook: " + member + " class: " + data.mClass
                                        + " label: " + label + " count: " + count);
                            } catch (Throwable e) {
                                logE(utils.getTAG(), name + " hook method: " + member + " e: " + e);
                            }
                        }
                        setState(name, data, members);
                    }
                }
            }
            if (!useMethodIndex) {
                if (count + 1 < size)
                    data.count = count + 1;
                else
                    logW(utils.getTAG(), "this is a list can hook member: " + members);
            }
            utils.members.put(label, data);
        } else {
            logW(utils.getTAG(), name + " member data is null!");
        }
    }

    private boolean isHooked(String name, MemberData data, ArrayList<Member> members) {
        switch (name) {
            case "hook", "doNothing", "returnResult" -> {
                return data.stateMap.get(members) == StateEnum.HOOK;
            }
            default -> {
                return false;
            }
        }
    }

    private void setState(String name, MemberData data, ArrayList<Member> members) {
        switch (name) {
            case "hook", "doNothing", "returnResult" -> {
                data.stateMap.put(members, StateEnum.HOOK);
            }
        }
    }

    private Action hookTool(Member member, IAction iAction) {
        ParamTool paramTool = new ParamTool(member, utils.getTAG());
        StaticTool staticTool = new StaticTool(utils.getClassLoader(), utils.getTAG());
        return new Action(utils.getTAG()) {
            @Override
            protected void before(MethodHookParam param) {
                paramTool.setParam(param);
                iAction.before(paramTool, staticTool);
            }

            @Override
            protected void after(MethodHookParam param) {
                paramTool.setParam(param);
                iAction.after(paramTool, staticTool);
            }
        };
    }

    public boolean actionSafe(String name, Object iAction) {
        if (iAction == null) {
            logW(utils.getTAG(), name + " is null!");
            return false;
        }
        return true;
    }

    private interface IActionTool {
        Action action(Member member);
    }

    public MethodTool methodTool() {
        return utils.getMethodTool();
    }
}
