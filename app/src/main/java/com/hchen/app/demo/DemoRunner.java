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
package com.hchen.app.demo;

import androidx.annotation.NonNull;

import com.hchen.hooktool.log.AndroidLog;

/**
 * 调用目标类并汇总结果，供界面展示。
 *
 * @author 焕晨HChen
 */
public final class DemoRunner {
    private static final String TAG = "DemoRunner";

    private DemoRunner() {
    }

    /**
     * 依次调用目标类的各个方法。
     *
     * @return 调用结果汇总
     */
    @NonNull
    public static String run() {
        Target target = new Target("demo");
        target.noise();

        String summary = "greet=" + target.greet("world")
            + "\nadd=" + Target.add(1, 2)
            + "\ndescribe=" + target.describe()
            + "\ndescribe(prefix)=" + target.describe("p:")
            + "\nsecret=" + target.getSecret()
            + "\nclinit=" + Target.getLoaded();

        AndroidLog.logI(TAG, summary);
        return summary;
    }
}
