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
package com.hchen.hooktool.util.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.log.AndroidLog;

import java.io.ByteArrayOutputStream;

/**
 * Bitmap 图像处理工具集。
 *
 * @author 焕晨HChen
 */
public final class BitmapTool {
    private static final String TAG = "BitmapTool";
    /**
     * 圆角裁剪时用于绘制圆角矩形遮罩的颜色，最终由原图经 {@link PorterDuff.Mode#SRC_IN} 覆盖，取值不影响结果。
     */
    private static final int ROUND_RECT_COLOR = 0xff424242;

    private BitmapTool() {
    }

    /**
     * 将 {@link Drawable} 对象转换为 {@link Bitmap}。
     * <p>
     * 传入 {@link BitmapDrawable} 时直接返回其内部 Bitmap，不会拷贝像素。
     *
     * @param drawable Drawable 对象
     * @return 转换得到的 {@link Bitmap} 对象
     */
    @NonNull
    public static Bitmap drawableToBitmap(@NonNull Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap != null) {
                return bitmap;
            }
        }
        return drawableToBitmap(drawable, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    }

    /**
     * 将 {@link Drawable} 对象转换为指定尺寸的 {@link Bitmap}。
     * <p>
     * 宽高无效时退化为 1x1 像素，像素格式按 Drawable 的不透明度选取。
     *
     * @param drawable Drawable 对象
     * @param width    目标宽度（单位：px），不大于 0 时取 Drawable 固有宽度
     * @param height   目标高度（单位：px），不大于 0 时取 Drawable 固有高度
     * @return 转换得到的 {@link Bitmap} 对象
     */
    @NonNull
    public static Bitmap drawableToBitmap(@NonNull Drawable drawable, int width, int height) {
        if (width <= 0 || height <= 0) {
            width = drawable.getIntrinsicWidth();
            height = drawable.getIntrinsicHeight();
        }

        if (width <= 0) width = 1;
        if (height <= 0) height = 1;

        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * 对指定 {@link Bitmap} 进行圆角裁剪，生成四角带有圆弧效果的新 Bitmap。
     *
     * @param bitmap 待处理的原始 Bitmap
     * @param radius 圆角半径（单位：px）
     * @return 带有圆角效果的新 {@link Bitmap} 实例
     */
    @NonNull
    public static Bitmap getRoundedCornerBitmap(@NonNull Bitmap bitmap, float radius) {
        // 负数半径会导致 drawRoundRect 抛 IllegalArgumentException，钳制为非负值
        radius = Math.max(0, radius);
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(ROUND_RECT_COLOR);
        canvas.drawRoundRect(rectF, radius, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    /**
     * 按照指定的目标宽高对 {@link Bitmap} 进行缩放。
     *
     * @param bitmap    待缩放的原始 Bitmap
     * @param newWidth  缩放后的目标宽度（单位：px），必须大于 0
     * @param newHeight 缩放后的目标高度（单位：px），必须大于 0
     * @return 缩放后的新 {@link Bitmap} 实例
     * @throws IllegalArgumentException 当 {@code newWidth} 或 {@code newHeight} 不大于 0 时抛出
     */
    @NonNull
    public static Bitmap scaleBitmap(@NonNull Bitmap bitmap, int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) {
            throw new IllegalArgumentException(
                "newWidth and newHeight must be greater than 0, but got: " + newWidth + " x " + newHeight
            );
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * 将 {@link Bitmap} 以 PNG 格式压缩为 {@code byte[]}。
     * <p>
     * 编码失败时返回长度为 0 的数组，不抛异常。
     *
     * @param bitmap 待压缩的 Bitmap 对象
     * @return PNG 格式的字节数组，编码失败时为长度为 0 的数组
     */
    @NonNull
    public static byte[] bitmapToBytes(@NonNull Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        boolean success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        if (!success) {
            AndroidLog.logW(TAG, "Bitmap compress failed, returning empty byte array.");
            return new byte[0];
        }
        return stream.toByteArray();
    }

    /**
     * 将 {@code byte[]} 解码还原为 {@link Bitmap} 对象。
     *
     * @param bytes 包含 Bitmap 编码数据的字节数组
     * @return 解码成功返回 {@link Bitmap} 对象；若数组为空或解码失败则返回 {@code null}
     */
    @Nullable
    public static Bitmap bytesToBitmap(@NonNull byte[] bytes) {
        if (bytes.length == 0) {
            return null;
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap == null) {
            AndroidLog.logW(TAG, "Failed to decode bitmap from byte array, returning null.");
        }
        return bitmap;
    }
}