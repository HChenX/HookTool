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
package com.hchen.hooktool.core;

import static com.hchen.hooktool.core.CoreTool.findClass;

import androidx.annotation.NonNull;

import com.hchen.hooktool.data.ChainData;
import com.hchen.hooktool.data.ChainType;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.log.LogExpand;
import com.hchen.hooktool.log.XposedLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
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
    private final Class<?> clazz;
    private final ChainHook chainHook;
    private ChainData chainData;

    private ChainTool(@NonNull Class<?> clazz) {
        chainHook = new ChainHook();
        this.clazz = clazz;
    }

    /**
     * 构建链式
     *
     * @param classPath 类
     */
    public static ChainTool buildChain(String classPath) {
        return new ChainTool(findClass(classPath));
    }

    /**
     * 构建链式
     *
     * @param classPath   类
     * @param classLoader 类加载器
     */
    public static ChainTool buildChain(String classPath, ClassLoader classLoader) {
        return new ChainTool(findClass(classPath, classLoader));
    }

    /**
     * 构建链式
     *
     * @param clazz 类
     */
    public static ChainTool buildChain(@NonNull Class<?> clazz) {
        Objects.requireNonNull(clazz, "[ChainTool]: Clazz must not is null!");
        return new ChainTool(clazz);
    }

    /**
     * 查找方法
     */
    public ChainHook findMethod(String methodName, @NonNull Object... params) {
        chainData = new ChainData(methodName, params);
        return chainHook;
    }

    /**
     * 查找方法，如果存在
     */
    public ChainHook findMethodIfExist(String methodName, @NonNull Object... params) {
        chainData = new ChainData(methodName, params);
        chainData.setIfExist(true);
        return chainHook;
    }

    /**
     * 查找全部指定名称的方法
     */
    public ChainHook findAllMethod(String methodName) {
        chainData = new ChainData(methodName);
        return chainHook;
    }

    /**
     * 传入指定方法
     */
    public ChainHook withMethod(@NonNull Method method) {
        chainData = new ChainData(method);
        return chainHook;
    }

    /**
     * 查找构造函数
     */
    public ChainHook findConstructor(@NonNull Object... params) {
        chainData = new ChainData(params);
        return chainHook;
    }

    /**
     * 查找构造方法，如果存在
     */
    public ChainHook findConstructorIfExist(@NonNull Object... params) {
        chainData = new ChainData(params);
        chainData.setIfExist(true);
        return chainHook;
    }

    /**
     * 查找全部构造函数
     */
    public ChainHook findAllConstructor() {
        chainData = new ChainData();
        return chainHook;
    }

    /**
     * 传入指定构造函数
     */
    public ChainHook withConstructor(@NonNull Constructor<?> constructor) {
        chainData = new ChainData(constructor);
        return chainHook;
    }

    private void runChain() {
        Objects.requireNonNull(chainData, "[ChainTool]: Chain data must not is null!");

        if (ChainData.chainDataSet.isEmpty()) {
            runFind();
            ChainData.chainDataSet.add(chainData);
            CoreTool.hookAll(chainData.members, chainData.iHook);
        } else {
            if (ChainData.chainDataSet.contains(chainData)) {
                runFind();
                ChainData.chainDataSet.add(chainData);
                CoreTool.hookAll(chainData.members, chainData.iHook);
            } else {
                XposedLog.logW(LogExpand.getTag(), "Duplicate content will be skipped: " + chainData);
            }
        }
        chainData = null;
    }

    private void runFind() {
        switch (chainData.chainType) {
            case METHOD -> {
                chainData.members[0] = chainData.method;
            }
            case ChainType.FIND_METHOD -> {
                if (chainData.ifExist) {
                    chainData.members[0] = CoreTool.findMethodIfExists(clazz, chainData.methodName, chainData.methodParams);
                } else {
                    chainData.members[0] = CoreTool.findMethod(clazz, chainData.methodName, chainData.methodParams);
                }
            }
            case FIND_ALL_METHOD -> {
                chainData.members = CoreTool.findAllMethod(clazz, chainData.methodName);
            }
            case CONSTRUCTOR -> {
                chainData.members[0] = chainData.constructor;
            }
            case FIND_CONSTRUCTOR -> {
                if (chainData.ifExist) {
                    chainData.members[0] = CoreTool.findConstructorIfExists(clazz, chainData.constructorParams);
                } else {
                    chainData.members[0] = CoreTool.findConstructor(clazz, chainData.constructorParams);
                }
            }
            case FIND_ALL_CONSTRUCTOR -> {
                chainData.members = CoreTool.findAllConstructor(clazz);
            }
        }
    }

    public class ChainHook {
        private ChainHook() {
        }

        /**
         * Hook
         */
        public ChainTool hook(@NonNull IHook iHook) {
            chainData.iHook = iHook;
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
