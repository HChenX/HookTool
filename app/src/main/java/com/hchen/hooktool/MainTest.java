package com.hchen.hooktool;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.tool.ParamTool;
import com.hchen.hooktool.tool.StaticTool;

public class MainTest {
    public void test() {
        new HCHook().methodTool().actionTool().after(new IAction() {
            @Override
            public void action(ParamTool<Object> param, StaticTool<Object> staticTool) {

            }
        });
    }
}
