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

public class HCState {
    static boolean isEnabled = false;
    static String mFramework = "Unknown";
    static int mVersion = -1;

    public static boolean isEnabled() {
        return isEnabled;
    }

    public static String getFramework() {
        return mFramework;
    }

    public static int getVersion() {
        return mVersion;
    }

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
