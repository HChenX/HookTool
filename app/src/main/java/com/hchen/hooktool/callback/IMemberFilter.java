package com.hchen.hooktool.callback;

public interface IMemberFilter<T> {
    boolean test(T member);
}
