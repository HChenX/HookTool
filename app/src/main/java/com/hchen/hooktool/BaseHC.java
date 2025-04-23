package com.hchen.hooktool;

import android.content.Context;

import com.hchen.hooktool.core.CoreTool;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public abstract class BaseHC extends CoreTool {
    public String TAG = getClass().getSimpleName();
    public static ClassLoader classLoader;
    public static XC_LoadPackage.LoadPackageParam loadPackageParam;
    public static ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

    protected boolean isEnabled() {
        return true;
    }

    protected abstract void init();

    protected void init(ClassLoader classLoader) {
    }

    protected void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
    }

    protected void onApplication(Context context) {
    }

    protected void onThrowable(Throwable e) {
    }

    final public void onLoadPackage() {
        try {
            if (!isEnabled()) return;
            init();
        } catch (Throwable e) {
            onThrowable(e);
        }
    }

    final public void onClassLoader(ClassLoader classLoader) {
        try {
            if (!isEnabled()) return;
            init(classLoader);
        } catch (Throwable e) {
            onThrowable(e);
        }
    }
}
