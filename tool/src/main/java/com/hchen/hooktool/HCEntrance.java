/*
 * This file is part of HookTool.
 *
 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * HookTool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with HookTool. If not, see <https://www.gnu.org/licenses/lgpl-2.1>.
 *
 * Copyright (C) 2023–2025 HChenX
 */
package com.hchen.hooktool;

import androidx.annotation.NonNull;

import com.hchen.hooktool.core.CoreTool;

import java.util.Arrays;
import java.util.Objects;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook 入口
 *
 * @author 焕晨HChen
 */
public abstract class HCEntrance implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    /**
     * 初始化工具
     */
    @NonNull
    public abstract HCInit.BasicData initHC(@NonNull HCInit.BasicData basicData);

    /**
     * onLoadPackage 阶段
     */
    public abstract void onLoadPackage(@NonNull XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable;

    /**
     * onInitZygote 阶段
     */
    public void onInitZygote(@NonNull StartupParam startupParam) throws Throwable {
    }

    /**
     * 模块自身加载阶段
     */
    public void onLoadModule(@NonNull XC_LoadPackage.LoadPackageParam loadPackageParam) {
    }

    /**
     * 忽略的包名列表
     */
    @NonNull
    public String[] ignorePackageNameList() {
        return new String[]{};
    }

    @Override
    public final void handleLoadPackage(@NonNull XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (ignorePackageNameList().length != 0) {
            if (Arrays.stream(ignorePackageNameList()).anyMatch(s -> Objects.equals(s, loadPackageParam.packageName)))
                return;
        }

        if (Objects.equals(HCData.getModulePackageName(), loadPackageParam.packageName)) {
            HCInit.initLoadPackageParam(loadPackageParam);
            initHCState(loadPackageParam);
            onLoadModule(loadPackageParam);
            return;
        }

        onLoadPackage(loadPackageParam);
    }

    @Override
    public final void initZygote(@NonNull StartupParam startupParam) throws Throwable {
        HCInit.initBasicData(initHC(new HCInit.BasicData()));
        HCInit.initStartupParam(startupParam);
        onInitZygote(startupParam);
    }

    private void initHCState(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        CoreTool.hookMethod("com.hchen.hooktool.HCState", loadPackageParam.classLoader, "isXposedEnabled", CoreTool.returnResult(true));
        CoreTool.hookMethod("com.hchen.hooktool.HCState", loadPackageParam.classLoader, "getVersion", CoreTool.returnResult(XposedBridge.getXposedVersion()));

        String bridgeTag = (String) CoreTool.getStaticField(XposedBridge.class, "TAG");
        if (bridgeTag == null) return;

        if (bridgeTag.startsWith("LSPosed")) bridgeTag = "LSPosed";
        else if (bridgeTag.startsWith("EdXposed")) bridgeTag = "EdXposed";
        else if (bridgeTag.startsWith("Xposed")) bridgeTag = "Xposed";
        else bridgeTag = "Unknown";
        CoreTool.hookMethod("com.hchen.hooktool.HCState", loadPackageParam.classLoader, "getFramework", CoreTool.returnResult(bridgeTag));
    }
}
