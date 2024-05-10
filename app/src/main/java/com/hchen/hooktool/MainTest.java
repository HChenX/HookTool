package com.hchen.hooktool;

import com.hchen.hooktool.callback.IAllAction;
import com.hchen.hooktool.tool.ParamTool;

public class MainTest {
    public void test() {
        new HCHook().classTool().findClass("")
                .findClass("")
                .findClass("")
                .methodTool().getMethod("").after(
                        null
                ).next().getMethod("").allAction(new IAllAction() {

                    @Override
                    public void before(ParamTool param) throws Throwable {

                    }

                    @Override
                    public void after(ParamTool param) throws Throwable {

                    }
                }).getAnyConstructor().doNothing();
    }
}
