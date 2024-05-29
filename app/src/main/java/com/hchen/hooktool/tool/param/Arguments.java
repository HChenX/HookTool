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
package com.hchen.hooktool.tool.param;

import static com.hchen.hooktool.log.XposedLog.logE;

import com.hchen.hooktool.utils.DataUtils;

public class Arguments extends ActAchieve {

    public Arguments(DataUtils utils) {
        super(utils);
    }

    public <T> T getParam(int index) {
        if (size() == -1) {
            return null;
        } else if (size() < index + 1) {
            logE(utils.getTAG(), "method: [" + param.method.getName() +
                    "] param max size: [" + size() + "] index: [" + index + "] !");
            return null;
        }
        return (T) param.args[index];
    }

    // ------- 提供五个快捷获取 ---------

    public <T> T first() {
        return getParam(0);
    }

    public <T> T second() {
        return getParam(1);
    }

    public <T> T third() {
        return getParam(2);
    }

    public <T> T fourth() {
        return getParam(3);
    }

    public <T> T fifth() {
        return getParam(4);
    }

    public <T> Arguments setParam(int index, T value) {
        if (size() == -1) {
            return this;
        } else if (size() < index + 1) {
            logE(utils.getTAG(), "method: [" + param.method.getName() +
                    "] param max size: [" + size() + "] index: [" + index + "] !");
            return this;
        }
        param.args[index] = value;
        return this;
    }

    // ------- 提供五个快捷设置 ---------

    public <T> Arguments first(T value) {
        return setParam(0, value);
    }

    public <T> Arguments second(T value) {
        return setParam(1, value);
    }

    public <T> Arguments third(T value) {
        return setParam(2, value);
    }

    public <T> Arguments fourth(T value) {
        return setParam(3, value);
    }

    public <T> Arguments fifth(T value) {
        return setParam(4, value);
    }

    public int size() {
        return param.args.length;
    }
}
