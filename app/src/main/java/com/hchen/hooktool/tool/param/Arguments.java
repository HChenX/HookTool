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

/**
 * 参数操作
 */
public class Arguments extends ActAchieve {

    // ------- 提供快捷获取 ---------
    // 依次返回指定的参数。

    final public <T> T first() {
        return getParam(0);
    }

    final public <T> T second() {
        return getParam(1);
    }

    final public <T> T third() {
        return getParam(2);
    }

    final public <T> T fourth() {
        return getParam(3);
    }

    final public <T> T fifth() {
        return getParam(4);
    }

    final public <T> T sixth() {
        return getParam(5);
    }

    final public <T> T seventh() {
        return getParam(6);
    }

    final public <T> T eighth() {
        return getParam(7);
    }

    final public <T> T ninth() {
        return getParam(8);
    }

    final public <T> T tenth() {
        return getParam(9);
    }

    // ------- 提供快捷设置 ---------
    // 依次设置指定的参数

    final public <T> Arguments first(T value) {
        return setParam(0, value);
    }

    final public <T> Arguments second(T value) {
        return setParam(1, value);
    }

    final public <T> Arguments third(T value) {
        return setParam(2, value);
    }

    final public <T> Arguments fourth(T value) {
        return setParam(3, value);
    }

    final public <T> Arguments fifth(T value) {
        return setParam(4, value);
    }

    final public <T> Arguments sixth(T value) {
        return setParam(5, value);
    }

    final public <T> Arguments seventh(T value) {
        return setParam(6, value);
    }

    final public <T> Arguments eighth(T value) {
        return setParam(7, value);
    }

    final public <T> Arguments ninth(T value) {
        return setParam(8, value);
    }

    final public <T> Arguments tenth(T value) {
        return setParam(9, value);
    }

    /**
     * 当前方法参数的数量。
     */
    final public int size() {
        return methodHookParam.args.length;
    }

    /**
     * 获取指定参数。
     *
     * @param index 索引
     * @return 获取到的参数或 null
     */
    final public <T> T getParam(int index) {
        if (size() < index + 1) {
            logE(mTag, "method: [" + methodHookParam.method.getName() +
                    "], param max size: [" + size() + "], index: [" + index + "]!!");
            return null;
        }
        return (T) methodHookParam.args[index];
    }

    /**
     * 设置指定参数。
     *
     * @param index 索引
     * @param value 目标值
     */
    final public <T> Arguments setParam(int index, T value) {
        if (size() < index + 1) {
            logE(mTag, "method: [" + methodHookParam.method.getName() +
                    "], param max size: [" + size() + "], index: [" + index + "]!!");
            return this;
        }
        methodHookParam.args[index] = value;
        return this;
    }

}
