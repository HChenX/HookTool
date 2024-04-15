package com.hchen.hooktool.tool;

import android.support.annotation.Nullable;

import com.hchen.hooktool.hc.HCHook;
import com.hchen.hooktool.utils.Utils;

import java.lang.reflect.Field;

import de.robv.android.xposed.XposedHelpers;

public class FieldTool extends Utils {
    public FieldTool() {
    }

    public HCHook findField(String name) {
        if (!safe.classSafe()) return hcHook;
        try {
            findField = XposedHelpers.findField(findClass, name);
            safe.setFindField(findField);
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
        if (!safe.fieldSafe()) return null;
        Object obj = null;
        try {
            if (param != null) {
                obj = param.thisObject;
                return findField.get(obj);
            } else logE(useTAG(), "param is null!!");
        } catch (IllegalAccessException e) {
            logE(useTAG(), "Failed to read field contents: " + findField + " obj: " + obj);
        }
        return null;
    }

    @Nullable
    public Object getStatic() {
        if (!safe.fieldSafe()) return null;
        try {
            return findField.get(null);
        } catch (IllegalAccessException e) {
            logE(useTAG(), "Failed to read field contents: " + findField);
        }
        return null;
    }

    public void set(Object value) {
        if (!safe.fieldSafe()) return;
        Object obj = null;
        try {
            if (param != null) {
                obj = param.thisObject;
                findField.set(obj, value);
            } else logE(useTAG(), "param is null!!");
        } catch (IllegalAccessException e) {
            logE(useTAG(), "Failed to write field contents: " + findField + " obj: " + obj);
        }
    }

    public void setStatic(Object value) {
        if (!safe.fieldSafe()) return;
        try {
            findField.set(null, value);
        } catch (IllegalAccessException e) {
            logE(useTAG(), "Failed to read field contents: " + findField);
        }
    }
}
