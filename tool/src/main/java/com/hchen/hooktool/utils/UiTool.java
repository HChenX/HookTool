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
package com.hchen.hooktool.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.log.AndroidLog;

/**
 * Android UI 界面相关工具类。
 *
 * @author 焕晨HChen
 */
public final class UiTool {
    private static final String TAG = "UiTool";
    private static final ThreadLocal<Paint> MEASURE_PAINT = ThreadLocal.withInitial(Paint::new);

    private UiTool() {
    }

    // ----------------------- 单位转换 -------------------------

    /**
     * 将像素值（px）转换为密度无关像素值（dp）。
     * <p>
     * 基于 {@link #px2dpFloat} 四舍五入取整。
     *
     * @param context 上下文，用于获取屏幕密度
     * @param pxValue 待转换的像素值
     * @return 对应的 dp 值
     */
    public static int px2dp(@NonNull Context context, float pxValue) {
        return Math.round(px2dpFloat(context, pxValue));
    }

    /**
     * 将像素值（px）转换为字体缩放无关像素值（sp）。
     * <p>
     * 基于 {@link #px2spFloat} 四舍五入取整。
     *
     * @param context 上下文，用于获取字体缩放密度
     * @param pxValue 待转换的像素值
     * @return 对应的 sp 值
     */
    public static int px2sp(@NonNull Context context, float pxValue) {
        return Math.round(px2spFloat(context, pxValue));
    }

    /**
     * 将密度无关像素值（dp）转换为像素值（px）。
     * <p>
     * 基于 {@link #dp2pxFloat} 四舍五入取整。
     *
     * @param context 上下文，用于获取屏幕密度
     * @param dpValue 待转换的 dp 值
     * @return 对应的像素值
     */
    public static int dp2px(@NonNull Context context, float dpValue) {
        return Math.round(dp2pxFloat(context, dpValue));
    }

    /**
     * 将字体缩放无关像素值（sp）转换为像素值（px）。
     * <p>
     * 基于 {@link #sp2pxFloat} 四舍五入取整。
     *
     * @param context 上下文，用于获取字体缩放密度
     * @param spValue 待转换的 sp 值
     * @return 对应的像素值
     */
    public static int sp2px(@NonNull Context context, float spValue) {
        return Math.round(sp2pxFloat(context, spValue));
    }

    /**
     * 将密度无关像素值（dp）精确转换为像素值（px，float）。
     *
     * @param context 上下文
     * @param dpValue 待转换的 dp 值
     * @return 对应的精确像素值
     */
    public static float dp2pxFloat(@NonNull Context context, float dpValue) {
        return applyDimension(context, TypedValue.COMPLEX_UNIT_DIP, dpValue);
    }

    /**
     * 将字体缩放无关像素值（sp）精确转换为像素值（px，float）。
     *
     * @param context 上下文
     * @param spValue 待转换的 sp 值
     * @return 对应的精确像素值
     */
    public static float sp2pxFloat(@NonNull Context context, float spValue) {
        return applyDimension(context, TypedValue.COMPLEX_UNIT_SP, spValue);
    }

    /**
     * 将像素值（px）精确转换为密度无关像素值（dp，float）。
     *
     * @param context 上下文
     * @param pxValue 待转换的像素值
     * @return 对应的精确 dp 值
     */
    public static float px2dpFloat(@NonNull Context context, float pxValue) {
        return pxValue / context.getResources().getDisplayMetrics().density;
    }

