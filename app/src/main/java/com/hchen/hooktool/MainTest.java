package com.hchen.hooktool;

import android.content.Context;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.tool.hook.ParamTool;
import com.hchen.hooktool.tool.hook.StaticTool;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class MainTest {

    public void test() {
        new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Context context = (Context) param.thisObject;
                String string = (String) param.args[0];
                param.args[1] = 1;
                String result = (String) XposedHelpers.callMethod(param.thisObject, "call",
                        param.thisObject, param.args[0]);
                XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.demo.Main", ClassLoader.getSystemClassLoader()),
                        "callStatic", param.thisObject, param.args[1]);
                int i = (int) XposedHelpers.getStaticObjectField(XposedHelpers.findClass("com.demo.Main", ClassLoader.getSystemClassLoader()),
                        "field");
            }
        };

        new IAction() {
            @Override
            public void before(ParamTool param, StaticTool staticTool) {
                Context context = param.thisObject();
                String string = param.first();
                param.second(1);
                String result = param.callMethod("call", new Object[]{param.thisObject(), param.first()});
                staticTool.findClass("com.demo.Main");
                staticTool.callStaticMethod("callStatic", new Object[]{param.thisObject(), param.second()});
                int i = staticTool.getStaticField("field");
            }
        };
    }
}
