<div align="center">
<h1>HookTool</h1>

![stars](https://img.shields.io/github/stars/HChenX/HookTool?style=flat)
![Github repo size](https://img.shields.io/github/repo-size/HChenX/HookTool)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/HChenX/HookTool)](https://github.com/HChenX/HookTool/releases)
[![GitHub Release Date](https://img.shields.io/github/release-date/HChenX/HookTool)](https://github.com/HChenX/HookTool/releases)
![last commit](https://img.shields.io/github/last-commit/HChenX/HookTool?style=flat)
![language](https://img.shields.io/badge/language-java-purple)

[//]: # (<p><b><a href="README-en.md">English</a> | <a href="README.md">简体中文</a></b></p>)
<p>基于 Xposed 而来的 Hook 工具！</p>
</div>

# ✨ 工具亮点

### 1. **链式调用**

### 2. **泛型转换**

### 3. **安全调用**

### 4. **简洁干练**

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
    implementation 'com.github.HChenX:HookTool:v.0.9.8'
}
```

#### 3. 同步项目，下载依赖即可在代码内调用。

#### 4. 使用工具。

- 在 Xposed 入口处初始化本工具。

```java

@Override
public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
    HCInit.initStartupParam(startupParam); // 初始化
}

@Override
public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
    HCInit.initOther(/* 你模块的包名 */, /* tag */, /* 日志等级 */); // 必须，tip：建议放在第一位
    HCInit.initLoadPackageParam(lpparam); // 必须
}
```

- 如果需要使用 prefs 工具或者使用模块的 log 类，那么你还需要在模块主界面初始化。

```java
public static class MainActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        HCInit.initOther(/* 你模块的包名 */, /* tag */, /* 日志等级 */); // 必须
    }
}
```

- 在代码处调用

```java
public void test() {
    HCHook hcHook = new HCHook(); // 实例工具
    hcHook.setThisTag(TAG); // 设置具体 TAG，比如本类名 "test"。
}
```

- 当然你也可以直接继承本工具打包好的类
- // 强烈建议继承 BaseHC 使用！

```java
// Hook 方
public class MainTest extends BaseHC {
    @Override
    public void init() {
        // BaseHC 包含已经初始化的工具，直接调用即可。
    }

    // 可选项。
    // 时机为 zygote。
    // 使用 initZygote 必须在 hook 入口处初始化 HCInit.initStartupParam(startupParam);
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        super.initZygote(startupParam);
    }
}

// 执行方
public class RunHook {
    public void run() {
        new MainTest().onCreate(); // 即可执行 Hook。
    }
}

```

- 到此完成全部工作，可以愉快的使用了！

# 💡链式调用

- 本工具支持链式调用，使用 chain() 方法创建链式。
- 这是本工具重构提供的全新链式方案，是否更简洁高效了呢？
- 代码示例:

```java
// 链式调用
public class MainTest {
    public void test() {
        // 看！是不是很简洁易懂？
        // 链式调用时任何抛错将不会引起进程中断，请放心使用！
        chain("com.hchen.demo", method("test")
                .hook(new IAction() {
                    @Override
                    public void before() throws Throwable {
                        super.before();
                    }
                })

                .anyMethod("test")
                .hook(new IAction() {
                    @Override
                    public void after() throws Throwable {
                        super.after();
                    }
                })

                .constructor()
                .hook(new IAction() {
                    @Override
                    public void after() throws Throwable {
                        super.after();
                    }
                })
        );
    }
}
```

```java
// 核心工具，更建议直接继承 BaseHC 类获取更好体验！
public class MainTest {
    public void test() {
        Object object = null;
        Class<?> clazz = null;
        HCHook hcHook = new HCHook();
        CoreTool coreTool = new HCHook().coreTool();
        coreTool.callMethod(object, "call", new Object[]{});
        coreTool.setField(object, "field", null);
        coreTool.getField(object, "field");
        coreTool.callStaticMethod(clazz, "callStatic");
        coreTool.setStaticField(clazz, "fieldStatic", null);
        coreTool.getStaticField(clazz, "fieldStatic");
    }
}
```

# 🔥泛型转换

- 传统 Xposed MethodHookParam 的各种方法返回都是 Object。 这就使得我们必须显性的进行类型转换才能使用。
- 本工具则充分使用泛型，就不需要显性的进行类型转换啦！

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
                String result = (String) XposedHelpers.callMethod(param.thisObject, "call",
                        param.thisObject, param.args[0]);
                XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.demo.Main", ClassLoader.getSystemClassLoader()),
                        "callStatic", param.thisObject, param.args[1]);
                int i = (int) XposedHelpers.getStaticObjectField(XposedHelpers.findClass("com.demo.Main", ClassLoader.getSystemClassLoader()),
                        "field");
            }
        };

        new IAction() {
            @Override
            public void before() throws Throwable {
                // hook 方法所属的类
                Class<?> c = mClass;

                Context context = thisObject();
                String string = first();
                second(1);

                // 非静态本类内
                setThisField("demo", 1);
                callThisMethod("method");
                getThisField("test");

                // 非静态本类外
                Object o = null;
                setField(o, "demo", 1);
                callMethod(o, "method");
                getField(o, "test");

                // 静态需要 class
                String result = callMethod("call", new Object[]{thisObject(), first()});
                callStaticMethod("com.demo.Main", "callStatic", new Object[]{thisObject(), second()});
                int i = getStaticField("com.demo.Main", "field");
                setStaticField("com.demo.Main", "test", true);
                
                // 你可调用此方法，使得挂钩自己失效
                removeSelf();
            }
        };
    }
}

```

- ### **⚠️重要提醒**
- 因为泛型和可变参数的冲突，所以在使用工具中接收泛型多个参数的方法时，需要 **`new Object[]{}`** 包裹！！！

# ⚡安全调用

- **本工具致力于安全调用，即:**
- 尽量不触发崩溃、
- 尽量执行全部逻辑、
- 拥有较好的容错率等。
- 适合于需要非中断执行的情况。

# 📌简洁干练

- 工具追求提供简洁干练的方法，比如：

```java
public class MainTest extends BaseHC {
    @Override
    public void init() {
        new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                // 需要使用 param 来调出 param.thisObject, param.args,等内容。
                // 需要使用 XposedHelpers.callMethod(), XposedHelpers.callStaticMethod(),等来调用动作。
                // 十分麻烦，繁杂。
            }
        };

        new IAction() {
            @Override
            public void before() throws Throwable {
                // 本工具将其封装，可直接使用！
                // thisObject(), first(), callThisMethod(), callStaticMethod(),等，所见即所用。
                // 注：部分方法调用需要继承 BaseHC 才能简洁调用！
            }
        };
    }

    // 对于静态，工具提供了一些方法，具体参考 BaseHC.java 代码。
    public static void test() {
        sChain.chain("com.hchen.demo", sChain.method("test")
                .hook(new IAction() {
                    @Override
                    public void before() throws Throwable {
                        super.before();
                    }
                })

                .anyConstructor()
                .hook(new IAction() {
                    @Override
                    public void after() throws Throwable {
                        super.after();
                    }
                })
        );

        sCore.callStaticMethod("com.hchen.demo", "test", "hello");
    }
}
```

- 不知道客官是否喜欢呢？

- ### **⚠️重要提醒**
- 本工具十分建议您继承 BaseHC 类使用，以获得更佳的体验！

# 🔥工具附加提供

- ContextUtils 类:
- 更方便的获取 context 。

```java
public class MainTest {
    public void test() {
        // 即可最简单的获取 context
        Context context = ContextUtils.getContext(ContextUtils.FLAG_ALL);
    }
}
```

----

- InvokeUtils 类:
- 更方便稳健的反射类。

```java
public class MainTest {
    public void test() {
        // 即可反射调用方法，其他反射操作同理。
        InvokeUtils.callMethod(InvokeUtils.findClass("com.hchen.hooktool.MainTest",
                getClass().getClassLoader()), "test", new Class[]{});
    }
}
```

----

- PropUtils 类:
- 更方便的 prop 读取修改工具。

```java
public class MainTest {
    public void test() {
        // 只能在系统核心中调用才能设置 prop
        PropUtils.setProp("ro.test.prop", "1");
        // 获取可以随意
        String result = PropUtils.getProp("ro.test.prop");
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
        // 注意 xprefs 模式，即新模式下，寄生应用不能修改配置只能读取。
        String s = prefs().getString("test", "1");  // 即可读取
        s = prefs("myPrefs").getString("test", "1");  // 可指定读取文件名
        Context context = null;
        // nativePrefs() 即可切换为原生模式，配置会保存到寄生应用的私有目录，读取也会从寄生应用私有目录读取。
        nativePrefs().prefs(context).editor().putString("test", "1").commit(); 
        
        // 如果不方便获取 context 可用使用此方法，异步获取寄生应用 context，再设置。
        asynPrefs(new PrefsTool.IAsynPrefs() {
            @Override
            public void asyn(Context context) {
                prefs(context).editor().putString("test", "1").commit();
            }
        });

        // 切换回新模式。
        xposedPrefs();
        // 注意 nativePrefs() 和 xposedPrefs() 作用域是寄生应用全局。
    }
}

// 模块内
public class MainTest {
    public void test() {
        // 模块内使用必须传入上下文 context！
        // 读取，写入同理。
        Context context = null;
        prefs(context).editor().putString("test", "1").commit();
        prefs(context,"myPrefs").editor().putString("test", "1").commit(); 
    }
}

```

---

- CoreTool 类:
- 提供超完善的 Hook 方法！
- 绝对满足需求！

----

- SystemSDK 类:
- 方便的获取系统基本信息。
- 具体参见源代码和注释。

----

- ResHelper 类:
- 将模块资源注入目标作用域。
- 具体参加源代码与注释。

----

- 其他更多精彩正在加载···

# 💕工具使用者

- 以下项目使用了本工具！

|      项目名称      |                            项目链接                            |
|:--------------:|:----------------------------------------------------------:|
| ForegroundPin  |  [ForegroundPin](https://github.com/HChenX/ForegroundPin)  |
| AutoSEffSwitch | [AutoSEffSwitch](https://github.com/HChenX/AutoSEffSwitch) |

- 如果你的项目使用了本工具，可以告诉我，我将会把其加入表格。
- 想要详细了解本工具也可以参考上述项目，希望给你带来帮助！

# 📢项目声明

- **本工具基于：**
- [LSPosed](https://github.com/LSPosed/LSPosed)

- 使用本工具请注明。

# 🎉结尾

- 感谢您愿意使用本工具！Enjoy your day! ♥️
