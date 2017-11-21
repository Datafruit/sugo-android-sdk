# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Android\android_sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

#保护注解
-keepattributes *Annotation*
#不混淆资源类
-keepclassmembers class **.R$* {
    public static <fields>;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepattributes *JavascriptInterface*

-keep class io.sugo.* { *; }
-keep class io.sugo.android.mpmetrics.SugoAPI { *; }
-keep class io.sugo.android.mpmetrics.SugoWebEventListener { *; }
-keep class io.sugo.android.mpmetrics.SugoWebNodeReporter { *; }