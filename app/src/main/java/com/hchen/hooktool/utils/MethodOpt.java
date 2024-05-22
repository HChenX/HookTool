package com.hchen.hooktool.utils;

import com.hchen.hooktool.tool.ActionTool;

// 优化调用，只提供基本用法，详细用法请获取工具类对象
public class MethodOpt {
    private final DataUtils utils;

    public MethodOpt(DataUtils utils) {
        this.utils = utils;
    }

    public ActionTool getMethod(String name, Object... objs) {
        return utils.getMethodTool().getMethod(name, objs);
    }

    public ActionTool getAnyMethod(String name) {
        return utils.getMethodTool().getAnyMethod(name);
    }

    public ActionTool getConstructor(Object... objs) {
        return utils.getMethodTool().getConstructor(objs);
    }

    public ActionTool getAnyConstructor() {
        return utils.getMethodTool().getAnyConstructor();
    }
}
