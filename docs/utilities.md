# 工具库

```groovy
implementation 'com.github.HChenX.HookTool:hooktool-utils:4.0.0'
implementation 'com.github.HChenX.HookTool:hooktool-shell:4.0.0'
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
// 宿主进程内读写远程配置（只读）
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

```java
ResInject.injectModuleRes();

int fakeId = ResInject.createFakeResId(resources, android.R.string.ok);
ResInject.setObjectReplacement(packageName, "string", "demo_replaced", "替换文本");
ResInject.setResReplacement(packageName, "string", "demo_replaced", fakeId);
ResInject.setDensityReplacement(packageName, "dimen", "demo_dimen", 2.0f);
```

调用 `injectModuleRes` 后会 Hook 资源加载流程，替换表在首次设置替换项时生效。