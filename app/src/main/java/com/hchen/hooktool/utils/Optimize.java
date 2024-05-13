package com.hchen.hooktool.utils;

import com.hchen.hooktool.tool.ActionTool;

// 优化调用，只提供基本用法，详细用法请获取工具类对象
public class Optimize {
    private final DataUtils utils;

    public Optimize(DataUtils utils) {
        this.utils = utils;
    }

    public ActionTool getMethod(String name, Class<?>... clzzs) {
        return utils.getMethodTool().getMethod(name, clzzs);
    }

    public ActionTool getIndexMethod(int index, String name, Class<?>... clzzs) {
        return utils.getMethodTool().getIndexMethod(index, name, clzzs);
    }

    public ActionTool getAnyMethod(String name) {
        return utils.getMethodTool().getAnyMethod(name);
    }

    public ActionTool getAnyMethodByIndex(int index, String name) {
        return utils.getMethodTool().getAnyMethodByIndex(index, name);
    }


    public ActionTool getConstructor(Class<?>... obj) {
        return utils.getMethodTool().getConstructor(obj);
    }

    public ActionTool getAnyConstructor() {
        return utils.getMethodTool().getAnyConstructor();
    }

    public ActionTool getConstructorByIndex(int index, Class<?>... classes) {
        return utils.getMethodTool().getConstructorByIndex(index, classes);
    }

    public ActionTool getAnyConstructorByIndex(int index) {
        return utils.getMethodTool().getAnyConstructorByIndex(index);
    }

}
