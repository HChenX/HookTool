<div align="center">
<h1>HookTool</h1>

![stars](https://img.shields.io/github/stars/HChenX/HookTool?style=flat)
![Github repo size](https://img.shields.io/github/repo-size/HChenX/HookTool)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/HChenX/HookTool)](https://github.com/HChenX/HookTool/releases)
[![GitHub Release Date](https://img.shields.io/github/release-date/HChenX/HookTool)](https://github.com/HChenX/HookTool/releases)
![last commit](https://img.shields.io/github/last-commit/HChenX/HookTool?style=flat)
![language](https://img.shields.io/badge/language-java%20%2F%20kotlin-purple)

<p>简易的 Hook 工具，旨在降低 Hook 编写复杂度</p>
<p><b>4.0.0 起按能力拆分为多个组件库，按需引入即可。</b></p>
</div>

### 📦 组件库

| 坐标 | 语言 | 依赖 | 用途 |
|---|---|---|---|
| `hooktool-reflect` | Java | 无 | 反射内核，纯 JVM，可在任何项目使用 |
| `hooktool-common` | Java | 无 | 配置、日志、HostEnv SPI、ResultData |
| `hooktool-xposed` | Java | common（`api`）、reflect（`implementation`）、`compileOnly` libxposed | Hook 引擎 + 模块入口生命周期 |
| `hooktool-res` | Java | xposed, reflect | 资源注入 |
| `hooktool-shell` | Java | common | Shell 会话与命令 |
| `hooktool-utils` | Java | reflect, common | 设备/ROM、UI 尺寸、包信息、跨进程配置 |
| `hooktool-kt` | **Kotlin** | `hooktool-xposed`、`hooktool-reflect` | Hook DSL，语法糖层 |
| `hooktool-bom` | — | — | 版本对齐 |

只有 `hooktool-kt` 携带 `kotlin-stdlib`。其余 6 个库是纯 Java 产物，**POM 中不含任何 Kotlin 依赖**（由构建门禁强制保证）。

### 🔧 使用方法

#### 1. 添加仓库

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
        maven { url "https://api.xposed.info/" }
    }
}
```

#### 2. 按需引入

```groovy
dependencies {
    // 写完整的 Xposed 模块：入口 + Hook 引擎（自动带入 common）
    implementation 'com.github.HChenX.HookTool:hooktool-xposed:4.0.0'
    implementation 'com.github.HChenX.HookTool:hooktool-reflect:4.0.0'

    // Kotlin 用户改用语法糖层，它会带上 xposed、reflect 与 common
    // implementation 'com.github.HChenX.HookTool:hooktool-kt:4.0.0'

    // 其余按需挑选
    // implementation 'com.github.HChenX.HookTool:hooktool-res:4.0.0'
    // implementation 'com.github.HChenX.HookTool:hooktool-shell:4.0.0'
    // implementation 'com.github.HChenX.HookTool:hooktool-utils:4.0.0'

    compileOnly 'io.github.libxposed:api:102.0.0'
}
```

或统一版本：

```groovy
implementation platform('com.github.HChenX.HookTool:hooktool-bom:4.0.0')
```

#### 3. 写 Hook

**Java**

```java
import static com.hchen.hooktool.hook.Hook.hookMethod;
import com.hchen.hooktool.hook.AbsHook;
import com.hchen.hooktool.reflect.Reflect;

hookMethod(Reflect.findClass("com.example.Target"), "check",
    Context.class, int.class, new AbsHook() {
        @Override public void before() {
            if (mEnabled) setResult(true);
        }
    });
```

**Kotlin（DSL）**

```kotlin
import com.hchen.hooktool.kt.hook
import com.hchen.hooktool.kt.resultAs
import com.hchen.hooktool.kt.thisAs

hook("com.example.Target") {
    method("check", Context::class, Int::class) {
        before { if (enabled) skipWith(true) }
        after  { result = resultAs<Boolean>()?.not() }
    }
    constructor(Context::class) {
        after { thisAs<Target>()?.init() }
    }
}
```

> 只想用反射工具、不写 Xposed 模块？只引入 `hooktool-reflect` 即可，它不依赖 Android、libxposed 或 Kotlin。

### 🧱 工程结构

```
build-logic/        约定插件（统一 compileSdk / Java 21 / 发布 / 门禁）
hooktool-reflect/   纯 JVM 反射内核（java-library）
hooktool-common/    配置 + 日志 + HostEnv
hooktool-xposed/    Hook 引擎与模块生命周期
hooktool-res/       资源注入
hooktool-shell/     Shell
hooktool-utils/     设备 / UI / 包 / 配置
hooktool-kt/        全新 Kotlin Hook DSL
hooktool-bom/       版本对齐
app/                Java + Kotlin 双示例
app-java/           纯 Java 消费方（CI 门禁）
docs/               设计文档与迁移指引
```

### ✅ 构建与验证

```bash
./gradlew build                                  # 全量构建 + 单元测试 + 门禁
./gradlew :hooktool-reflect:test                 # 反射内核单测
./gradlew :hooktool-xposed:testDebugUnitTest     # Hook 状态机单测
./gradlew :hooktool-kt:testDebugUnitTest         # DSL 声明期单测
./gradlew :app-java:assembleDebug                # 纯 Java 消费方
```

构建内置三条门禁：非 `-kt` 模块不得出现 `.kt` 文件；`hooktool-reflect` 的运行期依赖不得包含 Android 或 libxposed；非 `-kt` 模块的 POM 不得包含 Kotlin 依赖。

### 📚 文档

- [模块与依赖](docs/modules.md)
- [Java 使用指南](docs/java-usage.md)
- [Kotlin DSL 使用指南](docs/kotlin-dsl.md)
- [工具库](docs/utilities.md)
- [3.x → 4.x 迁移指引](docs/migration-3x-to-4x.md)

### 🎉 结语

- 感谢您使用本工具！Enjoy your day! ♥️