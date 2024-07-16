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

 * Copyright (C) 2023-2024 HookTool Contributions
 */
package com.hchen.hooktool.utils;

import static com.hchen.hooktool.utils.PropUtils.getProp;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.os.Build;

import java.util.Locale;

/**
 * 此类用于获取设备基本信息
 */
public class SystemSDK {
    private static String TAG;

    public SystemSDK() {

    }

    public SystemSDK(String tag) {
        TAG = tag;
    }

    public static String getSystemVersionIncremental() {
        return getProp("ro.system.build.version.incremental");
    }

    public static String getBuildDate() {
        return getProp("ro.system.build.date");
    }

    public static String getHost() {
        return Build.HOST;
    }

    public static String getBuilder() {
        return getProp("ro.build.user");
    }

    public static String getBaseOs() {
        return getProp("ro.build.version.base_os");
    }

    public static String getRomAuthor() {
        return getProp("ro.rom.author") + getProp("ro.romid");
    }

    /**
     * 获取安卓设备版本。
     */
    public static int getAndroidVersion() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * 获取小米设备 MIUI 版本
     * 将获取到的字符串转换为浮点，以提供判断
     *
     * @return 一个 Float 值
     */
    public static float getMiuiVersion() {
        switch (getProp("ro.miui.ui.version.name")) {
            case "V150" -> {
                return 15f;
            }
            case "V140" -> {
                return 14f;
            }
            case "V130" -> {
                return 13f;
            }
            case "V125" -> {
                return 12.5f;
            }
            case "V12" -> {
                return 12f;
            }
            case "V11" -> {
                return 11f;
            }
            case "V10" -> {
                return 10f;
            }
            default -> {
                return 0f;
            }
        }
    }

    /**
     * 获取小米设备 HyperOS 版本
     * 将获取到的字符串转换为浮点，以提供判断
     *
     * @return 一个 Float 值
     */
    public static float getHyperOSVersion() {
        switch (getProp("ro.mi.os.version.name")) {
            case "OS2.0" -> {
                return 2f;
            }
            case "OS1.0" -> {
                return 1f;
            }
            default -> {
                return 0f;
            }
        }
    }

    /**
     * 判断是否为指定某个 Android 版本
     *
     * @param version 传入的 Android SDK Int 数值
     * @return 一个 Boolean 值
     */
    public static boolean isAndroidVersion(int version) {
        return getAndroidVersion() == version;
    }

    /**
     * 判断是否大于等于某个 Android 版本
     *
     * @param version 传入的 Android SDK Int 数值
     * @return 一个 Boolean 值
     */
    public static boolean isMoreAndroidVersion(int version) {
        return getAndroidVersion() >= version;
    }

    /**
     * 判断是否为指定某个 MIUI 版本
     *
     * @param version 传入的 MIUI 版本 Float 数值
     * @return 一个 Boolean 值
     */
    public static boolean isMiuiVersion(float version) {
        return getMiuiVersion() == version;
    }

    /**
     * 判断是否大于等于某个 MIUI 版本
     *
     * @param version 传入的 MIUI 版本 Float 数值
     * @return 一个 Boolean 值
     */
    public static boolean isMoreMiuiVersion(float version) {
        return getMiuiVersion() >= version;
    }

    /**
     * 判断是否为指定某个 HyperOS 版本
     *
     * @param version 传入的 HyperOS 版本 Float 数值
     * @return 一个 Boolean 值
     */
    public static boolean isHyperOSVersion(float version) {
        return getHyperOSVersion() == version;
    }

    /**
     * 判断是否大于等于某个 HyperOS 版本
     *
     * @param version 传入的 HyperOS 版本 Float 数值
     * @return 一个 Boolean 值
     */
    public static boolean isMoreHyperOSVersion(float version) {
        return getHyperOSVersion() >= version;
    }

    // --------- 其他 --------
    public static boolean IS_TABLET() {
        return Boolean.TRUE.equals(InvokeUtils.getStaticField(
                InvokeUtils.findClass("miui.os.Build", null), "IS_TABLET"));
    }

    public static boolean IS_INTERNATIONAL_BUILD() {
        return Boolean.TRUE.equals(InvokeUtils.getStaticField(
                InvokeUtils.findClass("miui.os.Build", null), "IS_INTERNATIONAL_BUILD"));
    }

    /**
     * 函数调用，适用于其他一些需要判断的情况，仅支持小米设备的判断
     * 2024-04-20 更新对非小米设备的判断方式，仅防止闪退
     *
     * @return 一个 Boolean 值，true 代表是平板，false 代表不是平板
     */
    public static boolean isPad() {
        if (IS_TABLET()) return true;
        return isPadDevice();
    }

    /**
     * 函数调用，适用于其他一些需要判断的情况，仅支持小米设备的判断
     *
     * @return 一个 Boolean 值，true 代表是国际版系统，false 代表不是国际版系统
     */
    public static boolean isInternational() {
        return IS_INTERNATIONAL_BUILD();
    }

    public static String getFingerPrint() {
        return Build.FINGERPRINT;
    }

    public static String getLocale() {
        return getProp("ro.product.locale");
    }

    public static String getLanguage() {
        return Locale.getDefault().toString();
    }

    public static String getBoard() {
        return Build.BOARD;
    }

    public static String getSoc() {
        return getProp("ro.soc.model");
    }

    public static String getDeviceName() {
        return Build.DEVICE;
    }

    public static String getMarketName() {
        return getProp("ro.product.marketname");
    }

    public static String getModelName() {
        return Build.MODEL;
    }

    public static String getBrand() {
        return Build.BRAND;
    }

    public static String getManufacture() {
        return Build.MANUFACTURER;
    }

    public static String getModDevice() {
        return getProp("ro.product.mod_device");
    }

    public static String getCharacteristics() {
        return getProp("ro.build.characteristics");
    }

    public static String getSerial() {
        return getProp("ro.serialno").replace("\n", "");
    }

    public static int getDensityDpi() {
        return (int) (ContextUtils.getContext(ContextUtils.FLAG_ALL).getResources().getDisplayMetrics().widthPixels /
                ContextUtils.getContext(ContextUtils.FLAG_ALL).getResources().getDisplayMetrics().density);
    }

    @SuppressLint("DiscouragedApi")
    public static int getCornerRadiusTop() {
        int resourceId = ContextUtils.getContext(ContextUtils.FLAG_ALL).getResources().getIdentifier(
                "rounded_corner_radius_top", "dimen", "android");
        return resourceId > 0 ? ContextUtils.getContext(ContextUtils.FLAG_ALL).getResources().getDimensionPixelSize(resourceId) : 100;
    }

    public static boolean isTablet() {
        return Resources.getSystem().getConfiguration().smallestScreenWidthDp >= 600;
    }

    public static boolean isPadDevice() {
        return isTablet() || PropUtils.getProp("persist.sys.muiltdisplay_type", 0) == 2;
    }

    public static boolean isDarkMode() {
        return (ContextUtils.getContext(ContextUtils.FLAG_ALL).getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    public static BlendModeColorFilter colorFilter(int colorInt) {
        return new BlendModeColorFilter(colorInt, BlendMode.SRC_IN);
    }

}
