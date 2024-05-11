package com.hchen.hooktool.data;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;

public class MemberData {
    public Class<?> mClass;
    public ArrayList<Member> mMethod;
    public ArrayList<Member> mConstructor;
    public Field mField;
    public boolean isHooked;
    public boolean allAction;

    public boolean after;
    public boolean before;

    public MemberData(Class<?> mClass) {
        this.mClass = mClass;
    }
}
