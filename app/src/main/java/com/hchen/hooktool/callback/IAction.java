package com.hchen.hooktool.callback;

import com.hchen.hooktool.tool.ParamTool;
import com.hchen.hooktool.tool.StaticTool;

public interface IAction {
    void action(ParamTool<Object> param, StaticTool<Object> staticTool);
}
