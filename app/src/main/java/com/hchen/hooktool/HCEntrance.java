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

 * Copyright (C) 2023-2025 HChenX
 */
package com.hchen.hooktool;

import com.hchen.hooktool.core.CoreTool;

import java.util.Arrays;
import java.util.Objects;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public abstract class HCEntrance implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    public abstract HCInit.BasicData initHC(HCInit.BasicData basicData);

    public abstract void onLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable;

    public void onInitZygote(StartupParam startupParam) throws Throwable {
    }

    public void onModuleLoad(XC_LoadPackage.LoadPackageParam lpparam) {
    }

    public String[] ignorePackageNameList() {
        return null;
    }

    @Override
    public final void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (ignorePackageNameList() != null) {
            if (Arrays.stream(ignorePackageNameList()).anyMatch(s -> Objects.equals(s, lpparam.packageName)))
                return;
        }

        if (Objects.equals(HCData.getModulePackageName(), lpparam.packageName)) {
            HCInit.initLoadPackageParam(lpparam);
            initHCState();
            onModuleLoad(lpparam);
            return;
        }

        onLoadPackage(lpparam);
    }

    @Override
    public final void initZygote(StartupParam startupParam) throws Throwable {
        HCInit.initBasicData(initHC(new HCInit.BasicData()));
        HCInit.initStartupParam(startupParam);
        // ResInjectTool.init(startupParam.modulePath);
        onInitZygote(startupParam);
    }

    private void initHCState() {
        CoreTool.setStaticField("com.hchen.hooktool.HCState", "isEnabled", true);
        CoreTool.setStaticField("com.hchen.hooktool.HCState", "mVersion", XposedBridge.getXposedVersion());

        String bridgeTag = (String) CoreTool.getStaticField(XposedBridge.class, "TAG");
        if (bridgeTag == null) return;

        if (bridgeTag.startsWith("LSPosed")) {
            bridgeTag = "LSPosed";
        } else if (bridgeTag.startsWith("EdXposed")) {
            bridgeTag = "EdXposed";
        } else if (bridgeTag.startsWith("Xposed")) {
            bridgeTag = "Xposed";
        } else
            bridgeTag = "Unknown";
        CoreTool.setStaticField("com.hchen.hooktool.HCState", "mFramework", bridgeTag);
    }
}
