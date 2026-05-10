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
 * 应用数据
 *
 * @author 焕晨HChen
 */
public class AppData implements Parcelable {
    public PackageInfo packageInfo;
    public ApplicationInfo applicationInfo;
    public int user = -1;
    public int uid = -1;
    public Bitmap icon;
    public String label;
    public String packageName;
    public String versionName; // 仅 PackageInfo 下填充数据
    public String versionCode; // 仅 PackageInfo 下填充数据
    public boolean isSystemApp;
    public boolean isEnabled;

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

    @Override
    public int describeContents() {
        return 0;
    }

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
