/*
 * This file is part of HookTool.
 *
 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * HookTool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with HookTool. If not, see <https://www.gnu.org/licenses/lgpl-2.1>.
 *
 * Copyright (C) 2024–2026 HChenX
 */
package com.hchen.hooktool.hook;

import androidx.annotation.NonNull;

import com.hchen.hooktool.log.LogExpand;

import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.github.libxposed.api.XposedInterface;

/**
 * AbsHook
 *
 * @author 焕晨HChen
 */
public abstract class AbsHook {
    private XposedInterface.HookHandle handle;
    private XposedInterface.Chain chain;
    private Object[] args;
    private Object oriResult;
    private Object newResult;
    private Object replaceObject;
    private boolean isProceedCalled = false;
    private boolean isArgsChanged = false;
    private boolean isResultChanged = false;

    public enum StageEnum {
        BEFORE,
        PROCEED,
        AFTER
    }

    public void before() {
    }

    public void after() {
    }

    public boolean onThrow(@NonNull StageEnum stage, @NonNull Throwable e) {
        return false;
    }

    @NonNull
    public final Executable getExecutable() {
        Objects.requireNonNull(chain);
        return chain.getExecutable();
    }

    public final Object getThisObject() {
        Objects.requireNonNull(chain);
        return chain.getThisObject();
    }

    @NonNull
    public final List<Object> getArgs() {
        Objects.requireNonNull(chain);
        return chain.getArgs();
    }

    public final Object getArg(int index) throws IndexOutOfBoundsException, ClassCastException {
        Objects.requireNonNull(chain);
        return chain.getArg(index);
    }

    public final void setArg(int index, Object value) {
        Objects.requireNonNull(chain);
        if (args == null) {
            args = getArgs().toArray(new Object[0]);
        }

        args[index] = value;
        isArgsChanged = true;
    }

    public final void setArgs(Object... args) {
        Objects.requireNonNull(chain);
        this.args = args;
        isArgsChanged = true;
    }

    public final Object getResult() {
        Objects.requireNonNull(chain);

        if (isResultChanged) {
            return newResult;
        }
        return oriResult;
    }

    public final void setResult(Object result) {
        Objects.requireNonNull(chain);
        newResult = result;
        isResultChanged = true;
    }

    public final void setReplaceObject(@NonNull Object replaceObject) {
        Objects.requireNonNull(chain);
        Objects.requireNonNull(replaceObject);
        this.replaceObject = replaceObject;
    }

    final void setHandle(@NonNull XposedInterface.HookHandle handle) {
        this.handle = handle;
    }

    final void setChain(@NonNull XposedInterface.Chain chain) {
        this.chain = chain;
    }

    final void callProceed() throws Throwable {
        Objects.requireNonNull(chain);
        if (isProceedCalled) {
            throw new RuntimeException("The 'proceed/proceedWith' method can only be called once!!");
        }
        if (isResultChanged) {
            return;
        }

        isProceedCalled = true;
        if (Modifier.isStatic(getExecutable().getModifiers())) {
            if (isArgsChanged) {
                oriResult = chain.proceed(args);
            } else {
                oriResult = chain.proceed();
            }
        } else {
            if (isArgsChanged) {
                if (replaceObject != null) {
                    oriResult = chain.proceedWith(replaceObject, args);
                } else {
                    oriResult = chain.proceed(args);
                }
            } else {
                if (replaceObject != null) {
                    oriResult = chain.proceedWith(replaceObject);
                } else {
                    oriResult = chain.proceed();
                }
            }
        }
    }

    final void reset() {
        args = null;
        oriResult = null;
        newResult = null;
        replaceObject = null;
        isProceedCalled = false;
        isResultChanged = false;
        isArgsChanged = false;
    }

    final public void unHookSelf() {
        Objects.requireNonNull(handle);
        handle.unhook();
    }

    final public String observeCall() {
        return LogExpand.observeCall(this);
    }

    @NonNull
    @Override
    public String toString() {
        return "AbsHook{" +
            "handle=" + handle +
            ", chain=" + chain +
            ", args=" + Arrays.toString(args) +
            ", oriResult=" + oriResult +
            ", newResult=" + newResult +
            ", replaceObject=" + replaceObject +
            ", isProceedCalled=" + isProceedCalled +
            ", isArgsChanged=" + isArgsChanged +
            ", isResultChanged=" + isResultChanged +
            '}';
    }
}
