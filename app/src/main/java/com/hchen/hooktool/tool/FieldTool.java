package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;

import android.support.annotation.Nullable;

import java.lang.reflect.Field;

import de.robv.android.xposed.XposedHelpers;

public class FieldTool {
    private final UtilsTool utils;
    private final SafeTool safe;

    public FieldTool(UtilsTool utils) {
        this.utils = utils;
        this.safe = utils.safeTool;
    }

    public FieldTool findField(String name) {
        if (!safe.classSafe()) return getFieldTool();
        try {
            utils.findField = XposedHelpers.findField(utils.findClass, name);
        } catch (NoSuchFieldError e) {
            logE(getTAG(), "Failed to get claim field: " + name + " class: " + utils.findClass + " e: " + e);
        }
        return getFieldTool();
    }

    @Nullable
    public Field getField() {
        return utils.findField;
    }

    @Nullable
    public Object get() {
        if (!safe.fieldSafe()) return null;
        Object obj = null;
        try {
            if (!safe.paramSafe()) return null;
            obj = utils.param.thisObject;
            return utils.findField.get(obj);
        } catch (IllegalAccessException e) {
            logE(getTAG(), "Failed to read field contents: " + utils.findField + " obj: " + obj);
        }
        return null;
    }

    @Nullable
    public Object getStatic() {
        if (!safe.fieldSafe()) return null;
        try {
            return utils.findField.get(null);
        } catch (IllegalAccessException e) {
            logE(getTAG(), "Failed to read field contents: " + utils.findField);
        }
        return null;
    }

    public void set(Object value) {
        if (!safe.fieldSafe()) return;
        Object obj = null;
        try {
            if (!safe.paramSafe()) return;
            obj = utils.param.thisObject;
            utils.findField.set(obj, value);
        } catch (IllegalAccessException e) {
            logE(getTAG(), "Failed to write field contents: " + utils.findField + " obj: " + obj);
        }
    }

    public void setStatic(Object value) {
        if (!safe.fieldSafe()) return;
        try {
            utils.findField.set(null, value);
        } catch (IllegalAccessException e) {
            logE(getTAG(), "Failed to read field contents: " + utils.findField);
        }
    }

    private String getTAG() {
        return utils.useTAG();
    }

    private FieldTool getFieldTool() {
        FieldTool fieldTool = utils.fieldTool;
        if (fieldTool == null)
            throw new RuntimeException(getTAG() + ": FieldTool is null!!");
        return fieldTool;
    }
}
