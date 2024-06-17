/*
 * This file is part of HookTool.

 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2024 HookTool Contributions
 */
package com.hchen.hooktool;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.utils.DataUtils.spareTag;

import com.hchen.hooktool.tool.ActionTool;
import com.hchen.hooktool.tool.ClassTool;
import com.hchen.hooktool.tool.ExpandTool;
import com.hchen.hooktool.tool.FieldTool;
import com.hchen.hooktool.tool.MethodTool;
import com.hchen.hooktool.utils.DataUtils;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 工具入口
 */
public class HCHook {
    private final DataUtils utils;

    static {
        initSafe();
        try {
            spareTag = HCInit.spareTag;
            DataUtils.lpparam = HCInit.getLoadPackageParam();
            DataUtils.useLogExpand = HCInit.getUseLogExpand();
            DataUtils.useFieldObserver = HCInit.getUseFieldObserver();
            DataUtils.filter = HCInit.getFilter();
            DataUtils.classLoader = HCInit.getClassLoader();
        } catch (Throwable e) {
            logE(spareTag, e);
        }
    }

    /**
     * 实例化本类开始使用
     */
    public HCHook() {
        utils = new DataUtils();
        utils.hcHook = this;
        utils.actionTool = new ActionTool(utils);
        utils.classTool = new ClassTool(utils);
        utils.fieldTool = new FieldTool(utils);
        utils.methodTool = new MethodTool(utils);
        utils.expandTool = new ExpandTool(utils);
    }

    public HCHook setThisTag(String tag) {
        utils.mThisTag = tag;
        return utils.getHCHook();
    }

    // 更棒的无缝衔接
    public ClassTool findClass(Object label, String className) {
        return utils.getClassTool().findClass(label, className);
    }

    public ClassTool findClass(Object label, String className, ClassLoader classLoader) {
        return utils.getClassTool().findClass(label, className, classLoader);
    }

    /* 因为 ClassTool 是本工具基准入口，所以初始化使用必须进入此类。 */
    public ClassTool classTool() {
        return utils.getClassTool();
    }

    public MethodTool methodTool() {
        return utils.getMethodTool();
    }

    public FieldTool fieldTool() {
        return utils.getFieldTool();
    }

    public ExpandTool expandTool() {
        return utils.getExpandTool();
    }

    /* 设置自定义 class loader */
    public HCHook setClassLoader(ClassLoader classLoader) {
        utils.mCustomClassLoader = classLoader;
        return utils.getHCHook();
    }

    /* 设置自定义 lpparam */
    public HCHook setLpparam(XC_LoadPackage.LoadPackageParam lpparam) {
        utils.mCustomLpparam = lpparam;
        setClassLoader(lpparam.classLoader);
        return utils.getHCHook();
    }

    public XC_LoadPackage.LoadPackageParam getLpparam() {
        return utils.getLpparam();
    }

    public ClassLoader getClassLoader() {
        return utils.getClassLoader();
    }

    public static void initSafe() {
        if (!HCInit.isInitDone())
            throw new RuntimeException(HCInit.getTAG() + "[E]: HookInit not initialized!");
    }
}
