package com.hchen.hooktool.callback;

import com.hchen.hooktool.tool.ParamTool;

public interface IAllAction {
    void before(ParamTool param) throws Throwable;

    void after(ParamTool param) throws Throwable;
}
