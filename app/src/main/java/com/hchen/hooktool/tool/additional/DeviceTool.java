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

import static com.hchen.hooktool.log.AndroidLog.logE;
import static com.hchen.hooktool.tool.additional.InvokeTool.findClass;
import static com.hchen.hooktool.tool.additional.InvokeTool.getStaticField;
import static com.hchen.hooktool.tool.additional.PropTool.getProp;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;

import com.hchen.hooktool.log.LogExpand;

import java.util.Locale;

/**
 * 此类用于获取设备基本信息
 */
public class DeviceTool {
    /**
     * 获取安卓设备版本。
     */
    public static int getAndroidVersion() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * 获取小米设备 MIUI 版本
     * 将获取到的字符串转换为浮点，以提供判断。
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
     * 获取小米设备 HyperOS 版本
     * 将获取到的字符串转换为浮点，以提供判断。
     */
    public static float getHyperOSVersion() {
        return switch (getProp("ro.mi.os.version.name").trim()) {
            case "OS2.0" -> 2f;
            case "OS1.0" -> 1f;
            default -> 0f;
        };
    }

    /**
     * 判断是否为指定某个 Android 版本。
     */
    public static boolean isAndroidVersion(int version) {
        return getAndroidVersion() == version;
    }

    /**
     * 判断是否大于等于某个 Android 版本。
     */
    public static boolean isMoreAndroidVersion(int version) {
        return getAndroidVersion() >= version;
    }

    /**
     * 判断是否为指定某个 MIUI 版本。
     */
    public static boolean isMiuiVersion(float version) {
        return getMiuiVersion() == version;
    }

    /**
     * 判断是否大于等于某个 MIUI 版本。
     */
    public static boolean isMoreMiuiVersion(float version) {
        return getMiuiVersion() >= version;
    }

    /**
     * 判断是否为指定某个 HyperOS 版本。
     */
    public static boolean isHyperOSVersion(float version) {
        return getHyperOSVersion() == version;
    }

    /**
     * 判断是否大于等于某个 HyperOS 版本。
     */
    public static boolean isMoreHyperOSVersion(float version) {
        return getHyperOSVersion() >= version;
    }


    // --------- 手机品牌 -------------
    public static final String[] ROM_HUAWEI = {"huawei"};
    public static final String[] ROM_VIVO = {"vivo"};
    public static final String[] ROM_XIAOMI = {"xiaomi"};
    public static final String[] ROM_OPPO = {"oppo"};
    public static final String[] ROM_LEECO = {"leeco", "letv"};
    public static final String[] ROM_360 = {"360", "qiku"};
    public static final String[] ROM_ZTE = {"zte"};
    public static final String[] ROM_ONEPLUS = {"oneplus"};
    public static final String[] ROM_NUBIA = {"nubia"};
    public static final String[] ROM_SAMSUNG = {"samsung"};
    public static final String[] ROM_HONOR = {"honor"};

    private static final String ROM_NAME_MIUI = "ro.miui.ui.version.name";
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
     * 判断当前厂商系统是否为 emui。
     */
    public static boolean isEmui() {
        return !TextUtils.isEmpty(getProp(VERSION_PROPERTY_HUAWEI));
    }

    /**
     * 判断当前厂商系统是否为 miui。
     */
    public static boolean isMiui() {
        return !TextUtils.isEmpty(getProp(ROM_NAME_MIUI));
    }

