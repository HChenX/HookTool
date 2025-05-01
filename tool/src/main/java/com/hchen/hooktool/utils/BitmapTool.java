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
package com.hchen.hooktool.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;

/**
 * Bitmap 工具
 *
 * @author 焕晨HChen
 */
public class BitmapTool {
    private BitmapTool() {
    }

    /**
     * 将 Drawable 对象转换为 Bitmap 对象，使用 Drawable 的固有宽度和高度
     *
     * @param drawable 要转换的 Drawable 对象
     * @return 转换后的 Bitmap 对象
     */
    @NonNull
    public static Bitmap drawableToBitmap(@NonNull Drawable drawable) {
        return drawableToBitmap(drawable, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    }

    /**
     * 将 Drawable 对象转换为 Bitmap 对象，可以指定宽度和高度
     * <p>
     * 如果指定的宽度或高度小于等于 0，则使用 Drawable 的固有宽度和高度
     *
     * @param drawable 要转换的 Drawable 对象
     * @param width    指定的宽度
     * @param height   指定的高度
     * @return 转换后的 Bitmap 对象
     */
    @NonNull
    public static Bitmap drawableToBitmap(@NonNull Drawable drawable, int width, int height) {
        if (width <= 0 || height <= 0) {
            width = drawable.getIntrinsicWidth();
            height = drawable.getIntrinsicHeight();
        }

        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * 将 Bitmap 对象转换为字节数组，使用 PNG 格式进行压缩
     *
     * @param bitmap 要转换的 Bitmap 对象
     * @return 转换后的字节数组
     */
    @NonNull
    public static byte[] bitmapToBytes(@NonNull Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    /**
     * 将字节数组转换为 Bitmap 对象如果字节数组为空，则返回 null
     *
     * @param bytes 要转换的字节数组
     * @return 转换后的 Bitmap 对象，如果字节数组为空则返回 null
     */
    @Nullable
    public static Bitmap bytesToBimap(@NonNull byte[] bytes) {
        if (bytes.length != 0) {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }
}
