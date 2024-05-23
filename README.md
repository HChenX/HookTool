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

### 4. **一次性使用**

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
    implementation 'com.github.HChenX:HookTool:v.0.7.0'
}
```

#### 3. 同步项目，下载依赖即可在代码内调用。

#### 4. 使用工具。

- 在 Xposed 入口处初始化本工具。

```java

@Override
public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
    HookInit.setTAG("YourTag"); // 设置日志TAG
    HookInit.initLoadPackageParam(lpparam); // 初始化
}
```

- 在代码处调用

```java
public void test() {
    HCHook hcHook = new HCHook(); // 实例工具
    hcHook.setThisTag(TAG); // 设置具体TAG，比如本类名。
}
```

- 到此完成全部工作，可以愉快的使用了！

# 💡链式调用

- 本工具支持链式调用，获取一次`Class`终身使用 (bushi) 。
- ⚠️对`Class`的定位获取需要借助标签来辅助。
- 标签类型为 Object，所以可以随便写。
- 代码示例:

```java
// Method 的链式调用
public class MainTest {
    public void test() {
        HCHook hcHook = new HCHook();
        hcHook.findClass("main", "com.demo.Main").
                getMethod("test1").getMethod("test2").getMethod("test3"); // 即可持续的获取方法。
        // 什么？怎么 Hook ？同样简单！
        hcHook.methodTool().hook(new IAction() {
            //......
        }).hook(new IAction() {
            //......
        }).hook(new IAction() {
            //......
        });
        // 看，是不是很简单？因为上面获取了三个方法，所以下面也同样的可以 hook 三次。
        // 当然你可以指定顺序，比如 hook(1,new IAction() {});
        // 不用担心前面可能的报错导致 hook 无法进行，还记得吗？本工具的亮点 “安全调用”。
        // 如果 getMethod() 数量少于 hook() 调用数量则会自动停止执行 hook()，不会影响后续代码！
    }
}
```

```java
// Class 的链式调用
public class MainTest {
    public void test() {
        HCHook hcHook = new HCHook();
        hcHook.findClass("main1", "com.demo.Main1").findClass("main2", "com.demo.Main2")
                .findClass("main3", "com.demo.Main3")
                .getMethod("main1").hook(new IAction() {
                    //......
                }).to("main2") // 调用 to() 则会转为使用指定枚举对象的类进行方法查找与Hook。
                .getMethod("main2").hook(new IAction() {
                    //......
                }).to("main3") // 调用 to() 则会转为使用指定枚举对象的类进行方法查找与Hook。
                .getMethod("main3").hook(new IAction() {
                    //......
                }).to("main2") // 调用 to() 则会转为使用指定枚举对象的类进行方法查找与Hook。
                .getMethod("main2-1").hook(new IAction() {
                    //......
                });
    }
}
```

```java
// 独立工具
public class MainTest {
    public void test() {
        Object object = null;
        Class<?> clazz = null;
        HCHook hcHook = new HCHook();
        INDTool indTool = hcHook.indTool();
        indTool.callMethod(object, "call", new Object[]{});
        indTool.setField(object, "field", null);
        indTool.getField(object, "field");
        indTool.callStaticMethod(clazz, "callStatic");
        indTool.setStaticField(clazz, "fieldStatic", null);
        indTool.getStaticField(clazz, "fieldStatic");
    }
}
```

# 🔥泛型转换

- 传统 Xposed MethodHookParam 的各种方法返回都是 Object。 这就使得我们必须显性的进行类型转换才能使用。
- 本工具则充分使用泛型，就不需要显性的进行类型转换啦！

```java
public class MainTest {
    public void test() {
        // Xposed 代码
        new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Context context = (Context) param.thisObject; // 显性的转换
                String string = (String) param.args[0]; // 复杂的 args[]
                param.args[1] = 1;
                String result = (String) XposedHelpers.callMethod(param.thisObject, "call",
                        param.thisObject, param.args[0]);
                XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.demo.Main",
                                ClassLoader.getSystemClassLoader()),
                        "callStatic", param.thisObject, param.args[1]);
                int i = (int) XposedHelpers.getStaticObjectField(XposedHelpers.findClass("com.demo.Main",
                                ClassLoader.getSystemClassLoader()),
                        "field");
            }
        };

        // HookTool 代码
        new IAction() {
            @Override
            public void before(ParamTool param, StaticTool staticTool) {
                Context context = param.thisObject(); // 无显性转换
                String string = param.first(); // 简单且无需显性转换
                param.second(1); // 直达式设置
                String result = param.callMethod("call", new Object[]{param.thisObject(), param.first()});
                staticTool.findClass("com.demo.Main"); // 一次获取多次使用
                staticTool.callStaticMethod("callStatic", new Object[]{param.thisObject(), param.second()});
                int i = staticTool.getStaticField("field");
            }
        };
        // 是不是方便了许多呢？
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

# 📌一次性使用

- 工具追求在目标类内实例化一次即可执行完全部需要执行的操作，拒绝多次重复实例。

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

- InvokeUtils 类:
- 更方便稳健的反射类。

```java
public class MainTest {

    public void test() {
        // 即可反射调用方法，其他反射操作同理。
        InvokeUtils.callMethod("com.hchen.hooktool.MainTest",
                getClass().getClassLoader(), "test", new Class[]{});
    }
}
```

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

- 其他更多精彩正在加载···

# 📢项目声明

- **本工具基于：**
- [LSPosed](https://github.com/LSPosed/LSPosed)

- 使用本工具请注明。

# 🎉结尾

- 感谢您愿意使用本工具！Enjoy your day! ♥️
