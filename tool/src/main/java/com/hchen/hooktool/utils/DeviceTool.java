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
 * Copyright (C) 2023–2025 HChenX
 */
package com.hchen.hooktool.utils;

import static com.hchen.hooktool.helper.RangeHelper.EQ;
import static com.hchen.hooktool.helper.RangeHelper.GE;
import static com.hchen.hooktool.helper.RangeHelper.GT;
import static com.hchen.hooktool.helper.RangeHelper.LE;
import static com.hchen.hooktool.helper.RangeHelper.LT;
import static com.hchen.hooktool.utils.InvokeTool.getStaticField;
import static com.hchen.hooktool.utils.SystemPropTool.getProp;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.hchen.hooktool.helper.RangeHelper;
import com.hchen.hooktool.helper.TryHelper;

import java.util.Objects;

/**
 * 设备工具
 *
 * @author 焕晨HChen
 */
public class DeviceTool {
    private DeviceTool() {
    }

    /**
     * 获取安卓版本
     */
    public static int getAndroidVersion() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * 获取 MIUI 版本
     */
    public static float getMiuiVersion() {
        return switch (getProp(VERSION_PROPERTY_MIUI).trim()) {
            case "V150" -> 15f;
            case "V140" -> 14f;
            case "V130" -> 13f;
            case "V125" -> 12.5f;
            case "V12" -> 12f;
            case "V11" -> 11f;
            case "V10" -> 10f;
            default -> 0f;
        };
    }

    /**
     * 获取 HyperOS 版本
     */
    public static float getHyperOSVersion() {
        float os = switch (getProp(VERSION_PROPERTY_HYPER_OS).trim()) {
            case "OS3.0" -> 3f;
            case "OS2.0" -> 2f;
            case "OS1.0" -> 1f;
            default -> 0f;
        };
        if (os == 0f) {
            try {
                return Float.parseFloat(getProp(VERSION_PROPERTY_HYPER_OS).trim().replace("OS", ""));
            } catch (Throwable ignore) {
                return 0f;
            }
        }
        return os;
    }

    /**
     * 判断是否为指定 Android 版本
     */
    public static boolean isAndroidVersion(int version) {
        return isAndroidVersion(version, EQ);
    }

    /**
     * 根据指定模式匹配 Android 版本是否符合要求
     */
    public static boolean isAndroidVersion(int version, @RangeHelper.RangeModeFlag int mode) {
        return isMatchVersion(getAndroidVersion(), version, mode);
    }

    /**
     * 判断是否为指定 MIUI 版本
     */
    public static boolean isMiuiVersion(float version) {
        return isMiuiVersion(version, EQ);
    }

    /**
     * 根据指定模式匹配 Miui 版本是否符合要求
     */
    public static boolean isMiuiVersion(float version, @RangeHelper.RangeModeFlag int mode) {
        return isMatchVersion(getMiuiVersion(), version, mode);
    }

    /**
     * 判断是否为指定 HyperOS 版本
     */
    public static boolean isHyperOSVersion(float version) {
        return isHyperOSVersion(version, EQ);
    }

    /**
     * 根据指定模式匹配 HyperOS 版本是否符合要求
     */
    public static boolean isHyperOSVersion(float version, @RangeHelper.RangeModeFlag int mode) {
        return isMatchVersion(getHyperOSVersion(), version, mode);
    }

    /**
     * 是否是指定 HyperOS 的指定小版本
     * <p>
     * 例如：OS2.0.201.0.VOMCNXM
     * <p>
     * HyperOS -> 2.0
     * <p>
     * Small Version -> 201
     */
    public static boolean isHyperOSSmallVersion(float osVersion, int smallVersion) {
        return isHyperOSSmallVersion(osVersion, smallVersion, EQ);
    }

    /**
     * 根据指定模式匹配指定 HyperOS 版本的小版本是否符合要求
     * <p>
     * 例如：OS2.0.201.0.VOMCNXM
     * <p>
     * HyperOS -> 2.0
     * <p>
     * Small Version -> 201
     */
    public static boolean isHyperOSSmallVersion(float osVersion, int smallVersion, @RangeHelper.RangeModeFlag int mode) {
        if (isHyperOSVersion(osVersion)) {
            String version = getXiaomiVersion();
            String[] vs = version.trim().split("\\.");
            if (vs.length >= 3) return isMatchVersion(Integer.parseInt(vs[2]), smallVersion, mode);
            return false;
        }
        return false;
    }

