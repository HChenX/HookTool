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
package com.hchen.hooktool.util.device;

import static com.hchen.hooktool.util.device.SystemPropTool.getProp;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.hchen.hooktool.reflect.Reflect;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Android 设备与 ROM 信息查询工具类。
 *
 * @author 焕晨HChen
 */
public final class DeviceTool {
    /**
     * 屏幕对角线达到该英寸数即判定为平板。
     */
    private static final double PAD_DIAGONAL_INCHES = 7.0;
    /**
     * {@code persist.sys.muiltdisplay_type} 中表示平板的取值。
     */
    private static final int DISPLAY_TYPE_PAD = 2;

    private DeviceTool() {
    }

    // ----------------------- 版本信息 -------------------------

    private static final String VERSION_PROPERTY_MIUI = "ro.miui.ui.version.name";
    private static final String VERSION_PROPERTY_HYPER_OS = "ro.mi.os.version.name";
    private static final String VERSION_PROPERTY_XIAOMI_MARKET = "ro.product.marketname";
    private static final String[] VERSION_PROPERTY_XIAOMI = {"ro.mi.os.version.incremental", "ro.build.version.incremental"};
    private static final String VERSION_PROPERTY_COLOROS = "ro.build.version.oplusrom.display";
    private static final String VERSION_PROPERTY_COLOROS_FULL = "persist.sys.oplus.ota_ver_display";
    private static final String VERSION_PROPERTY_COLOROS_MARKET = "ro.vendor.oplus.market.name";
    /**
     * 版本号的首个数字段及其后一段小数，如 {@code "OS3.0"}、{@code "V14.0.5.0"}。
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)(?:\\.(\\d+))?");

    private static final String BRAND_LOWER = Build.BRAND.toLowerCase(Locale.ROOT);
    private static final String MANUFACTURER_LOWER = Build.MANUFACTURER.toLowerCase(Locale.ROOT);
    /**
     * 小米系设备品牌名称数组，包含 {@code "xiaomi"} 和 {@code "redmi"}。
     */
    private static final String[] DEVICE_XIAOMI = {"xiaomi", "redmi"};
    /**
     * ColorOS 系设备品牌名称数组，包含 {@code "oppo"}、{@code "realme"}、{@code "oneplus"} 和 {@code "oplus"}。
     */
    private static final String[] DEVICE_COLOROS = {"oppo", "realme", "oneplus", "oplus"};
    /**
     * 三星设备品牌名称数组，包含 {@code "samsung"}。
     */
    private static final String[] DEVICE_SAMSUNG = {"samsung"};

    /**
     * 读取当前设备的 MIUI 主版本号。
     * <p>
     * 兼容 {@code "V150"}、{@code "V125"}、{@code "V12"} 与 {@code "V14.0.5.0"} 等写法，新版本无需维护映射表。
     *
     * @return MIUI 版本号（如 {@code 14f}、{@code 12.5f}），非 MIUI 环境返回 {@code 0f}
     */
    public static float getMiuiVersion() {
        return Math.max(0f, parseMiuiVersion(getProp(VERSION_PROPERTY_MIUI)));
    }

    /**
     * 读取当前设备的 HyperOS（小米澎湃 OS）主版本号。
     * <p>
     * 兼容 {@code "OS3.0"}、{@code "OS2.0.201.0"}、{@code "3.0"} 等写法，新版本无需维护映射表。
     *
     * @return HyperOS 版本号（如 {@code 2f}、{@code 3f}），非 HyperOS 环境返回 {@code 0f}
     */
    public static float getHyperOSVersion() {
        return Math.max(0f, parseHyperOSVersion(getProp(VERSION_PROPERTY_HYPER_OS)));
    }

