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

 * Copyright (C) 2023-2024 HChenX
 */
package com.hchen.hooktool.helper;

import static com.hchen.hooktool.log.LogExpand.getTag;
import static com.hchen.hooktool.log.XposedLog.logE;

import com.hchen.hooktool.tool.SingleMember;

/**
 * 方法执行与异常处理类
 *
 * @author 焕晨HChen
 */
public final class TryHelper {
    /*
     * 执行并返回执行的结果或抛错。
     * */
    public static <V> Result<V> run(Run<V> supplier) {
        return new Result<>(supplier);
    }

    /*
     * 执行并储存执行的结果与抛错。
     * */
    public static <V> SingleMember<V> createSingleMember(Run<V> supplier) {
        return new Result<>(supplier).create();
    }

    /*
     * 简单的执行代码并获取返回值，与此同时主动抛出可能的异常。
     */
    public interface Run<V> {
        V run() throws Throwable;
    }

    public static class Result<V> {
        private V mResult;
        private boolean isSuccess;
        private Throwable mThrowable;

        public Result(Run<V> supplier) {
            doRun(supplier);
        }

        private void doRun(Run<V> supplier) {
            try {
                mResult = supplier.run();
                isSuccess = true;
                mThrowable = null;
            } catch (Throwable throwable) {
                mThrowable = throwable;
                isSuccess = false;
                mResult = null;
            }
        }

        private SingleMember<V> create() {
            return new SingleMember<V>(mResult, mThrowable);
        }

        // 获取执行结果
        public V get() {
            return mResult;
        }

        // 失败返回 or 值
        public V or(V or) {
            if (isSuccess) return mResult;
            return or;
        }

        // 如果失败返回指定 or 值，并执行异常回调
        public V orErr(V or, Err err) {
            if (isSuccess) return mResult;
            err.err(mThrowable);
            return or;
        }

        public V orErrMag(V or, String msg) {
            if (isSuccess) return mResult;
            logE(getTag(), msg, mThrowable);
            return or;
        }

        // 返回代码执行结束状态
        public boolean isSuccess() {
            return isSuccess;
        }

        public boolean isFailed() {
            return !isSuccess;
        }

        public interface Err {
            void err(Throwable throwable);
        }
    }
}
