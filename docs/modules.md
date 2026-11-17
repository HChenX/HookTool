# 模块与依赖

HookTool 4.0.0 按能力拆分为多个库，按需引入即可。

## 模块列表

| 模块 | 语言 | 依赖 | 用途 |
|---|---|---|---|
| `hooktool-reflect` | Java | 无 | 反射查找与调用，纯 JVM |
| `hooktool-common` | Java | 无 | 配置、日志、宿主环境接口 |
| `hooktool-xposed` | Java | reflect、common、libxposed | Hook 引擎与模块入口 |
| `hooktool-res` | Java | xposed、reflect | 资源注入 |
| `hooktool-shell` | Java | common | Shell 命令执行 |
| `hooktool-utils` | Java | reflect、common | 设备、包、配置、界面工具 |
| `hooktool-kt` | Kotlin | xposed、reflect | Kotlin Hook DSL |
| `hooktool-bom` | — | — | 版本对齐 |

只有 `hooktool-kt` 会引入 Kotlin 运行时。

## 依赖方向

```
hooktool-kt ──> hooktool-xposed ──> hooktool-common（api）
      │                  └────────> hooktool-reflect（implementation）
      └─> hooktool-reflect（api）

hooktool-res   ──> hooktool-xposed + hooktool-reflect + hooktool-common
hooktool-shell ──> hooktool-common
hooktool-utils ──> hooktool-reflect + hooktool-common
```

## 依赖边界

| 模块 | 直接依赖 | 原因 |
|---|---|---|
| `hooktool-xposed` | common、reflect | 日志与配置属于模块编写契约；反射仅内部使用 |
| `hooktool-res` | xposed、reflect、common | 需要安装资源 Hook、调用反射、写日志 |
| `hooktool-shell` | common | 只需要日志 |
| `hooktool-utils` | reflect、common | 需要反射读取系统字段，并写日志 |
| `hooktool-kt` | xposed、reflect | DSL 只用到 Hook API 与反射，工具库按需另行引入 |

`hooktool-kt` 不引入 res、shell、utils，需要这些能力时单独添加。

## 按场景选择

| 场景 | 依赖 |
|---|---|
| 编写完整 Xposed 模块 | `hooktool-xposed` + `hooktool-reflect` |
| 编写 Kotlin 模块 | `hooktool-kt` |
| 只做反射调用 | `hooktool-reflect` |
| 只执行 Shell 命令 | `hooktool-shell` |
| 只读取设备或包信息 | `hooktool-utils` |
| 只做资源替换 | `hooktool-res` |

## 坐标

```groovy
implementation 'com.github.HChenX.HookTool:hooktool-xposed:4.0.0'
implementation 'com.github.HChenX.HookTool:hooktool-kt:4.0.0'
compileOnly     'io.github.libxposed:api:102.0.0'

// 或统一版本
implementation platform('com.github.HChenX.HookTool:hooktool-bom:4.0.0')
```

`io.github.libxposed:api` 需由使用方自行声明为 `compileOnly`，运行期由框架提供。

每个模块同时发布 `-sources.jar`，在 IDE 中可直接查看源码。
