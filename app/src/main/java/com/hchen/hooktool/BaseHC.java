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

import static com.hchen.hooktool.data.ToolData.isZygote;
import static com.hchen.hooktool.log.XposedLog.logE;

import android.content.pm.ApplicationInfo;

import com.hchen.hooktool.data.ToolData;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 对需要使用工具的类继承本类，可快速使用工具
 * <p>
 * This class inherits from the class that requires the use of the tool, so that you can quickly use the tool
 * 
 * @author 焕晨HChen
 */
public abstract class BaseHC {
    public String TAG = getClass().getSimpleName();

    // onZygote 阶段以下均为 null 或 false
    public XC_LoadPackage.LoadPackageParam lpparam;
    public ClassLoader classLoader;
    public ApplicationInfo appInfo;
    public String packageName;
    public boolean isFirstApplication;
    public String processName;
    // END
    
    /**
     * 正常阶段。
     * <p>
     * Normal stage.
     */
    public abstract void init();

    /**
     * zygote 阶段。
     * <p>
     * 如果 startupParam 为 null，请检查是否为工具初始化。
     * <p>
     * Zygote stages.
     * <p>
     * If startupParam is null, check if it is initialized for the tool.
     */
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
    }

    final public void onCreate() {
        initTool();
        try {
            init();
        } catch (Throwable e) {
            logE(TAG, e);
        }
    }

    final public void onZygote() {
        initTool();
        try {
            initZygote(ToolData.startupParam);
        } catch (Throwable e) {
            logE(TAG, e);
        }
    }

    private void initTool() {
        if (!isZygote) {
            lpparam = ToolData.lpparam;
            classLoader = lpparam.classLoader;
            appInfo = lpparam.appInfo;
            packageName = lpparam.packageName;
            isFirstApplication = lpparam.isFirstApplication;
            processName = lpparam.processName;
        }
    }
}
