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
package com.hchen.hooktool.core;

import static com.hchen.hooktool.core.CoreTool.findClass;
import static com.hchen.hooktool.log.XposedLog.logW;

import androidx.annotation.NonNull;

import com.hchen.hooktool.data.ChainData;
import com.hchen.hooktool.data.ChainType;
import com.hchen.hooktool.hook.AbsHook;
import com.hchen.hooktool.log.LogExpand;
import com.hchen.hooktool.log.XposedLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;

/**
 * 链式调用工具
 * <p>
 * 使用方法：
 * <pre>{@code
 * ChainTool.buildChain("com.hchen.demo.Test")
 *  .findMethod("test")
 *  .hook(new IHook() {
 *      // TODO
 *  })
 *  .findMethod("test1")
 *  .hook(new IHook() {
 *       // TODO
 *  });
 * }
 *
 * @author 焕晨HChen
 */
public class ChainTool {
    @NonNull
    private final Class<?> clazz;
    @NonNull
    private final ChainHook chainHook;
    private ChainData chainData;
    private final HashSet<ChainData> chainDataSet = new HashSet<>();

    private ChainTool(@NonNull Class<?> clazz) {
        Objects.requireNonNull(clazz, "class must not be null.");
        this.clazz = clazz;
        chainHook = new ChainHook();
    }

    public static ChainTool buildChain(@NonNull String classPath) {
        return new ChainTool(findClass(classPath));
    }

    public static ChainTool buildChain(@NonNull String classPath, ClassLoader classLoader) {
        return new ChainTool(findClass(classPath, classLoader));
    }

    public static ChainTool buildChain(@NonNull Class<?> clazz) {
        return new ChainTool(clazz);
    }

    public ChainHook findMethod(@NonNull String methodName, @NonNull Object... params) {
        chainData = new ChainData(methodName, params);
        return chainHook;
    }

    public ChainHook findMethodIfExist(@NonNull String methodName, @NonNull Object... params) {
        chainData = new ChainData(methodName, params);
        chainData.setIfExist(true);
        return chainHook;
    }

    public ChainHook findAllMethod(@NonNull String methodName) {
        chainData = new ChainData(methodName);
        return chainHook;
    }

    public ChainHook withMethod(@NonNull Method method) {
        chainData = new ChainData(method);
        return chainHook;
    }

    public ChainHook findConstructor(@NonNull Object... params) {
        chainData = new ChainData(params);
        return chainHook;
    }

    public ChainHook findConstructorIfExist(@NonNull Object... params) {
        chainData = new ChainData(params);
        chainData.setIfExist(true);
        return chainHook;
    }

    public ChainHook findAllConstructor() {
        chainData = new ChainData();
        return chainHook;
    }

    public ChainHook withConstructor(@NonNull Constructor<?> constructor) {
        chainData = new ChainData(constructor);
        return chainHook;
    }

    private void runChain() {
        Objects.requireNonNull(chainData, "[ChainTool]: Chain data must not be null!!");

        final ChainData tempChainData = chainData;
        if (!chainDataSet.contains(chainData)) {
            runFind();
            if (shouldHook()) {
                chainDataSet.add(tempChainData);
                for (Executable executable : chainData.executables) {
                    CoreTool.hook(executable).intercept(chainData.absHook);
                }
            }
        } else {
            XposedLog.logW(LogExpand.getTag(), "Duplicate content will be skipped: " + chainData);
        }
        chainData = null;
    }

    private void runFind() {
        switch (chainData.chainType) {
            case METHOD -> {
                chainData.executables[0] = chainData.method;
            }
            case ChainType.FIND_METHOD -> {
                if (chainData.ifExist) {
                    chainData.executables[0] = CoreTool.findMethodIfExists(clazz, chainData.methodName, chainData.methodParams);
                } else {
                    chainData.executables[0] = CoreTool.findMethod(clazz, chainData.methodName, chainData.methodParams);
                }
            }
            case FIND_ALL_METHOD -> {
                chainData.executables = CoreTool.findAllMethod(clazz, chainData.methodName);
            }
            case CONSTRUCTOR -> {
                chainData.executables[0] = chainData.constructor;
            }
            case FIND_CONSTRUCTOR -> {
                if (chainData.ifExist) {
                    chainData.executables[0] = CoreTool.findConstructorIfExists(clazz, chainData.constructorParams);
                } else {
                    chainData.executables[0] = CoreTool.findConstructor(clazz, chainData.constructorParams);
                }
            }
            case FIND_ALL_CONSTRUCTOR -> {
                chainData.executables = CoreTool.findAllConstructor(clazz);
            }
        }
    }

    private boolean shouldHook() {
        if (chainData.executables.length == 0) {
            logW(LogExpand.getTag(), "Hook method list cannot be empty! chainData: " + chainData);
            return false;
        }
        if (chainData.executables[0] == null) {
            if (!chainData.ifExist)
                logW(LogExpand.getTag(), "There is an abnormal null object in the member list, skip hook: " + chainData);
            return false;
        }
        return true;
    }

    public class ChainHook {
        private ChainHook() {
        }

        /**
         * Hook
         */
        public ChainTool hook(@NonNull AbsHook absHook) {
            chainData.absHook = absHook;
            runChain();
            return ChainTool.this;
        }

        /**
         * Hook 并返回值
         */
        public ChainTool returnResult(final Object result) {
            return hook(CoreTool.returnResult(result));
        }

        /**
         * 拦截方法执行
         */
        public ChainTool doNothing() {
            return hook(CoreTool.doNothing());
        }

        /**
         * 修改指定参数
         */
        public ChainTool setArg(int index, Object value) {
            return hook(CoreTool.setArg(index, value));
        }
    }
}
