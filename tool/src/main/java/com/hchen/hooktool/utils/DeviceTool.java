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

import com.hchen.hooktool.callback.IDecomposer;
import com.hchen.hooktool.helper.RangeHelper;
import com.hchen.hooktool.helper.TryHelper;

/**
 * Android 设备信息查询工具类。
 * <p>
 * 提供以下功能：
 * <ul>
 *     <li>Android SDK 版本号获取与条件判断</li>
 *     <li>国产 ROM 版本识别（MIUI / HyperOS / ColorOS）及版本比较</li>
 *     <li>设备品牌与 ROM 类型判定（小米、ColorOS、三星等）</li>
 *     <li>屏幕尺寸、窗口尺寸及显示密度查询</li>
 *     <li>像素（px）与 dp / sp 单位之间的相互转换</li>
 *     <li>平板设备识别</li>
 *     <li>屏幕方向与深色模式检测</li>
 * </ul>
 * <p>
 * 该类为纯工具类，所有方法均为静态方法，不允许实例化。
 *
 * @author 焕晨HChen
 */
public final class DeviceTool {
    private DeviceTool() {
    }

    /**
     * 获取当前设备的 Android SDK API 级别。
     * <p>
     * 直接返回 {@link Build.VERSION#SDK_INT} 的值。
     *
     * @return SDK API 级别整数值，例如 34 表示 Android 14
     */
    public static int getAndroidVersion() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * 读取当前设备的 MIUI 主版本号。
     * <p>
     * 通过读取系统属性 {@code ro.miui.ui.version.name} 并将其映射为对应的浮点版本号。
     * 支持 V10 至 V150 的版本映射。若当前系统并非 MIUI，返回 {@code 0f}。
     *
     * @return MIUI 版本号（如 {@code 14f}、{@code 12.5f}），非 MIUI 环境返回 {@code 0f}
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
     * 读取当前设备的 HyperOS（小米澎湃 OS）主版本号。
     * <p>
     * 优先通过预定义的映射表匹配已知版本字符串（如 {@code "OS3.0"} 映射为 {@code 3f}）；
     * 若映射未命中，则尝试解析去除 "OS" 前缀后的数值。解析失败时返回 {@code 0f}。
     *
     * @return HyperOS 版本号（如 {@code 2f}、{@code 3f}），非 HyperOS 环境返回 {@code 0f}
     */
    public static float getHyperOSVersion() {
        String raw = getProp(VERSION_PROPERTY_HYPER_OS).trim();
        float os = switch (raw) {
            case "OS3.0" -> 3f;
            case "OS2.0" -> 2f;
            case "OS1.0" -> 1f;
            default -> 0f;
        };
        if (os == 0f) {
            try {
                os = Float.parseFloat(raw.replace("OS", ""));
            } catch (Throwable ignore) {
                os = 0f;
            }
        }
        return os;
    }

    /**
     * 判断当前 Android SDK 版本是否等于指定版本。
     * <p>
     * 等价于以 {@link RangeHelper#EQ} 模式调用 {@link #isAndroidVersion(int, int)}。
     *
     * @param version 目标 SDK API 级别
     * @return 当前 SDK 版本等于目标版本时返回 {@code true}
     */
    public static boolean isAndroidVersion(int version) {
        return isAndroidVersion(version, EQ);
    }

    /**
     * 按指定比较模式判断当前 Android SDK 版本是否满足条件。
     *
     * @param version 目标 SDK API 级别
     * @param mode    比较模式，取值为 {@link RangeHelper} 中定义的常量：{@code EQ}（等于）、{@code GT}（大于）、
     *                {@code LT}（小于）、{@code GE}（大于等于）、{@code LE}（小于等于）
     * @return 满足比较条件时返回 {@code true}
     */
    public static boolean isAndroidVersion(int version, @RangeHelper.RangeModeFlag int mode) {
        return isMatchVersion(getAndroidVersion(), version, mode);
    }

