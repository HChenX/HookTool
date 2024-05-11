package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import com.hchen.hooktool.HCHook;
import com.hchen.hooktool.data.MemberData;
import com.hchen.hooktool.utils.DataUtils;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class MethodTool {
    private final DataUtils utils;

    public MethodTool(DataUtils utils) {
        this.utils = utils;
        clear();
    }

    public MethodTool reset() {
        utils.reset();
        return utils.getMethodTool();
    }

    public MethodTool next() {
        utils.next();
        return utils.getMethodTool();
    }

    public MethodTool back() {
        utils.back();
        return utils.getMethodTool();
    }

    /**
     * 按顺序索引类并查找方法。
     */
    public ActionTool getMethod(String name, Class<?>... clzzs) {
        return getIndexMethod(utils.next, name, clzzs);
    }

    /**
     * 根据索引获取指定索引类中的指定方法，请注意多次调用查找同索引类中的方法，上次查找到的方法将会被覆盖！
     */
    public ActionTool getIndexMethod(int index, String name, Class<?>... clzzs) {
        return findMethod(index, name, new IMethodTool() {
            @Override
            public ArrayList<Member> doFindMethod(Class<?> c, String name, Class<?>... classes) {
                ArrayList<Member> arrayList = new ArrayList<>();
                try {
                    arrayList.add(c.getMethod(name, classes));
                } catch (NoSuchMethodException e) {
                    logE(utils.getTAG(), "The method to get the claim failed name: " + name +
                            " class: " + c + " classes: " + Arrays.toString(classes) + " \ne: " + e);
                }
                return arrayList;
            }
        }, clzzs);
    }

    /**
     * 查找全部名称匹配方法。
     */
    public ActionTool getAnyMethod(String name) {
        return getAnyMethodByIndex(utils.next, name);
    }

    /**
     * 获取指定索引类中全部名称匹配方法。
     */
    public ActionTool getAnyMethodByIndex(int index, String name) {
        return findMethod(index, name, new IMethodTool() {
            @Override
            public ArrayList<Member> doFindMethod(Class<?> c, String name, Class<?>... classes) {
                ArrayList<Member> list = new ArrayList<>();
                Method[] methods = c.getMethods();
                for (Method m : methods) {
                    if (name.equals(m.getName())) {
                        list.add(m);
                    }
                }
                return list;
            }
        });
    }

    private ActionTool findMethod(int index, String name, IMethodTool iMethodTool, Class<?>... clzzs) {
        if (utils.classes.isEmpty()) {
            logW(utils.getTAG(), "the class list is empty! cant find method: " + name);
            return utils.getActionTool();
        }
        if (utils.classes.size() - 1 < index) {
            logW(utils.getTAG(), "classes size < index! the method [" + name + "] cant find! size: " +
                    (utils.classes.size() - 1) + " index: " + index);
            // utils.methods.put(index, new ArrayList<>());
            return utils.getActionTool();
        }
        MemberData data = utils.classes.get(index);
        if (data == null) {
            logW(utils.getTAG(), "memberData is null, cant find: " + name + " index: " + index);
            utils.members.put(index, null);
            utils.actionTool.members = null;
            return utils.getActionTool();
        }
        Class<?> c = data.mClass;
        if (c == null) {
            logW(utils.getTAG(), "class is null! cant find: " + name + " index: " + index);
            // utils.methods.put(index, new ArrayList<>());
            utils.members.put(index, data);
            utils.actionTool.members = null;
            return utils.getActionTool();
        }
        data.mMethod = iMethodTool.doFindMethod(c, name, clzzs);
        data.isHooked = false;
        data.mConstructor = null;
        utils.members.put(index, data);
        utils.actionTool.members = data.mMethod;
        return utils.getActionTool();
    }

    //--------------------构造函数---------------------

    /**
     * 按顺序获取指定构造函数。
     */
    public ActionTool getConstructor(Class<?>... obj) {
        return getConstructorByIndex(utils.next, obj);
    }

    /**
     * 按顺序获取全部构造函数。
     */
    public ActionTool getAnyConstructor() {
        return getAnyConstructorByIndex(utils.next);
    }

    /**
     * 按索引获取指定构造函数。
     */
    public ActionTool getConstructorByIndex(int index, Class<?>... classes) {
        return findConstructor(index, new IConstructorTool() {
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
    }

    /**
     * 按索引获取全部构造函数。
     */
    private ActionTool getAnyConstructorByIndex(int index) {
        return findConstructor(index, new IConstructorTool() {
            @Override
            public ArrayList<Member> doFindConstructor(Class<?> c, Class<?>... classes) {
                return new ArrayList<>(Arrays.asList(c.getDeclaredConstructors()));
            }
        });
    }

    private ActionTool findConstructor(int index, IConstructorTool iConstructorTool, Class<?>... classes) {
        if (utils.classes.isEmpty()) {
            logW(utils.getTAG(), "The class list is empty!");
            return utils.getActionTool();
        }
        if (utils.classes.size() - 1 < index) {
            logW(utils.getTAG(), "classes size < index! the constructor cant find! size: " +
                    (utils.classes.size() - 1) + " index: " + index);
            // utils.methods.put(index, new ArrayList<>());
            return utils.getActionTool();
        }
        // utils.constructors.add(utils.classes.get(0).getConstructor(obj));
        MemberData data = utils.classes.get(index);
        if (data == null) {
            logW(utils.getTAG(), "memberData is null, index: " + index);
            utils.members.put(index, null);
            utils.actionTool.members = null;
            return utils.getActionTool();
        }
        Class<?> c = data.mClass;
        if (c == null) {
            logW(utils.getTAG(), "class is null! index: " + index);
            utils.actionTool.members = null;
            utils.members.put(index, data);
            // utils.constructors.put(new Constructor[]{});
            return utils.getActionTool();
        }
        ArrayList<Member> members = iConstructorTool.doFindConstructor(c, classes);
        data.mConstructor = members;
        data.mMethod = null;
        data.isHooked = false;
        utils.members.put(index, data);
        utils.actionTool.members = members;
        return utils.getActionTool();
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
        if (!utils.members.isEmpty()) utils.members.clear();
    }

    interface IMethodTool {
        ArrayList<Member> doFindMethod(Class<?> c, String name, Class<?>... classes);
    }

    interface IConstructorTool {
        ArrayList<Member> doFindConstructor(Class<?> c, Class<?>... classes);
    }
}
