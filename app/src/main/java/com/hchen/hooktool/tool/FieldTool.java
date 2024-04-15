package com.hchen.hooktool.tool;

import android.support.annotation.Nullable;

import com.hchen.hooktool.hc.HCHook;
import com.hchen.hooktool.safe.Safe;

import java.lang.reflect.Field;

import de.robv.android.xposed.XposedHelpers;

public class FieldTool extends Safe {
    public FieldTool() {
    }

    public HCHook findField(String name) {
        if (!classSafe()) return hcHook;
        try {
            findField = XposedHelpers.findField(findClass, name);
        } catch (NoSuchFieldError e) {
            logE(useTAG(), "Failed to get claim field: " + name + " class: " + findClass + " e: " + e);
        }
        return hcHook;
    }

    @Nullable
    public Field getField() {
        return findField;
    }

    @Nullable
    public Object get() {
        if (!fieldSafe()) return null;
        Object obj = null;
        try {
            if (!paramSafe()) return null;
            obj = param.thisObject;
            return findField.get(obj);
        } catch (IllegalAccessException e) {
            logE(useTAG(), "Failed to read field contents: " + findField + " obj: " + obj);
        }
        return null;
    }

    @Nullable
    public Object getStatic() {
        if (!fieldSafe()) return null;
        try {
            return findField.get(null);
        } catch (IllegalAccessException e) {
            logE(useTAG(), "Failed to read field contents: " + findField);
        }
        return null;
    }

    public void set(Object value) {
        if (!fieldSafe()) return;
        Object obj = null;
        try {
            if (!paramSafe()) return;
            obj = param.thisObject;
            findField.set(obj, value);
        } catch (IllegalAccessException e) {
            logE(useTAG(), "Failed to write field contents: " + findField + " obj: " + obj);
        }
    }

    public void setStatic(Object value) {
        if (!fieldSafe()) return;
        try {
            findField.set(null, value);
        } catch (IllegalAccessException e) {
            logE(useTAG(), "Failed to read field contents: " + findField);
        }
    }
}
