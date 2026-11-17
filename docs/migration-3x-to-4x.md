# 3.x → 4.x 迁移指引

4.0.0 是破坏性升级：坐标、包名、门面 API 全部改变。需要旧版时继续使用 3.2.2 的坐标即可。

## 1. 坐标

```groovy
// 3.x
implementation 'com.github.HChenX:HookTool:3.2.2'

// 4.x
implementation 'com.github.HChenX.HookTool:hooktool-xposed:4.0.0'
implementation 'com.github.HChenX.HookTool:hooktool-kt:4.0.0'   // Kotlin 用户追加
compileOnly     'io.github.libxposed:api:102.0.0'
```

## 2. 包结构

```
com.hchen.hooktool.config        HookConfig（原 ModuleConfig）
com.hchen.hooktool.log           AndroidLog / LogExpand
com.hchen.hooktool.host          HostEnv / Host
com.hchen.hooktool.result        ResultData / IDecomposer
com.hchen.hooktool.reflect       Reflect / ReflectEnv / ReflectException / Lookup / Throwables
com.hchen.hooktool.hook          AbsHook / Hook / HookRegistry / HookInterop
com.hchen.hooktool.hook.chain    ChainTool（ChainData、ChainType 为包级）
com.hchen.hooktool.entry         ModuleEntrance / AbsModule / ModuleData
com.hchen.hooktool.res           ResInject
com.hchen.hooktool.shell         ShellTool / ShellResult / CommandResult / ShellFailureReason / I*Listener
com.hchen.hooktool.util          Timing
com.hchen.hooktool.util.ui       UiTool / BitmapTool
com.hchen.hooktool.util.device   DeviceTool / SystemPropTool / RangeMode
com.hchen.hooktool.util.pkg      PackageTool / AppData / IAppDataGetter
com.hchen.hooktool.util.prefs    PrefsTool
com.hchen.hooktool.kt            Kotlin Hook DSL
```

`com.hchen.hooktool.data` 与 `com.hchen.hooktool.callback` 随拆分撤销，相关类型分散到各自模块。

## 3. 类映射

| 3.x | 4.x |
|---|---|
| `ModuleConfig` | `com.hchen.hooktool.config.HookConfig` |
| `ModuleEntrance` / `AbsModule` | `com.hchen.hooktool.entry.*` |
| `ModuleData` | `com.hchen.hooktool.entry.ModuleData`，外部请改用 `Host.get()` |
| `XposedLog` | `AndroidLog`，内部按需路由到 Xposed 通道 |
| `CoreHelper` | `com.hchen.hooktool.reflect.Reflect` |
| `ChainTool` / `ChainData` / `ChainType` | `com.hchen.hooktool.hook.chain.*` |
| `ResInjectTool` | `com.hchen.hooktool.res.ResInject` |
| `PrefsTool` / `UiTool` / `DeviceTool` / `PackageTool` / `SystemPropTool` / `BitmapTool` | `com.hchen.hooktool.util.*` |
| `AppData` / `RangeMode` / `IAppDataGetter` | 随各自工具类迁移 |
| `ShellTool` / `ShellResult` / `CommandResult` / `ShellFailureReason` / `IExecListener` / `ICommandListener` | `com.hchen.hooktool.shell.*` |
| `ResultData` / `IDecomposer` | `com.hchen.hooktool.result.*` |
| `core.CoreTool` | 撤销，见下表 |

## 4. CoreTool 替换表

