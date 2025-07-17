<div align="center">
<h1>HookTool</h1>

![stars](https://img.shields.io/github/stars/HChenX/HookTool?style=flat)
![Github repo size](https://img.shields.io/github/repo-size/HChenX/HookTool)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/HChenX/HookTool)](https://github.com/HChenX/HookTool/releases)
[![GitHub Release Date](https://img.shields.io/github/release-date/HChenX/HookTool)](https://github.com/HChenX/HookTool/releases)
![last commit](https://img.shields.io/github/last-commit/HChenX/HookTool?style=flat)
![language](https://img.shields.io/badge/language-java-purple)

[//]: # (<p><b><a href="README-en.md">English</a> | <a href="README.md">简体中文</a></b></p>)
<p>使用 Java 编写的 Hook 工具！帮助你减轻编写 Hook 代码的复杂度！</p>
</div>

# ✨ 工具特性

### 1. **链式调用**

### 2. **全面便利**

#### Tip: 重构声明: v.2.0.5 再次重构，使工具更加优雅更加便利。 ~~v.1.0.0

版本和之前版本有较大不同，新版本工具完成静态化，更符合工具特征，拥有更好的使用体验和性能~~

# 🔧 使用方法

#### 1. 向项目 settings.gradle 文件添加如下代码

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

#### 2. 向项目 app 内 build.gradle 文件添加如下代码

```groovy
dependencies {
    implementation 'com.github.HChenX:HookTool:v.2.1.6'
}
```

#### 3. 同步项目，下载依赖后即可使用

#### 4. 使用工具

- HCInit 介绍

```java
public void init() {
    HCInit.initBasicData(); // 初始化模块基本信息
    HCInit.initStartupParam(); // zygote 阶段初始化工具
    HCInit.initLoadPackageParam(); // loadPackage 阶段初始化工具
    HCData.setClassLoader(); // 更换全局 Classloader
    HCData....
}
```

- 在 Hook 入口处初始化本工具

```java
public class HookInit extends HCEntrance /* 建议继承 HCEntrance 类作为入口 */ {
    @NonNull
    @Override
    public HCInit.BasicData initHC(@NonNull HCInit.BasicData basicData) {
        return basicData
            .setModulePackageName("com.hchen.demo") // 模块包名
            .setTag("HChenDemo") // 日志 tag
            .setLogLevel(LOG_D) // 日志等级
            .setPrefsName("hchen_prefs") // prefs 文件名 (可选)
            .setAutoReload(true) // 是否自动更新共享首选项，默认开启 (可选)
            .setLogExpandPath("com.hchen.demo.hook") // 日志增强功能 (可选)
            .setLogExpandIgnoreClassNames("Demo"); // 排除指定类名 (可选)
    }

    @NonNull
    @Override
    public String[] ignorePackageNameList() {
        // 指定忽略的包名
        return new String[]{
            "com.android.test"
        };
    }

    @Override
    public void onModuleLoad(@NonNull XC_LoadPackage.LoadPackageParam loadPackageParam) {
        super.onModuleLoad(loadPackageParam); // 模块自身被加载时调用
    }

    @Override
    public void onLoadPackage(@NonNull XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        HCInit.initLoadPackageParam(loadPackageParam); // 必须，初始化工具
        new HookDemo().onApplication().onLoadPackage(); // 添加 onApplication 后才会执行 onApplicationBefore/After() 回调
    }

    @Override
    public void onInitZygote(@NonNull StartupParam startupParam) throws Throwable {
        new HookDemo().onZygote();
    }
}
```

- 在模块主界面初始化

```java
public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();

        HCInit.initBasicData(new HCInit.BasicData()
            .setModulePackageName("com.hchen.demo") // 模块包名
            .setTag("HChenDemo") // 日志 tag
            .setLogLevel(LOG_D) // 日志等级
            .setPrefsName("hchen_prefs") // prefs 存储文件名 (可选)
        );
    }
}
```

- **Hook 类内强烈建议继承 HCBase 使用！**

```java
public class HookDemo extends HCBase /* 建议继承 HCBase 使用 */ {
    @Override
    protected boolean isEnabled() {
        // 是否启用本 Hook
        return super.isEnabled();
    }

    @Override
    protected void init() { // loadPackage 阶段
        boolean isExists = existsClass("com.hchen.demo.Demo"); // 是否存在类
        Class<?> clazz = findClass("com.hchen.demo.Demo"); // 查找类

        hookMethod("com.hchen.demo.Demo", "demo", boolean.class, new IHook() {
            @Override
            public void before() {
                // 在 demo 方法调用前执行
                // 可以拦截方法执行，或者修改方法参数值
                setResult(true); // 拦截并返回 true
                setArg(0, false); // 设置方法第一个参数为 false
            }

            @Override
            public void after() {
                // 在 demo 方法执行后调用
                // 可以用于修改方法返回结果
                setResult(true);
            }

            @Override
            public boolean onThrow(int flag, Throwable e) {
                // before 或者 after 内代码抛错时会调用
                // 返回 true 代表已处理异常，工具将不会自动处理
                return super.onThrow(flag, e);
            }
        });
    }

    @Override
    protected void init(@NonNull ClassLoader classLoader) { // loadPackage 阶段
        // 区别是可以指定自定义的 classloader
        findClass("com.hchen.demo.Demo", classLoader);
    }

    @Override
    protected void initZygote(@NonNull IXposedHookZygoteInit.StartupParam startupParam) { // zygote 阶段
        findClass("com.hchen.demo.Demo", startupParam.getClass().getClassLoader()); // 可以这样写
    }

    @Override
    protected void onApplicationBefore(@NonNull Context context) {
        // 目标应用创建 Application 之前回调
    }

    @Override
    protected void onApplicationAfter(@NonNull Context context) {
        // 目标应用创建 Application 之后回调
    }

    @Override
    protected void onThrowable(int flag, @NonNull Throwable e) {
        // 上述方法发生抛错时调用，你可以在此处执行清理操作，不建议继续执行 Hook 逻辑
    }
}
```

- 混淆配置:

```text
// 如果你不需要使用日志增强功能，也可以只加入 (对于继承 HCBase 使用的情况):
-keep class * extends com.hchen.hooktool.HCBase
 
// 如果需要使用日志增强功能，那么建议加入混淆规则:
// 假设存放 hook 类的目录为 com.hchen.demo.hook
// 如果有多个存放的目录，建议都分别加入。
-keep class com.hchen.demo.hook.**
-keep class com.hchen.demo.hook.**$*

// 其他建议配置:
-keep class com.hchen.hooktool.HCState {
       private final static boolean isXposedEnabled;
       private final static java.lang.String framework;
       private final static int version;
 }
 
-keep class com.hchen.hooktool.data.AppData {*;} 
// or
-keep class * implements android.os.Parcelable {
        public static ** CREATOR;
}
```

- 到此完成全部工作，可以愉快的使用了！

# 💡 链式调用

- 本工具支持链式调用，使用 buildChain() 方法创建链式。
- 这是本工具重构提供的全新链式方案，是否更简洁高效了呢？
- 代码示例:

```java
// 链式调用
public class MainTest extends HCBase {
    public void test() {
        // 看！是不是很简洁易懂？
        buildChain("com.hchen.demo")
            .findMethod("test")
            .hook(new IHook() {
                @Override
                public void before() {
                    super.before();
                }
            })
            .findAllMethod("test")
            .hook(new IHook() {
                @Override
                public void after() {
                    super.after();
                }
            })
            .findConstructor()
            .returnResult(false);
    }
}
```

# 📌 全面便利

- 工具提供了全面丰富的方法供你调用
- 包括:

----

- ContextTool 类:
- 更方便的获取 context

```java
public class MainTest {
    public void test() {
        // 即可最简单的获取 context
        Context context = ContextTool.getContext(ContextUtils.FLAG_ALL);
    }
}
```

----

- InvokeTool 类:
- 更方便稳健的反射类

```java
public class MainTest {
    public void test() {
        // 即可反射调用方法，其他反射操作同理
        InvokeTool.callMethod(InvokeTool.findClass("com.hchen.demo.Main"), "test", new Class[]{});
    }
}
```

----

- SystemPropTool 类:
- 更方便的 prop 读取修改工具

```java
public class MainTest {
    public void test() {
        // 只能在系统框架中调用才能设置 persist 类型的 prop
        SystemPropTool.setProp("persist.test.prop", "1");
        // 获取应该可以随意
        String result = SystemPropTool.getProp("persist.test.prop");
    }
}
```

---

- PrefsTool 类:
- 提供 prefs 读取修改功能

```java
public class HookDemo extends HCBase {
    @Override
    public void init() {
        // xprefs 模式：
        // 注意 xprefs 模式，寄生应用不能修改配置只能读取
        String s = prefs().getString("test", "1");  // 即可读取
        s = prefs("myPrefs").getString("test", "1");  // 可指定读取文件名

        // sprefs 模式：
        // 配置会保存到寄生应用的私有目录，读取也会从寄生应用私有目录读取
        prefs(context).editor().putString("test", "1").commit();
        // 如果没有继承 HCBase 可以这样调用
        PrefsTool.prefs(context).editor().putString("test", "2").commit();
        // 注意 sprefs 模式 是和 xprefs 模式相互独立的，可共同存在

        // 如果不方便获取 context 可用使用此方法，异步获取寄生应用上下文后再设置
        asyncPrefs(new IAsyncPrefs() {
            @Override
            public void async(@NonNull IPrefsApply sPrefs) {
                sPrefs.editor().putString("test", "1").commit();
            }
        });
    }
}

public class Application extends android.app.Application {
    @Override
    protected void onCreate() {
        // 重要提醒：
        // 如果需要使用 xprefs 模式，请务必在模块主界面调用 PrefsTool.prefs(context); 进行初始化，否则可能不可用！
        PrefsTool.prefs(this); // 或
        PrefsTool.prefs(this,/* 你自己的 prefs 名称 */);

        // 使用方法
        prefs(this).editor().putString("test", "1").commit();
        prefs(this, "myPrefs").editor().putString("test", "1").commit();
    }
}
```

---

- ShellTool 类：
- 提供简易的执行 Shell 命令的能力
- 使用方法:

```java
public class MainTest {
    public void test() {
        ShellTool shellTool = ShellTool.builder().isRoot(true).create();
        shellTool = ShellTool.obtain();
        ShellResult shellResult = shellTool.cmd("ls").exec();
        if (shellResult != null) {
            boolean result = shellResult.isSuccess();
        }
        shellTool.cmd("""
            if [[ 1 == 1 ]]; then
                echo hello;
            elif [[ 1 == 2 ]]; then
                echo world;
            fi
            """).exec();
        shellTool.cmd("echo hello").async();
        shellTool.cmd("echo world").async(new IExecListener() {
            @Override
            public void output(@NonNull String command, @NonNull String exitCode, @NonNull String[] outputs) {
                IExecListener.super.output(command, exitCode, outputs);
            }
        });
        shellTool.addExecListener(new IExecListener() {
            @Override
            public void output(@NonNull String command, @NonNull String exitCode, @NonNull String[] outputs) {
                IExecListener.super.output(command, exitCode, outputs);
            }

            @Override
            public void error(@NonNull String command, @NonNull String exitCode, @NonNull String[] errors) {
                IExecListener.super.error(command, exitCode, errors);
            }

            @Override
            public void rootResult(boolean hasRoot, @NonNull String exitCode) {
                IExecListener.super.rootResult(hasRoot, exitCode);
            }

            @Override
            public void brokenPip(@NonNull String reason, @NonNull String[] errors) {
                IExecListener.super.brokenPip(reason, errors);
            }
        });
        shellTool.close();
    }
}
```

---

- CoreTool 类:
- 提供完善的 Hook 方法！
- 绝对满足需求！

----

- DeviceTool 类:
- 方便的获取系统基本信息
- 具体参见源代码和注释

----

- ResInjectTool 类:
- 将模块资源注入目标作用域
- 具体参见源代码与注释

----

- PackagesTool 类:
- 快速获取软件包信息！

----

- 其他更多精彩正在加载···

# 💕 工具使用者

- 以下项目使用了本工具！

|       项目名称       |                              项目链接                              |
|:----------------:|:--------------------------------------------------------------:|
|   AppRetention   |   [AppRetention](https://github.com/HChenX/AppRetention)       |
|  AutoSEffSwitch  |   [AutoSEffSwitch](https://github.com/HChenX/AutoSEffSwitch)   |
|  SwitchFreeForm  |   [SwitchFreeForm](https://github.com/HChenX/SwitchFreeForm)   |
|  ForegroundPin   |    [ForegroundPin](https://github.com/HChenX/ForegroundPin)    |
|  ClipboardList   |    [ClipboardList](https://github.com/HChenX/ClipboardList)    |
| SplitScreenPlus  |  [SplitScreenPlus](https://github.com/HChenX/SplitScreenPlus)  |
|    SuperLyric    |       [SuperLyric](https://github.com/HChenX/SuperLyric)       | 

- 如果你的项目使用了本工具，可以告诉我，我将会把其加入表格
- 想要详细了解本工具也可以参考上述项目，希望给你带来帮助！

# 🎉 结尾

- 感谢您愿意使用本工具！Enjoy your day! ♥️
