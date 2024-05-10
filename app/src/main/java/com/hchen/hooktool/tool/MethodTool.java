package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import android.support.annotation.Nullable;

import com.hchen.hooktool.HCHook;
import com.hchen.hooktool.utils.DataUtils;
import com.hchen.hooktool.utils.SafeUtils;

import java.lang.reflect.Member;
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

    public MethodTool resetCount() {
        next = 0;
        return utils.getMethodTool();
    }

    public MethodTool next() {
        next = next + 1;
        return utils.getMethodTool();
    }

    public MethodTool back() {
        next = next - 1;
        return utils.getMethodTool();
    }

    /**
     * 按顺序索引类并查找方法。
     */
    public ActionTool getMethod(String name, Class<?>... clzzs) {
        return getIndexMethod(next, name, clzzs);
    }

    /**
     * 根据索引获取指定索引类中的指定方法，请注意多次调用查找同索引类中的方法，上次查找到的方法将会被覆盖！
     */
    public ActionTool getIndexMethod(int index, String name, Class<?>... clzzs) {
        ArrayList<Member> method = findMethod(index, name, new IMethodTool() {
            @Override
            public ArrayList<Member> doFindMethod(Class<?> c, String name, Class<?>... classes) {
                ArrayList<Member> arrayList = new ArrayList<>();
                try {
                    arrayList.add(c.getMethod(name, classes));
                } catch (NoSuchMethodException e) {
                    logE(utils.getTAG(), "The method to get the claim failed name: " + name +
                            " class: " + c + " obj: " + Arrays.toString(classes) + " e: " + e);
                }
                return arrayList;
            }
        }, clzzs);
        if (method == null) {
            return utils.getActionTool();
        }
        utils.methods.put(index, method);
        utils.actionTool.members = method;
        return utils.getActionTool();
    }

    public ActionTool getAnyMethod(String name) {
        return getAnyMethodByIndex(next, name);
    }

    /**
     * 获取指定索引类中全部方法。
     */
    public ActionTool getAnyMethodByIndex(int index, String name) {
        ArrayList<Member> methods = findMethod(index, name, new IMethodTool() {
            @Override
            public ArrayList<Member> doFindMethod(Class<?> c, String name, Class<?>... classes) {
                ArrayList<Member> list = new ArrayList<>();
                Method[] methods = c.getDeclaredMethods();
                for (Method m : methods) {
                    if (name.equals(m.getName())) {
                        list.add(m);
                    }
                }
                return list;
            }
        });
        if (methods == null) {
            return utils.getActionTool();
        }
        utils.methods.put(index, methods);
        utils.actionTool.members = methods;
        return utils.getActionTool();
    }

    private ArrayList<Member> findMethod(int index, String name, IMethodTool iMethodTool, Class<?>... clzzs) {
        if (utils.classes.isEmpty()) {
            logW(utils.getTAG(), "the class list is empty! cant find method: " + name);
            return null;
        }
        if (utils.classes.size() - 1 < index) {
            logW(utils.getTAG(), "classes size < index! this method [" + name + "] will is empty! size: " +
                    (utils.classes.size() - 1) + " index: " + index);
            // utils.methods.put(index, new ArrayList<>());
            return null;
        }
        Class<?> c = utils.classes.get(index);
        if (c == null) {
            logW(utils.getTAG(), "class is null! cant find: " + name + " index: " + index);
            // utils.methods.put(index, new ArrayList<>());
            return new ArrayList<>();
        }
        return iMethodTool.doFindMethod(c, name, clzzs);
    }

    public ActionTool getConstructor(Class<?>... obj) {
        return getConstructorByIndex(next, obj);
    }

    public ActionTool getAnyConstructor() {
        return getConstructorByIndex(next);
    }

    public ActionTool getConstructorByIndex(int index, Class<?>... classes) {
        ArrayList<Member> members = findConstructor(index, new IConstructorTool() {
            @Override
            public ArrayList<Member> doFindConstructor(Class<?> c, Class<?>... classes) {
                ArrayList<Member> members = new ArrayList<>();
                try {
                    members.add(c.getConstructor(classes));
                } catch (NoSuchMethodException e) {
                    logE(utils.getTAG(), "The specified constructor could not be found: " + c +
                            " classes: " + Arrays.toString(classes) + " e: " + e);
                }
                return members;
            }
        }, classes);
        if (members == null) {
            return utils.getActionTool();
        }
        utils.constructors.put(index, members.toArray(new Member[0]));
        utils.actionTool.members = members;
        return utils.getActionTool();
    }

    private ActionTool getAnyConstructorByIndex(int index) {
        ArrayList<Member> members = findConstructor(index, new IConstructorTool() {
            @Override
            public ArrayList<Member> doFindConstructor(Class<?> c, Class<?>... classes) {
                return new ArrayList<>(Arrays.asList(c.getDeclaredConstructors()));
            }
        });
        if (members == null) {
            return utils.getActionTool();
        }
        utils.constructors.put(index, members.toArray(new Member[0]));
        utils.actionTool.members = members;
        return utils.getActionTool();
    }

    private ArrayList<Member> findConstructor(int index, IConstructorTool iConstructorTool, Class<?>... classes) {
        if (utils.classes.isEmpty()) {
            logW(utils.getTAG(), "The class list is empty!");
            return null;
        }
        if (utils.classes.size() - 1 < index) {
            logW(utils.getTAG(), "classes size < index! this constructor will is empty! size: " +
                    (utils.classes.size() - 1) + " index: " + index);
            // utils.methods.put(index, new ArrayList<>());
            return null;
        }
        // utils.constructors.add(utils.classes.get(0).getConstructor(obj));
        Class<?> c = utils.classes.get(index);
        if (c == null) {
            logW(utils.getTAG(), "class is null! index: " + index);
            // utils.constructors.put(new Constructor[]{});
            return new ArrayList<>();
        }
        return iConstructorTool.doFindConstructor(c, classes);
    }

    @Nullable
    public Object callStaticMethod(String method, Object... args) {
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

    interface IMethodTool {
        ArrayList<Member> doFindMethod(Class<?> c, String name, Class<?>... classes);
    }

    interface IConstructorTool {
        ArrayList<Member> doFindConstructor(Class<?> c, Class<?>... classes);
    }
}
