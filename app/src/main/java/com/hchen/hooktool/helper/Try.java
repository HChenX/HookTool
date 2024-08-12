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

/**
 * 方法执行与异常处理类
 * <p>
 * Run code and try
 *
 * @author 焕晨HChen
 */
public class Try {

    public static <T> Result<T> run(Run<T> supplier) {
        return new Result<T>(supplier);
    }

    /**
     * 简单的执行代码并获取返回值，与此同时主动抛出可能的异常。
     * <p>
     * Simply execute the code and retrieve the return value, while actively throwing possible exceptions.
     *
     * @author 焕晨HChen
     */
    public interface Run<T> {
        T run() throws Throwable;
    }

    public static class Result<T> {
        private T result = null;
        private boolean isSuccess;
        private Throwable throwable;

        public Result(Run<T> supplier) {
            try {
                result = supplier.run();
                isSuccess = true;
            } catch (Throwable throwable) {
                this.throwable = throwable;
                isSuccess = false;
            }
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

        // 返回代码执行结束状态
        public boolean isOk() {
            return isSuccess;
        }

        public interface Err {
            void err(Throwable throwable);
        }
    }
}
