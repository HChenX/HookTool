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
 * Copyright (C) 2024–2026 HChenX
 */
package com.hchen.app.hook;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.hchen.hooktool.log.AndroidLog;
import com.hchen.hooktool.res.ResInject;
import com.hchen.hooktool.shell.ShellResult;
import com.hchen.hooktool.shell.ShellTool;
import com.hchen.hooktool.util.device.DeviceTool;
import com.hchen.hooktool.util.device.SystemPropTool;
import com.hchen.hooktool.util.pkg.AppData;
import com.hchen.hooktool.util.pkg.PackageTool;
import com.hchen.hooktool.util.prefs.PrefsTool;
import com.hchen.hooktool.util.ui.UiTool;

import java.util.Arrays;

/**
 * 工具库用法示例。
 * <p>
 * 覆盖 Shell、设备信息、包信息、配置与资源替换。
 *
 * @author 焕晨HChen
 */
public final class UtilitySamples {
    private static final String TAG = "UtilitySamples";

    private UtilitySamples() {
    }

    /**
     * 依次执行各工具库示例。
     *
     * @param context 应用上下文
     */
    public static void run(@NonNull Context context) {
        shell();
        device();
        packages(context);
        preferences(context);
        ui(context);
        resource(context);
    }

    private static void shell() {
        ShellTool shell = ShellTool.obtain(true);
        ShellResult result = shell.cmd("id").exec();
        if (result instanceof ShellResult.Completed completed && completed.isSuccess()) {
            AndroidLog.logI(TAG, "shell 输出: " + Arrays.toString(completed.result().outputs()));
        } else if (result instanceof ShellResult.Failed failed) {
            AndroidLog.logW(TAG, "shell 失败: " + failed.reason());
        }

        shell.cmd("echo hello").async()
            .thenAccept(async -> AndroidLog.logI(TAG, "异步结果: " + async));
        AndroidLog.logI(TAG, "Root 可用: " + ShellTool.isRootAvailable());
        ShellTool.close();
    }

    private static void device() {
        AndroidLog.logI(TAG, "指纹: " + DeviceTool.getDeviceFingerprint()
            + ", 小米: " + DeviceTool.isXiaomi()
            + ", HyperOS: " + DeviceTool.getHyperOSVersion()
            + ", ROM 版本: " + DeviceTool.getRomVersion("ro.mi.os.version.name")
            + ", SDK: " + SystemPropTool.getProp("ro.build.version.sdk", 0));
    }

    private static void packages(@NonNull Context context) {
        String packageName = context.getPackageName();
        AndroidLog.logI(TAG, "已安装: " + PackageTool.isInstalled(context, packageName)
            + ", 版本: " + PackageTool.getVersionName(context, packageName)
            + ", 系统应用: " + PackageTool.isSystem(context, packageName));

        AppData[] apps = PackageTool.getAppData(context,
            pm -> pm.getInstalledApplications(0));
        AndroidLog.logI(TAG, "已安装应用数量: " + apps.length);
    }

    private static void preferences(@NonNull Context context) {
        SharedPreferences local = PrefsTool.prefs(context);
        local.edit().putLong("demo_time", System.currentTimeMillis()).apply();
        AndroidLog.logI(TAG, "本地配置时间: " + local.getLong("demo_time", 0L));

        try {
            AndroidLog.logI(TAG, "远程配置项数量: " + PrefsTool.prefs().getAll().size());
        } catch (Throwable throwable) {
            AndroidLog.logW(TAG, "远程配置不可用", throwable);
        }
    }

    private static void ui(@NonNull Context context) {
        AndroidLog.logI(TAG, "16dp=" + UiTool.dp2px(context, 16) + "px"
            + ", 屏幕: " + UiTool.getScreenWidth(context) + "x" + UiTool.getScreenHeight(context)
            + ", 深色模式: " + UiTool.isDarkMode(context));
    }

    private static void resource(@NonNull Context context) {
        try {
            ResInject.injectModuleRes();
            int fakeId = ResInject.createFakeResId(context.getResources(),
                android.R.string.ok);
            ResInject.setObjectReplacement(context.getPackageName(), "string",
                "demo_replaced", "替换后的文本");
            ResInject.setResReplacement(context.getPackageName(), "string",
                "demo_replaced", fakeId);
            AndroidLog.logI(TAG, "资源替换已注册, id=" + fakeId);
        } catch (Throwable throwable) {
            AndroidLog.logW(TAG, "资源替换不可用", throwable);
        }
    }
}