    /**
     * 判断是否为指定 ColorOS 版本
     */
    public static boolean isColorOSVersion(float version) {
        return isColorOSVersion(version, EQ);
    }

    /**
     * 根据指定模式匹配 ColorOS 版本是否符合要求
     */
    public static boolean isColorOSVersion(float version, @RangeHelper.RangeModeFlag int mode) {
        String v = getProp(VERSION_PROPERTY_COLOROS); // result like "15.0"
        return isMatchVersion(Float.parseFloat(v), version, mode);
    }

    private static boolean isMatchVersion(float version, float targetVersion, @RangeHelper.RangeModeFlag int mode) {
        switch (mode) {
            case EQ -> {
                return version == targetVersion;
            }
            case GT -> {
                return version > targetVersion;
            }
            case LT -> {
                return version < targetVersion;
            }
            case GE -> {
                return version >= targetVersion;
            }
            case LE -> {
                return version <= targetVersion;
            }
            default -> {
                return false;
            }
        }
    }

    // ----------------------- 手机品牌 -------------------------
    public static final String[] DEVICE_XIAOMI = {"xiaomi", "redmi"};
    public static final String[] DEVICE_COLOROS = {"oppo", "realme", "oneplus", "oplus"};
    public static final String[] DEVICE_SAMSUNG = {"samsung"};
    // ---------------------------------------------------------

    // ----------------------- 各系统版本号 prop 条目 -------------------------
    private static final String VERSION_PROPERTY_MIUI = "ro.miui.ui.version.name";
    private static final String VERSION_PROPERTY_HYPER_OS = "ro.mi.os.version.name";
    private static final String VERSION_PROPERTY_XIAOMI_MARKET = "ro.product.marketname";
    private static final String[] VERSION_PROPERTY_XIAOMI = {"ro.mi.os.version.incremental", "ro.build.version.incremental"};
    private static final String VERSION_PROPERTY_COLOROS = "ro.build.version.oplusrom.display";
    private static final String VERSION_PROPERTY_COLOROS_FULL = "persist.sys.oplus.ota_ver_display";
    private static final String VERSION_PROPERTY_COLOROS_MARKET = "ro.vendor.oplus.market.name";
    // --------------------------------------------------------------------

    /**
     * 判断当前厂商是否为 Xiaomi
     */
    public static boolean isXiaomi() {
        return isRightRom(DEVICE_XIAOMI);
    }

    /**
     * 判断当前厂商系统是否为 Miui
     */
    public static boolean isMiui() {
        return !getRomVersion(VERSION_PROPERTY_MIUI).isEmpty();
    }

    /**
     * 判断当前厂商系统是否为 HyperOS
     */
    public static boolean isHyperOS() {
        return !getRomVersion(VERSION_PROPERTY_HYPER_OS).isEmpty();
    }

    /**
     * 判断当前厂商是否为 ColoOS
     */
    public static boolean isColorOS() {
        return isRightRom(DEVICE_COLOROS);
    }

    /**
     * 判断当前厂商是否为 Samsung
     */
    public static boolean isSamsung() {
        return isRightRom(DEVICE_SAMSUNG);
    }

    /**
     * 判断是否是指定的 ROM
     */
    public static boolean isRightRom(@NonNull final String... names) {
        for (String name : names) {
            if (
                Build.BRAND.toLowerCase().contains(name.toLowerCase()) ||
                    Build.MANUFACTURER.toLowerCase().contains(name.toLowerCase())
            ) {
                return true;
            }
        }
        return false;
    }

    public static String getXiaomiVersion() {
        return getRomVersion(VERSION_PROPERTY_XIAOMI);
    }

    public static String getXiaomiMarketName() {
        return getProp(VERSION_PROPERTY_XIAOMI_MARKET);
    }

    public static String getColorOSVersion() {
        return getProp(VERSION_PROPERTY_COLOROS_FULL);
    }

