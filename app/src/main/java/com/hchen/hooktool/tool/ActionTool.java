package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;
import static de.robv.android.xposed.callbacks.XCallback.PRIORITY_HIGHEST;

import com.hchen.hooktool.action.Action;
import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.callback.IAllAction;
import com.hchen.hooktool.utils.DataUtils;
import com.hchen.hooktool.utils.MapUtils;
import com.hchen.hooktool.utils.SafeUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;

import de.robv.android.xposed.XposedBridge;

public class ActionTool {
    private DataUtils utils;
    private final SafeUtils safe;
    protected ArrayList<Method> methods = null;

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
                public Action action(Method method) {
                    return allActionTool(method, iAllAction);
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
                public Action action(Method method) {
                    return afterTool(method, iAction);
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
                public Action action(Method method) {
                    return beforeTool(method, iAction);
                }
            });
        }
        return utils.getMethodTool();
    }

    public void returnConstant(final Object result) {
        for (MapUtils<ArrayList<Method>> m : utils.methods) {
            XposedBridge.hookMethod(m,
                    new Action(utils.getTAG()) {
                        @Override
                        protected void before(MethodHookParam param) {
                            param.setResult(result);
                        }
                    }
            );
        }
    }

    public void doNothing() {
        for (MapUtils<ArrayList<Method>> m : utils.methods) {
            XposedBridge.hookMethod(m, DO_NOTHING);
        }
    }

    private final Action DO_NOTHING = new Action(utils.getTAG(), PRIORITY_HIGHEST * 2) {
        @Override
        protected void before(MethodHookParam param) {
            param.setResult(null);
        }
    };

    private void hookTool(String name, int index, IActionTool tool) {
        if (index == -1) {
            if (methods != null) {
                if (!methods.isEmpty()) {
                    for (Method method : methods) {
                        try {
                            XposedBridge.hookMethod(method, tool.action(method));
                        } catch (Throwable e) {
                            logE(utils.getTAG(), name + " hook method: " + method + " e: " + e);
                        }
                    }
                } else {
                    logW(utils.getTAG(), name + " methods is empty, hook nothing.");
                }
            } else {
                logW(utils.getTAG(), name + " methods is null, cant use this action.");
            }
            methods = null;
            return;
        }
        if (utils.methods.size() > index) {
            ArrayList<Method> methods = utils.methods.get(index);
            if (!methods.isEmpty()) {
                for (Method method : methods) {
                    try {
                        XposedBridge.hookMethod(method, tool.action(method));
                    } catch (Throwable e) {
                        logE(utils.getTAG(), name + " hook method: " + method + " e: " + e);
                    }
                }
            } else {
                logW(utils.getTAG(), name + " methods is empty!");
            }
        } else {
            logW(utils.getTAG(), name + " methods size < index!");
        }
    }

    private Action allActionTool(Method method, IAllAction iAllAction) {
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

    private Action afterTool(Method method, IAction iAction) {
        ParamTool paramTool = new ParamTool(utils.getTAG());
        return new Action(utils.getTAG()) {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                paramTool.setParam(param);
                iAction.action(paramTool);
            }
        };
    }

    private Action beforeTool(Method method, IAction iAction) {
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
        Action action(Method method);
    }

    public MethodTool methodTool() {
        return utils.getMethodTool();
    }
}
