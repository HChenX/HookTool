package com.hchen.hooktool.tool;

import static com.hchen.hooktool.HCHook.initSafe;
import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;
import static com.hchen.hooktool.utils.DataUtils.classLoader;

import com.hchen.hooktool.data.MemberData;
import com.hchen.hooktool.utils.DataUtils;
import com.hchen.hooktool.utils.MapUtils;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class ClassTool {
    private final DataUtils utils;

    public ClassTool(DataUtils utils) {
        this.utils = utils;
        clear();
        utils.classTool = this;
    }

    public MethodTool next() {
        utils.next();
        return utils.getMethodTool();
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


    // ---------- 实例方法 -----------

    /**
     * 实例当前索引类。
     */
    @Nullable
    public Object newInstance(Object... args) {
        return newInstance(utils.getCount(), args);
    }

    /**
     * 实例指定索引类，索引从 0 开始。
     */
    @Nullable
    public Object newInstance(int index, Object... args) {
        if (utils.classes.size() - 1 < index || index < 0) {
            logE(utils.getTAG(), "The index is out of range!");
            return null;
        }
        MemberData memberData = utils.classes.get(index);
        if (memberData != null && memberData.mClass != null) {
            try {
                return XposedHelpers.newInstance(memberData.mClass, args);
            } catch (Throwable e) {
                logE(utils.getTAG(), "new instance class: " + memberData.mClass + " e: " + e);
            }
        } else logW(utils.getTAG(), "class is null, cant new instance. index: " + index);
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

    // 无需再回归此类。
    // public HCHook hcHook() {
    //     return utils.getHCHook();
    // }

    public MethodTool methodTool() {
        return utils.getMethodTool();
    }

    public FieldTool fieldTool() {
        return utils.getFieldTool();
    }

    /* 不建议使用 clear 本工具应该是一次性的。 */
    private void clear() {
        utils.classes.clear();
    }
}
