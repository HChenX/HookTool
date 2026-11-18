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
package com.hchen.appjava;

import com.hchen.hooktool.reflect.Reflect;
import com.hchen.hooktool.shell.ShellTool;
import com.hchen.hooktool.util.device.DeviceTool;

import java.lang.reflect.Method;

/**
 * 纯 Java 消费方示例。
 * <p>
 * 仅依赖 reflect、shell、utils 三个叶子库，不引入 libxposed 与 Kotlin 运行时。
 *
 * @author 焕晨HChen
 */
public final class JavaConsumer {
    private JavaConsumer() {
    }

    /**
     * 读取设备型号、ROM 类型与 Root 状态。
     *
     * @return 单行描述信息
     */
    public static String describe() {
        Class<?> build = Reflect.findClass("android.os.Build");
        Object model = Reflect.getStaticObjectField(build, "MODEL");

        String rom;
        if (DeviceTool.isHyperOS()) {
            rom = "HyperOS";
        } else if (DeviceTool.isMiui()) {
            rom = "MIUI";
        } else if (DeviceTool.isColorOS()) {
            rom = "ColorOS";
        } else {
            rom = "other";
        }

        return "model=" + model + ", rom=" + rom + ", root=" + ShellTool.isRootAvailable();
    }

    /**
     * 演示带缓存的方法查找。
     *
     * @return 方法的声明类名与方法名
     */
    public static String lookupSample() {
        Method method = Reflect.findMethodExact(Thread.class, "getName");
        return method.getDeclaringClass().getName() + "#" + method.getName();
    }
}
