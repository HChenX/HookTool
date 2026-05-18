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
package com.hchen.hooktool;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Objects;

import kotlin.text.Charsets;

/**
 * 模块状态检测工具类。提供检测太极（TaiChi）和 LSPatch 等第三方 Xposed 框架环境的能力。
 *
 * @author 焕晨HChen
 */
public final class ModuleState {
    private ModuleState() {
    }

    /**
     * 检测当前是否处于太极（TaiChi）环境中。
     *
     * @param context 上下文
     * @return 是否为太极环境
     */
    public static boolean isExpActive(@NonNull Context context) {
        try {
            context.getPackageManager().getPackageInfo("me.weishu.exp", PackageManager.GET_ACTIVITIES);

            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = Uri.parse("content://me.weishu.exposed.CP/");
            Bundle result = null;
            try {
                result = contentResolver.call(uri, "active", null, null);
            } catch (Throwable ignore) {
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
     * 检测当前是否处于 LSPatch 环境中，并返回配置信息。
     * 需要声明 android.permission.QUERY_ALL_PACKAGES 权限。
     *
     * @param context     上下文
     * @param packageName 目标应用包名
     * @return 包含配置信息的 HashMap，如果非 LSPatch 环境则返回空 Map
     * @noinspection ExtractMethodRecommender
     */
    @NonNull
    public static HashMap<String, String> isLSPatchActive(@NonNull Context context, @NonNull String packageName) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
            String config = Objects.requireNonNull(info.applicationInfo).metaData.getString("lspatch");
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
