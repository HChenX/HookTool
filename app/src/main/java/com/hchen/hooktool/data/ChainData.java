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
package com.hchen.hooktool.data;

import com.hchen.hooktool.callback.IAction;

import java.lang.reflect.Member;
import java.util.ArrayList;

/**
 * 链式调用数据
 */
public class ChainData {
    public ArrayList<Member> members = new ArrayList<>(); /*目标成员组*/
    public IAction iAction; /*hook 动作*/
    public StateEnum stateEnum; /*状态*/
    public String clazz; /* 类名 */

    public ChainData(String clazz, String name, String type,
                     ArrayList<Member> members, IAction iAction, StateEnum stateEnum) {
        this.clazz = clazz;
        this.mName = name;
        this.mType = type;
        this.members = members;
        this.iAction = iAction;
        this.stateEnum = stateEnum;
    }

    //################################

    public static final String TYPE_METHOD = "METHOD";
    public static final String TYPE_ANY_METHOD = "ANY_METHOD";
    public static final String TYPE_CONSTRUCTOR = "CONSTRUCTOR";
    public static final String TYPE_ANY_CONSTRUCTOR = "ANY_CONSTRUCTOR";

    public String mName; /*方法名*/
    public String mType; /*类型*/
    public Object[] mParams; /*参数*/

    public ChainData(String name, Object... params) {
        mName = name;
        mType = TYPE_METHOD;
        mParams = params;
    }

    public ChainData(String name) {
        mName = name;
        mType = TYPE_ANY_METHOD;
    }

    public ChainData(Object... params) {
        mName = "";
        mType = TYPE_CONSTRUCTOR;
        mParams = params;
    }

    public ChainData() {
        mName = "";
        mType = TYPE_ANY_CONSTRUCTOR;
    }
}
