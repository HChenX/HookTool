package com.hchen.hooktool;

import com.hchen.hooktool.callback.IAllAction;
import com.hchen.hooktool.tool.ParamTool;

public class MainTest {
    public void test() {
        new HCHook().classTool().findClass("")
                .findClass("")
                .findClass("")
                .methodTool().getNextMethod("").after(
                        null
                ).getNextMethod("").allAction(new IAllAction() {
                    @Override
                    public void before(ParamTool param) throws Throwable {

                    }

                    @Override
                    public void after(ParamTool param) throws Throwable {

                    }
                });
    }
}
