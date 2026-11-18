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

`ModuleEntrance` 负责分发框架回调、注入类加载器与处理热更新；回调不做去重，同一包重复触发时需要自行保证安装逻辑幂等。

## 业务分包

继承 `AbsModule` 可按类拆分业务，每个实例独立管理自己的生命周期：

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

`Hook` 是按名称安装 Hook 的门面，`hookMethod`、`hookConstructor` 的可变参数末尾固定为 `AbsHook` 实例，类名与 `Class` 两种写法等价。

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

已持有 `Method` 或 `Constructor` 时用 `Hook.hook(executable, hook)` 直接安装；`hookAllMethod`、`hookAllConstructor`、`hookClassInitializer` 同样支持类名写法。

目标不存在时使用 `hookMethodIfExists` 与 `hookConstructorIfExists`，返回 `null` 而不抛异常。

## Hook 生命周期

| 阶段 | 说明 |
|---|---|
| `before()` | 原方法执行前，可修改参数或提前返回 |
| `proceed(chain)` | 调用原方法，可自定义调用参数 |
| `after()` | 原方法返回后，可替换返回值 |
| `onThrow(stage, e)` | 任一阶段出错时回调，返回 `true` 表示异常已消费 |

两条容易混淆的语义：

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
Method method = Reflect.findMethodExact(Target.class, "greet", String.class);
XposedInterface.Invoker<?, Method> invoker = HookInterop.getMethodInvoker(method);
invoker.setType(XposedInterface.Invoker.Type.ORIGIN);
Object result = invoker.invoke(target, "raw");

HookInterop.deoptimize(method);

// 按类与参数类型直接取调用器
XposedInterface.CtorInvoker<?> ctorInvoker =
    HookInterop.getConstructorInvoker(Target.class, String.class);
```

## 反射

`Reflect` 提供类、字段、方法、构造函数的查找与调用，查找结果按类缓存。

```java
Class<?> clazz = Reflect.findClass("com.example.Target");
Method method = Reflect.findMethodExact(clazz, "greet", String.class);
Object value = Reflect.callMethod(target, "greet", "world");
Reflect.setObjectField(target, "secret", "changed");
```

查找、构造、字段访问与静态调用都同时接受 `Class` 与类名，类名写法会顺带完成解析：

```java
Method method = Reflect.findMethodExact("com.example.Target", loader, "greet", String.class);
Field field = Reflect.findField("com.example.Target", loader, "secret");
Target instance = (Target) Reflect.newInstance("com.example.Target", loader, 1, 2);
Object created = Reflect.callStaticMethod("com.example.Target", loader, "create", "arg");
```

已持有 `Method`、`Constructor` 或 `Field` 时直接传入即可，不必再按名称查一次：

```java
Reflect.callMethod(method, target, "world");
Reflect.callStaticMethod(staticMethod, 1, 2);
Target instance = (Target) Reflect.newInstance(constructor, 1, 2);
Object secret = Reflect.getObjectField(field, target);
Reflect.setObjectField(field, target, "changed");
```

查找失败时抛出 `ReflectException`，可通过 `kind()` 区分原因。需要不抛异常的写法时使用 `Reflect.lookupMethod` 等 `lookup` 系列。

同一套约定贯穿 `Hook`：既可按类名或 `Class` 描述目标，也可直接传入已解析的 `Executable`。
