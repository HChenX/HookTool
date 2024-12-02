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
import static com.hchen.hooktool.log.LogExpand.getTag;
import static com.hchen.hooktool.tool.additional.InvokeTool.findClass;
import static com.hchen.hooktool.tool.additional.InvokeTool.getStaticField;
import static com.hchen.hooktool.tool.additional.SystemPropTool.getProp;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

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

    /**
     * 判断 miui 优化开关是否开启。
     */
    public static boolean isMiuiOptimization() {
        return SystemPropTool.getProp("persist.sys.miui_optimization", false);
    }

    /**
     * 获取系统是否已经启动完成。
     */
    public static boolean isBootCompleted() {
        return SystemPropTool.getProp("sys.boot_completed", false);
    }

    // --------- 手机品牌 -------------
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

    /*
     * 可能可以使用的获取各系统版本号的 prop 条目。
     * */
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
        return !getRomVersion(VERSION_PROPERTY_HUAWEI).isEmpty();
    }

    /**
     * 判断当前厂商系统是否为 miui。
     */
    public static boolean isMiui() {
        return !getRomVersion(ROM_NAME_MIUI).isEmpty();
    }

    /**
     * 判断当前厂商系统是否为 ColorOs。
     */
    public static boolean isColorOs() {
        return !getRomVersion(VERSION_PROPERTY_OPPO).isEmpty();
    }

    /**
     * 判断当前厂商系统是否为 OriginOS。
     */
    public static boolean isOriginOs() {
        return !getRomVersion(VERSION_PROPERTY_VIVO).isEmpty();
    }

    /**
     * 判断当前厂商系统是否为 OneUI。
     */
    public static boolean isOneUi() {
        return isRightRom(ROM_SAMSUNG);
    }

    /**
     * 判断当前是否为鸿蒙系统。
     */
    public static boolean isHarmonyOs() {
        // 鸿蒙系统没有 Android 10 以下的
        if (!isMoreAndroidVersion(Build.VERSION_CODES.Q)) return false;
        try {
            Object osBrand = InvokeTool.callStaticMethod(
                    InvokeTool.findClass("com.huawei.system.BuildEx"),
                    "getOsBrand", new Class[]{});
            return "Harmony".equalsIgnoreCase(String.valueOf(osBrand));
        } catch (Throwable throwable) {
            logE(getTag(), throwable);
            return false;
        }
    }

    /**
     * 判断当前是否为 MagicOs 系统（荣耀）。
     */
    public static boolean isMagicOs() {
        return isRightRom(ROM_HONOR);
    }

    /**
     * 判断是否是指定的 rom。
     */
    public static boolean isRightRom(final String... names) {
        if (names == null) return false;
        for (String name : names) {
            if (Build.BRAND.toLowerCase().contains(name.toLowerCase())
                    || Build.MANUFACTURER.toLowerCase().contains(name.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 通过 prop 获取系统版本号。
     */
    public static String getRomVersion(String... props) {
        for (String property : props) {
            String versionName = getProp(property);
            if (TextUtils.isEmpty(versionName))
                continue;
            return versionName;
        }
        return "";
    }

    /**
     * 是否为国际版小米系统。
     */
    public static boolean isMiuiInternational() {
        return Boolean.TRUE.equals(getStaticField(
                findClass("miui.os.Build", null), "IS_INTERNATIONAL_BUILD"));
    }

    /**
     * 是否是神色模式。
     */
    public static boolean isDarkMode(Resources resources) {
        return (resources.getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * 是否是平板。
     */
    public static boolean isPad(Context context) {
        int flag = 0;
        if (isMiuiPad()) return true;
        if (isPadByProp()) ++flag;
        if (isPadBySize(context)) ++flag;
        if (isPadByApi(context)) ++flag;
        return flag >= 2;
    }

    /**
     * 是否是小米平板。
     */
    public static boolean isMiuiPad() {
        return Boolean.TRUE.equals(getStaticField(
                findClass("miui.os.Build", null), "IS_TABLET"));
    }

    private static boolean isPadByProp() {
        String mDeviceType = getProp("ro.build.characteristics", "default");
        return (mDeviceType != null && mDeviceType.toLowerCase().contains("tablet"))
                || getProp("persist.sys.muiltdisplay_type", 0) == 2;
    }

    private static boolean isPadBySize(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
        double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
        return Math.sqrt(x + y) >= 7.0;
    }

    private static boolean isPadByApi(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}