    /**
     * 解析 MIUI 版本号：兼容带点的完整版本串与紧凑版本码（{@code "V150"} → {@code 15.0}、{@code "V125"} → {@code 12.5}、{@code "V12"} → {@code 12.0}）。
     *
     * @param raw 系统属性原始值
     * @return 解析结果；无法解析时返回 {@code -1f}
     */
    private static float parseMiuiVersion(@NonNull String raw) {
        String value = raw.trim();
        if (!value.isEmpty() && (value.charAt(0) == 'V' || value.charAt(0) == 'v')) {
            value = value.substring(1).trim();
        }
        Matcher matcher = VERSION_PATTERN.matcher(value);
        if (!matcher.find()) {
            return -1f;
        }
        String major = matcher.group(1);
        String minor = matcher.group(2);
        if (minor != null) {
            return Float.parseFloat(major + "." + minor);
        }
        // 紧凑版本码的最后一位是次版本号：V150 -> 15.0，V125 -> 12.5。
        if (major.length() >= 3) {
            return Float.parseFloat(major.substring(0, major.length() - 1)
                + "." + major.charAt(major.length() - 1));
        }
        return Float.parseFloat(major);
    }

    /**
     * 解析 HyperOS 版本号：取首个数字段及其后一段小数（{@code "OS3.0"} → {@code 3.0}、{@code "OS2.0.201.0"} → {@code 2.0}）。
     *
     * @param raw 系统属性原始值
     * @return 解析结果；无法解析时返回 {@code -1f}
     */
    private static float parseHyperOSVersion(@NonNull String raw) {
        Matcher matcher = VERSION_PATTERN.matcher(raw.trim());
        if (!matcher.find()) {
            return -1f;
        }
        String major = matcher.group(1);
        String minor = matcher.group(2);
        return Float.parseFloat(minor == null ? major : major + "." + minor);
    }

