# LogExpand 从堆栈推导日志标签，混淆时必须保留行号信息。
-keepattributes SourceFile,LineNumberTable

# androidx.annotation 为 compileOnly 依赖，使用方的运行期不存在这些注解类。
-dontwarn androidx.annotation.**
