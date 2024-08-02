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
package com.hchen.hooktool.helper.param;

import static com.hchen.hooktool.log.LogExpand.getStackTrace;
import static com.hchen.hooktool.log.XposedLog.logE;

/**
 * 参数操作
 * <p>
 * Parameter operations
 * 
 * @author 焕晨HChen
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
    final public void first(Object value) {
        setParam(0, value);
    }

    final public void second(Object value) {
        setParam(1, value);
    }

    final public void third(Object value) {
        setParam(2, value);
    }

    final public void fourth(Object value) {
        setParam(3, value);
    }

    final public void fifth(Object value) {
        setParam(4, value);
    }

    final public void sixth(Object value) {
        setParam(5, value);
    }

    final public void seventh(Object value) {
        setParam(6, value);
    }

    final public void eighth(Object value) {
        setParam(7, value);
    }

    final public void ninth(Object value) {
        setParam(8, value);
    }

    final public void tenth(Object value) {
        setParam(9, value);
    }

    /**
     * 当前方法参数的数量。
     * <p>
     * The number of current method parameters.
     */
    final public int size() {
        return MethodHookParam.args.length;
    }

    /**
     * 获取指定参数。
     * <p>
     * Obtain the specified parameters.
     */
    final public <T> T getParam(int index) {
        if (size() < index + 1) {
            logE(mTag, "Arguments: exceeding the index!" + getStackTrace());
            return null;
        }
        return (T) MethodHookParam.args[index];
    }

    /**
     * 设置指定参数。
     * <p>
     * Set the specified parameters.
     */
    final public void setParam(int index, Object value) {
        if (size() < index + 1) {
            logE(mTag, "Arguments: exceeding the index!" + getStackTrace());
            return;
        }
        MethodHookParam.args[index] = value;
    }

}
