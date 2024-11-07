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
package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.log.LogExpand;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;

/**
 * 储存成员与可能的抛错信息
 *
 * @author 焕晨HChen
 */
public class MemberData<T> {
    private final T mMember;
    private Throwable mThrowable;
    private String mMsg = "Unknown";

    public MemberData(T member, Throwable throwable) {
        mMember = member;
        mThrowable = throwable;
    }

    @Nullable
    public T get() {
        report();
        return mMember;
    }

    public T or(T or) {
        T get = get();
        if (get == null)
            return or;
        return get;
    }

    @Nullable
    public T getIfExists() {
        return mMember;
    }

    @Nullable
    public Throwable getThrowable() {
        return mThrowable;
    }

    public boolean isSuccess() {
        return mThrowable == null;
    }

    protected MemberData<T> setErrMsg(String msg) {
        mMsg = msg;
        return this;
    }

    protected String getErrMsg() {
        return mMsg;
    }

    /*
     * 将抛错由前至后进行拼接。
     * */
    protected MemberData<T> spiltThrowableMsg(Throwable... throwables) {
        if (throwables == null || throwables.length == 0) return this;
        if (mThrowable == null && Arrays.stream(throwables).allMatch(Objects::isNull))
            return this;
        StringBuilder builder = new StringBuilder(mThrowable == null ?
                "Top throwable is null, but bottom level calling have throwable, will show it!" : "");
        if (mThrowable != null)
            builder.append("\n").append(LogExpand.printStackTrace(mThrowable));
        for (Throwable throwable : throwables) {
            if (throwable == null) continue;
            builder.append("Caused by: ").append(throwable.getMessage());
        }
        mThrowable = new HookToolRuntimeException(builder.toString());
        return this;
    }

    private void report() {
        if (mThrowable != null)
            logE(LogExpand.getTag(), mMsg, mThrowable);
    }

    private static class HookToolRuntimeException extends RuntimeException {
        public HookToolRuntimeException(String message) {
            super(message);
        }

        @Override
        public void printStackTrace(@NonNull PrintWriter s) {
            synchronized ((Object) s) {
                s.println(this);
            }
        }
    }
}
