# 混淆规则（当前 release 构建未开启 minify，如需开启请在 app/build.gradle.kts 中设置 isMinifyEnabled = true）
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class androidx.datastore.** { *; }
-dontwarn kotlinx.serialization.**
