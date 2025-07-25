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
package com.hchen.hooktool;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.util.HashMap;

import kotlin.text.Charsets;

/**
 * 工具状态
 * <p>
 * 记得配置混淆，否则不可用:
 * <p>
 * <pre>{@code
 * -keep class com.hchen.hooktool.HCState {
 *        private final static boolean isXposedEnabled;
 *        private final static java.lang.String framework;
 *        private final static int version;
 *  }
 * }
 *
 * @author 焕晨HChen
 */
public class HCState {
    private static final boolean isXposedEnabled = false;
    private static final String framework = "Unknown";
    private static final int version = -1;

    private HCState() {
    }

    /**
     * 模块是否被激活
     */
    public static boolean isXposedEnabled() {
        return isXposedEnabled;
    }

    /**
     * 获取框架类型
     */
    @NonNull
    public static String getFramework() {
        return framework;
    }

    /**
     * 获取框架版本
     */
    public static int getVersion() {
        return version;
    }

    /**
     * 是否是太极环境
     */
    public static boolean isExpActive(@NonNull Context context) {
        try {
            context.getPackageManager().getPackageInfo("me.weishu.exp", PackageManager.GET_ACTIVITIES);

            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = Uri.parse("content://me.weishu.exposed.CP/");
            Bundle result = null;
            try {
                result = contentResolver.call(uri, "active", null, null);
            } catch (Throwable t) {
                try {
                    Intent intent = new Intent("me.weishu.exp.ACTION_ACTIVE");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Throwable ignore) {
                    return false;
                }
            }

            try {
                if (result == null)
                    result = contentResolver.call(uri, "active", null, null);
            } catch (Throwable ignore) {
            }
            if (result == null) return false;
            return result.getBoolean("active", false);
        } catch (PackageManager.NameNotFoundException ignore) {
            return false;
        }
    }

    /**
     * 是否是 LSPath 环境
     * <p>
     * 需要声明权限 android.permission.QUERY_ALL_PACKAGES
     *
     * @noinspection ExtractMethodRecommender
     */
    @NonNull
    public static HashMap<String, String> isLSPatchActive(@NonNull Context context, @NonNull String packageName) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
            String config = info.applicationInfo.metaData.getString("lspatch");
            if (config == null) return new HashMap<>();

            String json = new String(Base64.decode(config, Base64.DEFAULT), Charsets.UTF_8);
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
        } catch (Throwable e) {
            return new HashMap<>();
        }
    }
}
