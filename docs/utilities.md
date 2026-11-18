# 工具库

```groovy
implementation 'com.github.HChenX.HookTool:hooktool-utils:4.0.0'
implementation 'com.github.HChenX.HookTool:hooktool-shell:4.0.0'
implementation 'com.github.HChenX.HookTool:hooktool-res:4.0.0'   // 资源替换
```

## Shell

```java
ShellTool shell = ShellTool.obtain(true);
ShellResult result = shell.cmd("ls /data").exec();
if (result instanceof ShellResult.Completed completed && completed.isSuccess()) {
    String[] outputs = completed.result().outputs();
} else if (result instanceof ShellResult.Failed failed) {
    ShellFailureReason reason = failed.reason();
}

shell.cmd("echo hello").async().thenAccept(async -> { });
ShellTool.close();
```

拼接模式用于分段构造同一条命令：

```java
shell.enableSplicingMode()
    .cmd("if [[ -d /data ]]; then")
    .cmd("  echo exists")
    .cmd("fi")
    .exec();
```

## 设备与系统属性

```java
DeviceTool.getDeviceFingerprint();
DeviceTool.isXiaomi();
DeviceTool.getHyperOSVersion();
DeviceTool.getRomVersion("ro.mi.os.version.name");

SystemPropTool.getProp("ro.build.version.sdk", 0);
SystemPropTool.setProp("debug.mock", "1");
```

`RangeMode` 用于版本区间判断：

```java
DeviceTool.isAndroidVersion(34, RangeMode.GE);
```

## 包信息

```java
PackageTool.isInstalled(context, packageName);
PackageTool.getVersionName(context, packageName);
PackageTool.isSystem(context, packageName);

AppData[] apps = PackageTool.getAppData(context,
    pm -> pm.getInstalledApplications(0));
```

`AppData` 暴露 `packageName`、`label`、`icon`、`versionName` 等字段。

## 配置

```java
// 宿主进程内读取远程配置
SharedPreferences remote = PrefsTool.prefs();

// 应用进程内使用本地配置
SharedPreferences local = PrefsTool.prefs(context);
```

默认文件名取 `HookConfig.getPrefsName()`，也可显式传入名称。

## 界面与位图

```java
UiTool.dp2px(context, 16);
UiTool.getScreenWidth(context);
UiTool.isDarkMode(context);

Bitmap bitmap = BitmapTool.drawableToBitmap(drawable);
Bitmap rounded = BitmapTool.getRoundedCornerBitmap(bitmap, 12f);
byte[] bytes = BitmapTool.bitmapToBytes(bitmap);
```

## 资源替换

注入前必须在模块的 `build.gradle` 中指定资源包 ID，否则注入失败：

```groovy
android {
    androidResources.additionalParameters("--allow-reserved-package-id", "--package-id", "0x64")
}
```

```java
ResInject.injectModuleRes();

// 直接返回指定对象
ResInject.setObjectReplacement(packageName, "string", "demo_replaced", "替换文本");
// 按屏幕密度换算尺寸
ResInject.setDensityReplacement(packageName, "dimen", "demo_dimen", 2.0f);
// 重定向到模块内的资源
ResInject.setResReplacement(packageName, "drawable", "demo_icon", R.drawable.module_icon);
```

规则按 `包名:类型/资源名` 匹配，包名传 `*` 可命中所有应用。`setResReplacement` 的目标 ID 必须是注入模块资源后能解析到的模块资源。
