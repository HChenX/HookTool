package com.hchen.hooktool;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import java.io.FileNotFoundException;
import java.util.Objects;

import io.github.libxposed.api.XposedInterfaceWrapper;

public class ModuleData {
    private static XposedInterfaceWrapper wrapper;
    private static ClassLoader classLoader;

    static void setWrapper(XposedInterfaceWrapper wrapper) {
        ModuleData.wrapper = wrapper;
    }

    public static void setClassLoader(ClassLoader classLoader) {
        ModuleData.classLoader = classLoader;
    }

    @NonNull
    public static XposedInterfaceWrapper getWrapper() {
        Objects.requireNonNull(wrapper);
        return wrapper;
    }

    @NonNull
    public static SharedPreferences getRemotePreferences(@NonNull String name) {
        return getWrapper().getRemotePreferences(name);
    }

    @NonNull
    public static ApplicationInfo getModuleApplicationInfo() {
        return getWrapper().getModuleApplicationInfo();
    }

    @NonNull
    public static String[] listRemoteFiles() {
        return getWrapper().listRemoteFiles();
    }

    @NonNull
    public static ParcelFileDescriptor openRemoteFile(@NonNull String name) throws FileNotFoundException {
        return getWrapper().openRemoteFile(name);
    }

    @NonNull
    public static ClassLoader getClassLoader() {
        Objects.requireNonNull(classLoader);
        return classLoader;
    }

    @NonNull
    public static ClassLoader getSystemClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }
}
