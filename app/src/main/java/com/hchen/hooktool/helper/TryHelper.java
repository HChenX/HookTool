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
public class TryHelper {

    /*
     * 执行并返回执行的结果或抛错。
     * */
    public static <T> Result<T> run(Run<T> supplier) {
        return new Result<>(supplier);
    }
    
    /*
     * 执行并储存执行的结果与抛错。
     * */
    public static <T> MemberData<T> runDump(Run<T> supplier) {
        return (MemberData<T>) new Result<>(supplier, true).dump();
    }

    /*
     * 简单的执行代码并获取返回值，与此同时主动抛出可能的异常。
     */
    public interface Run<T> {
        T run() throws Throwable;
    }

    public static class Result<T> {
        private T result;
        private boolean dump;
        private MemberData<T> memberData;
        private boolean isSuccess;
        private Throwable throwable;

        public Result(Run<T> supplier) {
            doRun(supplier);
        }

        public Result(Run<T> supplier, boolean dump) {
            this.dump = dump;
            doRun(supplier);
        }

        private void doRun(Run<T> supplier) {
            try {
                result = supplier.run();
                isSuccess = true;
                throwable = null;
            } catch (Throwable throwable) {
                this.throwable = throwable;
                isSuccess = false;
                result = null;
            }
            if (dump)
                memberData = new MemberData<>(result, throwable);
        }

        public MemberData<?> dump() {
            return memberData;
        }

        // 获取执行结果
        public T get() {
            return result;
        }

        // 失败返回 or 值
        public T or(T or) {
            if (isSuccess) return result;
            return or;
        }

        // 如果失败返回指定 or 值，并执行异常回调
        public T orErr(T or, Err err) {
            if (isSuccess) return result;
            err.err(throwable);
            return or;
        }

        public T orErrMag(T or, String msg) {
            if (isSuccess) return result;
            logE(getTag(), msg, throwable);
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
