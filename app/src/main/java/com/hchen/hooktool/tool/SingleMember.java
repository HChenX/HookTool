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
package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;

import androidx.annotation.Nullable;

import com.hchen.hooktool.helper.TryHelper;
import com.hchen.hooktool.log.LogExpand;

/**
 * 储存成员与可能的抛错信息
 *
 * @author 焕晨HChen
 */
final class SingleMember<V> {
    private final V mMember;
    private final Throwable mThrowable;
    private String mErrorMsg = "Unknown";

    SingleMember() {
        this(null, null);
    }

    SingleMember(V member) {
        this(member, null);
    }

    SingleMember(V member, Throwable throwable) {
        mMember = member;
        mThrowable = throwable;
    }

    @Nullable
    V get() {
        report();
        return mMember;
    }

    @Nullable
    V or(V or) {
        if (isSuccess())
            return get();
        else {
            report();
            return or;
        }
    }

    @Nullable
    V getNotReport() {
        return mMember;
    }

    @Nullable
    Throwable getThrowable() {
        return mThrowable;
    }

    boolean isSuccess() {
        return mThrowable == null;
    }

    SingleMember<V> setErrorMsg(String msg) {
        mErrorMsg = msg;
        return this;
    }

    String getErrorMsg() {
        return mErrorMsg;
    }

    void report() {
        if (mThrowable != null)
            logE(LogExpand.getTag(), mErrorMsg, mThrowable);
    }

    static <V> SingleMember<V> createSingleMember(TryHelper.Run<V> supplier) {
        TryHelper.Result<V> result = new TryHelper.Result<>(supplier);
        return new SingleMember<>(result.get(), result.getThrowable());
    }

    <R> R exec(R def, Exec<V, R> exec) {
        if (mThrowable != null) {
            report();
            return def;
        }

        return (R) exec.exec(mMember);
    }

    interface Exec<V, R> {
        R exec(V value);
    }
}
