/*
 * This file is part of HookTool.
 *
 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * HookTool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with HookTool. If not, see <https://www.gnu.org/licenses/lgpl-2.1>.
 *
 * Copyright (C) 2024–2026 HChenX
 */
package com.hchen.hooktool.data;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * 已安装应用程序的元数据容器。
 * <p>
 * 该类聚合了 Android 设备上某个已安装应用的核心属性，包括但不限于
 * {@link PackageInfo}、{@link ApplicationInfo}、多用户 ID、Linux UID、
 * 应用图标、显示名称、包名、版本信息，以及系统应用标志和启用状态标志。
 * <p>
 * 本类实现了 {@link Parcelable} 协议，可借助 {@link Parcel} 在不同 Android
 * 进程间高效地进行序列化与反序列化传输。
 *
 * @author 焕晨HChen
 */
public class AppData implements Parcelable {
    /**
     * 与该应用关联的 {@link PackageInfo} 对象，其中包含完整的包级别元数据信息。
     */
    public PackageInfo packageInfo;
    /**
     * 与该应用关联的 {@link ApplicationInfo} 对象，记录应用级别的配置与属性。
     */
    public ApplicationInfo applicationInfo;
    /**
     * 多用户环境下的用户标识符。默认值 {@code -1} 表示未显式指定用户，通常对应设备的主用户。
     */
    public int user = -1;
    /**
     * 该应用在 Linux 层面的用户标识符（UID）。默认值 {@code -1} 表示尚未分配。
     */
    public int uid = -1;
    /**
     * 应用的图标位图。在图标加载失败或不可用时可能为 {@code null}。
     */
    public Bitmap icon;
    /**
     * 应用面向用户展示的名称标签。
     */
    public String label;
    /**
     * 应用的唯一包名（例如 {@code "com.android.settings"}）。
     */
    public String packageName;
    /**
     * 应用的版本名称字符串（例如 {@code "1.2.3"}）。仅当通过 {@link PackageInfo} 获取数据时才会被填充。
     */
    public String versionName; // 仅 PackageInfo 下填充数据
    /**
     * 应用的版本号字符串。仅当通过 {@link PackageInfo} 获取数据时才会被填充。
     */
    public String versionCode; // 仅 PackageInfo 下填充数据
    /**
     * 指示该应用是否为系统预装应用。{@code true} 表示为系统应用。
     */
    public boolean isSystemApp;
    /**
     * 指示该应用当前是否处于启用状态。{@code true} 表示已启用，{@code false} 表示已被停用。
     */
    public boolean isEnabled;

    /**
     * 构造一个所有字段均保持默认值的空 {@link AppData} 实例。
     */
    public AppData() {
    }

    @NonNull
    @Override
    public String toString() {
        return "AppData{" +
            "packageInfo=" + packageInfo +
            ", applicationInfo=" + applicationInfo +
            ", user=" + user +
            ", uid=" + uid +
            ", icon=" + icon +
            ", label='" + label + '\'' +
            ", packageName='" + packageName + '\'' +
            ", versionName='" + versionName + '\'' +
            ", versionCode='" + versionCode + '\'' +
            ", isSystemApp=" + isSystemApp +
            ", isEnabled=" + isEnabled +
            '}';
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof AppData appData)) return false;

        return user == appData.user &&
            uid == appData.uid &&
            isSystemApp == appData.isSystemApp &&
            isEnabled == appData.isEnabled &&
            Objects.equals(packageName, appData.packageName) &&
            Objects.equals(versionName, appData.versionName) &&
            Objects.equals(versionCode, appData.versionCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            user,
            uid,
            packageName,
            versionName,
            versionCode,
            isSystemApp,
            isEnabled
        );
    }

    /**
     * 从 {@link Parcel} 反序列化构造 {@link AppData} 实例的私有构造方法。
     * <p>
     * 字段的读取顺序必须与 {@link #writeToParcel(Parcel, int)} 中的写入顺序严格一致。
     *
     * @param in 携带序列化数据的源 {@link Parcel} 对象
     */
    private AppData(@NonNull Parcel in) {
        packageInfo = in.readParcelable(PackageInfo.class.getClassLoader());
        applicationInfo = in.readParcelable(ApplicationInfo.class.getClassLoader());
        user = in.readInt();
        uid = in.readInt();
        icon = in.readParcelable(Bitmap.class.getClassLoader());
        label = in.readString();
        packageName = in.readString();
        versionName = in.readString();
        versionCode = in.readString();
        isSystemApp = in.readByte() != 0;
        isEnabled = in.readByte() != 0;
    }

    /**
     * 将本实例的全部字段按固定顺序序列化写入指定的 {@link Parcel}。
     *
     * @param dest  目标 {@link Parcel} 对象，序列化数据将写入此对象中
     * @param flags 附加控制标志，用于指示是否需要写入特殊对象引用（如文件描述符），通常传入 {@code 0}
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(packageInfo, flags);
        dest.writeParcelable(applicationInfo, flags);
        dest.writeInt(user);
        dest.writeInt(uid);
        dest.writeParcelable(icon, flags);
        dest.writeString(label);
        dest.writeString(packageName);
        dest.writeString(versionName);
        dest.writeString(versionCode);
        dest.writeByte((byte) (isSystemApp ? 1 : 0));
        dest.writeByte((byte) (isEnabled ? 1 : 0));
    }

    /**
     * 返回此 Parcelable 对象的内容描述标志位。
     *
     * @return 固定返回 {@code 0}，表示该对象不包含需要特殊处理的文件描述符
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Parcelable 反序列化工厂实例，负责从 {@link Parcel} 中重建 {@link AppData} 对象，
     * 以及创建指定容量的 {@code AppData} 数组以供框架内部使用。
     */
    public static final Creator<AppData> CREATOR = new Creator<AppData>() {
        @Override
        public AppData createFromParcel(Parcel in) {
            return new AppData(in);
        }

        @Override
        public AppData[] newArray(int size) {
            return new AppData[size];
        }
    };
}