| 3.x | 4.x |
|---|---|
| `"cls".findClass()` / `findClassIfExists` | `Reflect.findClass(...)` / `findClassIfExists(...)` |
| `"cls".findMethod(name, ...)` | `Reflect.findMethod(clazz, name, tokens...)` |
| `clazz.findAllMethod(name)` | `Reflect.findAllMethod(clazz, name)` |
| `"cls".hookMethod(name, ..., hook)` | `Hook.hookMethod(clazz, name, ..., hook)` |
| `"cls".hookConstructor(..., hook)` | `Hook.hookConstructor(clazz, ..., hook)` |
| `clazz.hookClassInitializer(hook)` | `Hook.hookClassInitializer(clazz, hook)` |
| `executable.hook(hook)` | `Hook.install(executable, hook)` |
| `returnResult(v)` / `doNothing()` / `setArg(i, v)` | `Hook.returnResult(v)` / `Hook.doNothing()` / `Hook.setArg(i, v)` |
| `obj.getField(n)` / `obj.setField(n, v)` | `Reflect.getObjectField(obj, n)` / `Reflect.setObjectField(obj, n, v)` |
| `clazz.getStaticField(n)` | `Reflect.getStaticObjectField(clazz, n)` |
| `clazz.getStaticFieldIfExists(n)` | `Reflect.getStaticObjectFieldIfExists(clazz, n)` |
| `obj.callMethod(n, ...)` | `Reflect.callMethod(obj, n, ...)` |
| `"cls".callStaticMethod(n, ...)` | `Reflect.callStaticMethod(clazz, n, ...)` |
| `"cls".newInstance(...)` | `Reflect.newInstance(clazz, ...)` |
| `getMethodInvoker` / `getConstructorInvoker` / `deoptimize*` | `HookInterop.*` |
| `createFakeResId` / `setResReplacement` / `setDensityReplacement` / `setObjectReplacement` | `ResInject.*` |
| `prefs(...)` | `PrefsTool.prefs(...)` |
| `getStackTrace()` / `timeConsumption { }` | `LogExpand.getStackTrace()` / `Timing.measure { }` |
| `throwIt()` | `Throwables.throwIt(t)` |
| `getParameterTypes(...)` | `Reflect` 内部处理，通常不再需要 |

## 5. AbsModule 不再继承 CoreTool

3.x 的 `AbsModule` 继承 `CoreTool`，业务类因此拥有一个免 import 的全局工具作用域。4.x 移除了这层继承：

```java
// 3.x
public class MyHook extends AbsModule {
    void install() {
        hookMethod("com.x.Y", "z", String.class, hook);
        setField(obj, "field", 1);
    }
}

// 4.x
import static com.hchen.hooktool.hook.Hook.hookMethod;
import static com.hchen.hooktool.reflect.Reflect.setObjectField;

public class MyHook extends AbsModule {
    void install() {
        hookMethod(Reflect.findClass("com.x.Y"), "z", String.class, hook);
        setObjectField(obj, "field", 1);
    }
}
```

Kotlin 用户改用 DSL：

```kotlin
hook("com.x.Y") {
    method("z", String::class) { before { } }
}
```

## 6. 异常类型

反射失败统一为 `com.hchen.hooktool.reflect.ReflectException`，通过 `kind()` 区分原因：

```java
try {
    Reflect.findMethod(clazz, "gone");
} catch (ReflectException e) {
    switch (e.kind()) {
        case METHOD_NOT_FOUND: ...
        case PARAM_MISMATCH:   ...
        default:               ...
    }
}
```

需要不抛异常、带原因的写法时用 `Reflect.lookupMethod(...)` 返回 `Lookup<Method>`。

## 7. 默认 ClassLoader

`ModuleEntrance` 会自动调用 `ReflectEnv.setDefaultClassLoader(...)`。在框架外单独使用 `hooktool-reflect` 时请自行设置，否则回落到线程上下文 ClassLoader。

## 8. 常见问题

Q: 升级后 `getArgs()[0] = x` 不生效？

A: `getArgs()` 返回副本，只提供 `setArg(index, value)`。DSL 里同样只有 `setArg`。

Q: `before` 里设置结果后 `after` 不执行？

A: 这是引擎语义，`before` 中调用 `setResult` 会同时跳过原方法与 `after`。要保留 `after`，用 `proceed { v }`。

Q: 依赖里还有 `kotlin-stdlib`？

A: 只可能来自 `hooktool-kt` 或你自己的 Kotlin 代码。6 个 Java 模块的 POM 已由构建门禁保证无 Kotlin 依赖。
