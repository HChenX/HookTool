package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import android.support.annotation.Nullable;

import com.hchen.hooktool.HCHook;
import com.hchen.hooktool.utils.DataUtils;
import com.hchen.hooktool.utils.SafeUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XposedHelpers;

public class MethodTool {
    private final DataUtils utils;
    private final SafeUtils safe;
    private int next = 0;

    public MethodTool(DataUtils utils) {
        this.utils = utils;
        this.safe = utils.safeUtils;
        clear();
    }

    /**
     * 适用于仅获取一个类的情况。
     */
    public ActionTool getMethod(String name, Class<?>... clzzs) {
        return getIndexMethod(0, name, clzzs);
    }

    /**
     * 按顺序索引类并查找方法。
     */
    public ActionTool getNextMethod(String name, Class<?>... clzzs) {
        if (utils.classes.size() != -1) {
            if (utils.classes.size() < next) {
                logW(utils.getTAG(), "no next class can get!");
                return utils.getActionTool();
            }
            ActionTool actionTool = getIndexMethod(next, name, clzzs);
            next = next + 1;
            return actionTool;
        } else {
            logW(utils.getTAG(), "class count is -1, can't get next!");
            return utils.getActionTool();
        }
    }

    public MethodTool resetCount() {
        next = 0;
        return utils.getMethodTool();
    }

    /**
     * 根据索引获取指定索引类中的指定方法，请注意多次调用查找同索引类中的方法，上次查找到的方法将会被覆盖！
     */
    public ActionTool getIndexMethod(int index, String name, Class<?>... clzzs) {
        if (!safe.classSafe()) return utils.getActionTool();
        if (utils.classes.isEmpty()) {
            logW(utils.getTAG(), "The class list is empty!");
            return utils.getActionTool();
        }
        if (utils.classes.size() < index) {
            logW(utils.getTAG(), "classes size < index! this method will is empty!");
            // utils.methods.put(index, new ArrayList<>());
            return utils.getActionTool();
        }
        ArrayList<Method> arrayList = new ArrayList<>();
        Class<?> c = utils.classes.get(index);
        if (c == null) {
            logW(utils.getTAG(), "getMethod but class is null!");
            utils.methods.put(index, new ArrayList<>());
            return utils.getActionTool();
        }
        try {
            arrayList.add(c.getMethod(name, clzzs));
        } catch (NoSuchMethodException e) {
            logE(utils.getTAG(), "The method to get the claim failed: " + name + " obj: " +
                    Arrays.toString(clzzs) + " e: " + e);
        }
        utils.methods.put(index, arrayList);
        utils.actionTool.methods = arrayList;
        return utils.getActionTool();
    }

    /**
     * 获取索引类中全部方法。
     */
    public MethodTool getAnyMethod(String method) {
        if (!safe.classSafe()) return utils.getMethodTool();
        try {
            for (int i = 0; i < utils.classes.size(); i++) {
                Class<?> clzz = utils.classes.get(i);
                if (clzz == null) {
                    logW(utils.getTAG(), "getAnyMethod clzz is null, will skip " +
                            "and method will is empty.");
                    utils.methods.put(i, new ArrayList<>());
                    break;
                }
                Method[] methods = clzz.getDeclaredMethods();
                ArrayList<Method> list = new ArrayList<>();
                for (Method m : methods) {
                    if (method.equals(m.getName())) {
                        list.add(m);
                    }
                }
                utils.methods.put(i, list);
            }
        } catch (Throwable e) {
            logE(utils.getTAG(), "Error getting match method: " + method + " e: " + e);
        }
        return utils.getMethodTool();
    }

    public MethodTool getConstructor(Class<?>... obj) {
        if (!safe.classSafe()) return utils.getMethodTool();
        if (utils.classes.isEmpty()) {
            logW(utils.getTAG(), "The class list is empty!");
            return utils.getMethodTool();
        }
        // utils.constructors.add(utils.classes.get(0).getConstructor(obj));
        Class<?> c = utils.classes.get(0);
        if (c == null) {
            logW(utils.getTAG(), "getConstructor but class is null!");
            utils.constructors.put(new Constructor[]{});
            return utils.getMethodTool();
        }
        Constructor<?> constructor = null;
        try {
            constructor = c.getConstructor(obj);
        } catch (NoSuchMethodException e) {
            logE(utils.getTAG(), "The specified constructor could not be found: " + utils.classes.get(0).getName() +
                    " obj: " + Arrays.toString(obj) + " e: " + e);
        }
        utils.constructors.put(new Constructor[]{constructor});
        return utils.getMethodTool();
    }

    public MethodTool getAnyConstructor() {
        if (!safe.classSafe()) return utils.getMethodTool();
        try {
            for (int i = 0; i < utils.classes.size(); i++) {
                Class<?> clzz = utils.classes.get(i);
                if (clzz == null) {
                    logW(utils.getTAG(), "getAnyConstructor clzz is null, will skip " +
                            "and method will is empty.");
                    utils.constructors.put(i, new Constructor[]{});
                    break;
                }
                Constructor<?>[] constructors = clzz.getConstructors();
                utils.constructors.put(i, constructors);
            }
        } catch (Throwable e) {
            logE(utils.getTAG(), "The any constructor could not be found: " + e);
        }
        return utils.getMethodTool();
    }

    @Nullable
    public Object callStaticMethod(String method, Object... args) {
        if (!safe.classSafe()) return null;
        try {
            if (utils.classes.isEmpty()) {
                logE(utils.getTAG(), "The class list is empty!");
                return utils.getMethodTool();
            }
            return XposedHelpers.callStaticMethod(utils.classes.get(0), method, args);
        } catch (Throwable e) {
            logE(utils.getTAG(), "Error calling method: " + method + " class: " + utils.findClass
                    + " args: " + Arrays.toString(args));
        }
        return null;
    }

    public HCHook hcHook() {
        return utils.getHCHook();
    }

    public ActionTool actionTool() {
        return utils.getActionTool();
    }

    public ClassTool classTool() {
        return utils.getClassTool();
    }

    public FieldTool fieldTool() {
        return utils.getFieldTool();
    }

    public void clear() {
        if (!utils.methods.isEmpty()) utils.methods.clear();
        if (!utils.constructors.isEmpty()) utils.constructors.clear();
    }
}
