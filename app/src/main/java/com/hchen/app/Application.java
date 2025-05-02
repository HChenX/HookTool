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

import com.hchen.hooktool.HCInit;

/**
 * Application
 *
 * @author 焕晨HChen
 */
public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();

        HCInit.initBasicData(new HCInit.BasicData()
            .setModulePackageName("com.hchen.demo") // 模块包名
            .setTag("HChenDemo") // 日志 tag
            .setLogLevel(LOG_D) // 日志等级
            .setPrefsName("hchen_prefs") // prefs 存储文件名 (可选)
        );
    }
}
