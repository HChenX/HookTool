# Consumer rules for every module that depends on hooktool-common.
#
# LogExpand derives log tags from stack traces, so line information must survive shrinking.
-keepattributes SourceFile,LineNumberTable

# androidx.annotation is a compileOnly dependency: the annotations are present in class files but
# not on the runtime classpath of consumers.
-dontwarn androidx.annotation.**
