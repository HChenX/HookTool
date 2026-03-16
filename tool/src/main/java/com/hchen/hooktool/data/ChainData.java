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
package com.hchen.hooktool.data;

import androidx.annotation.NonNull;

import com.hchen.hooktool.hook.IHook;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * 链式数据
 *
 * @author 焕晨HChen
 */
public class ChainData {
    // -------------------------- Data ------------------------------

    public ChainType chainType;
    public IHook iHook;
    public boolean ifExist;
    public Member[] members = new Member[1];

    // -------------------------- Method ------------------------------

    public String methodName;
    public Object[] methodParams;
    public Method method;

    public ChainData(String methodName, Object... methodParams) {
        this.methodName = methodName;
        this.methodParams = methodParams;
        this.chainType = ChainType.FIND_METHOD;
    }

    public ChainData(String methodName) {
        this.methodName = methodName;
        this.chainType = ChainType.FIND_ALL_METHOD;
    }

    public ChainData(Method method) {
        this.method = method;
        this.chainType = ChainType.METHOD;
    }

    // -------------------------- Constructor ------------------------------

    public Object[] constructorParams;
    public Constructor<?> constructor;

    public ChainData() {
        this.chainType = ChainType.FIND_ALL_CONSTRUCTOR;
    }

    public ChainData(Object... constructorParams) {
        this.constructorParams = constructorParams;
        this.chainType = ChainType.FIND_CONSTRUCTOR;
    }

    public ChainData(Constructor<?> constructor) {
        this.constructor = constructor;
        this.chainType = ChainType.CONSTRUCTOR;
    }

    // ------------------------ Exist ---------------------------------

    public void setIfExist(boolean ifExist) {
        this.ifExist = ifExist;
    }

    @NonNull
    @Override
    public String toString() {
        return "ChainData{" +
            "chainType=" + chainType +
            ", iHook=" + iHook +
            ", ifExist=" + ifExist +
            ", members=" + Arrays.toString(members) +
            ", methodName='" + methodName + '\'' +
            ", methodParams=" + Arrays.toString(methodParams) +
            ", method=" + method +
            ", constructorParams=" + Arrays.toString(constructorParams) +
            ", constructor=" + constructor +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChainData chainData)) return false;
        return // ifExist == chainData.ifExist && ignore
            chainType == chainData.chainType &&
                // Objects.equals(iHook, chainData.iHook) && ignore
                // Objects.deepEquals(members, chainData.members) && ignore
                Objects.equals(method, chainData.method) &&
                Objects.equals(methodName, chainData.methodName) &&
                Objects.deepEquals(methodParams, chainData.methodParams) &&
                Objects.equals(constructor, chainData.constructor) &&
                Objects.deepEquals(constructorParams, chainData.constructorParams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chainType,
            /* ifExist ignore */ /* iHook ignore */ /* Arrays.hashCode(members) ignore */
            method, methodName, Arrays.hashCode(methodParams),
            Arrays.hashCode(constructorParams), constructor);
    }
}