    /**
     * 获取小米系统版本号的增量标识字符串。
     * <p>
     * 依次读取 {@code ro.mi.os.version.incremental} 与 {@code ro.build.version.incremental}，返回首个非空值。
     *
     * @return 系统版本增量标识字符串，未找到时返回空字符串 {@code ""}
     */
    @NonNull
    public static String getXiaomiIncrementalVersion() {
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
     * 读取系统属性 {@code persist.sys.oplus.ota_ver_display}，与 {@link #isColorOSVersion(float, RangeMode)} 使用的数值属性不同。
     *
     * @return ColorOS 完整版本号字符串
     */
    @NonNull
    public static String getColorOSFullVersion() {
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
            String versionName = getProp(property);
            if (!TextUtils.isEmpty(versionName)) {
                return versionName;
            }
        }
        return "";
    }

    // ----------------------- 版本判断 -------------------------

    /**
     * 按指定比较模式判断当前 Android SDK 版本是否满足条件。
     * <p>
     * 若仅需相等比较，可传 {@link RangeMode#EQ}。
     *
     * @param version 目标 SDK API 级别
     * @param mode    比较模式（{@link RangeMode}）
     * @return 满足比较条件时返回 {@code true}
     */
    public static boolean isAndroidVersion(int version, @NonNull RangeMode mode) {
        return mode.matches(Build.VERSION.SDK_INT, version);
    }

    /**
     * 按指定比较模式判断当前 MIUI 版本是否满足条件。
     * <p>
     * 若仅需相等比较，可传 {@link RangeMode#EQ}。
     *
     * @param version 目标 MIUI 版本号
     * @param mode    比较模式（{@link RangeMode}）
     * @return 满足比较条件时返回 {@code true}
     */
    public static boolean isMiuiVersion(float version, @NonNull RangeMode mode) {
        return mode.matches(getMiuiVersion(), version);
    }

    /**
     * 按指定比较模式判断当前 HyperOS 版本是否满足条件。
     * <p>
     * 若仅需相等比较，可传 {@link RangeMode#EQ}。
     *
     * @param version 目标 HyperOS 版本号
     * @param mode    比较模式（{@link RangeMode}）
     * @return 满足比较条件时返回 {@code true}
     */
    public static boolean isHyperOSVersion(float version, @NonNull RangeMode mode) {
        return mode.matches(getHyperOSVersion(), version);
    }

    /**
     * 按指定比较模式判断当前 HyperOS 的主版本号与小版本号是否满足条件。
     * <p>
     * 小版本号取自系统版本增量字符串按 {@code "."} 分割后的第三段。
     *
     * @param osVersion    目标 HyperOS 主版本号
     * @param smallVersion 目标小版本号
     * @param mode         比较模式（{@link RangeMode}）
     * @return 主版本匹配且小版本满足比较条件时返回 {@code true}
     */
    public static boolean isHyperOSSmallVersion(float osVersion, int smallVersion, @NonNull RangeMode mode) {
        if (isHyperOSVersion(osVersion, RangeMode.EQ)) {
            String versionName = getXiaomiIncrementalVersion();
            String[] vs = versionName.trim().split("\\.");
            if (vs.length >= 3) {
                try {
                    return mode.matches(Integer.parseInt(vs[2]), smallVersion);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return false;
        }
        return false;
    }

    /**
     * 按指定比较模式判断当前 ColorOS 版本是否满足条件。
     * <p>
     * 读取 {@code ro.build.version.oplusrom.display}（形如 {@code "15.0"}），多段版本串取前两段解析；仅需相等比较时传 {@link RangeMode#EQ}。
     *
     * @param version 目标 ColorOS 版本号
     * @param mode    比较模式（{@link RangeMode}）
     * @return 满足比较条件时返回 {@code true}
     */
    public static boolean isColorOSVersion(float version, @NonNull RangeMode mode) {
        String v = getProp(VERSION_PROPERTY_COLOROS);
        return mode.matches(parseVersionFloat(v), version);
    }

    /**
     * 将版本字符串解析为浮点数；支持多段版本串（如 {@code "2.0.201.0"}）取前两段解析，解析失败返回 {@code -1f} 表示无法判定。
     *
     * @param raw 版本字符串
     * @return 解析得到的版本号；无法解析时返回 {@code -1f}
     */
    private static float parseVersionFloat(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return -1f;
        }
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException e) {
            String[] parts = raw.split("\\.");
            if (parts.length >= 2) {
                try {
                    return Float.parseFloat(parts[0] + "." + parts[1]);
                } catch (NumberFormatException ignore) {
                    return -1f;
                }
            }
            return -1f;
        }
    }

    // ----------------------- 品牌 / ROM 判定 -------------------------

    /**
     * 获取小米系品牌名称的不可变列表。
     *
     * @return 包含 {@code "xiaomi"} 与 {@code "redmi"} 的不可变 {@link List}
     */
    @NonNull
    public static List<String> getXiaomiBrands() {
        return List.of(DEVICE_XIAOMI);
    }

    /**
     * 获取 ColorOS 系品牌名称的不可变列表。
     *
     * @return 包含 OPPO / realme / OnePlus / oplus 的不可变 {@link List}
     */
    @NonNull
    public static List<String> getColorOSBrands() {
        return List.of(DEVICE_COLOROS);
    }

    /**
     * 获取三星品牌名称的不可变列表。
     *
     * @return 包含 {@code "samsung"} 的不可变 {@link List}
     */
    @NonNull
    public static List<String> getSamsungBrands() {
        return List.of(DEVICE_SAMSUNG);
    }

    /**
     * 判断当前设备是否属于小米品牌（包括 Xiaomi 和 Redmi）。
     *
     * @return 属于小米品牌设备时返回 {@code true}
     */
    public static boolean isXiaomi() {
        return isRightRom(DEVICE_XIAOMI);
    }

    /**
     * 判断当前系统是否为 MIUI。
     * <p>
     * HyperOS 机型通常同时保留 MIUI 属性，{@link #isMiui()} 与 {@link #isHyperOS()} 可能同真。
     *
     * @return 当前系统为 MIUI 时返回 {@code true}
     */
    public static boolean isMiui() {
        return getMiuiVersion() != 0f;
    }

    /**
     * 判断当前系统是否为 HyperOS（小米澎湃 OS）。
     *
     * @return 当前系统为 HyperOS 时返回 {@code true}
     */
    public static boolean isHyperOS() {
        return getHyperOSVersion() != 0f;
    }

    /**
     * 判断当前设备是否属于 ColorOS 系品牌（OPPO、realme、OnePlus 等）。
     * <p>
     * 这是品牌判定，不是 ROM 版本判定。
     *
     * @return 当前设备为 ColorOS 系品牌时返回 {@code true}
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
     * 判断当前设备的品牌或制造商名称是否与指定关键字中的任意一个精确匹配（忽略大小写）。
     *
     * @param names 待匹配的品牌名称关键字列表（可变参数）
     * @return 品牌或制造商名称与任意关键字精确匹配时返回 {@code true}；传入空列表时返回 {@code false}
     */
    public static boolean isRightRom(@NonNull final String... names) {
        for (String name : names) {
            if (name == null) {
                continue;
            }
            String lower = name.toLowerCase(Locale.ROOT);
            if (BRAND_LOWER.equals(lower) || MANUFACTURER_LOWER.equals(lower)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断当前 MIUI 是否为国际版（Global ROM）。
     * <p>
     * 反射读取 {@code miui.os.Build.IS_INTERNATIONAL_BUILD}，非 MIUI 环境返回 {@code false}。
     *
     * @return 当前 MIUI 为国际版时返回 {@code true}
     */
    public static boolean isMiuiInternational() {
        return Boolean.TRUE.equals(Reflect.getStaticObjectFieldIfExists("miui.os.Build", "IS_INTERNATIONAL_BUILD"));
    }

    // ----------------------- 平板识别 -------------------------

    /**
     * 综合判断当前设备是否为平板。
     * <p>
     * 小米平板直接判定，其余三种检测至少满足两种才判定为平板。
     *
     * @param context 上下文
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
     * 反射读取 {@code miui.os.Build.IS_TABLET}，非 MIUI 环境返回 {@code false}。
     *
     * @return 是小米平板时返回 {@code true}
     */
    public static boolean isXiaomiPad() {
        return Boolean.TRUE.equals(Reflect.getStaticObjectFieldIfExists("miui.os.Build", "IS_TABLET"));
    }

    private static boolean isPadByProp() {
        String deviceType = getProp("ro.build.characteristics", "default");
        boolean isTablet = deviceType.toLowerCase(Locale.ROOT).contains("tablet");
        if (isTablet) {
            return true;
        }

        // persist.sys.muiltdisplay_type 为 OEM 真实键名（含拼写错误 muilt），需照搬
        int multiDisplayType = getProp("persist.sys.muiltdisplay_type", 0);
        return multiDisplayType == DISPLAY_TYPE_PAD;
    }

    private static boolean isPadBySize(@NonNull Context context) {
        Object service = context.getSystemService(Context.WINDOW_SERVICE);
        if (!(service instanceof WindowManager windowManager)) return false;
        Rect bounds = windowManager.getCurrentWindowMetrics().getBounds();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        // xdpi/ydpi 为 0 时（异常 ROM）避免除零产生 Infinity 误判平板。
        if (dm.xdpi == 0f || dm.ydpi == 0f) return false;
        double x = Math.pow(bounds.width() / dm.xdpi, 2);
        double y = Math.pow(bounds.height() / dm.ydpi, 2);
        return Math.sqrt(x + y) >= PAD_DIAGONAL_INCHES;
    }

    private static boolean isPadByApi(@NonNull Context context) {
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        return (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    // ----------------------- 设备标识 -------------------------

    /**
     * 获取当前设备的固件指纹标识。
     * <p>
     * 该标识随系统版本更新而变化，不能用于唯一设备识别。
     *
     * @return 设备固件指纹字符串
     */
    @NonNull
    public static String getDeviceFingerprint() {
        return Build.FINGERPRINT;
    }
}