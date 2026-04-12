<div align="center">
<h1>HookTool</h1>

![stars](https://img.shields.io/github/stars/HChenX/HookTool?style=flat)
![Github repo size](https://img.shields.io/github/repo-size/HChenX/HookTool)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/HChenX/HookTool)](https://github.com/HChenX/HookTool/releases)
[![GitHub Release Date](https://img.shields.io/github/release-date/HChenX/HookTool)](https://github.com/HChenX/HookTool/releases)
![last commit](https://img.shields.io/github/last-commit/HChenX/HookTool?style=flat)
![language](https://img.shields.io/badge/language-java-purple)

[//]: # (<p><b><a href="README-en.md">English</a> | <a href="README.md">简体中文</a></b></p>)
<p>简易的 Hook 工具，旨在降低 Hook 编写复杂度</p>
</div>

# 🔧 使用方法

#### 1. 向项目 settings.gradle 文件添加如下代码

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

#### 2. 向项目 app 内 build.gradle 文件添加如下代码

```groovy
dependencies {
    implementation 'com.github.HChenX:HookTool:3.0.0'
}
```

#### 3. 同步项目，下载依赖后即可使用

# 💕 以下项目使用了本工具

|      项目名称       |                             项目链接                             |
|:---------------:|:------------------------------------------------------------:|
|  AppRetention   |    [AppRetention](https://github.com/HChenX/AppRetention)    |
| AutoSEffSwitch  |  [AutoSEffSwitch](https://github.com/HChenX/AutoSEffSwitch)  |
| SwitchFreeForm  |  [SwitchFreeForm](https://github.com/HChenX/SwitchFreeForm)  |
|  ForegroundPin  |   [ForegroundPin](https://github.com/HChenX/ForegroundPin)   |
|  ClipboardList  |   [ClipboardList](https://github.com/HChenX/ClipboardList)   |
| SplitScreenPlus | [SplitScreenPlus](https://github.com/HChenX/SplitScreenPlus) |
|   SuperLyric    |      [SuperLyric](https://github.com/HChenX/SuperLyric)      |

# 🎉 结语

- 感谢您使用本工具！Enjoy your day! ♥️
