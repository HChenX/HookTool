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
package com.hchen.hooktool.helper;

import static com.hchen.hooktool.log.LogExpand.getTag;
import static com.hchen.hooktool.log.XposedLog.logE;

import com.hchen.hooktool.tool.MemberData;

/**
 * 方法执行与异常处理类
 *
 * @author 焕晨HChen
 */
public final class TryHelper {
    /*
     * 执行并返回执行的结果或抛错。
     * */
    public static <T> Result<T> run(Run<T> supplier) {
        return new Result<>(supplier);
    }

    /*
     * 执行并储存执行的结果与抛错。
     * */
    public static <T> MemberData<T> createData(Run<T> supplier) {
        return new Result<>(supplier, true).create();
    }

    /*
     * 简单的执行代码并获取返回值，与此同时主动抛出可能的异常。
     */
    public interface Run<T> {
        T run() throws Throwable;
    }

    public static class Result<T> {
        private T mResult;
        private boolean isSuccess;
        private Throwable mThrowable;
        private final boolean shouldCreateData;
        private MemberData<T> mMemberData;

        public Result(Run<T> supplier) {
            this(supplier, false);
        }

        public Result(Run<T> supplier, boolean shouldCreateData) {
            this.shouldCreateData = shouldCreateData;
            doRun(supplier);
        }

        private void doRun(Run<T> supplier) {
            try {
                mResult = supplier.run();
                isSuccess = true;
                mThrowable = null;
            } catch (Throwable throwable) {
                mThrowable = throwable;
                isSuccess = false;
                mResult = null;
            }
            if (shouldCreateData)
                mMemberData = new MemberData<>(mResult, mThrowable);
        }

        private MemberData<T> create() {
            return mMemberData;
        }

        // 获取执行结果
        public T get() {
            return mResult;
        }

        // 失败返回 or 值
        public T or(T or) {
            if (isSuccess) return mResult;
            return or;
        }

        // 如果失败返回指定 or 值，并执行异常回调
        public T orErr(T or, Err err) {
            if (isSuccess) return mResult;
            err.err(mThrowable);
            return or;
        }

        public T orErrMag(T or, String msg) {
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
