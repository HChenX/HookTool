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

import androidx.annotation.Nullable;

import com.hchen.hooktool.log.LogExpand;

/**
 * 储存成员与可能的抛错信息
 *
 * @author 焕晨HChen
 */
public final class SingleMember<T> {
    private final T mMember;
    private final Throwable mThrowable;
    private String mMsg = "Unknown";

    public SingleMember(T member) {
        this(member, null);
    }

    public SingleMember(T member, Throwable throwable) {
        mMember = member;
        mThrowable = throwable;
    }

    @Nullable
    public T get() {
        report();
        return mMember;
    }

    @Nullable
    public T or(T or) {
        T get = get();
        if (get == null)
            return or;
        return get;
    }

    @Nullable
    public T getNoReport() {
        return mMember;
    }

    @Nullable
    public Throwable getThrowable() {
        return mThrowable;
    }

    public boolean isSuccess() {
        return mThrowable == null;
    }

    SingleMember<T> setErrMsg(String msg) {
        mMsg = msg;
        return this;
    }

    String getErrMsg() {
        return mMsg;
    }

    private void report() {
        if (mThrowable != null)
            logE(LogExpand.getTag(), mMsg, mThrowable);
    }

    <R> R reportOrRun(Run<T> run) {
        return reportOrRun(run, null);
    }

    <R> R reportOrRun(Run<T> run, R def) {
        if (mThrowable != null) {
            report();
            return def;
        }
        return (R) run.run(mMember);
    }

    interface Run<T> {
        Object run(T member);
    }
}
