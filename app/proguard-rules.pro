# ==============================================================================
# PROGUARD / R8 OPTIMIZATION, OBFUSCATION & SHRINKING RULES
# ==============================================================================

# Enable access modification to permit aggressive class merging, inlining, and dead-code elimination
-allowaccessmodification

# Repackage all obfuscated classes into the root package to maximize obfuscation percentage
-repackageclasses ''

# Preserve source file and line numbers for crash reporting and mapping de-obfuscation
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod,MethodParameters

# ------------------------------------------------------------------------------
# 1. R8 Optimization & Logging Cleanup
# ------------------------------------------------------------------------------
# Strip verbose, debug, and info logs in release builds to eliminate dead code and improve security.
# Warning and error logs (Log.w, Log.e) are retained for critical diagnostics.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ------------------------------------------------------------------------------
# 2. Gson Serialization & Deserialization
# ------------------------------------------------------------------------------
# Preserve enum values and valueOf for enum serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve fields annotated with @SerializedName, while allowing class name obfuscation and method shrinking
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Preserve custom JsonDeserializers and JsonSerializers used in the project
-keep,allowobfuscation class * implements com.google.gson.JsonDeserializer {
    public <init>();
    <methods>;
}
-keep,allowobfuscation class * implements com.google.gson.JsonSerializer {
    public <init>();
    <methods>;
}
-keep,allowobfuscation class com.example.data.remote.SurahListDeserializer {
    public <init>();
    <methods>;
}

# ------------------------------------------------------------------------------
# 3. Moshi Serialization & Data Models
# ------------------------------------------------------------------------------
# Preserve Moshi runtime library classes and interfaces
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }

# Preserve classes annotated with @JsonClass and their constructors and fields for serialization
-keep,allowobfuscation @com.squareup.moshi.JsonClass class * {
    <init>(...);
    <fields>;
}
-keepclassmembers,allowobfuscation class * {
    @com.squareup.moshi.Json <fields>;
}

# Preserve generated Moshi adapters (**JsonAdapter matches in any package)
-keep class **JsonAdapter {
    public <init>(...);
    *;
}
-keep class * extends com.squareup.moshi.JsonAdapter { *; }

# Moshi Kotlin Metadata
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-dontwarn javax.annotation.**

# Data models and DTOs: allow obfuscation of class names and methods, keep fields for reflection/parsing
-keepclassmembers,allowobfuscation class com.example.data.model.** {
    <init>(...);
    <fields>;
}
-keepclassmembers,allowobfuscation class com.example.prayer.model.** {
    <init>(...);
    <fields>;
}
-keepclassmembers,allowobfuscation class com.example.haram.** {
    <init>(...);
    <fields>;
}
-keepclassmembers,allowobfuscation class com.example.watch.** {
    <init>(...);
    <fields>;
}
-keepclassmembers,allowobfuscation class com.example.data.provider.Archive** {
    <init>(...);
    <fields>;
}

# ------------------------------------------------------------------------------
# 4. Room Database Local Persistence
# ------------------------------------------------------------------------------
# Preserve RoomDatabase subclass constructors for Room.databaseBuilder reflection
-keep class * extends androidx.room.RoomDatabase {
    public <init>();
}

# Preserve Room Entity fields so SQLite column mapping remains intact
-keepclassmembers @androidx.room.Entity class * {
    <fields>;
}

# Preserve Room DAO interfaces
-keep @androidx.room.Dao interface * {
    <methods>;
}

# Preserve TypeConverters methods
-keepclassmembers class com.example.data.local.db.QuranTypeConverters {
    public <methods>;
}

# ------------------------------------------------------------------------------
# 5. Retrofit HTTP API Interfaces
# ------------------------------------------------------------------------------
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ------------------------------------------------------------------------------
# 6. Google Play In-App Updates
# ------------------------------------------------------------------------------
-keep class com.google.android.play.core.appupdate.** { *; }
-keep class com.google.android.play.core.install.** { *; }

# ------------------------------------------------------------------------------
# 7. Media3 / ExoPlayer Session Service
# ------------------------------------------------------------------------------
-keepclassmembers class androidx.media3.session.MediaSessionService {
    public <methods>;
}