    /**
     * 将像素值（px）精确转换为字体缩放无关像素值（sp，float）。
     *
     * @param context 上下文
     * @param pxValue 待转换的像素值
     * @return 对应的精确 sp 值
     */
    public static float px2spFloat(@NonNull Context context, float pxValue) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float fontScale = context.getResources().getConfiguration().fontScale;
        return pxValue / (dm.density * fontScale);
    }

    // ----------------------- 屏幕信息 -------------------------

    /**
     * 获取当前屏幕的显示密度因子（{@code density}，单位 dp 与 px 的换算系数）。
     *
     * @param context 上下文
     * @return 屏幕密度因子（如 {@code 2.0f} 表示 xxhdpi）
     */
    public static float getDensity(@NonNull Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    /**
     * 获取当前屏幕的字体缩放因子（用户字体大小偏好）。
     *
     * @param context 上下文
     * @return 字体缩放因子（{@code 1.0} 表示标准字体）
     */
    public static float getFontScale(@NonNull Context context) {
        return context.getResources().getConfiguration().fontScale;
    }

    /**
     * 获取当前屏幕的缩放密度（{@code density * fontScale}，即 sp 与 px 的换算系数）。
     *
     * @param context 上下文
     * @return 缩放密度
     */
    public static float getScaledDensity(@NonNull Context context) {
        return context.getResources().getDisplayMetrics().scaledDensity;
    }

    /**
     * 获取当前屏幕的显示密度（单位：dpi）。
     * <p>
     * 若发生异常，返回 {@link DisplayMetrics#DENSITY_DEFAULT}。
     *
     * @param context 上下文
     * @return 屏幕密度值（单位：dpi）
     */
    public static int getScreenDensity(@NonNull Context context) {
        try {
            return context.getResources().getDisplayMetrics().densityDpi;
        } catch (Throwable e) {
            AndroidLog.logD(TAG, "getScreenDensity failed, returning DENSITY_DEFAULT.", e);
            return DisplayMetrics.DENSITY_DEFAULT;
        }
    }

    /**
     * 获取屏幕物理宽度（单位：px），即屏幕最大可用尺寸的宽度。
     *
     * @param context 上下文
     * @return 屏幕物理宽度（px）
     */
    public static int getScreenWidth(@NonNull Context context) {
        return getMaximumScreenSize(context).x;
    }

    /**
     * 获取屏幕物理高度（单位：px），即屏幕最大可用尺寸的高度。
     *
     * @param context 上下文
     * @return 屏幕物理高度（px）
     */
    public static int getScreenHeight(@NonNull Context context) {
        return getMaximumScreenSize(context).y;
    }

    /**
     * 获取屏幕最大可用尺寸（单位：px）。
     *
     * @param context 上下文
     * @return 包含宽度与高度的 {@link Point} 对象
     */
    @NonNull
    public static Point getMaximumScreenSize(@NonNull Context context) {
        return getMaximumScreenSize(getWindowManager(context));
    }

    /**
     * 获取屏幕最大可用尺寸（单位：px），基于传入的 {@link WindowManager}。
     *
     * @param windowManager {@link WindowManager} 实例
     * @return 包含宽度与高度的 {@link Point} 对象
     */
    @NonNull
    public static Point getMaximumScreenSize(@NonNull WindowManager windowManager) {
        try {
            Point point = new Point();
            Rect bounds = windowManager.getMaximumWindowMetrics().getBounds();
            point.x = bounds.width();
            point.y = bounds.height();
            return point;
        } catch (Throwable e) {
            AndroidLog.logD(TAG, "getMaximumScreenSize failed, returning (0, 0).", e);
            return new Point();
        }
    }

    /**
     * 获取当前窗口的尺寸（单位：px）。
     *
     * @param context 上下文
     * @return 包含当前窗口宽度（{@code x}）和高度（{@code y}）的 {@link Point} 对象
     */
    @NonNull
    public static Point getCurrentWindowSize(@NonNull Context context) {
        return getCurrentWindowSize(getWindowManager(context));
    }

    /**
     * 获取当前窗口的尺寸（单位：px），基于传入的 {@link WindowManager}。
     *
     * @param windowManager {@link WindowManager} 实例
     * @return 包含当前窗口宽度（{@code x}）和高度（{@code y}）的 {@link Point} 对象
     */
    @NonNull
    public static Point getCurrentWindowSize(@NonNull WindowManager windowManager) {
        try {
            Point point = new Point();
            Rect bounds = windowManager.getCurrentWindowMetrics().getBounds();
            point.x = bounds.width();
            point.y = bounds.height();
            return point;
        } catch (Throwable e) {
            AndroidLog.logD(TAG, "getCurrentWindowSize failed, returning (0, 0).", e);
            return new Point();
        }
    }

    /**
     * 从 {@link Context} 中获取 {@link WindowManager} 系统服务实例。
     *
     * @param context 上下文
     * @return {@link WindowManager} 实例；系统服务不可用时可能返回 {@code null}
     */
    @Nullable
    public static WindowManager getWindowManager(@NonNull Context context) {
        Object service = context.getSystemService(Context.WINDOW_SERVICE);
        return service instanceof WindowManager ? (WindowManager) service : null;
    }

    /**
     * 获取当前设备的 {@link Display} 对象。
     *
     * @param context 上下文
     * @return 当前窗口关联的 {@link Display} 实例，可能为 {@code null}
     */
    @Nullable
    public static Display getDisplay(@NonNull Context context) {
        return context.getDisplay();
    }

    // ----------------------- 系统栏 -------------------------

    /**
     * 获取状态栏高度（单位：px）。
     * <p>
     * 读取系统资源 {@code status_bar_height}，资源不存在时返回 {@code 0}。
     *
     * @param context 上下文
     * @return 状态栏高度（px）
     */
    public static int getStatusBarHeight(@NonNull Context context) {
        return getSystemBarHeight(context, "status_bar_height");
    }

    /**
     * 获取导航栏高度（单位：px）。
     * <p>
     * 读取系统资源 {@code navigation_bar_height}；手势导航模式下可能仍返回非 0 的资源高度，精确判断请用 {@code WindowInsets}。
     *
     * @param context 上下文
     * @return 导航栏高度（px）
     */
    public static int getNavigationBarHeight(@NonNull Context context) {
        return getSystemBarHeight(context, "navigation_bar_height");
    }

    private static int getSystemBarHeight(@NonNull Context context, @NonNull String resName) {
        Resources resources = context.getResources();
        int resId = resources.getIdentifier(resName, "dimen", "android");
        if (resId > 0) {
            return resources.getDimensionPixelSize(resId);
        }
        return 0;
    }

    // ----------------------- 主题与方向 -------------------------

    /**
     * 判断当前系统是否处于深色模式。
     *
     * @param context 上下文
     * @return 深色模式已开启时返回 {@code true}
     */
    public static boolean isDarkMode(@NonNull Context context) {
        return isDarkMode(context.getResources());
    }

    /**
     * 判断当前系统是否处于深色模式。
     *
     * @param resources {@link Resources} 实例
     * @return 深色模式已开启时返回 {@code true}
     */
    public static boolean isDarkMode(@NonNull Resources resources) {
        return (resources.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
            == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * 判断当前设备是否处于横屏状态。
     *
     * @param context 上下文
     * @return 处于横屏状态时返回 {@code true}
     */
    public static boolean isHorizontalScreen(@NonNull Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * 判断当前设备是否处于竖屏状态。
     *
     * @param context 上下文
     * @return 处于竖屏状态时返回 {@code true}
     */
    public static boolean isVerticalScreen(@NonNull Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    // ----------------------- 键盘 -------------------------

    /**
     * 判断当前软键盘是否可见。
     *
     * @param activity 目标 {@link Activity}
     * @return 软键盘可见时返回 {@code true}
     */
    public static boolean isKeyboardVisible(@NonNull Activity activity) {
        return getKeyboardHeight(activity) > 0;
    }

    /**
     * 获取当前软键盘的高度（单位：px），基于 {@link WindowInsets.Type#ime()}。
     *
     * @param activity 目标 {@link Activity}
     * @return 软键盘高度（px），未弹出时返回 {@code 0}
     */
    public static int getKeyboardHeight(@NonNull Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        WindowInsets insets = decorView.getRootWindowInsets();
        if (insets == null) {
            return 0;
        }
        return insets.getInsets(WindowInsets.Type.ime()).bottom;
    }

    // ----------------------- 文本测量 -------------------------

    /**
     * 测量指定文本在给定字号（sp）下的渲染像素宽度。
     *
     * @param context 上下文
     * @param text    待测量的文本
     * @param spSize  文本字号（单位：sp）
     * @return 文本的像素宽度
     */
    public static float measureText(@NonNull Context context, @NonNull String text, float spSize) {
        Paint paint = MEASURE_PAINT.get();
        paint.setTextSize(applyDimension(context, TypedValue.COMPLEX_UNIT_SP, spSize));
        return paint.measureText(text);
    }

    private static float applyDimension(@NonNull Context context, int unit, float value) {
        return TypedValue.applyDimension(unit, value, context.getResources().getDisplayMetrics());
    }
}