    public static String getColorOSMarketName() {
        return getProp(VERSION_PROPERTY_COLOROS_MARKET);
    }

    /**
     * 通过 prop 获取系统版本号
     */
    @NonNull
    public static String getRomVersion(@NonNull String... props) {
        for (String property : props) {
            String versionName = getProp(property);
            if (TextUtils.isEmpty(versionName))
                continue;

            return versionName;
        }
        return "";
    }

    /**
     * 是否为国际版小米系统
     */
    public static boolean isMiuiInternational() {
        return TryHelper.doTry(() ->
            Boolean.TRUE.equals(getStaticField("miui.os.Build", "IS_INTERNATIONAL_BUILD"))
        ).orElse(false);
    }

    /**
     * 获取系统是否已经启动完成
     */
    public static boolean isBootCompleted() {
        return Objects.equals(getProp("sys.boot_completed", 0), 1);
    }

    /**
     * 获取 WindowManager 实例
     */
    public static WindowManager getWindowManager(@NonNull Context context) {
        return (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * 获取当前上下文的 Display 对象
     */
    public static Display getDisplay(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return context.getDisplay();
        } else {
            return getWindowManager(context).getDefaultDisplay();
        }
    }

    /**
     * 获取窗口尺寸
     */
    public static Point getWindowSize(@NonNull Context context) {
        return getWindowSize(getWindowManager(context));
    }

    /**
     * 获取窗口尺寸
     */
    public static Point getWindowSize(@NonNull WindowManager windowManager) {
        Point point = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Rect bounds = windowManager.getCurrentWindowMetrics().getBounds();
            point.x = bounds.width();
            point.y = bounds.height();
        } else {
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            point.x = metrics.widthPixels;
            point.y = metrics.heightPixels;
        }
        return point;
    }

    /**
     * 获取屏幕尺寸
     */
    public static Point getScreenSize(@NonNull Context context) {
        return getScreenSize(getWindowManager(context));
    }

    /**
     * 获取屏幕尺寸
     */
    public static Point getScreenSize(@NonNull WindowManager windowManager) {
        Point point = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Rect bounds = windowManager.getMaximumWindowMetrics().getBounds();
            point.x = bounds.width();
            point.y = bounds.height();
        } else {
            windowManager.getDefaultDisplay().getSize(point);
        }
        return point;
    }

    /**
     * 判断屏幕是否为横屏
     */
    public static boolean isHorizontalScreen(@NonNull Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * 判断屏幕是否为竖屏
     */
    public static boolean isVerticalScreen(@NonNull Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    /**
     * 是否是深色模式
     */
    public static boolean isDarkMode(@NonNull Resources resources) {
        return (resources.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * 将像素值转换为密度独立像素值
     */
    public static int px2dp(@NonNull Context context, float pxValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 将像素值转换为缩放独立的字体像素值
     */
    public static int px2sp(@NonNull Context context, float pxValue) {
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    /**
     * 将密度独立像素值转换为像素值
     */
    public static int dp2px(@NonNull Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 将缩放独立的字体像素值转换为像素值
     */
    public static int sp2px(@NonNull Context context, float spValue) {
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    /**
     * 是否是平板
     */
    public static boolean isPad(@NonNull Context context) {
        int flag = 0;
        if (isXiaomiPad()) return true;
        if (isPadByProp()) ++flag;
        if (isPadBySize(context)) ++flag;
        if (isPadByApi(context)) ++flag;
        return flag >= 2;
    }

    /**
     * 是否是小米平板
     */
    public static boolean isXiaomiPad() {
        return TryHelper.doTry(() -> Boolean.TRUE.equals(
            getStaticField(
                "miui.os.Build",
                "IS_TABLET"
            )
        )).orElse(false);
    }

    private static boolean isPadByProp() {
        String deviceType = getProp("ro.build.characteristics", "default");
        return (deviceType != null && deviceType.toLowerCase().contains("tablet")) || getProp("persist.sys.muiltdisplay_type", 0) == 2;
    }

    private static boolean isPadBySize(@NonNull Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
        double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
        return Math.sqrt(x + y) >= 7.0;
    }

    private static boolean isPadByApi(@NonNull Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}
