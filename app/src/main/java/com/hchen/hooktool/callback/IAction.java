package com.hchen.hooktool.callback;

import com.hchen.hooktool.hc.HCHook;

import de.robv.android.xposed.XC_MethodHook;

public interface IAction {
    void action(XC_MethodHook.MethodHookParam param, HCHook hcHook);
}
