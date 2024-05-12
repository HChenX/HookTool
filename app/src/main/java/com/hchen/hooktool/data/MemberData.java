package com.hchen.hooktool.data;

import com.hchen.hooktool.utils.MapUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.HashMap;

public class MemberData {
    public Class<?> mClass;
    // public ArrayList<Member> mMethod;
    // public ArrayList<Member> mConstructor;
    public Field mField;
    public boolean isHooked;
    public int count = 0;
    // public boolean allAction;
    // public boolean after;
    // public boolean before;
    public MapUtils<ArrayList<Member>> memberMap = new MapUtils<>();
    public HashMap<ArrayList<Member>, StateEnum> stateMap = new HashMap<>();

    public MemberData(Class<?> mClass) {
        this.mClass = mClass;
    }
}
