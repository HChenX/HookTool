package com.hchen.hooktool.callback;

import com.hchen.hooktool.tool.ParamTool;
import com.hchen.hooktool.tool.StaticTool;

public interface IAction {
    default void before(ParamTool param, StaticTool staticTool) {
    }

    default void after(ParamTool param, StaticTool staticTool) {
    }
}
