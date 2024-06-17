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

import static com.hchen.hooktool.log.XposedLog.logW;

import androidx.annotation.NonNull;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.data.MemberData;
import com.hchen.hooktool.data.StateEnum;
import com.hchen.hooktool.utils.ConvertHelper;
import com.hchen.hooktool.utils.DataUtils;

import java.lang.reflect.Member;
import java.util.ArrayList;

/**
 * 方法类
 */
public class MethodTool extends ConvertHelper {
    private ArrayList<Member> findMember = null;

    public MethodTool(DataUtils utils) {
        super(utils);
        clear();
    }

    public MethodTool to(@NonNull Object label) {
        utils.getClassTool().to(label);
        return utils.getMethodTool();
    }

    /**
     * 获取本次查找到的方法，下次查找方法将会覆盖本次。<br/>
     * 可能是构造函数，也可能是方法。
     */
    public ArrayList<Member> getFindMember() {
        return findMember;
    }

    /**
     * 获取标签类内的指定方法。
     */
    public ActionTool getMethod(String name, Object... objs) {
        Class<?>[] clist = objectArrayToClassArray(objs);
        if (clist == null) return utils.getActionTool();
        return findMethod(name, new IMethodTool() {
            @Override
            public ArrayList<Member> doFindMethod(Class<?> c, String name, Class<?>... classes) {
                ArrayList<Member> arrayList = new ArrayList<>();
                arrayList.add(utils.getExpandTool().findMethod(c, name, (Object[]) classes));
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
                ArrayList<Member> list = new ArrayList<>(utils.getExpandTool().findAnyMethod(c, name));
                if (list.isEmpty())
                    logW(utils.getTAG(), "find any method, but list is empty! class: [" + c + "], member: " + name);
                return list;
            }
        });
    }

    private ActionTool findMethod(String name, IMethodTool iMethodTool, Class<?>... clzzs) {
        findMember = new ArrayList<>();
        if (utils.members.isEmpty()) {
            logW(utils.getTAG(), "the class list is empty! can't find method: " + name);
            return utils.getActionTool();
        }
        Object label = utils.getLabel();
        MemberData data = utils.members.get(label);
        if (data == null) {
            logW(utils.getTAG(), "memberData is null, can't find: [" + name + "], label: " + label);
            return utils.getActionTool();
        }
        Class<?> c = data.mClass;
        if (c == null) {
            logW(utils.getTAG(), "class is null! can't find: [" + name + "], label: " + label);
            return utils.getActionTool();
        }
        findMember = iMethodTool.doFindMethod(c, name, clzzs);
        if (data.stateMap.get(findMember) == null) {
            data.memberMap.put(findMember);
            data.stateMap.put(findMember, StateEnum.NONE);
        }
        utils.members.put(label, data);
        return utils.getActionTool();
    }

    //--------------------构造函数---------------------

    /**
     * 按标签类获取指定构造函数。
     */
    public ActionTool getConstructor(Object... objs) {
        Class<?>[] clist = objectArrayToClassArray(objs);
        if (clist == null) return utils.getActionTool();
        return findConstructor(new IConstructorTool() {
            @Override
            public ArrayList<Member> doFindConstructor(Class<?> c, Class<?>... classes) {
                ArrayList<Member> members = new ArrayList<>();
                members.add(utils.getExpandTool().findConstructor(c, (Object[]) classes));
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
                return new ArrayList<>(utils.getExpandTool().findAnyConstructor(c));
            }
        });
    }

    private ActionTool findConstructor(IConstructorTool iConstructorTool, Class<?>... classes) {
        findMember = new ArrayList<>();
        if (utils.members.isEmpty()) {
            logW(utils.getTAG(), "the class list is empty!");
            return utils.getActionTool();
        }
        Object label = utils.getLabel();
        MemberData data = utils.members.get(label);
        if (data == null) {
            logW(utils.getTAG(), "memberData is null, label: " + label);
            return utils.getActionTool();
        }
        Class<?> c = data.mClass;
        if (c == null) {
            logW(utils.getTAG(), "class is null! label: " + label);
            return utils.getActionTool();
        }
        findMember = iConstructorTool.doFindConstructor(c, classes);
        if (data.stateMap.get(findMember) == null) {
            data.memberMap.put(findMember);
            data.stateMap.put(findMember, StateEnum.NONE);
        }
        utils.members.put(label, data);
        return utils.getActionTool();
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

    // 更棒的无缝衔接
    public ClassTool add(@NonNull Object label, Class<?> clazz) {
        return utils.getClassTool().add(label, clazz);
    }

    public ClassTool findClass(@NonNull Object label, String className) {
        return utils.getClassTool().findClass(label, className);
    }

    public ClassTool findClass(@NonNull Object label, String className, ClassLoader classLoader) {
        return utils.getClassTool().findClass(label, className, classLoader);
    }

    // 优化调用，只提供基本用法，详细用法请获取工具类对象
    public MethodTool hook(IAction iAction) {
        return utils.getActionTool().hook(iAction);
    }

    public MethodTool returnResult(final Object result) {
        return utils.getActionTool().returnResult(result);
    }

    public MethodTool doNothing() {
        return utils.getActionTool().doNothing();
    }

    private void clear() {
        if (!utils.members.isEmpty()) utils.members.clear();
    }

    private interface IMethodTool {
        ArrayList<Member> doFindMethod(Class<?> c, String name, Class<?>... classes);
    }

    private interface IConstructorTool {
        ArrayList<Member> doFindConstructor(Class<?> c, Class<?>... classes);
    }
}
