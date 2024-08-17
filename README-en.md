<div align="center">
<h1>HookTool</h1>

![stars](https://img.shields.io/github/stars/HChenX/HookTool?style=flat)
![Github repo size](https://img.shields.io/github/repo-size/HChenX/HookTool)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/HChenX/HookTool)](https://github.com/HChenX/HookTool/releases)
[![GitHub Release Date](https://img.shields.io/github/release-date/HChenX/HookTool)](https://github.com/HChenX/HookTool/releases)
![last commit](https://img.shields.io/github/last-commit/HChenX/HookTool?style=flat)
![language](https://img.shields.io/badge/language-java-purple)

<p><b><a href="README-en.md">English</a> | <a href="README.md">简体中文</a></b></p>
<p>Java Edition Hook Tool based on Xposed!</p>
</div>

# ✨ Tool highlights

### 1. **Chained calls**

### 2. **Generic conversions**

### 3. **All-round enrichment**

#### Tip: Refactoring Statement: The v.1.0.0 version is quite different from the previous version, and the new version of the tool is static, which is more in line with the characteristics of the tool, and has a better user experience and performance.

# 🔧 Usage

#### 1. Add the following code to the project settings. gradle file.

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

#### 2. Add the following code to the build.gradle file in the project app.

```groovy
dependencies {
    // jitpack
    implementation 'com.github.HChenX:HookTool:v.1.0.1'
    // maven
    implementation 'io.github.hchenx:hooktool:v.1.0.1'
    // Choose one of the two
}
```

#### 3. Synchronize the project, download the dependencies and call them in the code.

#### 4. Use tools.

- Introduction to HCInit.

```java
public void init() {
    HCinit.initBasicData(/* Package name, tag, Log level */); // Initialize the basic information of the module
    HCinit.initStartupParam(); // Initialize the tool in the zygote phase
    HCinit.initLoadPackageParam(); // Initialize the tool in the loadPackage phase
    HCinit.xPrefsAutoReload(); // Whether to automatically update sharing preferences is enabled by default
    HCinit.useLogExpand(); // For details about whether to use the log enhancement feature, see Method Annotation
}
```

- Initialize the tool at the Xposed entry.

```java

@Override
public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
    HCInit.initBasicData(/* The package name of your module */, /* tag */, /* Log level */); // Have to. Tip: Recommendations come first
    HCInit.initStartupParam(startupParam); // Initialize the tool in the zygote phase
}

@Override
public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
    HCInit.initLoadPackageParam(lpparam); // Initialize the tool in the loadPackage phase
}
```

- If you need to use the prefs tool or use the log class of this tool in the module, then you also
  need to initialize it on the main interface of the module.

```java
public static class MainActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        HCInit.initBasicData(/* The package name of your module */, /* tag */, /* Log level */); // 必须
    }
}
```

- Called at the code

```java
public class MainTest {
    public void test() {
        CoreTool.hook(/* content */); // To hook it
        CoreTool.findClass(); // Find the class
        CoreTool.callMethod(); // Call the method
        ChainTool.chain("com.hchen.demo", new ChainTool()
                .method("method")
                .hook()

                .method("method")
                .hook()
        ); // Can be chained
        PrefsTool.prefs().getString(); // To read the sharing preferences
        // ......
    }
}
```

- Of course, you can also directly inherit the packaged classes of this tool.
- // extends BaseHC is highly recommended!

```java
// Hook
public class MainTest extends BaseHC {
    @Override
    public void init() {
        //BaseHC extends the CoreTool tool, which can be called directly.
    }

    // Optional.
    // The timing is zygote.
    // The use of initZygote must be initialized at the hook entrance: HCInit.initStartupParam(startupParam);
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        findClass("com.hchen.demo.Main", classLoader); // In this phase, you need to pass classLoader or an error will be reported.
    }
}

// Caller
public class RunHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        new MainTest().onLoadPackage(); // The hook can be executed in the loadPackage phase.
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        new MainTest().onZygote(); // initZygote Hook.
    }
}

```

- Obfuscated configuration:

```text
// If you don't need to use the log enhancements, you can also just join (for the case of inherited BaseHC use):
-keep class * extends com.hchen.hooktool.BaseHC
 
// If the directory where you store the hook file is com. hchen. demo. hook
// If you need to use logging enhancements, it is recommended to include obfuscation rules:
// If there are multiple directories, it is recommended that you add them separately.
-keep class com.hchen.demo.hook.**
-keep class com.hchen.demo.hook.**$*

// If you don't extends BaseHC usage or use log enhancements, you don't need to configure obfuscation rules.
```

- Now that all the work is done, you can use it happily!

# 💡 Chained calls

- This tool supports chaining calls and uses the chain() method to create chains.
- This is a new chain solution provided by this tool refactoring, is it more concise and efficient?
- Code examples:

```java
// Chained calls
public class MainTest extends BaseHC {
    public void test() {
        // See! Isn't it concise and easy to understand?
        chain("com.hchen.demo", method("test")
                .hook(new IAction() {
                    @Override
                    public void before() {
                        super.before();
                    }
                })

                .anyMethod("test")
                .hook(new IAction() {
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

# 🔥 Generic conversions

- The various methods of the traditional Xposed MethodHookParam are all Objects. This makes it
  necessary to type explicitly in order to use it.
- This tool makes full use of generics, so you don't need to type explicitly anymore!

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
            public void before() {
                Context context = thisObject(); // There is no need to convert explicitly
                String string = first(); // More intuitive parameter acquisition :)
                second(1); // More intuitive parameterization :)
                // non static
                setThisField("demo", 1);
                callThisMethod("method",...);
                // Non-static outside of this class
                setField(obj, "demo", 1);
                callMethod(obj, "method");

                // Static requires class
                callStaticMethod("com.demo.Main", "callStatic", thisObject(), second());
                int i = getStaticField("com.demo.Main", "field");
                setStaticField("com.demo.Main", "test", true);

                removeSelf(); // You can call this method to invalidate the hook itself
                observeCall();  // Observe the call
                getStackTrace(); // Get the stack
            }
        };
    }
}

```

# 📌 All-round enrichment

- Tools provide a comprehensive set of methods for you to call.
- including:

----

- ContextTool class:
- More convenient access context.

```java
public class MainTest {
    public void test() {
        // This is the easiest way to obtain context
        Context context = ContextTool.getContext(ContextUtils.FLAG_ALL);
    }
}
```

----

- InvokeTool class:
- More convenient and robust reflection classes.

```java
public class MainTest {
    public void test() {
        // The same applies to the reflection method, and the same applies to other reflection operations.
        InvokeTool.callMethod(InvokeTool.findClass("com.hchen.demo.Main",
                getClass().getClassLoader()), "test", new Class[]{});
    }
}
```

----

- PropTool class:
- A more convenient tool for prop reading and modification.

```java
public class MainTest {
    public void test() {
        // A prop can only be set if it is called in the system framework
        PropTool.setProp("ro.test.prop", "1");
        // You can get it at will
        String result = PropTool.getProp("ro.test.prop");
    }
}
```

---

- PrefsTool class:
- Provides the function of prefs read modification.

```java
// Parasitic in-app
public class MainTest extends BaseHC {
    @Override
    public void init() {
        // xprefs mode:
        // Note the xprefs mode, the parasitic application cannot modify the configuration and can only read.
        String s = prefs().getString("test", "1");  // To read
        s = prefs("myPrefs").getString("test", "1");  // You can specify the name of the read file

        // sprefs mode:
        // The configuration is saved to the private directory of the parasitic application, and the reads are also read from the private directory of the parasitic application.
        prefs(context).editor().putString("test", "1").commit();
        // If there is no extends BaseHC can be called like this.
        PrefsTool.prefs(context).editor().putString("test", "2").commit();
        // Note that the sprefs pattern is separate from the xprefs pattern and can co-exist.

        // If it's not convenient to get the context, you can use this method to get the parasitic app context asynchronously before setting it.
        asyncPrefs(new PrefsTool.IAsyncPrefs() {
            @Override
            public void async(Context context) {
                prefs(context).editor().putString("test", "1").commit();
            }
        });
    }
}

// Module
public static class MainActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // ！！！If you are using the xprefs mode, call PrefsTool.prefs(context) on the main interface of the module; Initialize it, otherwise it may not be available ! !
        PrefsTool.prefs(this); // or
        PrefsTool.prefs(this,/* Your own prefs name */);

        // Usage
        prefs(this).editor().putString("test", "1").commit();
        prefs(this, "myPrefs").editor().putString("test", "1").commit();
    }
}
```

---

- CoreTool class:
- A complete hook method is available!
- Absolutely satisfying the needs!

----

- DeviceTool class:
- Convenient access to basic system information.
- See source code and comments for details.

----

- ResTool class:
- Inject module resources into the target scope.
- For details, see Source Code and Comments.

----

- PackagesTool class:
- Get package information quickly!

----

- BitmapTool class:
- Convert Drawable to Bitmap

----

- More is on the way···

# 💕 Tool users

- This tool was used for the following projects!

|  Project Name  |                        Project Link                        |
|:--------------:|:----------------------------------------------------------:|
| ForegroundPin  |  [ForegroundPin](https://github.com/HChenX/ForegroundPin)  |
| AutoSEffSwitch | [AutoSEffSwitch](https://github.com/HChenX/AutoSEffSwitch) |

- If your project uses this tool, let me know and I'll add it to the form.
- If you want to know more about this tool, you can also refer to the above items, I hope it will
  help you!

# 📢 Project Statement

- **This tool is based on:**
- [LSPosed](https://github.com/LSPosed/LSPosed)

- Please specify when using this tool.

# 🎉 Conclusion

- Thank you for your willingness to use this tool! Enjoy your day! ♥️
