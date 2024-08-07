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
package com.hchen.hooktool.tool.additional;

import static com.hchen.hooktool.tool.additional.InvokeTool.findClass;
import static com.hchen.hooktool.tool.additional.InvokeTool.getStaticField;
import static com.hchen.hooktool.tool.additional.PropTool.getProp;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.os.Build;

import java.util.Locale;

/**
 * 此类用于获取设备基本信息
 * <p>
 * This class is used to obtain basic information about the device
 */
public class DeviceTool {
    /**
     * 获取安卓设备版本。
     * <p>
     * Get the Android version.
     */
    public static int getAndroidVersion() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * 获取小米设备 MIUI 版本
     * 将获取到的字符串转换为浮点，以提供判断。
     * <p>
     * Get the MIUI version of the Xiaomi device converts the acquired string to a floating point to provide a judgment.
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
     * 将获取到的字符串转换为浮点，以提供判断。
     * <p>
     * Get Xiaomi Device HyperOS Version Convert the acquired string to floating-point to provide judgment.
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
     * 判断是否为指定某个 Android 版本。
     * <p>
     * Determines whether a specific Android version is specified.
     */
    public static boolean isAndroidVersion(int version) {
        return getAndroidVersion() == version;
    }

    /**
     * 判断是否大于等于某个 Android 版本。
     * <p>
     * Determine whether an Android version is greater than or greater.
     */
    public static boolean isMoreAndroidVersion(int version) {
        return getAndroidVersion() >= version;
    }

    /**
     * 判断是否为指定某个 MIUI 版本。
     * <p>
     * Determines whether a MIUI version is specified.
     */
    public static boolean isMiuiVersion(float version) {
        return getMiuiVersion() == version;
    }

    /**
     * 判断是否大于等于某个 MIUI 版本。
     * <p>
     * Determine whether a MIUI version is greater than or equal to.
     */
    public static boolean isMoreMiuiVersion(float version) {
        return getMiuiVersion() >= version;
    }

    /**
     * 判断是否为指定某个 HyperOS 版本。
     * <p>
     * Determines whether a HyperOS version is specified.
     */
    public static boolean isHyperOSVersion(float version) {
        return getHyperOSVersion() == version;
    }

    /**
     * 判断是否大于等于某个 HyperOS 版本。
     * <p>
     * Determine whether the HyperOS version is greater than or equal to.
     */
    public static boolean isMoreHyperOSVersion(float version) {
        return getHyperOSVersion() >= version;
    }

    // --------- 其他 --------
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
    
    public static boolean IS_TABLET() {
        return Boolean.TRUE.equals(getStaticField(
                findClass("miui.os.Build", null), "IS_TABLET"));
    }

    public static boolean IS_INTERNATIONAL_BUILD() {
        return Boolean.TRUE.equals(getStaticField(
                findClass("miui.os.Build", null), "IS_INTERNATIONAL_BUILD"));
    }
    
    public static boolean isPad() {
        if (IS_TABLET()) return true;
        return isPadDevice();
    }

    /**
     * 判断小米设备是否是国际版。
     * <p>
     * Determine if the Xiaomi device is an international version.
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

    public static int getDensityDpi(Resources resources) {
        return (int) (resources.getDisplayMetrics().widthPixels /
                resources.getDisplayMetrics().density);
    }

    @SuppressLint("DiscouragedApi")
    public static int getCornerRadiusTop(Resources resources) {
        int resourceId = resources.getIdentifier(
                "rounded_corner_radius_top", "dimen", "android");
        return resourceId > 0 ? resources.getDimensionPixelSize(resourceId) : 100;
    }

    public static boolean isTablet() {
        return Resources.getSystem().getConfiguration().smallestScreenWidthDp >= 600;
    }

    public static boolean isPadDevice() {
        return isTablet() || getProp("persist.sys.muiltdisplay_type", 0) == 2;
    }

    public static boolean isDarkMode(Resources resources) {
        return (resources.getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    public static BlendModeColorFilter colorFilter(int colorInt) {
        return new BlendModeColorFilter(colorInt, BlendMode.SRC_IN);
    }

}
