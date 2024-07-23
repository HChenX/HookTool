<div align="center">
<h1>HookTool</h1>

![stars](https://img.shields.io/github/stars/HChenX/HookTool?style=flat)
![Github repo size](https://img.shields.io/github/repo-size/HChenX/HookTool)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/HChenX/HookTool)](https://github.com/HChenX/HookTool/releases)
[![GitHub Release Date](https://img.shields.io/github/release-date/HChenX/HookTool)](https://github.com/HChenX/HookTool/releases)
![last commit](https://img.shields.io/github/last-commit/HChenX/HookTool?style=flat)
![language](https://img.shields.io/badge/language-java-purple)

<p><b><a href="README-en.md">English</a> | <a href="README.md">简体中文</a></b></p>
<p>A Hook tool based on Xposed!</p>
</div>

# ✨ Highlights

### 1. **Chained Calls**

### 2. **Generic Conversion**

### 3. **Safe Calls**

### 4. **Concise and Efficient**

# 🔧 Usage

#### 1. Add the following code to your project's settings.gradle file.

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

#### 2. Add the following code to the build.gradle file inside your app project.

```groovy
dependencies {
    // jitpack
    implementation 'com.github.HChenX:HookTool:v.0.9.9.6'
    // maven
    implementation 'io.github.hchenx:hooktool:0.9.9.6'
    // Choose either one
}
```

#### 3. Sync your project and download dependencies to call them in your code.

#### 4. Use the tool.

- Initialize this tool at the Xposed entry point.

```java

@Override
public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
    HCInit.initStartupParam(startupParam); // Initialization
}

@Override
public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
    HCInit.initBasicData(/* your module's package name */, /* tag */, /* log level */); // Mandatory. tip: recommended to be placed first
    HCInit.initLoadPackageParam(lpparam); // Mandatory
}
```

- If you need to use the prefs tool or the module's log class, initialize it on the module's main
  interface.

```java
public static class MainActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        HCInit.initBasicData(/* your module's package name */, /* tag */, /* log level */); // Mandatory
    }
}
```

- Call in the code

```java
public class MainTest {
  public void test() {
    HCHook hcHook = new HCHook(); // Instantiate the tool
    hcHook.setThisTag(TAG); // Set a specific TAG, such as the class name "MainTest".
  }
}
```

- You can also directly extend the class packaged by this tool.
- // Strongly recommend extending BaseHC!

```java
// Hook class
public class MainTest extends BaseHC {
    @Override
    public void init() {
        // BaseHC includes already initialized tools, just call them directly.
    }

    // Optional.
    // The timing is zygote.
    // Using initZygote requires initializing HCInit.initStartupParam(startupParam) at the hook entry.
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        super.initZygote(startupParam);
    }
}

// Execution class
public class RunHook {
    public void run() {
        new MainTest().onCreate(); // Execute the Hook.
    }
}

```

- All work is completed, you can use it happily!

# 💡Chained Calls

- This tool supports chained calls, using the chain() method to create chains.
- This is a new chain scheme provided by the reconstruction of this tool. Is it more concise and
  efficient?
- Code example:

```java
// Chained calls
public class MainTest {
    public void test() {
        // Look! Isn't it concise and easy to understand?
        // When using chained calls, any errors will not cause process interruption, feel free to use!
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
// Core tool, it is more recommended to directly extend the BaseHC class for a better experience!
public class MainTest {
    public void test() {
        Object object = null;
        Class<?> clazz = null;
        HCHook hcHook = new HCHook();
        CoreTool coreTool = new HCHook().core();
        coreTool.callMethod(object, "call");
        coreTool.setField(object, "field", null);
        coreTool.getField(object, "field");
        coreTool.callStaticMethod(clazz, "callStatic");
        coreTool.setStaticField(clazz, "fieldStatic", null);
        coreTool.getStaticField(clazz, "fieldStatic");
    }
}
```

# 🔥Generic Conversion

- Traditional Xposed MethodHookParam methods return Object. This means we must perform explicit type
  conversions to use them.
- This tool fully uses generics, so explicit type conversions are no longer needed!

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
                // The class to which the hook method belongs
                Class<?> c = mClass;
                Context context = thisObject();
                String string = first();
                second(1);

                // Non-static within this class
                setThisField("demo", 1);
                callThisMethod("method");
                getThisField("test");
                String result = callThisMethod("call", thisObject(), first());
                // Non-static outside this class
                Object o = null;
                setField(o, "demo", 1);
                callMethod(o, "method");
                getField(o, "test");

                // Static requires class
                callStaticMethod("com.demo.Main", "callStatic", thisObject(), second());
                int i = getStaticField("com.demo.Main", "field");
                setStaticField("com.demo.Main", "test", true);

                // You can call this method to invalidate the hook itself
                removeSelf();
                // Observe call
                observeCall();
                // Get stack trace
                getStackTrace();
            }
        };
    }
}

```

# ⚡Safe Calls

- **This tool is committed to safe calls, which means:**
- Minimize crashes,
- Execute all logic as much as possible,
- Have a good fault tolerance rate, etc
- Suitable for situations requiring non-interruptible execution.

# 📌Concise and Efficient

- The tool strives to provide concise and efficient methods, such as:

```java
public class MainTest extends BaseHC {
    @Override
    public void init() {
        new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                // Need to use param to call out param.thisObject, param.args, etc.
                // Need to use XposedHelpers.callMethod(), XposedHelpers.callStaticMethod(), etc. to perform actions.
                // Very cumbersome and complicated.
            }
        };

        new IAction() {
            @Override
            public void before() throws Throwable {
                // This tool encapsulates it for direct use!
                // thisObject(), first(), callThisMethod(), callStaticMethod(), etc., what you see is what you use.
                // Note: Some method calls require extending BaseHC for concise calls!
            }
        };
    }

    // For static, the tool provides some methods, refer to BaseHC.java code for details.
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

- I wonder if you like it?

- ### **⚠Important Reminder**
- It is highly recommended to inherit the `BaseHC.class` class when using this tool for a better
  experience!

# 🔥Additional Tools Provided

- ContextUtils class:
- Easier access to context.

```java
public class MainTest {
    public void test() {
        // Easily obtain the context
        Context context = ContextUtils.getContext(ContextUtils.FLAG_ALL);
    }
}
```

----

- InvokeUtils class:
- More convenient and robust reflection utilities.

```java
public class MainTest {
    public void test() {
        // Invoke method via reflection, other reflection operations are similar.
        InvokeUtils.callMethod(InvokeUtils.findClass("com.hchen.hooktool.MainTest",
                getClass().getClassLoader()), "test", new Class[]{});
    }
}
```

----

- PropUtils class:
- Easier prop reading and modification tools.

```java
public class MainTest {
    public void test() {
        // Can only set prop in the system core
        PropUtils.setProp("ro.test.prop", "1");
        // Can get prop freely
        String result = PropUtils.getProp("ro.test.prop");
    }
}
```

---

- PrefsTool class:
- Provides prefs reading and modification functions.

```java
// In the host application
public class MainTest extends BaseHC {
  @Override
  public void init() {
    // xprefs mode:
    // Note: In xprefs mode, the host application can only read the configuration, not modify it.
    String s = prefs().getString("test", "1");  // Read configuration
    s = prefs("myPrefs").getString("test", "1");  // Specify the file name to read

    // sprefs mode:
    Context context = null;
    // If you extend BaseHC, you can directly use prefs(Context). The configuration will be saved to the host application's private directory, and reading will also be from the host application's private directory.
    prefs(context).editor().putString("test", "1").commit();
    // If you do not extend BaseHC, you can call it like this.
    PrefsTool.prefs(context).editor().putString("test", "2").commit();
    // Note: sprefs mode and xprefs mode are independent of each other and can coexist.

    // If it is inconvenient to get context, you can use this method to asynchronously get the host application's context and then set it.
    asyncPrefs(new PrefsTool.IAsyncPrefs() {
      @Override
      public void async(Context context) {
        prefs(context).editor().putString("test", "1").commit();
      }
    });
  }
}

// In the module
public static class MainActivity {
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    // !!! If using xprefs mode, please call PrefsTool.prefs(context); in the main interface of the module to initialize it, otherwise it may not be available !!!
    PrefsTool.prefs(this); // or
    PrefsTool.prefs(this, /* your own prefs name */);

    // Usage
    prefs(this).editor().putString("test", "1").commit();
    prefs(this, "myPrefs").editor().putString("test", "1").commit();
  }
}
```

---

- CoreTool class:
- Provides comprehensive Hook methods!
- Absolutely meets the needs!

----

- SystemSDK class:
- Conveniently obtain basic system information.
- Refer to the source code and comments for details.

----

- ResHelper class:
- Inject module resources into the target scope.
- Refer to the source code and comments for details.

----

- PackagesUtils class:
- Quickly obtain package information!

----

- BitmapUtils class:
- Convert Drawable to Bitmap.

----

- More exciting features are coming soon...

# 💕Tool Users

- The following projects use this tool!

|  Project Name  |                        Project Link                        |
|:--------------:|:----------------------------------------------------------:|
| ForegroundPin  |  [ForegroundPin](https://github.com/HChenX/ForegroundPin)  |
| AutoSEffSwitch | [AutoSEffSwitch](https://github.com/HChenX/AutoSEffSwitch) |

- If your project uses this tool, you can let me know, and I will add it to the table.
- For more detailed information about this tool, you can refer to the above projects. I hope it
  helps you!

# 📢Project Statement

- **This tool is based on:**
- [LSPosed](https://github.com/LSPosed/LSPosed)

- Please indicate when using this tool.

# 🎉Conclusion

- Thank you for using this tool! Enjoy your day! ♥️
