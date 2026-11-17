# Java 使用指南

## 引入依赖

```groovy
implementation 'com.github.HChenX.HookTool:hooktool-xposed:4.0.0'
implementation 'com.github.HChenX.HookTool:hooktool-reflect:4.0.0'
compileOnly     'io.github.libxposed:api:102.0.0'
```

`hooktool-xposed` 已带上 `hooktool-common`，可直接使用 `HookConfig` 与 `AndroidLog`。

## 模块入口

继承 `ModuleEntrance`，在 `loadConfig` 中完成全局配置：

```java
public class MyEntrance extends ModuleEntrance {
    @Override
    public void loadConfig() {
        HookConfig.setLogTag("MyModule");
        HookConfig.setLogLevel(HookConfig.LOG_D);
        HookConfig.setPrefsName("my_module_prefs");
    }

    @Override
    public void handlePackageReady(@NonNull PackageReadyParam param) {
        MyHooks.install();
    }
}
```

`ModuleEntrance` 负责把框架回调分发给业务代码，并处理类加载器注入与热更新。

## 业务分包

继承 `AbsModule` 可以把业务按类拆分，每个实例独立管理自己的生命周期：

```java
public class MyHooks extends AbsModule {
    @Override
    protected void onPackageReady(@NonNull XposedModuleInterface.PackageReadyParam param) {
        install();
    }

    @Override
    protected void onHotReloaded(@NonNull XposedModuleInterface.HotReloadedParam param) {
        install();
    }
}
```

## 安装 Hook

`Hook` 是安装门面，参数列表的最后一个元素固定为 `AbsHook` 实例。

```java
import static com.hchen.hooktool.hook.Hook.hookMethod;

// 方法
hookMethod(Target.class, "greet", String.class, new AbsHook() {
    @Override
    public void before() {
        setArg(0, "hooktool");
    }

    @Override
    public void after() {
        AndroidLog.logD("Demo", "返回值: " + getResult());
    }
});

// 构造函数
hookConstructor(Target.class, String.class, new AbsHook() { });

// 静态初始化块
hookClassInitializer(Target.class, new AbsHook() { });

// 同名方法全部安装
hookAllMethod(Target.class, "describe", new AbsHook() { });
```

常用工厂：

```java
Hook.returnResult(100);   // 跳过原方法并返回指定值
Hook.doNothing();         // 跳过原方法并返回 null
Hook.setArg(0, "value");  // 替换指定参数
```

目标不存在时使用 `hookMethodIfExists`，返回 `null` 而不抛异常。

## Hook 生命周期

| 阶段 | 说明 |
|---|---|
| `before()` | 原方法执行前，可修改参数或提前返回 |
| `proceed(chain)` | 调用原方法，可自定义调用参数 |
| `after()` | 原方法返回后，可替换返回值 |
| `onThrow(stage, e)` | 任一阶段出错时回调，返回 `true` 表示异常已消费 |

语义差异：

| 写法 | 原方法 | `after` |
|---|---|---|
| `before` 中调用 `setResult` | 不执行 | 不执行 |
| 覆写 `proceed` 直接返回 | 不执行 | 执行 |

## 链式调用

```java
ChainTool.buildChain(Target.class)
    .findMethod("getSecret")
    .returnResult("hooked");

ChainTool.buildChain(Target.class)
    .findConstructor(String.class)
    .hook(myHook);
```

## 调用器与去优化

```java
Method method = Reflect.findMethod(Target.class, "greet", String.class);
Invoker<?, Method> invoker = HookInterop.getMethodInvoker(method);
invoker.setType(Invoker.Type.ORIGIN);
Object result = invoker.invoke(target, "raw");

HookInterop.deoptimize(method);
```

## 反射

`Reflect` 提供类、字段、方法、构造函数的查找与调用，查找结果按类缓存。

```java
Class<?> clazz = Reflect.findClass("com.example.Target");
Method method = Reflect.findMethod(clazz, "greet", String.class);
Object value = Reflect.callMethod(target, "greet", "world");
Reflect.setObjectField(target, "secret", "changed");
```

查找失败时抛出 `ReflectException`，可通过 `kind()` 区分原因；需要不抛异常的写法时使用 `Reflect.lookupMethod` 等 `lookup` 系列。