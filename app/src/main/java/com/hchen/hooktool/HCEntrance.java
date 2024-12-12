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

import android.app.Application;
import android.content.Context;

import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.tool.CoreTool;
import com.hchen.hooktool.tool.itool.IApplication;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook 入口，正常来说继承本类即可
 *
 * @author 焕晨HChen
 */
public abstract class HCEntrance implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    /**
     * 配置工具基本信息。请务必设置！
     */
    public abstract HCInit.BasicData initHC(HCInit.BasicData basicData);

    /**
     * 详见 {@link IXposedHookLoadPackage#handleLoadPackage(XC_LoadPackage.LoadPackageParam)}
     */
    public abstract void onLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable;

    /**
     * 详见 {@link IXposedHookZygoteInit#initZygote(StartupParam)}
     */
    public void onInitZygote(StartupParam startupParam) throws Throwable {
    }

    /**
     * Hook 应用 Application 类的 attach 方法，并获取 Context。
     */
    public HashMap<String, IApplication[]> onApplication() {
        return null;
    }

    /**
     * 忽略的包名。
     * <p>
     * Tip: 因为传入的 lpparam 偶尔会被其他系统应用干扰，所以可以配置排除名单。
     */
    public String[] ignoreList() {
        return null;
    }

    @Override
    public final void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (ignoreList() != null) {
            if (Arrays.stream(ignoreList()).anyMatch(s -> Objects.equals(s, lpparam.packageName)))
                return;
        }
        HCInit.initLoadPackageParam(lpparam);

        if (HCData.getModulePackageName() != null && Objects.equals(HCData.getModulePackageName(), lpparam.packageName)) {
            initHCState();
        }

        HashMap<String, IApplication[]> iApplications = onApplication();
        if (iApplications != null)
            applicationHook(iApplications.get(lpparam.packageName));

        onLoadPackage(lpparam);
    }

    @Override
    public final void initZygote(StartupParam startupParam) throws Throwable {
        HCInit.initBasicData(initHC(new HCInit.BasicData()));
        HCInit.initStartupParam(startupParam);
        onInitZygote(startupParam);
    }

    private void initHCState() {
        CoreTool.setStaticField(HCState.class, "isEnabled", true);
        CoreTool.setStaticField(HCState.class, "mVersion", XposedBridge.getXposedVersion());

        String bridgeTag = (String) CoreTool.getStaticField(XposedBridge.class, "TAG");
        if (bridgeTag.startsWith("LSPosed")) {
            bridgeTag = "LSPosed";
        } else if (bridgeTag.startsWith("EdXposed")) {
            bridgeTag = "EdXposed";
        } else if (bridgeTag.startsWith("Xposed")) {
            bridgeTag = "Xposed";
        } else
            bridgeTag = "Unknown";
        CoreTool.setStaticField(HCState.class, "mFramework", bridgeTag);
    }

    private void applicationHook(IApplication[] iApplications) {
        if (iApplications == null) return;

        CoreTool.hookMethod(Application.class, "attach", Context.class, new IHook() {
            @Override
            public void before() {
                Arrays.stream(iApplications).forEach(iApplication -> {
                    try {
                        iApplication.before((Context) getArgs(0));
                    } catch (Throwable e) {
                        logE("Application", e);
                    }
                });
            }

            @Override
            public void after() {
                Arrays.stream(iApplications).forEach(iApplication -> {
                    try {
                        iApplication.after((Context) getArgs(0));
                    } catch (Throwable e) {
                        logE("Application", e);
                    }
                });
            }
        });
    }
}