    /**
     * 判断当前 MIUI 版本是否等于指定版本。
     * <p>
     * 等价于以 {@link RangeHelper#EQ} 模式调用 {@link #isMiuiVersion(float, int)}。
     *
     * @param version 目标 MIUI 版本号
     * @return 当前 MIUI 版本等于目标版本时返回 {@code true}
     */
    public static boolean isMiuiVersion(float version) {
        return isMiuiVersion(version, EQ);
    }

    /**
     * 按指定比较模式判断当前 MIUI 版本是否满足条件。
     *
     * @param version 目标 MIUI 版本号
     * @param mode    比较模式
     * @return 满足比较条件时返回 {@code true}
     */
    public static boolean isMiuiVersion(float version, @RangeHelper.RangeModeFlag int mode) {
        return isMatchVersion(getMiuiVersion(), version, mode);
    }

    /**
     * 判断当前 HyperOS 版本是否等于指定版本。
     * <p>
     * 等价于以 {@link RangeHelper#EQ} 模式调用 {@link #isHyperOSVersion(float, int)}。
     *
     * @param version 目标 HyperOS 版本号
     * @return 当前 HyperOS 版本等于目标版本时返回 {@code true}
     */
    public static boolean isHyperOSVersion(float version) {
        return isHyperOSVersion(version, EQ);
    }

    /**
     * 按指定比较模式判断当前 HyperOS 版本是否满足条件。
     *
     * @param version 目标 HyperOS 版本号
     * @param mode    比较模式
     * @return 满足比较条件时返回 {@code true}
     */
    public static boolean isHyperOSVersion(float version, @RangeHelper.RangeModeFlag int mode) {
        return isMatchVersion(getHyperOSVersion(), version, mode);
    }

    /**
     * 判断当前 HyperOS 的主版本号与小版本号是否同时匹配（相等比较）。
     * <p>
     * 小版本号从系统版本增量字符串中提取：按 {@code "."} 分割后取第三段（索引 2）。
     * <p>
     * 示例：对于版本字符串 {@code "OS2.0.201.0.VOMCNXM"}，主版本号为 {@code 2.0}，小版本号为 {@code 201}。
     *
     * @param osVersion    目标 HyperOS 主版本号
     * @param smallVersion 目标小版本号
     * @return 主版本匹配且小版本等于目标值时返回 {@code true}
     */
    public static boolean isHyperOSSmallVersion(float osVersion, int smallVersion) {
        return isHyperOSSmallVersion(osVersion, smallVersion, EQ);
    }

