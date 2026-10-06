# Release rules for the playback engine, adapted from Metrolist (GPL-3.0)

# Methods the token page calls from JavaScript
-keepclassmembers class com.wavvy.app.core.playback.potoken.PoTokenWebView {
    @android.webkit.JavascriptInterface public *;
}

# Ktor finds its engine and its serializers at runtime
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# JavaScript engine that solves the signatures of YouTube, called from native code
-keep class com.dokar.quickjs.** { *; }

# Serializers of the models of the stream extraction library
-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclasseswithmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclasseswithmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Optional classes that OkHttp and Ktor look for and Android does not have
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn java.lang.management.**
