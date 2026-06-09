# Atlas — ProGuard rules
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Gson
-keepattributes EnclosingMethod
-keep class com.atlas.data.model.** { *; }
