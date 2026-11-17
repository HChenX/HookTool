# 模块入口与模块基类由框架按类名实例化，混淆后必须保留。
-keep class * extends com.hchen.hooktool.entry.ModuleEntrance
-keep class * extends com.hchen.hooktool.entry.AbsModule

# 框架要求模块类保持可实例化的公开无参构造。
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# libxposed 的注解仅在编译期可见。
-dontwarn io.github.libxposed.annotation.**
