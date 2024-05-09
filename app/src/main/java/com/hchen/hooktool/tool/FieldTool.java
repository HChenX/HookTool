package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import android.support.annotation.Nullable;

import com.hchen.hooktool.HCHook;
import com.hchen.hooktool.utils.DataUtils;
import com.hchen.hooktool.utils.SafeUtils;

import java.lang.reflect.Field;

import de.robv.android.xposed.XposedHelpers;

public class FieldTool {
    private final DataUtils utils;
    private final SafeUtils safe;

    public FieldTool(DataUtils utils) {
        this.utils = utils;
        this.safe = utils.safeUtils;
    }

    public FieldTool findField(String name) {
        return findIndexField(0, name);
    }

    public FieldTool findIndexField(int index, String name) {
        if (!safe.classSafe()) return utils.getFieldTool();
        utils.findField = null;
        if (utils.classes.isEmpty()) {
            logE(utils.getTAG(), "The class list is empty!");
            return utils.getFieldTool();
        }
        if (utils.classes.size() < index) {
            logW(utils.getTAG(), "index > class size, can't find field!");
            return utils.getFieldTool();
        }
        Class<?> c = utils.classes.get(index);
        if (c == null) {
            logW(utils.getTAG(), "findField but class is null!");
            return utils.getFieldTool();
        }
        try {
            utils.findField = XposedHelpers.findField(utils.classes.get(0), name);
        } catch (NoSuchFieldError e) {
            logE(utils.getTAG(), "Failed to get claim field: " + name + " class: " + utils.findClass + " e: " + e);
        }
        return utils.getFieldTool();
    }

    @Nullable
    public Field getField() {
        return utils.findField;
    }

    public HCHook hcHook() {
        return utils.getHCHook();
    }

    public ClassTool classTool() {
        return utils.getClassTool();
    }

    public MethodTool methodTool() {
        return utils.getMethodTool();
    }
}
