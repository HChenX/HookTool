package com.hchen.hooktool.tool.hook;

import static com.hchen.hooktool.log.XposedLog.logE;

import com.hchen.hooktool.action.Action;
import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.utils.DataUtils;

import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;

public class DexkitTool {
    private final DataUtils utils;

    public DexkitTool(DataUtils utils) {
        this.utils = utils;
    }

    public void hookMethod(Member member, IAction iAction) {
        try {
            hook(member, iAction);
        } catch (Throwable e) {
            logE(utils.getTAG(), "dexkit hook member: " + e);
        }
    }

    public void hookMethod(MethodData methodData, IAction iAction) {
        try {
            Method method = methodData.getMethodInstance(utils.getClassLoader());
            hook(method, iAction);
        } catch (Throwable e) {
            logE(utils.getTAG(), "dexkit instance method: " + e);
        }
    }

    public void hookMethod(ClassData classData, IAction iAction, Class<?>... clzs) {
        try {
            Class<?> clzz = classData.getInstance(utils.getClassLoader());
            Constructor<?> constructor = clzz.getConstructor(clzs);
            hook(constructor, iAction);
        } catch (Throwable e) {
            logE(utils.getTAG(), "dexkit instance constructor: " + e);
        }
    }

    private void hook(Member member, IAction iAction) throws Throwable {
        XposedBridge.hookMethod(member, hookTool(member, iAction));
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
}
