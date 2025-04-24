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
package com.hchen.hooktool.helper;

import java.util.Objects;

/**
 * TryHelper
 *
 * @author 焕晨HChen
 */
public class TryHelper {
    private TryHelper() {
    }
    
    public static <R> Result<R> doTry(IRun<R> supplier) {
        return new Result<R>(supplier);
    }

    public interface IRun<R> {
        R run() throws Throwable;
    }

    public static final class Result<R> {
        private R result;
        private Throwable throwable;

        private Result(IRun<R> iRun) {
            try {
                result = iRun.run();
                throwable = null;
            } catch (Throwable throwable) {
                this.throwable = throwable;
                result = null;
            }
        }

        public R get() {
            return result;
        }

        public R orElse(R or) {
            if (Objects.isNull(throwable))
                return result;
            return or;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public boolean isSuccess() {
            return Objects.isNull(throwable);
        }
    }
}
