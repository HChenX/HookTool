package com.hchen.hooktool.data;

import com.hchen.hooktool.utils.MapUtils;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.HashMap;

public class MemberData {
    @Nullable
    public Class<?> mClass; // 查找到的类
    // public ArrayList<Member> mMethod;
    // public ArrayList<Member> mConstructor;
    public Field mField;
    public int count = 0; // 计数
    // public boolean isHooked;
    // public boolean allAction;
    // public boolean after;
    // public boolean before;
    /* member 存储与读取 */
    public MapUtils<ArrayList<Member>> memberMap = new MapUtils<>();
    /* member 状态记录 */
    public HashMap<ArrayList<Member>, StateEnum> stateMap = new HashMap<>();

    public MemberData(@Nullable Class<?> mClass) {
        this.mClass = mClass;
    }
}
