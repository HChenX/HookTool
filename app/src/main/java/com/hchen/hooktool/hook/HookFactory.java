package com.hchen.hooktool.hook;

import com.hchen.hooktool.core.CoreTool;
import com.hchen.hooktool.log.LogExpand;
import com.hchen.hooktool.log.XposedLog;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XC_MethodHook;

public class HookFactory {
    public static XC_MethodHook createHook(IHook iHook) {
        String tag = LogExpand.getTag();
        iHook.INNER_TAG = tag;
        XC_MethodHook xcMethodHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    iHook.param = param;
                    iHook.before();
                } catch (Throwable e) {
                    if (!iHook.onThrow(IHook.BEFORE, e)) {
                        try {
                            param.setResult(CoreTool.invokeOriginalMethod(param.method, param.thisObject, param.args));
                        } catch (InvocationTargetException exception) {
                            param.setThrowable(exception.getCause());
                        } catch (Throwable ignore) {
                        }
                        XposedLog.logE(tag, "Before throw!", e);
                    }
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object lastResult = param.getResult();
                Throwable lastThrowable = param.getThrowable();

                try {
                    iHook.param = param;
                    iHook.after();
                } catch (Throwable e) {
                    if (!iHook.onThrow(IHook.AFTER, e)) {
                        if (lastThrowable == null) param.setResult(lastResult);
                        else param.setThrowable(lastThrowable);

                        XposedLog.logE(tag, "After throw!", e);
                    }
                }
            }
        };
        iHook.xcMethodHook = xcMethodHook;
        return xcMethodHook;
    }
}
