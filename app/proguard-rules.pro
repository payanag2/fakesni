# Keep Android components registered in the manifest
-keep class com.freenet.fakesni.SniService { *; }
-keep class com.freenet.fakesni.MainActivity { *; }

# Keep data/prefs classes (accessed by name in SharedPreferences)
-keep class com.freenet.fakesni.SniConfig { *; }
-keep class com.freenet.fakesni.SniPreferences { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Kotlin metadata (needed for reflection-free data classes)
-keepattributes *Annotation*, InnerClasses, Signature, SourceFile, LineNumberTable

# Remove verbose logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
