# Consumer rules applied to every module that depends on hooktool-xposed.
-keep class * extends com.hchen.hooktool.entry.ModuleEntrance
-keep class * extends com.hchen.hooktool.entry.AbsModule

-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

-dontwarn io.github.libxposed.annotation.**
