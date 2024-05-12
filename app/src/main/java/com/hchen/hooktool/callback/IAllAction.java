package com.hchen.hooktool.callback;

import com.hchen.hooktool.tool.ParamTool;
import com.hchen.hooktool.tool.StaticTool;

public interface IAllAction {
    void before(ParamTool<Object> param, StaticTool<Object> staticTool);

    void after(ParamTool<Object> param, StaticTool<Object> staticTool);
}
