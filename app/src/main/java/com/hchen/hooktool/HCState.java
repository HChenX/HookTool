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
package com.hchen.hooktool;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import kotlin.text.Charsets;

/**
 * 基本状态信息
 * <p>
 * 记得配置混淆，否则不可用:
 * <p>
 * {@code
 * -keep class com.hchen.hooktool.HCState
 * }
 *
 * @author 焕晨HChen
 */
public final class HCState {
    static boolean isEnabled = false;
    static String mFramework = "Unknown";
    static int mVersion = -1;

    /**
     * 判断模块是否被启用。
     */
    public static boolean isEnabled() {
        return isEnabled;
    }

    /**
     * 获取框架类型。
     */
    public static String getFramework() {
        return mFramework;
    }

    /**
     * 获取框架的版本。
     */
    public static int getVersion() {
        return mVersion;
    }

    /**
     * 判断是否处于太极环境。
     */
    public static boolean isExpActive(Context context) {
        try {
            context.getPackageManager().getPackageInfo("me.weishu.exp", PackageManager.GET_ACTIVITIES);

            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = Uri.parse("content://me.weishu.exposed.CP/");
            Bundle result = null;
            try {
                result = contentResolver.call(uri, "active", null, null);
            } catch (Throwable e) {
                try {
                    Intent intent = new Intent("me.weishu.exp.ACTION_ACTIVE");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Throwable i) {
                    return false;
                }
            }

            if (result == null)
                result = contentResolver.call(uri, "active", null, null);
            if (result == null) return false;
            return result.getBoolean("active", false);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * 判断模块是否由 LSPatch 内置进 App 内。
     */
    public static HashMap<String, String> isLSPatchActive(ApplicationInfo appInfo) {
        String config = appInfo.metaData.getString("lspatch");
        if (config == null) return new HashMap<>();

        String json = new String(Base64.decode(config, Base64.DEFAULT), Charsets.UTF_8);
        try {
            JSONObject pathConfig = new JSONObject(json);
            boolean useManager = pathConfig.getBoolean("useManager");
            JSONObject lspConfig = pathConfig.getJSONObject("lspConfig");
            String versionName = lspConfig.getString("VERSION_NAME");
            String versionCode = lspConfig.getString("VERSION_CODE");
            HashMap<String, String> configMap = new HashMap<>();
            configMap.put("useManager", useManager ? "本地模式" : "集成模式");
            configMap.put("versionName", versionName);
            configMap.put("versionCode", versionCode);
            return configMap;
        } catch (JSONException e) {
            return new HashMap<>();
        }
    }
}