    /**
     * 按指定比较模式判断当前 HyperOS 的主版本号与小版本号是否满足条件。
     * <p>
     * 小版本号从系统版本增量字符串中提取：按 {@code "."} 分割后取第三段（索引 2）。
     * <p>
     * 示例：对于版本字符串 {@code "OS2.0.201.0.VOMCNXM"}，主版本号为 {@code 2.0}，小版本号为 {@code 201}。
     *
     * @param osVersion    目标 HyperOS 主版本号
     * @param smallVersion 目标小版本号
     * @param mode         比较模式
     * @return 主版本匹配且小版本满足比较条件时返回 {@code true}
     */
    public static boolean isHyperOSSmallVersion(float osVersion, int smallVersion, @RangeHelper.RangeModeFlag int mode) {
        if (isHyperOSVersion(osVersion)) {
            String version = getXiaomiVersion();
            String[] vs = version.trim().split("\\.");
            if (vs.length >= 3) {
                try {
                    return isMatchVersion(Integer.parseInt(vs[2]), smallVersion, mode);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return false;
        }
        return false;
    }

    /**
     * 判断当前 ColorOS 版本是否等于指定版本。
     * <p>
     * 等价于以 {@link RangeHelper#EQ} 模式调用 {@link #isColorOSVersion(float, int)}。
     *
     * @param version 目标 ColorOS 版本号
     * @return 当前 ColorOS 版本等于目标版本时返回 {@code true}
     */
    public static boolean isColorOSVersion(float version) {
        return isColorOSVersion(version, EQ);
    }

    /**
     * 按指定比较模式判断当前 ColorOS 版本是否满足条件。
     *
     * @param version 目标 ColorOS 版本号
     * @param mode    比较模式
     * @return 满足比较条件时返回 {@code true}
     */
    public static boolean isColorOSVersion(float version, @RangeHelper.RangeModeFlag int mode) {
        String v = getProp(VERSION_PROPERTY_COLOROS); // result like "15.0"
        try {
            return isMatchVersion(Float.parseFloat(v), version, mode);
        } catch (NumberFormatException e) {
            return false;
        }
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
    private static final String BRAND_LOWER = Build.BRAND.toLowerCase();
    private static final String MANUFACTURER_LOWER = Build.MANUFACTURER.toLowerCase();
    /**
     * 小米系设备品牌名称数组，包含 {@code "xiaomi"} 和 {@code "redmi"}。
     */
    public static final String[] DEVICE_XIAOMI = {"xiaomi", "redmi"};
    /**
     * ColorOS 系设备品牌名称数组，包含 {@code "oppo"}、{@code "realme"}、{@code "oneplus"} 和 {@code "oplus"}。
     */
    public static final String[] DEVICE_COLOROS = {"oppo", "realme", "oneplus", "oplus"};
    /**
     * 三星设备品牌名称数组，包含 {@code "samsung"}。
     */
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
     * 判断当前设备是否属于小米品牌（包括 Xiaomi 和 Redmi）。
     * <p>
     * 通过比对 {@link Build#BRAND} 和 {@link Build#MANUFACTURER} 字段进行判断。
     *
     * @return 属于小米品牌设备时返回 {@code true}
     */
    public static boolean isXiaomi() {
        return isRightRom(DEVICE_XIAOMI);
    }

    /**
     * 判断当前系统是否为 MIUI。
     * <p>
     * 通过检查系统属性 {@code ro.miui.ui.version.name} 是否非空来判定。
     *
     * @return 当前系统为 MIUI 时返回 {@code true}
     */
    public static boolean isMiui() {
        return !getRomVersion(VERSION_PROPERTY_MIUI).isEmpty();
    }

    /**
     * 判断当前系统是否为 HyperOS（小米澎湃 OS）。
     * <p>
     * 通过检查系统属性 {@code ro.mi.os.version.name} 是否非空来判定。
     *
     * @return 当前系统为 HyperOS 时返回 {@code true}
     */
    public static boolean isHyperOS() {
        return !getRomVersion(VERSION_PROPERTY_HYPER_OS).isEmpty();
    }

    /**
     * 判断当前系统是否为 ColorOS（适用于 OPPO、realme、OnePlus 等品牌）。
     * <p>
     * 通过比对设备品牌名称列表进行判定。
     *
     * @return 当前系统为 ColorOS 时返回 {@code true}
     */
    public static boolean isColorOS() {
        return isRightRom(DEVICE_COLOROS);
    }

    /**
     * 判断当前设备是否为三星品牌。
     *
     * @return 属于三星设备时返回 {@code true}
     */
    public static boolean isSamsung() {
        return isRightRom(DEVICE_SAMSUNG);
    }

    /**
     * 判断当前设备的品牌或制造商名称是否包含指定关键字中的任意一个。
     * <p>
     * 比对时忽略大小写，同时检查 {@link Build#BRAND} 和 {@link Build#MANUFACTURER} 两个字段。
     *
     * @param names 待匹配的品牌名称关键字列表（可变参数）
     * @return 品牌或制造商名称中包含任意一个指定关键字时返回 {@code true}
     */
    public static boolean isRightRom(@NonNull final String... names) {
        for (String name : names) {
            if (
                BRAND_LOWER.contains(name.toLowerCase()) ||
                    MANUFACTURER_LOWER.contains(name.toLowerCase())
            ) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取小米系统版本号的增量标识字符串。
     * <p>
     * 依次尝试读取 {@code ro.mi.os.version.incremental} 和 {@code ro.build.version.incremental}
     * 系统属性，返回首个非空值。
     *
     * @return 系统版本增量标识字符串，未找到时返回空字符串 {@code ""}
     */
    @NonNull
    public static String getXiaomiVersion() {
        return getRomVersion(VERSION_PROPERTY_XIAOMI);
    }

    /**
     * 获取小米设备的市场销售名称（例如 {@code "Xiaomi 14 Ultra"}）。
     * <p>
     * 读取系统属性 {@code ro.product.marketname}。
     *
     * @return 设备市场名称字符串
     */
    @NonNull
    public static String getXiaomiMarketName() {
        return getProp(VERSION_PROPERTY_XIAOMI_MARKET);
    }

    /**
     * 获取 ColorOS 完整版本号字符串。
     * <p>
     * 读取系统属性 {@code persist.sys.oplus.ota_ver_display}。
     *
     * @return ColorOS 完整版本号字符串
     */
    @NonNull
    public static String getColorOSVersion() {
        return getProp(VERSION_PROPERTY_COLOROS_FULL);
    }

    /**
     * 获取 ColorOS 设备的市场销售名称。
     * <p>
     * 读取系统属性 {@code ro.vendor.oplus.market.name}。
     *
     * @return 设备市场名称字符串
     */
    @NonNull
    public static String getColorOSMarketName() {
        return getProp(VERSION_PROPERTY_COLOROS_MARKET);
    }

    /**
     * 根据一组系统属性名依次尝试获取 ROM 版本号。
     * <p>
     * 按顺序读取每个属性，返回首个非空的属性值。若所有属性均为空或读取过程中发生异常，
     * 则返回空字符串。
     *
     * @param props 系统属性名列表（可变参数）
     * @return ROM 版本号字符串，未找到有效值时返回空字符串 {@code ""}
     */
    @NonNull
    public static String getRomVersion(@NonNull String... props) {
        for (String property : props) {
            if (TextUtils.isEmpty(property)) {
                continue;
            }
            try {
                String versionName = getProp(property);
                if (!TextUtils.isEmpty(versionName)) {
                    return versionName;
                }
            } catch (Throwable ignore) {
            }
        }
        return "";
    }

    /**
     * 判断当前 MIUI 是否为国际版（Global ROM）。
     * <p>
     * 通过反射读取 {@code miui.os.Build.IS_INTERNATIONAL_BUILD} 静态字段来判定。
     * 若反射失败（例如非 MIUI 环境），则返回 {@code false}。
     *
     * @return 当前 MIUI 为国际版时返回 {@code true}
     */
    public static boolean isMiuiInternational() {
        return TryHelper.doTry(new IDecomposer<Boolean>() {
            @Override
            public Boolean get() throws Throwable {
                return Boolean.TRUE.equals(getStaticField("miui.os.Build", "IS_INTERNATIONAL_BUILD"));
            }
        }).getOrDefault(false);
    }

    /**
     * 从 {@link Context} 中获取 {@link WindowManager} 系统服务实例。
     *
     * @param context 上下文对象，不得为 {@code null}
     * @return {@link WindowManager} 实例
     */
    @NonNull
    public static WindowManager getWindowManager(@NonNull Context context) {
        return (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * 获取当前设备的 {@link Display} 对象。
     * <p>
     * 在 Android R（API 30）及以上版本中直接使用 {@link Context#getDisplay()}；
     * 在低版本中回退使用 {@code WindowManager.getDefaultDisplay()}。
     *
     * @param context 上下文对象，不得为 {@code null}
     * @return {@link Display} 实例
     */
    @NonNull
    public static Display getDisplay(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return context.getDisplay();
        } else {
            return getWindowManager(context).getDefaultDisplay();
        }
    }

    /**
     * 获取当前窗口的尺寸（单位：px）。
     * <p>
     * 委托 {@link #getScreenSize(WindowManager)} 进行实际测量。
     *
     * @param context 上下文对象，不得为 {@code null}
     * @return 包含窗口宽度（{@code x}）和高度（{@code y}）的 {@link Point} 对象
     */
    @NonNull
    public static Point getWindowSize(@NonNull Context context) {
        return getWindowSize(getWindowManager(context));
    }

    /**
     * 获取当前窗口的尺寸（单位：px）。
     * <p>
     * Android R（API 30）及以上通过 {@code WindowMetrics.getBounds()} 获取；
     * 低版本通过 {@link DisplayMetrics} 获取。
     *
     * @param windowManager {@link WindowManager} 实例，不得为 {@code null}
     * @return 包含窗口宽度（{@code x}）和高度（{@code y}）的 {@link Point} 对象
     */
    @NonNull
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
     * 获取屏幕的物理尺寸（单位：px）。
     *
     * @param context 上下文对象，不得为 {@code null}
     * @return 包含屏幕宽度（{@code x}）和高度（{@code y}）的 {@link Point} 对象
     */
    @NonNull
    public static Point getScreenSize(@NonNull Context context) {
        return getScreenSize(getWindowManager(context));
    }

    /**
     * 获取屏幕的物理尺寸（单位：px）。
     * <p>
     * Android R（API 30）及以上通过 {@code MaximumWindowMetrics.getBounds()} 获取最大可用尺寸；
     * 低版本通过 {@code Display.getSize()} 获取。
     *
     * @param windowManager {@link WindowManager} 实例，不得为 {@code null}
     * @return 包含屏幕宽度（{@code x}）和高度（{@code y}）的 {@link Point} 对象
     */
    @NonNull
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
     * 判断当前设备是否处于横屏状态。
     *
     * @param context 上下文对象，不得为 {@code null}
     * @return 处于横屏状态时返回 {@code true}
     */
    public static boolean isHorizontalScreen(@NonNull Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * 判断当前设备是否处于竖屏状态。
     *
     * @param context 上下文对象，不得为 {@code null}
     * @return 处于竖屏状态时返回 {@code true}
     */
    public static boolean isVerticalScreen(@NonNull Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    /**
     * 判断当前系统是否处于深色模式。
     *
     * @param resources {@link Resources} 实例，不得为 {@code null}
     * @return 深色模式已开启时返回 {@code true}
     */
    public static boolean isDarkMode(@NonNull Resources resources) {
        return (resources.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * 将像素值（px）转换为密度无关像素值（dp）。
     * <p>
     * 转换公式：{@code dp = px / density + 0.5f}，结果四舍五入取整。
     *
     * @param context 上下文对象，用于获取屏幕密度，不得为 {@code null}
     * @param pxValue 待转换的像素值
     * @return 对应的 dp 值
     */
    public static int px2dp(@NonNull Context context, float pxValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 将像素值（px）转换为字体缩放无关像素值（sp）。
     * <p>
     * 转换公式：{@code sp = px / scaledDensity + 0.5f}，结果四舍五入取整。
     *
     * @param context 上下文对象，用于获取字体缩放密度，不得为 {@code null}
     * @param pxValue 待转换的像素值
     * @return 对应的 sp 值
     */
    public static int px2sp(@NonNull Context context, float pxValue) {
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    /**
     * 将密度无关像素值（dp）转换为像素值（px）。
     * <p>
     * 转换公式：{@code px = dp * density + 0.5f}，结果四舍五入取整。
     *
     * @param context 上下文对象，用于获取屏幕密度，不得为 {@code null}
     * @param dpValue 待转换的 dp 值
     * @return 对应的像素值
     */
    public static int dp2px(@NonNull Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 将字体缩放无关像素值（sp）转换为像素值（px）。
     * <p>
     * 转换公式：{@code px = sp * scaledDensity + 0.5f}，结果四舍五入取整。
     *
     * @param context 上下文对象，用于获取字体缩放密度，不得为 {@code null}
     * @param spValue 待转换的 sp 值
     * @return 对应的像素值
     */
    public static int sp2px(@NonNull Context context, float spValue) {
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    /**
     * 综合判断当前设备是否为平板。
     * <p>
     * 判定策略如下：
     * <ol>
     *     <li>若为小米平板（{@link #isXiaomiPad()} 返回 {@code true}），直接判定为平板；</li>
     *     <li>否则综合以下三种检测方式，至少满足其中两种则判定为平板：
     *         <ul>
     *             <li>系统属性检测（{@code ro.build.characteristics} 是否包含 "tablet"）</li>
     *             <li>屏幕物理尺寸检测（对角线 ≥ 7 英寸）</li>
     *             <li>屏幕布局配置检测（屏幕布局大小 ≥ {@code SCREENLAYOUT_SIZE_LARGE}）</li>
     *         </ul>
     *     </li>
     * </ol>
     *
     * @param context 上下文对象，不得为 {@code null}
     * @return 判定为平板设备时返回 {@code true}
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
     * 判断当前设备是否为小米平板。
     * <p>
     * 通过反射读取 {@code miui.os.Build.IS_TABLET} 静态字段来判定。
     * 若反射失败（例如非 MIUI 环境），则返回 {@code false}。
     *
     * @return 是小米平板时返回 {@code true}
     */
    public static boolean isXiaomiPad() {
        return TryHelper.doTry(new IDecomposer<Boolean>() {
            @Override
            public Boolean get() throws Throwable {
                return Boolean.TRUE.equals(
                    getStaticField(
                        "miui.os.Build",
                        "IS_TABLET"
                    )
                );
            }
        }).getOrDefault(false);
    }

    private static boolean isPadByProp() {
        String deviceType = getProp("ro.build.characteristics", "default");
        boolean isTablet = (deviceType != null && deviceType.toLowerCase().contains("tablet"));
        if (isTablet) {
            return true;
        }

        int multiDisplayType = getProp("persist.sys.muiltdisplay_type", 0);
        return multiDisplayType == 2;
    }

    private static boolean isPadBySize(@NonNull Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Rect bounds = windowManager.getCurrentWindowMetrics().getBounds();
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            double x = Math.pow(bounds.width() / dm.xdpi, 2);
            double y = Math.pow(bounds.height() / dm.ydpi, 2);
            return Math.sqrt(x + y) >= 7.0;
        } else {
            Display display = windowManager.getDefaultDisplay();
            DisplayMetrics dm = new DisplayMetrics();
            display.getMetrics(dm);
            double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
            double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
            return Math.sqrt(x + y) >= 7.0;
        }
    }

    private static boolean isPadByApi(@NonNull Context context) {
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        return (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * 获取当前屏幕的显示密度（单位：dpi）。
     * <p>
     * 若 {@link Resources} 为 {@code null} 或发生异常，返回 {@link DisplayMetrics#DENSITY_DEFAULT}。
     *
     * @param context 上下文对象，不得为 {@code null}
     * @return 屏幕密度值（单位：dpi）
     */
    public static int getScreenDensity(@NonNull Context context) {
        try {
            Resources resources = context.getResources();
            if (resources == null) {
                return DisplayMetrics.DENSITY_DEFAULT;
            }
            return resources.getDisplayMetrics().densityDpi;
        } catch (Throwable ignore) {
            return DisplayMetrics.DENSITY_DEFAULT;
        }
    }

    /**
     * 基于设备硬件信息生成一个伪唯一设备标识符。
     * <p>
     * 标识符格式为 {@code "{brand}_{model}_{sdkVersion}_{fingerprintHash}"}。
     * 若生成过程中发生异常，返回 {@code "unknown_device"}。
     *
     * @return 设备标识字符串
     */
    @NonNull
    public static String getDeviceId() {
        try {
            return Build.BRAND + "_" +
                Build.MODEL + "_" +
                Build.VERSION.SDK_INT + "_" +
                Build.FINGERPRINT.hashCode();
        } catch (Throwable ignore) {
            return "unknown_device";
        }
    }
}
