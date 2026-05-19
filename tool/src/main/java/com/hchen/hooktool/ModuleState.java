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
 * Xposed 模块运行环境检测工具类。
 * <p>
 * 提供对第三方 Xposed 宿主环境的检测能力，目前支持识别太极（TaiChi）和
 * LSPatch 两种常见的 Xposed 框架宿主。开发者可在运行时据此判断模块所处的
 * 环境类型并执行相应的适配策略。
 *
 * @author 焕晨HChen
 */
public final class ModuleState {
    private ModuleState() {
    }

    /**
     * 检测当前设备是否运行在太极（TaiChi）Xposed 宿主环境中。
     * <p>
     * 检测逻辑分为两步：首先检查太极应用是否已安装，然后通过 ContentProvider
     * 查询太极框架的激活状态。内部会执行两次 ContentProvider 调用以应对
     * 首次调用可能失败的情况。
     *
     * @param context 应用上下文，用于访问 PackageManager 和 ContentResolver
     * @return {@code true} 表示处于太极环境且框架已激活，{@code false} 表示不在太极环境中
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
     * 检测目标应用是否通过 LSPatch 框架加载，并提取其配置信息。
     * <p>
     * 通过读取目标应用 {@code AndroidManifest.xml} 中的 {@code lspatch}
     * 元数据字段进行检测。该字段以 Base64 编码的 JSON 格式存储 LSPatch 配置。
     * 解析成功后返回包含以下键值的 {@link HashMap}：
     * <ul>
     *   <li>{@code useManager} - 使用模式，值为 "本地模式" 或 "集成模式"</li>
     *   <li>{@code versionName} - LSPatch 框架版本名称</li>
     *   <li>{@code versionCode} - LSPatch 框架版本号</li>
     * </ul>
     * <p>
     * 调用此方法需确保应用已声明 {@code android.permission.QUERY_ALL_PACKAGES} 权限。
     *
     * @param context     应用上下文，用于访问 PackageManager
     * @param packageName 目标应用的包名
     * @return 包含 LSPatch 配置信息的 HashMap；若目标应用未使用 LSPatch 或解析失败则返回空 Map
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