    /**
     * 判断当前厂商系统是否为 ColorOs。
     */
    public static boolean isColorOs() {
        for (String property : VERSION_PROPERTY_OPPO) {
            String versionName = getProp(property);
            if (TextUtils.isEmpty(versionName)) {
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * 判断当前厂商系统是否为 OriginOS。
     */
    public static boolean isOriginOs() {
        return !TextUtils.isEmpty(getProp(VERSION_PROPERTY_VIVO));
    }

    /**
     * 判断当前厂商系统是否为 OneUI。
     */
    public static boolean isOneUi() {
        return isRightRom(getBrand(), getManufacturer(), ROM_SAMSUNG);
    }

    /**
     * 判断当前是否为鸿蒙系统。
     */
    public static boolean isHarmonyOs() {
        // 鸿蒙系统没有 Android 10 以下的
        if (!isMoreAndroidVersion(Build.VERSION_CODES.Q))
            return false;
        try {
            Object osBrand = InvokeTool.callStaticMethod(
                    InvokeTool.findClass("com.huawei.system.BuildEx"),
                    "getOsBrand", new Class[]{});
            return "Harmony".equalsIgnoreCase(String.valueOf(osBrand));
        } catch (Throwable throwable) {
            logE(tag(), throwable);
            return false;
        }
    }

    /**
     * 判断当前是否为 MagicOs 系统（荣耀）。
     */
    public static boolean isMagicOs() {
        return isRightRom(getBrand(), getManufacturer(), ROM_HONOR);
    }

    /**
     * 判断是否是指定的 rom。
     */
    public static boolean isAppointRom(String[] rom) {
        return isRightRom(getBrand(), getManufacturer(), rom);
    }

    /**
     * 尝试获取 rom 的版本，可能不准确。
     */
    public static String getRomVersionName() {
        final String brand = getBrand();
        final String manufacturer = getManufacturer();
        if (isRightRom(brand, manufacturer, ROM_HUAWEI)) {
            String version = getProp(VERSION_PROPERTY_HUAWEI);
            String[] temp = version.split("_");
            if (temp.length > 1) {
                return temp[1];
            } else {
                // 需要注意的是 华为畅享 5S Android 5.1 获取到的厂商版本号是 EmotionUI 3，而不是 3.1 或者 3.0 这种
                if (version.contains("EmotionUI")) {
                    return version.replaceFirst("EmotionUI\\s*", "");
                }
                return version;
            }
        }
        if (isRightRom(brand, manufacturer, ROM_VIVO)) {
            // 需要注意的是 vivo iQOO 9 Pro Android 12 获取到的厂商版本号是 OriginOS Ocean
            return getProp(VERSION_PROPERTY_VIVO);
        }
        if (isRightRom(brand, manufacturer, ROM_XIAOMI)) {
            return getProp(VERSION_PROPERTY_XIAOMI);
        }
        if (isRightRom(brand, manufacturer, ROM_OPPO)) {
            for (String property : VERSION_PROPERTY_OPPO) {
                String versionName = getProp(property);
                if (TextUtils.isEmpty(property)) {
                    continue;
                }
                return versionName;
            }
            return "";
        }
        if (isRightRom(brand, manufacturer, ROM_LEECO)) {
            return getProp(VERSION_PROPERTY_LEECO);
        }

        if (isRightRom(brand, manufacturer, ROM_360)) {
            return getProp(VERSION_PROPERTY_360);
        }
        if (isRightRom(brand, manufacturer, ROM_ZTE)) {
            return getProp(VERSION_PROPERTY_ZTE);
        }
        if (isRightRom(brand, manufacturer, ROM_ONEPLUS)) {
            return getProp(VERSION_PROPERTY_ONEPLUS);
        }
        if (isRightRom(brand, manufacturer, ROM_NUBIA)) {
            return getProp(VERSION_PROPERTY_NUBIA);
        }
        if (isRightRom(brand, manufacturer, ROM_HONOR)) {
            for (String property : VERSION_PROPERTY_MAGIC) {
                String versionName = getProp(property);
                if (TextUtils.isEmpty(property)) {
                    continue;
                }
                return versionName;
            }
            return "";
        }

        return "";
    }

    /**
     * 判断 miui 优化开关是否开启。
     */
    public static boolean isMiuiOptimization() {
        return PropTool.getProp("persist.sys.miui_optimization", false);
    }

    private static boolean isRightRom(String brand, String manufacturer, final String... names) {
        brand = brand.toLowerCase();
        manufacturer = manufacturer.toLowerCase();
        if (names == null) return false;
        for (String name : names) {
            if (brand.contains(name) || manufacturer.contains(name)) {
                return true;
            }
        }
        return false;
    }

    // ------------- 其他 -------------
    public static String getSystemVersionIncremental() {
        return getProp("ro.system.build.version.incremental");
    }

    public static String getBuildDate() {
        return getProp("ro.system.build.date");
    }

    public static String getBuilder() {
        return getProp("ro.build.user");
    }

    /**
     * 是否为小米 pad。
     */
    public static boolean isMiuiTablet() {
        return Boolean.TRUE.equals(getStaticField(
                findClass("miui.os.Build", null), "IS_TABLET"));
    }

    /**
     * 是否为国际版小米系统。
     */
    public static boolean isMiuiInternational() {
        return Boolean.TRUE.equals(getStaticField(
                findClass("miui.os.Build", null), "IS_INTERNATIONAL_BUILD"));
    }

    public static boolean isPad() {
        if (isMiuiTablet()) return true;
        return isPadDevice();
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

    public static String getManufacturer() {
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

    private static String tag() {
        String tag = LogExpand.tag();
        if (tag == null) return "DeviceTool";
        return tag;
    }
}
