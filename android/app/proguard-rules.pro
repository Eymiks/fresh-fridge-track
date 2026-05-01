# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.freshtrack.**$$serializer { *; }
-keepclassmembers class com.freshtrack.** {
    *** Companion;
}
-keepclasseswithmembers class com.freshtrack.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# supabase-kt / ktor
-keep class io.github.jan.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Retrofit / OkHttp (si transitif)
-dontwarn okhttp3.**
-dontwarn okio.**
