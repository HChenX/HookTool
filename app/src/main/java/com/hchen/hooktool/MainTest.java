package com.hchen.hooktool;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.tool.ParamTool;

public class MainTest {
    public void test() {
        new HCHook().classTool().findClass("")
                .findClass("")
                .findClass("")
                .methodTool().getNextMethod("").hcHook().after(new IAction() {
                    @Override
                    public void action(ParamTool param) {
                    }
                });
    }
}
