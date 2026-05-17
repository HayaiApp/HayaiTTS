# Keep TextToSpeechService implementation; the framework instantiates it by name.
-keep class dev.ahmedmohamed.hayaitts.tts.HayaiTtsService { *; }

# ---------------------------------------------------------------------------
# sherpa-onnx JNI bindings
# ---------------------------------------------------------------------------
# The native side reads field names from these classes via JNI reflection.
# Stripping any of them = NoSuchFieldError at runtime, with a stack trace that
# never mentions ProGuard. Keep the entire package + every nested config
# data class, plus the `external fun` JNI entry points.
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keepclassmembers class com.k2fsa.sherpa.onnx.** { *; }
# Also keep any *Config nested data class shape — the native side reads
# `OfflineTtsKokoroModelConfig.voices`, `OfflineTtsConfig.silenceScale`, etc
# by name even when the AAR ships them.
-keep class **OfflineTts*Config { *; }
-keepclassmembers class **OfflineTts*Config { *; }

# ---------------------------------------------------------------------------
# Koin (reflective injection)
# ---------------------------------------------------------------------------
# ViewModels are resolved by class literal; keep them all so `viewModel<T>()`
# inside composables still finds the matching factory after minification.
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# ---------------------------------------------------------------------------
# Room
# ---------------------------------------------------------------------------
# Room generates `*_Impl` subclasses next to each @Database / @Dao. The
# default AGP rules cover most of this, but we add belt-and-braces for the
# entity + dao surface to keep the schema export reflective lookups working.
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase$Builder { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep class **_Impl { *; }

# ---------------------------------------------------------------------------
# kotlinx.serialization
# ---------------------------------------------------------------------------
# Catalog JSON decoding uses generated `$$serializer` companions. Without these
# rules R8 inlines the companion + drops the synthetic serializer, breaking
# CatalogRepositoryImpl on first parse.
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Project-specific @Serializable surface (catalog + speakers list).
-keep @kotlinx.serialization.Serializable class dev.ahmedmohamed.hayaitts.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class dev.ahmedmohamed.hayaitts.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# WorkManager
# ---------------------------------------------------------------------------
# JobScheduler-backed worker; AGP usually keeps this but the rule is cheap.
-keep class androidx.work.impl.background.systemjob.SystemJobService { *; }
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ---------------------------------------------------------------------------
# Kotlin coroutines (debug agent reflective field access)
# ---------------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
