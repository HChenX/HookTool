package com.hchen.hooktool.tool.param;

import static com.hchen.hooktool.log.XposedLog.logE;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Member;

public class Arguments<T> extends ActAchieve<T> {

    public Arguments(Member member, String tag) {
        super(member, tag);
    }

    @Nullable
    public T get(int index) {
        if (size() == -1) {
            return null;
        } else if (size() < index + 1) {
            logE(TAG, "method: " + member.getName() +
                    " param max size: " + size() + " index: " + index + " !");
            return null;
        }
        return (T) param.args[index];
    }

    // ------- 提供五个快捷获取 ---------
    @Nullable
    public T one() {
        return get(0);
    }

    @Nullable
    public T two() {
        return get(1);
    }

    @Nullable
    public T three() {
        return get(2);
    }

    @Nullable
    public T four() {
        return get(3);
    }

    @Nullable
    public T five() {
        return get(4);
    }

    @Nullable
    public Arguments<T> set(int index, T value) {
        if (size() == -1) {
            return this;
        } else if (size() < index + 1) {
            logE(TAG, "method: " + member.getName() +
                    " param max size: " + size() + " index: " + index + " !");
            return this;
        }
        param.args[index] = value;
        return this;
    }

    // ------- 提供五个快捷设置 ---------
    @Nullable
    public Arguments<T> one(T value) {
        return set(0, value);
    }

    @Nullable
    public Arguments<T> two(T value) {
        return set(1, value);
    }

    @Nullable
    public Arguments<T> three(T value) {
        return set(2, value);
    }

    @Nullable
    public Arguments<T> four(T value) {
        return set(3, value);
    }

    @Nullable
    public Arguments<T> five(T value) {
        return set(4, value);
    }

    public int size() {
        paramSafe();
        return param.args.length;
    }

    // ------- 不常用工具 ----------

}
