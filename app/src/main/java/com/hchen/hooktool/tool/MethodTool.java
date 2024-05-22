/*
 * This file is part of HookTool.

 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2024 HookTool Contributions
 */
package com.hchen.hooktool.tool;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logW;

import androidx.annotation.Nullable;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.data.MemberData;
import com.hchen.hooktool.data.StateEnum;
import com.hchen.hooktool.utils.DataUtils;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XposedHelpers;

public class MethodTool {
    private final DataUtils utils;

    private ArrayList<Member> findMember = null;

    public MethodTool(DataUtils utils) {
        this.utils = utils;
        clear();
    }

    public MethodTool to(Object label) {
        utils.getClassTool().to(label);
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

    @Nullable
    private Class<?> findClass(String name) {
        try {
            return XposedHelpers.findClass(name,
                    utils.getClassLoader());
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(utils.getTAG(), "The specified class could not be found: " + name + " e: " + e);
        }
        return null;
    }

    /**
     * 获取标签类内的指定方法。
     */
    public ActionTool getMethod(String name, Object... objs) {
        Class<?>[] clist = objsToClist(objs);
        if (clist == null) return utils.getActionTool();
        return findMethod(name, new IMethodTool() {
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
        }, clist);
    }

    /**
     * 获取标签类中全部名称匹配方法。
     */
    public ActionTool getAnyMethod(String name) {
        return findMethod(name, new IMethodTool() {
            @Override
            public ArrayList<Member> doFindMethod(Class<?> c, String name, Class<?>... classes) {
                ArrayList<Member> list = new ArrayList<>();
                Method[] methods = c.getMethods();
                for (Method m : methods) {
                    if (name.equals(m.getName())) {
                        list.add(m);
                    }
                }
                if (list.isEmpty())
                    logW(utils.getTAG(), "find any method, but list is empty! class: " + c + " member: " + name);
                return list;
            }
        });
    }

    private ActionTool findMethod(String name, IMethodTool iMethodTool, Class<?>... clzzs) {
        if (utils.labelClasses.isEmpty()) {
            logW(utils.getTAG(), "the class list is empty! cant find method: " + name);
            return utils.getActionTool();
        }
        Object label = utils.getLabel();
        MemberData data = utils.labelClasses.get(label);
        if (data == null) {
            logW(utils.getTAG(), "memberData is null, cant find: " + name + " label: " + label);
            utils.members.put(label, data);
            return utils.getActionTool();
        }
        Class<?> c = data.mClass;
        if (c == null) {
            logW(utils.getTAG(), "class is null! cant find: " + name + " label: " + label);
            // utils.methods.put(index, new ArrayList<>());
            utils.members.put(label, data);
            return utils.getActionTool();
        }
        ArrayList<Member> members = iMethodTool.doFindMethod(c, name, clzzs);
        findMember = members;
        if (data.stateMap.get(members) == null) {
            data.memberMap.put(members);
            data.stateMap.put(members, StateEnum.NONE);
        }
        // data.isHooked = false;
        // data.mConstructor = null;
        utils.members.put(label, data);
        return utils.getActionTool();
    }

    //--------------------构造函数---------------------

    /**
     * 按标签类获取指定构造函数。
     */
    public ActionTool getConstructor(Object... objs) {
        Class<?>[] clist = objsToClist(objs);
        if (clist == null) return utils.getActionTool();
        return findConstructor(new IConstructorTool() {
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
        }, clist);
    }

    /**
     * 按标签类获取全部构造函数。
     */
    public ActionTool getAnyConstructor() {
        return findConstructor(new IConstructorTool() {
            @Override
            public ArrayList<Member> doFindConstructor(Class<?> c, Class<?>... classes) {
                return new ArrayList<>(Arrays.asList(c.getDeclaredConstructors()));
            }
        });
    }

    private ActionTool findConstructor(IConstructorTool iConstructorTool, Class<?>... classes) {
        if (utils.labelClasses.isEmpty()) {
            logW(utils.getTAG(), "The class list is empty!");
            return utils.getActionTool();
        }
        Object label = utils.getLabel();
        // utils.constructors.add(utils.classes.get(0).getConstructor(obj));
        MemberData data = utils.labelClasses.get(label);
        if (data == null) {
            logW(utils.getTAG(), "memberData is null, label: " + label);
            utils.members.put(label, null);
            return utils.getActionTool();
        }
        Class<?> c = data.mClass;
        if (c == null) {
            logW(utils.getTAG(), "class is null! label: " + label);
            utils.members.put(label, data);
            // utils.constructors.put(new Constructor[]{});
            return utils.getActionTool();
        }
        ArrayList<Member> members = iConstructorTool.doFindConstructor(c, classes);
        findMember = members;
        if (data.stateMap.get(members) == null) {
            data.memberMap.put(members);
            data.stateMap.put(members, StateEnum.NONE);
        }
        // data.mConstructor = members;
        // data.mMethod = null;
        // data.isHooked = false;
        utils.members.put(label, data);
        return utils.getActionTool();
    }

    private Class<?>[] objsToClist(Object... objs) {
        ArrayList<Class<?>> classes = new ArrayList<>();
        for (Object o : objs) {
            if (o instanceof Class<?> c) {
                classes.add(c);
            } else if (o instanceof String s) {
                Class<?> ct = findClass(s);
                if (ct == null) {
                    logW(utils.getTAG(), "this string to class is null: " + s);
                    return null;
                }
                classes.add(ct);
            } else {
                logW(utils.getTAG(), "unknown type: " + o);
                return null;
            }
        }
        return classes.toArray(new Class<?>[classes.size()]);
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
    public ClassTool findClass(Object label, String className) {
        return utils.getClassTool().findClass(label, className);
    }

    public ClassTool findClass(Object label, String className, ClassLoader classLoader) {
        return utils.getClassTool().findClass(label, className, classLoader);
    }

    // 优化调用，只提供基本用法，详细用法请获取工具类对象
    public MethodTool hook(IAction iAction) {
        return utils.getActionTool().hook(iAction);
    }

    public MethodTool hook(int methodIndex, IAction iAction) {
        return utils.getActionTool().hook(methodIndex, iAction);
    }

    private interface IMethodTool {
        ArrayList<Member> doFindMethod(Class<?> c, String name, Class<?>... classes);
    }

    private interface IConstructorTool {
        ArrayList<Member> doFindConstructor(Class<?> c, Class<?>... classes);
    }
}
