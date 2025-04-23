package com.hchen.hooktool.data;

import androidx.annotation.NonNull;

import com.hchen.hooktool.hook.IHook;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

public class ChainData {
    // -------------------------- Data ------------------------------
    public ChainType chainType;
    public IHook iHook;
    public boolean ifExist;
    public Member[] members;
    public static final CopyOnWriteArraySet<ChainData> chainDataSet = new CopyOnWriteArraySet<>();

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

    public void setIfExist(boolean ifExist) {
        this.ifExist = ifExist;
    }

    @Override
    @NonNull
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
        return ifExist == chainData.ifExist &&
            chainType == chainData.chainType &&
            // Objects.equals(iHook, chainData.iHook) && ignore
            // Objects.deepEquals(members, chainData.members) && ignore
            Objects.equals(methodName, chainData.methodName) &&
            Objects.deepEquals(methodParams, chainData.methodParams) &&
            Objects.equals(method, chainData.method) &&
            Objects.deepEquals(constructorParams, chainData.constructorParams) &&
            Objects.equals(constructor, chainData.constructor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chainType, /* iHook ignore*/ ifExist,
            /* Arrays.hashCode(members) ignore */ methodName, Arrays.hashCode(methodParams),
            method, Arrays.hashCode(constructorParams), constructor);
    }
}
