package com.hchen.hooktool.utils;

import com.hchen.hooktool.tool.hook.ActionTool;

// 优化调用，只提供基本用法，详细用法请获取工具类对象
public class MethodOpt {
    private final DataUtils utils;

    public MethodOpt(DataUtils utils) {
        this.utils = utils;
    }

    public ActionTool getMethod(String name, Class<?>... clzzs) {
        return utils.getMethodTool().getMethod(name, clzzs);
    }

    public ActionTool getAnyMethod(String name) {
        return utils.getMethodTool().getAnyMethod(name);
    }

    public ActionTool getConstructor(Class<?>... obj) {
        return utils.getMethodTool().getConstructor(obj);
    }

    public ActionTool getAnyConstructor() {
        return utils.getMethodTool().getAnyConstructor();
    }
}
