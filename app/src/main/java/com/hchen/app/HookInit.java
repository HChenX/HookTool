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
package com.hchen.app;

import static com.hchen.hooktool.HCInit.LOG_D;

import androidx.annotation.NonNull;

import com.hchen.hooktool.HCEntrance;
import com.hchen.hooktool.HCInit;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook 入口
 *
 * @author 焕晨HChen
 */
public class HookInit extends HCEntrance /* 建议继承 HCEntrance 类作为入口 */ {
    @NonNull
    @Override
    public HCInit.BasicData initHC(@NonNull HCInit.BasicData basicData) {
        return basicData
            .setModulePackageName("com.hchen.demo") // 模块包名
            .setTag("HChenDemo") // 日志 tag
            .setLogLevel(LOG_D) // 日志等级
            .setPrefsName("hchen_prefs") // prefs 文件名 (可选)
            .setAutoReload(true) // 是否自动更新共享首选项，默认开启 (可选)
            .setLogExpandPath("com.hchen.demo.hook") // 日志增强功能 (可选)
            .setLogExpandIgnoreClassNames("Demo"); // 排除指定类名 (可选)
    }

    @NonNull
    @Override
    public String[] ignorePackageNameList() {
        // 指定忽略的包名
        return new String[]{
            "com.android.test"
        };
    }

    @Override
    public void onLoadModule(@NonNull XC_LoadPackage.LoadPackageParam loadPackageParam) {
        super.onLoadModule(loadPackageParam); // 模块自身被加载时调用
    }

    @Override
    public void onLoadPackage(@NonNull XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        HCInit.initLoadPackageParam(loadPackageParam); // 必须，初始化工具
        new HookDemo().onApplication().onLoadPackage(); // 添加 onApplication 后才会执行 onApplication() 回调
    }

    @Override
    public void onInitZygote(@NonNull StartupParam startupParam) throws Throwable {
        new HookDemo().onZygote();
    }
}
