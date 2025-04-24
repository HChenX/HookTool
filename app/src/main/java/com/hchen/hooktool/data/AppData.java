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

 * Copyright (C) 2023-2025 HChenX
 */
package com.hchen.hooktool.data;

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
    public int user = 0;
    public int uid = -1;
    public Bitmap icon;
    public String label;
    public String packageName;
    public String versionName;
    public String versionCode;
    public boolean isSystemApp;
    public boolean isEnabled;

    public AppData() {
    }

    private AppData(Parcel in) {
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

    @NonNull
    public Parcel marshall() {
        Parcel parcel = Parcel.obtain();
        writeToParcel(parcel, 0);
        return parcel;
    }

    @NonNull
    public static AppData unmarshall(@NonNull Parcel parcel) {
        parcel.setDataPosition(0);
        return new AppData(parcel);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AppData appData)) return false;
        return user == appData.user &&
            uid == appData.uid &&
            isSystemApp == appData.isSystemApp &&
            isEnabled == appData.isEnabled &&
            Objects.equals(icon, appData.icon) &&
            Objects.equals(label, appData.label) &&
            Objects.equals(packageName, appData.packageName) &&
            Objects.equals(versionName, appData.versionName) &&
            Objects.equals(versionCode, appData.versionCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, uid, icon, label, packageName, versionName, versionCode, isSystemApp, isEnabled);
    }

    @Override
    @NonNull
    public String toString() {
        return "AppData{" +
            "user=" + user +
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
}
