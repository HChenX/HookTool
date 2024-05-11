package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;
import static de.robv.android.xposed.callbacks.XCallback.PRIORITY_HIGHEST;

import com.hchen.hooktool.action.Action;
import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.callback.IAllAction;
import com.hchen.hooktool.data.MemberData;
import com.hchen.hooktool.utils.DataUtils;

import java.lang.reflect.Member;
import java.util.ArrayList;

import de.robv.android.xposed.XposedBridge;

public class ActionTool {
    private final DataUtils utils;
    // protected ArrayList<Member> members = null;

    public ActionTool(DataUtils data) {
        this.utils = data;
    }

    /**
     * {@link ActionTool#allAction(int, IAllAction)}
     */
    public MethodTool allAction(IAllAction iAllAction) {
        return allAction(utils.getCount(), iAllAction);
    }

    /**
     * 使用全部回调接口
     */
    public MethodTool allAction(int index, IAllAction iAllAction) {
        if (actionSafe("iAllAction", iAllAction)) {
            hookTool("allAction", index, new IActionTool() {
                @Override
                public Action action(Member member) {
                    return allActionTool(member, iAllAction);
                }
            });
        }
        return utils.getMethodTool();
    }

    /**
     * {@link ActionTool#after(int, IAction)}
     */
    public MethodTool after(IAction iAction) {
        return after(utils.getCount(), iAction);
    }

    /**
     * 使用 after
     */
    public MethodTool after(int index, IAction iAction) {
        if (actionSafe("iAction after", iAction)) {
            hookTool("after", index, new IActionTool() {
                @Override
                public Action action(Member member) {
                    return afterTool(member, iAction);
                }
            });
        }
        return utils.getMethodTool();
    }

    /**
     * {@link ActionTool#before(int, IAction)}
     */
    public MethodTool before(IAction iAction) {
        return before(utils.getCount(), iAction);
    }

    /**
     * 使用 before
     */
    public MethodTool before(int index, IAction iAction) {
        if (actionSafe("iAction before", iAction)) {
            hookTool("before", index, new IActionTool() {
                @Override
                public Action action(Member member) {
                    return beforeTool(member, iAction);
                }
            });
        }
        return utils.getMethodTool();
    }

    /**
     * {@link ActionTool#returnResult(int, Object)}
     */
    public void returnResult(final Object result) {
        returnResult(utils.getCount(), result);
    }

    /**
     * 直接返回指定值
     */
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

    /**
     * {@link ActionTool#doNothing(int)}
     */
    public void doNothing() {
        doNothing(utils.getCount());
    }

    /**
     * 是 hook 方法失效
     */
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
        if (utils.members.size() > index) {
            MemberData data = utils.members.get(index);
            if (data != null) {
                ArrayList<Member> members = data.mMethod;
                if (members == null) {
                    members = data.mConstructor;
                }
                if (members == null) {
                    logW(utils.getTAG(), name + " don't have anything can hook.");
                    return;
                }
                if (isHooked(name, data)) {
                    logW(utils.getTAG(), "this method or constructor is hooked [" + name + "]! members: " + members);
                    return;
                }
                for (Member member : members) {
                    try {
                        XposedBridge.hookMethod(member, tool.action(member));
                    } catch (Throwable e) {
                        logE(utils.getTAG(), name + " hook method: " + member + " e: " + e);
                    }
                }
                setState(name, data);
                utils.members.put(index, data);
            } else {
                logW(utils.getTAG(), name + " member data is null!");
            }
        } else {
            logW(utils.getTAG(), name + " members size < index!");
        }
    }

    private boolean isHooked(String name, MemberData data) {
        if (data.allAction) return true;
        switch (name) {
            case "after" -> {
                return data.after;
            }
            case "before", "returnResult", "doNothing" -> {
                return data.before;
            }
            case "allAction" -> {
                if (data.after || data.before) {
                    return true;
                }
            }
            default -> {
                return false;
            }
        }
        return false;
    }

    private void setState(String name, MemberData data) {
        switch (name) {
            case "after" -> data.after = true;
            case "before", "returnResult", "doNothing" -> data.before = true;
            case "allAction" -> data.allAction = true;
        }
    }

    private Action allActionTool(Member member, IAllAction iAllAction) {
        ParamTool<Object> paramTool = new ParamTool<>(member, utils.getTAG());
        StaticTool<Object> staticTool = new StaticTool<>(utils.getClassLoader(), utils.getTAG());
        return new Action(utils.getTAG()) {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                paramTool.setParam(param);
                iAllAction.before(paramTool, staticTool);
            }

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                paramTool.setParam(param);
                iAllAction.after(paramTool, staticTool);
            }
        };
    }

    private Action afterTool(Member member, IAction iAction) {
        ParamTool<Object> paramTool = new ParamTool<>(member, utils.getTAG());
        StaticTool<Object> staticTool = new StaticTool<>(utils.getClassLoader(), utils.getTAG());
        return new Action(utils.getTAG()) {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                paramTool.setParam(param);
                iAction.action(paramTool, staticTool);
            }
        };
    }

    private Action beforeTool(Member member, IAction iAction) {
        ParamTool<Object> paramTool = new ParamTool<>(member, utils.getTAG());
        StaticTool<Object> staticTool = new StaticTool<>(utils.getClassLoader(), utils.getTAG());
        return new Action(utils.getTAG()) {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                paramTool.setParam(param);
                iAction.action(paramTool, staticTool);
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

    interface IActionTool {
        Action action(Member member);
    }

    public MethodTool methodTool() {
        return utils.getMethodTool();
    }
}
