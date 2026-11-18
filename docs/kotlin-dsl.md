# Kotlin DSL 使用指南

## 引入依赖

```groovy
implementation 'com.github.HChenX.HookTool:hooktool-kt:4.0.0'
compileOnly     'io.github.libxposed:api:102.0.0'
```

`hooktool-kt` 已带上 `hooktool-xposed`、`hooktool-reflect` 与 `hooktool-common`，无需重复声明。

## 基本用法

```kotlin
import com.hchen.hooktool.kt.hook

hook("com.example.Target") {
    method("greet", String::class) {
        before { setArg(0, "hooktool") }
        after { AndroidLog.logD("Demo", "返回值: " + result) }
    }
}
```

`hook` 返回 `HookGroup`，可读取安装结果或整体卸载：

```kotlin
val group = hook("com.example.Target") { method("greet", String::class) { } }
group.report().missing.forEach { AndroidLog.logW("Demo", "未安装: " + it.target) }
group.unhook()
```

目标类也可以直接传入 `Class`，或额外指定类加载器。

## 声明成员

| 声明 | 说明 |
|---|---|
| `method(name, vararg params)` | 精确参数匹配的方法 |
| `constructor(vararg params)` | 构造函数 |
| `classInitializer` | 静态初始化块 |
| `allMethods` | 全部已声明方法 |
| `methods(predicate)` | 满足条件的方法 |

参数类型可以是 `KClass`、`Class` 或类名字符串：

```kotlin
hook(MyTarget::class.java) {
    method("load", String::class, Int::class) { }
    method("parse", "java.lang.String") { }
}
```

## 阶段回调

```kotlin
method("sum", Int::class, Int::class) {
    before {
        setArg(0, 41)
    }
    proceed {
        (proceed() as? Int ?: 0) * 2
    }
    after {
        AndroidLog.logD("Demo", "结果: " + result)
    }
    onThrow {
        recoverWith(-1)
        true
    }
}
```

跳过原方法的方式：

| 写法 | 原方法 | `after` |
|---|---|---|
| `before { skip() }` | 不执行 | 不执行 |
| `before { skipWith(v) }` | 不执行 | 不执行 |
| `proceed { v }` | 不执行 | 执行 |

## 参数与数据

```kotlin
method("greet", String::class) {
    before {
        val name = argAs<String>(0)
        data = System.nanoTime()
    }
    after {
        val cost = System.nanoTime() - (data as Long)
        val target = thisAs<MyTarget>()
    }
}
```

`data` 属于单次调用，重入与并发调用互不影响。

## 缺失处理

```kotlin
hook("com.example.Target") {
    onMissing(MissingPolicy.REPORT)
    method("maybeAbsent") { optional() }
}
```

| 策略 | 行为 |
|---|---|
| `STRICT` | 抛出异常，默认 |
| `IGNORE` | 跳过该成员 |
| `REPORT` | 跳过并记录到 `report()` |

单个成员可以用 `optional()` 单独放宽。

## 批量安装

```kotlin
hook(MyTarget::class.java) {
    methods({ method -> method.name.startsWith("on") }) {
        before { AndroidLog.logD("Demo", method.name) }
    }
}
```

## 热更新

```kotlin
hook(MyTarget::class.java) {
    var count = 0
    method("greet") { before { count++ } }
    onReloading { state -> state["count"] = count }
    onReloaded { state -> count = (state["count"] as? Int) ?: 0 }
}
```

## 作用域隔离

所有作用域共用 `@DslMarker`，内层不能隐式调用外层的成员方法：

```kotlin
hook("com.example.Target") {
    method("a") {
        before {
            // method("b") { } 无法编译
        }
    }
}
```

需要在回调中再装一个类级别的 Hook 时，直接调用顶层 `hook` 即可。

## 已知限制

- 方法名没有编译期校验，改名只在运行期暴露。
- 不支持以 `KFunction` 引用声明目标，避免引入 `kotlin-reflect`。
- `suspend` 函数需要显式匹配 `Continuation` 参数，挂起时 `result` 为 `COROUTINE_SUSPENDED`。
