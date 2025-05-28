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
        return switch (getProp("ro.miui.ui.version.name").trim()) {
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
        return switch (getProp("ro.mi.os.version.name").trim()) {
            case "OS2.0" -> 2f;
            case "OS1.0" -> 1f;
            default -> 0f;
        };
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
    public static boolean isSmallHyperOSVersion(float osVersion, int smallVersion) {
        return isSmallHyperOSVersion(osVersion, smallVersion, EQ);
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
    public static boolean isSmallHyperOSVersion(float osVersion, int smallVersion, @RangeHelper.RangeModeFlag int mode) {
        if (isHyperOSVersion(osVersion)) {
            String version = getProp("ro.mi.os.version.incremental");
            if (version.isEmpty()) version = getProp("ro.system.build.version.incremental");
            String[] vs = version.trim().split(".");
            if (vs.length >= 3) return isMatchVersion(Integer.parseInt(vs[2]), smallVersion, mode);
            return false;
        }
        return false;
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
    public static final String[] ROM_HUAWEI = {"huawei"};
    public static final String[] ROM_VIVO = {"vivo"};
    public static final String[] ROM_XIAOMI = {"xiaomi", "redmi"};
    public static final String[] ROM_OPPO = {"oppo"};
    public static final String[] ROM_LEECO = {"leeco", "letv"};
    public static final String[] ROM_360 = {"360", "qiku"};
    public static final String[] ROM_ZTE = {"zte"};
    public static final String[] ROM_ONEPLUS = {"oneplus"};
    public static final String[] ROM_NUBIA = {"nubia"};
    public static final String[] ROM_SAMSUNG = {"samsung"};
    public static final String[] ROM_HONOR = {"honor"};
    // ---------------------------------------------------------

    private static final String VERSION_PROPERTY_HUAWEI = "ro.build.version.emui";
    private static final String VERSION_PROPERTY_VIVO = "ro.vivo.os.build.display.id";
    private static final String VERSION_PROPERTY_XIAOMI = "ro.build.version.incremental";
    private static final String[] VERSION_PROPERTY_OPPO = {"ro.build.version.opporom", "ro.build.version.oplusrom.display"};
    private static final String VERSION_PROPERTY_LEECO = "ro.letv.release.version";
    private static final String VERSION_PROPERTY_360 = "ro.build.uiversion";
    private static final String VERSION_PROPERTY_ZTE = "ro.build.MiFavor_version";
    private static final String VERSION_PROPERTY_ONEPLUS = "ro.rom.version";
    private static final String VERSION_PROPERTY_NUBIA = "ro.build.rom.id";
    private static final String[] VERSION_PROPERTY_MAGIC = {"msc.config.magic.version", "ro.build.version.magic"};

    /**
     * 判断当前厂商系统是否为 Emui
     */
    public static boolean isEmui() {
        return !getRomVersion(VERSION_PROPERTY_HUAWEI).isEmpty();
    }

    /**
     * 判断当前厂商系统是否为 Xiaomi
     */
    public static boolean isXiaomi() {
        return !getRomVersion(VERSION_PROPERTY_XIAOMI).isEmpty();
    }

    /**
     * 判断当前厂商系统是否为 ColorOS
     */
    public static boolean isColorOS() {
        return !getRomVersion(VERSION_PROPERTY_OPPO).isEmpty();
    }

    /**
     * 判断当前厂商系统是否为 OriginOS
     */
    public static boolean isOriginOS() {
        return !getRomVersion(VERSION_PROPERTY_VIVO).isEmpty();
    }

    /**
     * 判断当前厂商系统是否为 OneUI
     */
    public static boolean isOneUi() {
        return isRightRom(ROM_SAMSUNG);
    }

    /**
     * 判断当前是否为鸿蒙系统
     */
    public static boolean isHarmonyOS() {
        // 鸿蒙系统没有 Android 10 以下的
        if (isAndroidVersion(Build.VERSION_CODES.Q, LT))
            return false;

        try {
            Object osBrand = InvokeTool.callStaticMethod(
                "com.huawei.system.BuildEx",
                "getOsBrand",
                new Class[]{}
            );
            return "Harmony".equalsIgnoreCase(String.valueOf(osBrand));
        } catch (Throwable ignore) {
            return false;
        }
    }

    /**
     * 判断当前是否为 MagicOS 系统（荣耀）
     */
    public static boolean isMagicOS() {
        return isRightRom(ROM_HONOR);
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

    /**
     * 通过 prop 获取系统版本号
     */
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
        return Boolean.TRUE.equals(getStaticField("miui.os.Build", "IS_INTERNATIONAL_BUILD"));
    }

    /**
     * 判断 MIUI 优化是否开启
     */
    public static boolean isMiuiOptimization() {
        return getProp("persist.sys.miui_optimization", false);
    }

    /**
     * 获取系统是否已经启动完成
     */
    public static boolean isBootCompleted() {
        return getProp("sys.boot_completed", false);
    }

    /**
     * 获取 WindowManager 实例
     *
     * @param context 上下文对象，用于获取系统服务
     * @return WindowManager 对象，用于管理窗口
     */
    public static WindowManager getWindowManager(@NonNull Context context) {
        return (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * 获取当前上下文的 Display 对象
     *
     * @param context 上下文对象
     * @return Display 对象，表示当前设备的显示屏幕
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
     *
     * @param context 上下文对象
     * @return Point 对象，包含窗口的宽度和高度
     */
    public static Point getWindowSize(@NonNull Context context) {
        return getWindowSize(getWindowManager(context));
    }

    /**
     * 获取窗口尺寸
     *
     * @param windowManager WindowManager 对象
     * @return Point 对象，包含窗口的宽度和高度
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
     *
     * @param context 上下文对象
     * @return Point 对象，包含屏幕的宽度和高度
     */
    public static Point getScreenSize(@NonNull Context context) {
        return getScreenSize(getWindowManager(context));
    }

    /**
     * 获取屏幕尺寸
     *
     * @param windowManager WindowManager 对象
     * @return Point 对象，包含屏幕的宽度和高度
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
     *
     * @param context 上下文对象
     * @return 如果屏幕为横屏返回 true，否则返回 false
     */
    public static boolean isHorizontalScreen(@NonNull Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * 判断屏幕是否为竖屏
     *
     * @param context 上下文对象
     * @return 如果屏幕为竖屏返回 true，否则返回 false
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
     *
     * @param context 上下文对象，用于获取屏幕密度信息
     * @param pxValue 像素值
     * @return 转换后的密度独立像素值
     */
    public static int px2dp(@NonNull Context context, float pxValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 将像素值转换为缩放独立的字体像素值
     *
     * @param context 上下文对象，用于获取字体的缩放密度信息
     * @param pxValue 像素值
     * @return 转换后的缩放独立的字体像素值
     */
    public static int px2sp(@NonNull Context context, float pxValue) {
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    /**
     * 将密度独立像素值转换为像素值
     *
     * @param context 上下文对象，用于获取屏幕密度信息
     * @param dpValue 密度独立像素值
     * @return 转换后的像素值
     */
    public static int dp2px(@NonNull Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 将缩放独立的字体像素值转换为像素值
     *
     * @param context 上下文对象，用于获取字体的缩放密度信息
     * @param spValue 缩放独立的字体像素值
     * @return 转换后的像素值
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
        return (deviceType != null && deviceType.toLowerCase().contains("tablet"))
            || getProp("persist.sys.muiltdisplay_type", 0) == 2;
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
