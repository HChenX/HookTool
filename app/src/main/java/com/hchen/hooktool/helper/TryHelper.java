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

public class TryHelper {
    public static <V> Result<V> doTry(IRun<V> supplier) {
        return new Result<>(supplier);
    }

    public interface IRun<V> {
        V run() throws Throwable;
    }

    public static final class Result<V> {
        private V result;
        private Throwable throwable;

        public Result(IRun<V> iRun) {
            try {
                result = iRun.run();
                throwable = null;
            } catch (Throwable throwable) {
                this.throwable = throwable;
                result = null;
            }
        }

        public V get() {
            return result;
        }

        public V orElse(V or) {
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
