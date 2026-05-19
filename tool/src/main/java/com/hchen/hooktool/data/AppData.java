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
 * 应用数据类。封装应用的基本信息，包括包信息、应用信息、用户 ID、UID、图标、标签、包名、版本号等。
 * 实现 Parcelable 接口以支持跨进程传输。
 *
 * @author 焕晨HChen
 */
public class AppData implements Parcelable {
    /**
     * 包信息。
     */
    public PackageInfo packageInfo;
    /**
     * 应用信息。
     */
    public ApplicationInfo applicationInfo;
    /**
     * 用户 ID，-1 表示默认用户。
     */
    public int user = -1;
    /**
     * 应用的 UID，-1 表示未设置。
     */
    public int uid = -1;
    /**
     * 应用图标。
     */
    public Bitmap icon;
    /**
     * 应用标签名称。
     */
    public String label;
    /**
     * 包名。
     */
    public String packageName;
    /**
     * 版本名称，仅 PackageInfo 下填充数据。
     */
    public String versionName; // 仅 PackageInfo 下填充数据
    /**
     * 版本号，仅 PackageInfo 下填充数据。
     */
    public String versionCode; // 仅 PackageInfo 下填充数据
    /**
     * 是否为系统应用。
     */
    public boolean isSystemApp;
    /**
     * 是否已启用。
     */
    public boolean isEnabled;

    /**
     * 创建空的应用数据实例。
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
     * 将应用数据写入 Parcel 以进行序列化传输。
     *
     * @param dest  目标 Parcel 对象
     * @param flags 附加标志位
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
     * 获取 Parcelable 的内容描述标志。
     *
     * @return 始终返回 0，表示不包含特殊对象
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Parcelable 创建器。
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
