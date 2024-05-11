package com.hchen.hooktool.callback;

import com.hchen.hooktool.tool.ParamTool;
import com.hchen.hooktool.tool.StaticTool;

public interface IAllAction {
    void before(ParamTool param, StaticTool staticTool) throws Throwable;

    void after(ParamTool param, StaticTool staticTool) throws Throwable;
}
