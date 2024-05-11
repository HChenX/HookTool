package com.hchen.hooktool.tool;

import static com.hchen.hooktool.HCHook.initSafe;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;
import static com.hchen.hooktool.utils.DataUtils.classLoader;

import com.hchen.hooktool.HCHook;
import com.hchen.hooktool.data.MemberData;
import com.hchen.hooktool.utils.DataUtils;
import com.hchen.hooktool.utils.MapUtils;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XposedHelpers;

public class ClassTool {
    private final DataUtils utils;

    public ClassTool(DataUtils utils) {
        this.utils = utils;
        utils.classes.clear();
        utils.classTool = this;
    }

    /**
     * 查找指定类
     */
    public ClassTool findClass(String className) {
        initSafe();
        if (utils.findClass != null) utils.findClass = null;
        try {
            utils.findClass = XposedHelpers.findClass(className,
                    utils.mCustomClassLoader == null ? classLoader : utils.mCustomClassLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(utils.getTAG(), "The specified class could not be found: " + className + " e: " + e);
            utils.findClass = null;
        }
        // utils.classes.add(utils.findClass);
        utils.classes.put(new MemberData(utils.findClass));
        return utils.getClassTool();
    }

    /* 获取本次查找到的类，必须在下次查找前调用才能获取本次。 */
    @Nullable
    public Class<?> get() {
        return utils.findClass;
    }

    public int size() {
        return utils.classes.size();
    }

    /**
     * 实例列表第一个类。
     */
    @Nullable
    public Object newInstance(Object... args) {
        return newInstance(0, args);
    }

    /**
     * 实例指定索引类，索引从 0 开始。
     */
    @Nullable
    public Object newInstance(int index, Object... args) {
        try {
            if (utils.classes.size() < index || index < 0) {
                logE(utils.getTAG(), "The index is out of range!");
                return null;
            }
            MemberData memberData = utils.classes.get(index);
            if (memberData != null && memberData.mClass != null) {
                return XposedHelpers.newInstance(memberData.mClass, args);
            } else logW(utils.getTAG(), "class is null, cant new instance. index: " + index);
        } catch (Throwable e) {
            logE(utils.getTAG(), "Error creating instance: " + utils.findClass + " args: " + Arrays.toString(args));
        }
        return null;
    }

    /**
     * 实例全部类。
     * 不建议使用。
     */
    public ArrayList<Object> newInstanceAll(MapUtils<Object[]> mapUtils) {
        utils.newInstances.clear();
        if (utils.newInstances.size() != mapUtils.getHashMap().size()) {
            logE(utils.getTAG(), "The length of the instance parameter list is inconsistent!");
            return new ArrayList<>();
        }
        for (int i = 0; i < size(); i++) {
            utils.newInstances.add(newInstance(i, mapUtils.get(i)));
        }
        return utils.newInstances;
    }

    public HCHook hcHook() {
        return utils.getHCHook();
    }

    public MethodTool methodTool() {
        return utils.getMethodTool();
    }

    public FieldTool fieldTool() {
        return utils.getFieldTool();
    }

    public ClassTool clear() {
        utils.classes.clear();
        return utils.getClassTool();
    }
}
