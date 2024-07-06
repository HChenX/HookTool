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

/**
 * 链式调用数据
 */
public class ChainData {
    public Member member; /*目标成员*/
    public IAction iAction; /*hook 动作*/
    public StateEnum stateEnum; /*状态*/

    public ChainData(Member member, IAction iAction, StateEnum stateEnum) {
        this.member = member;
        this.iAction = iAction;
        this.stateEnum = stateEnum;
    }

    //################################

    public static final String TYPE_METHOD = "METHOD";
    public static final String TYPE_CONSTRUCTOR = "CONSTRUCTOR";

    public String mName; /*方法名*/
    public String mType; /*类型*/
    public Object[] mParams; /*参数*/

    public ChainData(String name, Object... params) {
        mName = name;
        mType = TYPE_METHOD;
        mParams = params;
    }

    public ChainData(Object... params) {
        mName = "";
        mType = TYPE_CONSTRUCTOR;
        mParams = params;
    }
}
