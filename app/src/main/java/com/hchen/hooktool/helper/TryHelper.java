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
