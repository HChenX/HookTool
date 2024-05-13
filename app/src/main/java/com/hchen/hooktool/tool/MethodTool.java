package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.data.MemberData;
import com.hchen.hooktool.utils.DataUtils;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class MethodTool {
    private final DataUtils utils;

    private ArrayList<Member> findMember = null;

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
     * 获取本次查找到的方法，下次查找方法将会覆盖本次。<br/>
     * 可能是构造函数，也可能是方法。
     */
    @Nullable
    public ArrayList<Member> getFindMember() {
        return findMember;
    }

    /**
     * 按顺序索引类并查找方法。
     */
    public ActionTool getMethod(String name, Class<?>... clzzs) {
        return getIndexMethod(utils.getCount(), name, clzzs);
    }

    /**
     * 根据索引获取指定索引类中的指定方法.
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
        return getAnyMethodByIndex(utils.getCount(), name);
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
            utils.members.put(index, data);
            return utils.getActionTool();
        }
        Class<?> c = data.mClass;
        if (c == null) {
            logW(utils.getTAG(), "class is null! cant find: " + name + " index: " + index);
            // utils.methods.put(index, new ArrayList<>());
            utils.members.put(index, data);
            return utils.getActionTool();
        }
        ArrayList<Member> members = iMethodTool.doFindMethod(c, name, clzzs);
        findMember = members;
        data.memberMap.put(members);
        // data.isHooked = false;
        // data.mConstructor = null;
        utils.members.put(index, data);
        return utils.getActionTool();
    }

    //--------------------构造函数---------------------

    /**
     * 按顺序获取指定构造函数。
     */
    public ActionTool getConstructor(Class<?>... obj) {
        return getConstructorByIndex(utils.getCount(), obj);
    }

    /**
     * 按顺序获取全部构造函数。
     */
    public ActionTool getAnyConstructor() {
        return getAnyConstructorByIndex(utils.getCount());
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
    public ActionTool getAnyConstructorByIndex(int index) {
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
            return utils.getActionTool();
        }
        Class<?> c = data.mClass;
        if (c == null) {
            logW(utils.getTAG(), "class is null! index: " + index);
            utils.members.put(index, data);
            // utils.constructors.put(new Constructor[]{});
            return utils.getActionTool();
        }
        ArrayList<Member> members = iConstructorTool.doFindConstructor(c, classes);
        findMember = members;
        data.memberMap.put(members);
        // data.mConstructor = members;
        // data.mMethod = null;
        // data.isHooked = false;
        utils.members.put(index, data);
        return utils.getActionTool();
    }

    /* 不需要再回到此类 */
    // public HCHook hcHook() {
    //     return utils.getHCHook();
    // }

    public ActionTool actionTool() {
        return utils.getActionTool();
    }

    public ClassTool classTool() {
        return utils.getClassTool();
    }

    public FieldTool fieldTool() {
        return utils.getFieldTool();
    }

    /* 不建议使用 clear 本工具应该是一次性的。 */
    private void clear() {
        if (!utils.members.isEmpty()) utils.members.clear();
    }

    // 更棒的无缝衔接
    public ClassTool findClass(String name) {
        return utils.getClassTool().findClass(name);
    }

    // 优化调用，只提供基本用法，详细用法请获取工具类对象
    public MethodTool hook(IAction iAction) {
        return utils.getActionTool().hook(iAction);
    }

    private interface IMethodTool {
        ArrayList<Member> doFindMethod(Class<?> c, String name, Class<?>... classes);
    }

    private interface IConstructorTool {
        ArrayList<Member> doFindConstructor(Class<?> c, Class<?>... classes);
    }
}
