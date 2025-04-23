package com.hchen.hooktool.callback;

import android.content.Context;

import androidx.annotation.Nullable;

public interface IContextGetter {
    void onContext(@Nullable Context context);
}
