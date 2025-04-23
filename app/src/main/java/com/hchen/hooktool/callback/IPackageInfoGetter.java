package com.hchen.hooktool.callback;

import android.content.pm.PackageManager;
import android.os.Parcelable;

public interface IPackageInfoGetter {
    Parcelable[] packageInfoGetter(PackageManager pm) throws PackageManager.NameNotFoundException;
}
