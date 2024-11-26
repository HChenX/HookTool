<div align="center">
<h1>HookTool</h1>

![stars](https://img.shields.io/github/stars/HChenX/HookTool?style=flat)
![Github repo size](https://img.shields.io/github/repo-size/HChenX/HookTool)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/HChenX/HookTool)](https://github.com/HChenX/HookTool/releases)
[![GitHub Release Date](https://img.shields.io/github/release-date/HChenX/HookTool)](https://github.com/HChenX/HookTool/releases)
![last commit](https://img.shields.io/github/last-commit/HChenX/HookTool?style=flat)
![language](https://img.shields.io/badge/language-java-purple)

<p><b><a href="README-en.md">English</a> | <a href="README.md">简体中文</a></b></p>
<p>使用 Java 编写的 Hook 工具！帮助你减轻编写 Hook 代码的复杂度！</p>
</div>

# ✨ 工具特性

### 1. **链式调用**

### 2. **泛型转换**

### 2. **安全使用**

### 4. **全面丰富**

#### Tip: 重构声明: v.1.0.0 版本和之前版本有较大不同，新版本工具完成静态化，更符合工具特征，拥有更好的使用体验和性能。

# 🔧 使用方法

#### 1. 向项目 settings.gradle 文件添加如下代码。

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

#### 2. 向项目 app 内 build.gradle 文件添加如下代码。

```groovy
dependencies {
    // 二选一即可，推荐使用 jitpack，maven 可能不会同步更新！
    // Tip: v.*.*.* 填写当前最新发行版版本号即可！
    implementation 'com.github.HChenX:HookTool:v.1.0.8' // jitpack
    implementation 'io.github.hchenx:hooktool:v.1.0.8' // maven Tip: 几乎废弃，请不要使用！
}
```

#### 3. 同步项目，下载依赖即可在代码内调用。

#### 4. 使用工具。

- HCInit 介绍。

```java
public void init() {
    HCinit.initBasicData(); // 初始化模块基本信息
    HCinit.initStartupParam(); // 在 zygote 阶段初始化工具
    HCinit.initLoadPackageParam(); // 在 loadPackage 阶段初始化工具
    HCinit.xPrefsAutoReload(); // 是否自动更新共享首选项，默认开启
    HCinit.useLogExpand(); // 是否使用日志增强功能，具体参见方法注释内容
}
```

- 在 Xposed 入口处初始化本工具。

```java

@Override
public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
    HCInit.initBasicData(new BasicData()
            .setModulePackageName("com.hchen.demo") // 模块包名
            .setTag("HChenDemo") // 日志 tag
            .setLogLevel(LOG_D) // 日志等级
            .setPrefsName("hchen_prefs") // prefs 存储文件名 (可选)
    ); // Tip: 若有使用 initZygote 建议配置在这里，因为时机很早。
    HCInit.initStartupParam(startupParam); // 在 zygote 阶段初始化工具
}

@Override
public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
    HCInit.initLoadPackageParam(lpparam); // 在 loadPackage 阶段初始化工具
}
```

- 如果需要使用 prefs 工具或者在模块内使用本工具的 log 类，那么你还需要在模块主界面初始化。

```java
public static class MainActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        HCInit.initBasicData(new BasicData()
                .setModulePackageName("com.hchen.demo") // 模块包名
                .setTag("HChenDemo") // 日志 tag
                .setLogLevel(LOG_D) // 日志等级
                .setPrefsName("hchen_prefs") // prefs 存储文件名。(可选) Tip: 请保持与 Xposed 内填写的文件名一致
        );
    }
}
```

- 在代码处调用

```java
public class MainTest {
    public void test() {
        CoreTool.hookMethod(/* 内容 */); // 即可 hook
        CoreTool.findClass().get(); // 查找类
        CoreTool.callMethod(); // 调用方法
        ChainTool.chain("com.hchen.demo", new ChainTool()
                .method("method")
                .hook()

                .method("method")
                .hook()
        ); // 即可链式调用
        PrefsTool.prefs().getString(); // 即可读取共享首选项
        // ......
    }
}
```

- 当然你也可以直接继承本工具打包好的类。
- **强烈建议继承 BaseHC 使用！**

```java
// Hook 方
public class MainTest extends BaseHC {
    @Override
    public void init() {
        // BaseHC 继承了 CoreTool 工具，直接调用即可。
    }

    // 可选项。
    // 时机为 zygote。
    // 请务必在 hook 入口处初始化 HCInit.initStartupParam(startupParam);
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        Class<?> c = findClass("com.hchen.demo.Main").get();
        hookMethod(c, "test", new IHook() {
            /* 内容 */
        });
    }
}

// 执行方
public class RunHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        new MainTest().onLoadPackage(); // 即可在 loadPackage 阶段执行 Hook。
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        new MainTest().onZygote(); // 即可在 initZygote 阶段 Hook。
    }
}

```

- 混淆配置:

```text
// 如果你不需要使用日志增强功能，也可以只加入（对于继承 BaseHC 使用的情况）:
-keep class * extends com.hchen.hooktool.BaseHC
 
// 如果需要使用日志增强功能，那么建议加入混淆规则:
// 假设存放 hook 文件的目录为 com.hchen.demo.hook
// 如果有多个存放的目录，建议都分别加入。
-keep class com.hchen.demo.hook.**
-keep class com.hchen.demo.hook.**$*

// 如果既不继承 BaseHC 使用，也不使用日志增强功能则不需要配置混淆规则。
```

- 到此完成全部工作，可以愉快的使用了！

# 💡 链式调用

- 本工具支持链式调用，使用 chain() 方法创建链式。
- 这是本工具重构提供的全新链式方案，是否更简洁高效了呢？
- 代码示例:

```java
// 链式调用
public class MainTest extends BaseHC {
    public void test() {
        // 看！是不是很简洁易懂？
        chain("com.hchen.demo", method("test")
                .hook(new IHook() {
                    @Override
                    public void before() {
                        super.before();
                    }
                })

                .anyMethod("test")
                .hook(new IHook() {
                    @Override
                    public void after() {
                        super.after();
                    }
                })

                .constructor()
                .returnResult(false)
        );
    }
}
```

# 🔥 泛型转换

- 传统 Xposed MethodHookParam 的各种方法返回都是 Object。 这就使得我们必须显性的进行类型转换才能使用。
- 本工具则充分使用泛型，在非特殊场景就不需要进行显性的转换啦！

```java
public class MainTest extends BaseHC {
    @Override
    public void init() {
        new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Context context = (Context) param.thisObject;
                String string = (String) param.args[0];
                param.args[1] = 1;
                String result = (String) XposedHelpers.callMethod(param.thisObject, "call", param.thisObject, param.args[0]);
                XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.demo.Main", ClassLoader.getSystemClassLoader()), "callStatic", param.thisObject, param.args[1]);
                int i = (int) XposedHelpers.getStaticObjectField(XposedHelpers.findClass("com.demo.Main", ClassLoader.getSystemClassLoader()), "field");
            }
        };

        new IAction() {
            @Override
            public void before() {
                Context context = thisObject(); // 无需显式转换
                String string = getArgs(0); // 获取第一个方法的参数。
                setArgs(1, context); // 设置第二个方法参数。

                // 非静态本类内
                setThisField("demo", 1);
                callThisMethod("method", objs);
                // 非静态本类外
                setField(obj /* 实例 */, "demo", 1);
                callMethod(obj /* 实例 */, "method", objs);

                // 静态需要 class
                callStaticMethod("com.demo.Main", "callStatic", thisObject(), getArgs(1));
                int i = getStaticField("com.demo.Main", "field");
                setStaticField("com.demo.Main", "test", true);

                removeSelf(); // 你可调用此方法，使得挂钩自己失效
                observeCall();  // 方法被调用时输出基本信息
                getStackTrace(); // 获取方法的调用堆栈
            }
        };
    }
}

```

# 📌 安全使用

- 本工具致力于构建全面完善的抛错处理逻辑，尽量不会中断 hook 进程。
- 例如：

```java
public class MainTest extends BaseHC {
    public void init() {
        Class<?> c = findClass("com.hchen.demo.Demo").get(); // 如果无法获取 class 则会记录 Error 日志并返回 null。
        hookMethod(c, "test", new IHook() { // c 为 null 也会记录 Error 日志，并跳过 hook 继续执行后面逻辑。
            @Override
            public void before() {
                ((Object) null).getClass(); // 虽然抛错但会被记录而不会直接抛给寄生应用或者导致 hook 流程中断。
            }
        });
        setStaticField("com.hchen.demo.Demo", "demo", true);
        callStaticMethod("com.hchen.demo.Demo", "isDemo", false);
        ((Object) null).getClass(); // 如果在这里抛出，会导致 hook 流程终止，但工具会给出日志提示，请手动避免！
    }
}
```

- 非常适合于在多 hook 点内需要流程持续执行不被中断的场景！

# 📌 全面丰富

- 工具提供了全面丰富的方法供你调用。
- 包括:

----

- ContextTool 类:
- 更方便的获取 context 。

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
- 更方便稳健的反射类。

```java
public class MainTest {
    public void test() {
        // 即可反射调用方法，其他反射操作同理。
        InvokeTool.callMethod(InvokeTool.findClass("com.hchen.demo.Main",
                getClass().getClassLoader()), "test", new Class[]{});
    }
}
```

----

- SystemPropTool 类:
- 更方便的 prop 读取修改工具。

```java
public class MainTest {
    public void test() {
        // 只能在系统框架中调用才能设置 prop
        SystemPropTool.setProp("ro.test.prop", "1");
        // 获取可以随意
        String result = SystemPropTool.getProp("ro.test.prop");
    }
}
```

---

- PrefsTool 类:
- 提供 prefs 读取修改功能。

```java
// 寄生应用内
public class MainTest extends BaseHC {
    @Override
    public void init() {
        // xprefs 模式：
        // 注意 xprefs 模式，寄生应用不能修改配置只能读取。
        String s = prefs().getString("test", "1");  // 即可读取
        s = prefs("myPrefs").getString("test", "1");  // 可指定读取文件名

        // sprefs 模式：
        // 配置会保存到寄生应用的私有目录，读取也会从寄生应用私有目录读取。
        prefs(context).editor().putString("test", "1").commit();
        // 如果没有继承 BaseHC 可以这样调用。
        PrefsTool.prefs(context).editor().putString("test", "2").commit();
        // 注意 sprefs 模式 是和 xprefs 模式相互独立的，可共同存在。

        // 如果不方便获取 context 可用使用此方法，异步获取寄生应用上下文后再设置。
        asyncPrefs(new PrefsTool.IAsyncPrefs() {
            @Override
            public void async(Context context) {
                prefs(context).editor().putString("test", "1").commit();
            }
        });
    }
}

// 模块内
public static class MainActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // ！！！如果使用 xprefs 模式，请在模块主界面调用 PrefsTool.prefs(context); 初始化一下，否则可能不可用！！！
        PrefsTool.prefs(this); // 或
        PrefsTool.prefs(this,/* 你自己的 prefs 名称 */);

        // 使用方法
        prefs(this).editor().putString("test", "1").commit();
        prefs(this, "myPrefs").editor().putString("test", "1").commit();
    }
}
```

---

- CoreTool 类:
- 提供完善的 Hook 方法！
- 绝对满足需求！

----

- DeviceTool 类:
- 方便的获取系统基本信息。
- 具体参见源代码和注释。

----

- ResInjectTool 类:
- 将模块资源注入目标作用域。
- 具体参见源代码与注释。

----

- PackagesTool 类:
- 快速获取软件包信息！

----

- 其他更多精彩正在加载···

# 💕 工具使用者

- 以下项目使用了本工具！

|      项目名称      |                            项目链接                            |
|:--------------:|:----------------------------------------------------------:|
| ForegroundPin  |  [ForegroundPin](https://github.com/HChenX/ForegroundPin)  |
| AutoSEffSwitch | [AutoSEffSwitch](https://github.com/HChenX/AutoSEffSwitch) |
| ClipboardList  |  [ClipboardList](https://github.com/HChenX/ClipboardList)  |

- 如果你的项目使用了本工具，可以告诉我，我将会把其加入表格。
- 想要详细了解本工具也可以参考上述项目，希望给你带来帮助！

# 📢 项目声明

- **本工具基于：**
- [LSPosed](https://github.com/LSPosed/LSPosed)

- 使用本工具请注明。

# 🎉 结尾

- 感谢您愿意使用本工具！Enjoy your day! ♥️
