# The recommended Proguard from AppSee Documentation
-keep class com.appsee.** { *; }
-dontwarn com.appsee.**
#-keep class android.support.** { *; }
#-keep interface android.support.** { *; }
#-keep class androidx.** { *; }
#-keep interface androidx.** { *; }
-keepattributes SourceFile,LineNumberTable
