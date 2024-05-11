package com.hchen.hooktool;

import com.hchen.hooktool.callback.IAllAction;
import com.hchen.hooktool.tool.ParamTool;
import com.hchen.hooktool.tool.StaticTool;

public class MainTest {
    public void test() {
        new HCHook().classTool().findClass("")
                .findClass("")
                .findClass("")
                .methodTool().getMethod("").after(
                        null
                ).next().getMethod("").allAction(new IAllAction() {

                    @Override
                    public void before(ParamTool param, StaticTool staticTool) throws Throwable {
                        staticTool.findClass("").callStaticMethod("");

                    }

                    @Override
                    public void after(ParamTool param, StaticTool staticTool) throws Throwable {

                    }
                }).getAnyConstructor().doNothing();
    }
}
