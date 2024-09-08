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

import com.hchen.hooktool.hook.IAction;

import java.lang.reflect.Member;
import java.util.ArrayList;

/**
 * 链式调用数据
 *
 * @author 焕晨HChen
 */
public class ChainData {
    public ArrayList<ChainData> memberWithState = new ArrayList<>(); /* 目标成员组 */
    public Member member; /* 查找到的成员 */
    public IAction iAction; /* hook 动作 */
    public HookState hookState; /* 状态 */
    public String UUID = "UNKNOWN"; /* 唯一标识符 */

    // 数据存储
    public ChainData(ArrayList<ChainData> memberWithState, IAction iAction, String uuid) {
        this.memberWithState = memberWithState;
        this.iAction = iAction;
        this.UUID = uuid;
    }

    // memberWithState 内数据
    public ChainData(Member member) {
        this.member = member;
        this.hookState = HookState.NONE;
    }

    //################################

    public static final String TYPE_METHOD = "METHOD";
    public static final String TYPE_ANY_METHOD = "ANY_METHOD";
    public static final String TYPE_CONSTRUCTOR = "CONSTRUCTOR";
    public static final String TYPE_ANY_CONSTRUCTOR = "ANY_CONSTRUCTOR";

    public String mName; /* 方法名 */
    public String mType; /* 类型 */
    public Object[] mParams; /* 参数 */

    // 类信息
    public ChainData(String name, Object... params) {
        mName = name;
        mType = TYPE_METHOD;
        mParams = params;
    }

    // 类信息
    public ChainData(String name) {
        mName = name;
        mType = TYPE_ANY_METHOD;
    }

    // 构造函数信息
    public ChainData(Object... params) {
        mName = "";
        mType = TYPE_CONSTRUCTOR;
        mParams = params;
    }

    // 构造函数信息
    public ChainData() {
        mName = "";
        mType = TYPE_ANY_CONSTRUCTOR;
    }
}
