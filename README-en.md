<div align="center">
<h1>HookTool</h1>

![stars](https://img.shields.io/github/stars/HChenX/HookTool?style=flat)
![Github repo size](https://img.shields.io/github/repo-size/HChenX/HookTool)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/HChenX/HookTool)](https://github.com/HChenX/HookTool/releases)
[![GitHub Release Date](https://img.shields.io/github/release-date/HChenX/HookTool)](https://github.com/HChenX/HookTool/releases)
![last commit](https://img.shields.io/github/last-commit/HChenX/HookTool?style=flat)
![language](https://img.shields.io/badge/language-java-purple)

<p><b><a href="README-en.md">English</a> | <a href="README.md">简体中文</a></b></p>
<p>A Java-based Hook tool to simplify the process of writing hook code!</p>
</div>

# ✨ Features

### 1. **Chained Calls**

### 2. **Safe Usage**

### 3. **Comprehensive Support**

#### Tip: Version Notice: Version 1.0.0 introduces significant changes, making the tool more static, better suited to its purpose, and offering improved performance and usability.

# 🔧 Usage

#### 1. Add the following code to the project's settings.gradle file.

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

#### 2. Add the following dependency to the build.gradle file within the app directory.

```groovy
dependencies {
    // Choose one of these options; jitpack is recommended as maven might not update as frequently.
    // Tip: Replace v.*.*.* with the latest release version.
    implementation 'com.github.HChenX:HookTool:v.1.3.2' // jitpack
    // implementation 'io.github.hchenx:hooktool:v.1.2.8'
    // maven Tip: Almost abandoned, please do not use!
}
```

#### 3. Sync the project to download dependencies, then you can call the tool in your code.

#### 4. Usage

- HCInit Example.

```java
public void init() {
    HCInit.initBasicData(); // Initialize basic module information
    HCInit.initStartupParam(); // Initialize the tool during the zygote phase
    HCInit.initLoadPackageParam(); // Initialize the tool during the loadPackage phase
}
```

- Initialize the tool in the Xposed entry point.

```java

@Override
public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
    HCInit.initBasicData(new BasicData()
        .setModulePackageName("com.hchen.demo") // Module package name
        .setTag("HChenDemo") // Log tag
        .setLogLevel(LOG_D) // Log level
        .setPrefsName("hchen_prefs") // Prefs storage file name (optional)
        .xPrefsAutoReload(true) // Automatically reload shared preferences, enabled by default (optional)
        .useLogExpand(new String[]{
            "com.hchen.demo.hook"
        }) // Enable logging enhancement features, see method comments for details
    ); // Tip: Recommended to configure here if using initZygote, as it is initialized early on.
    HCInit.initStartupParam(startupParam); // Initialize the tool during the zygote phase
}

@Override
public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
    HCInit.initLoadPackageParam(lpparam); // Initialize the tool during the loadPackage phase
}
```

- If you need to use the prefs tool or the log class in the module, initialize them in the main
  module screen.

```java
public static class MainActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        HCInit.initBasicData(new BasicData()
            .setModulePackageName("com.hchen.demo") // Module package name
            .setTag("HChenDemo") // Log tag
            .setLogLevel(LOG_D) // Log level
            .setPrefsName("hchen_prefs") // Prefs storage file name. (Optional) Tip: Ensure it matches the file name in Xposed.
        );
    }
}
```

- Usage Examples

```java
public class MainTest {
    public void test() {
        CoreTool.hookMethod(/* content */); // Hook method
        CoreTool.findClass(); // Find class
        CoreTool.callMethod(); // Call method
        ChainTool.chain("com.hchen.demo", new ChainTool()
            .method("method")
            .hook()

            .method("method")
            .hook()
        ); // Chain calls
        PrefsTool.prefs().getString(); // Access shared preferences
        // ......
    }
}
```

- Alternatively, you can inherit classes packaged within this tool.
- **Strongly recommended to inherit BaseHC for use!**

```java
// Hook implementation
public class MainTest extends BaseHC {
    @Override
    public void init() {
        // BaseHC inherits CoreTool tool, so you can call methods directly.
    }

    // Optional.
    // Executed in the zygote phase.
    // Be sure to initialize HCInit.initStartupParam(startupParam) at the hook entry point.
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        Class<?> c = findClass("com.hchen.demo.Main");
        hookMethod(c, "test", new IHook() {
            /* content */
        });
    }
}

// Executor
public class RunHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        new MainTest().onLoadPackage(); // Hook at the loadPackage stage.
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        new MainTest().onZygote(); // Hook at the initZygote stage.
    }
}
```

- ProGuard configuration:

```text
// If you do not require log enhancement, add only the following rule (for classes inheriting BaseHC):
-keep class * extends com.hchen.hooktool.BaseHC
 
// If using log enhancement, add the following rules:
// Suppose hook files are in the directory com.hchen.demo.hook
// If stored in multiple directories, add each directory accordingly.
-keep class com.hchen.demo.hook.**
-keep class com.hchen.demo.hook.**$*

// If neither BaseHC inheritance nor log augmentation is used, there is no need to configure obfuscation rules.

// Other suggested configurations:
-keep class  com.hchen.hooktool.HCState {
        static boolean isEnabled;
        static java.lang.String mFramework;
        static int  mVersion;
 }
-keep class * implements android.os.Parcelable {
        public static ** CREATOR;
}
```

- Done! Enjoy using the tool!

# 💡 Chained calls

- The tool supports chained calls using the chain() method.
- This is a newly structured chaining solution introduced in the refactored version, making it more
  efficient and concise.
- Example Code:

```java
// Chain call example
public class MainTest extends BaseHC {
    public void test() {
        // Look! Isn't it clear and easy to understand?
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

# 📌 Safe Usage

- This tool is designed with a comprehensive error handling logic, aiming to prevent interruptions
  in the hook process.
- Example:

```java
public class MainTest extends BaseHC {
    public void init() {
        Class<?> c = findClass("com.hchen.demo.Demo"); // If the class cannot be retrieved, an error log is recorded and null is returned.
        hookMethod(c, "test", new IHook() { // If c is null, an error log is recorded and the hook is skipped, allowing the rest of the code to continue.
            @Override
            public void before() {
                ((Object) null).getClass(); // Even if an error occurs, it will be logged without being passed to the host app or disrupting the hook process.
            }
        });
        setStaticField("com.hchen.demo.Demo", "demo", true);
        callStaticMethod("com.hchen.demo.Demo", "isDemo", false);
        ((Object) null).getClass(); // If an error is thrown here, it will terminate the hook process, but a log will indicate the error. Please manually avoid such cases!
    }
}
```

- Ideal for scenarios with multiple hook points where process continuity is essential without being
  disrupted!

# 📌 Comprehensive and Versatile

- The tool provides a rich set of methods available for use, including:

----

- ContextTool Class:
- Facilitates easier context retrieval.

```java
public class MainTest {
    public void test() {
        // Simple way to obtain context.
        Context context = ContextTool.getContext(ContextUtils.FLAG_ALL);
    }
}
```

----

- InvokeTool Class:
- Provides more convenient and robust reflection utilities.

```java
public class MainTest {
    public void test() {
        // Reflectively calls a method; other reflection operations are similar.
        InvokeTool.callMethod(InvokeTool.findClass("com.hchen.demo.Main",
            getClass().getClassLoader()), "test", new Class[]{});
    }
}
```

----

- SystemPropTool Class:
- Provides an easy-to-use tool for reading and modifying properties.

```java
public class MainTest {
    public void test() {
        // Can set properties only in system frameworks
        SystemPropTool.setProp("ro.test.prop", "1");
        // Reading is unrestricted
        String result = SystemPropTool.getProp("ro.test.prop");
    }
}
```

---

- PrefsTool Class:
- Enables reading and modifying preferences.

```java
// In the host app
public class MainTest extends BaseHC {
    @Override
    public void init() {
      // Xprefs mode:
      // Note the xprefs mode, parasitic applications cannot modify configurations and can only read.
      String s = prefs().getString("test", "1");  // Just read it
      s = prefs("myPrefs").getString("test", "1");  // Can specify the file name for reading

      // Sprefs mode:
      // The configuration will be saved to the private directory of the parasitic application, and read from the private directory of the parasitic application.
      prefs(context).editor().putString("test", "1").commit();
      // If BaseHC is not inherited, it can be called in this way.
      PrefsTool.prefs(context).editor().putString("test", "2").commit();
      // Note that the spreads mode is independent of the xprefs mode and can coexist.

      // If it is inconvenient to obtain the context, this method can be used to asynchronously obtain the parasitic application context before setting it.
      asyncPrefs(new IAsyncPrefs() {
        @Override
        public void async(IPrefsApply sp) {
          sp.editor().put("test", "1").commit();
        }
      });
    }
}

// In the module
public static class MainActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // IMPORTANT: If using xprefs mode, initialize with PrefsTool.prefs(context) on the main module screen, or it may not function!
        PrefsTool.prefs(this); // Or
        PrefsTool.prefs(this, /* your prefs name */);

        // Usage
        prefs(this).editor().putString("test", "1").commit();
        prefs(this, "myPrefs").editor().putString("test", "1").commit();
    }
}
```

---

- ShellTool Class：
- Provide the ability to execute simple Shell commands:
- usage method:

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
            public void output(String command, String[] outputs, String exitCode) {
                IExecListener.super.output(command, outputs, exitCode);
            }
        });
        shellTool.addExecListener(new IExecListener() {
            @Override
            public void output(String command, String[] outputs, String exitCode) {
                IExecListener.super.output(command, outputs, exitCode);
            }

            @Override
            public void error(String command, String[] errors, String exitCode) {
                IExecListener.super.error(command, errors, exitCode);
            }

            @Override
            public void notRoot(String exitCode) {
                IExecListener.super.notRoot(exitCode);
            }

            @Override
            public void brokenPip(String command, String[] errors, String reason) {
                IExecListener.super.brokenPip(command, errors, reason);
            }
        });
        shellTool.close();
    }
}
```

---

- CoreTool class:
- Provides complete Hook methods!
- Fully meets your needs!

----

- DeviceTool class:
- Facilitates access to basic system information.
- Refer to source code and comments for more details.

----

- ResInjectTool Class:
- Injects module resources into the target scope.
- See source code and comments for details.

----

- PackagesTool Class:
- Quickly retrieves package information!

----

- More great tools coming soon …

# 💕 Tool Users

- The following projects use this tool:

|   Project Name   |                          Project Link                          |
|:----------------:|:--------------------------------------------------------------:|
| AppRetentionHook | [AppRetentionHook](https://github.com/HChenX/AppRetentionHook) |
|  AutoSEffSwitch  |   [AutoSEffSwitch](https://github.com/HChenX/AutoSEffSwitch)   |
|  SwitchFreeForm  |   [SwitchFreeForm](https://github.com/HChenX/SwitchFreeForm)   |
|  ForegroundPin   |    [ForegroundPin](https://github.com/HChenX/ForegroundPin)    |
|  ClipboardList   |    [ClipboardList](https://github.com/HChenX/ClipboardList)    |

- If your project uses this tool, let me know, and I’ll add it to this list.
- For more details on using this tool, check out the projects above—hope they’re helpful!

# 📢 Project Acknowledgements

- **This tool is based on:**
- [LSPosed](https://github.com/LSPosed/LSPosed)

- Please acknowledge this tool if you use it.

# 🎉 Conclusion

- Thank you for using this tool! Enjoy your day! ♥️
