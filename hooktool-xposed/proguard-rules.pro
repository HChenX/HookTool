# 模块入口由框架按类名实例化，混淆后必须保留。
-keep class * extends com.hchen.hooktool.entry.ModuleEntrance

# 框架要求模块类保持可实例化的公开无参构造。
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# 入口类被重命名时同步改写清单里的类名。
-adaptresourcefilecontents META-INF/xposed/java_init.list

# libxposed 的注解仅在编译期可见。
-dontwarn io.github.libxposed.annotation.**
