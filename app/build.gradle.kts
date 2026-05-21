import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Pin Kotlin + Java toolchain to JDK 21 so the build is reproducible regardless
// of which JDK happens to be on PATH. Kotlin 2.1.x cannot parse JDK 26's
// version string, so without this the build crashes when JDK 26 is the system
// default (Android Studio ships JBR 21, which is the canonical target).
kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}

android {
    namespace = "dev.ahmedmohamed.hayaitts"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.ahmedmohamed.hayaitts"
        minSdk = 26
        targetSdk = 36
        versionCode = 202
        versionName = "2.0.0-b3"

        // Room schema export. KSP picks this up via the `room` argument and
        // writes JSON snapshots of each entity into app/schemas/. Schemas are
        // tracked in git so migrations stay reviewable.
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        // Only ship native ABIs we actually care about. arm64-v8a covers every
        // modern phone, armeabi-v7a covers the long tail of 32-bit ARM devices,
        // x86_64 lets the app run in the Android Studio emulator. Shipping x86
        // (32-bit) would bloat the APK with binaries no real user runs.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    // Release signing reads credentials from `signing.properties` at the repo
    // root (gitignored). When the file is absent the release variant emits
    // UNSIGNED APKs — CI's signer step (`build_push.yml` →
    // `null2264/actions/android-signer`) attaches the production key from
    // GitHub secrets. See `signing.properties.template` for the local-dev
    // path that wires a real keystore into ./gradlew assembleRelease.
    val signingProps = Properties().apply {
        val f = rootProject.file("signing.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    if (signingProps.getProperty("storeFile") != null) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            // R8 + resource shrinker stay on regardless of signing source.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Use the real release key when `signing.properties` is present;
            // otherwise produce unsigned APKs (CI signs them post-build).
            signingConfig = signingConfigs.findByName("release")
        }
    }

    // Per-ABI release APKs cut the universal 130+ MB binary down to ~50–75 MB
    // by stripping the native libraries for the other architectures. Universal
    // is kept on for sideloading from a single asset.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    buildFeatures {
        compose = true
        // BuildConfig is needed by SettingsActivity to display the version name.
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // Note: kotlinOptions DSL was deprecated in Kotlin 2.2 — switched to the
    // Kotlin Gradle Plugin compilerOptions DSL at the top of the file.

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Suppress the lint version-bump advisories. The "newer version available"
    // checks here all point at versions we have already decided not to take:
    // AGP 9.x conflicts with the standalone kotlin-android plugin,
    // material3 alpha20+ transitively requires AGP 9.x, Kotlin 2.3.21 has no
    // matching KSP build, and okhttp 5.x is a major API migration. The
    // rationale is documented in gradle/libs.versions.toml. Leaving these
    // enabled would mean every CI run reports the same known-rejected bumps.
    lint {
        disable += setOf(
            "AndroidGradlePluginVersion",
            "GradleDependency",
            "NewerVersionAvailable",
        )
    }

    // Keep .onnx weights uncompressed inside the APK so the sherpa-onnx native
    // layer can mmap them directly from the install location instead of
    // inflating ~28 MB into RAM on every cold start.
    androidResources {
        noCompress += "onnx"
    }
}

// Sync the hand-authored catalog at the repo root into the app assets folder
// so the bundled fallback is always identical to the canonical source. We do
// not generate this file — the root copy is the source of truth — but copying
// keeps a single editing surface.
val syncCatalogAssets = tasks.register<Copy>("syncCatalogAssets") {
    from(rootProject.layout.projectDirectory.dir("catalog"))
    into(layout.projectDirectory.dir("src/main/assets/catalog"))
    include("**/*.json")
}

tasks.named("preBuild") {
    dependsOn(syncCatalogAssets)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.text.google.fonts)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    implementation(libs.okhttp)
    implementation(libs.datastore.preferences)
    implementation(libs.workmanager)
    implementation(libs.kermit)
    implementation(libs.commons.compress)
    implementation(libs.kotlinx.serialization.json)

    // Official upstream k2-fsa/sherpa-onnx Android AAR, vendored in app/libs/.
    // Phase 7 swap: the previous Maven Central mirror (com.bihe0832.android,
    // upstream v1.10.x) was missing Kokoro / Kitten / ZipVoice / Pocket /
    // Supertonic JNI symbols, so we vendored the upstream v1.13.2 release.
    // The AAR contains classes.jar (Kotlin glue for every TTS family) plus
    // jni/<abi>/{libonnxruntime,libsherpa-onnx-c-api,libsherpa-onnx-cxx-api,libsherpa-onnx-jni}.so
    // for arm64-v8a, armeabi-v7a, x86, and x86_64.
    implementation(files("libs/sherpa-onnx-1.13.2.aar"))

    testImplementation(libs.junit4)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.konsist)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
