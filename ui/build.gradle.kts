@file:Suppress("UnstableApiUsage")

val pkg: String = providers.gradleProperty("wireguardPackageName").get()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.legacy.kapt)
}

android {
    // PATCHED (Expressive redesign): Material 3 Expressive needs the newest
    // stable platform, so this compiles and targets it rather than trailing
    // it. compileSdkMinor is an AGP 9 addition; 37.2 is the highest stable
    // platform published (37.2-beta* exist only on the canary channel).
    compileSdk = 37
    compileSdkMinor = 2
    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
    }
    namespace = pkg
    defaultConfig {
        applicationId = pkg
        minSdk = 24
        // Explicit rather than inherited from compileSdk, so that bumping
        // the compile platform is not silently also a behaviour change.
        targetSdk = 37
        versionCode = providers.gradleProperty("wireguardVersionCode").get().toInt()
        versionName = providers.gradleProperty("wireguardVersionName").get()
        buildConfigField("int", "MIN_SDK_VERSION", minSdk.toString())
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    // PATCHED (Expressive redesign): release signing config. This build keeps
    // the official package name (com.wireguard.android), so installing it
    // requires uninstalling the official app first — a same-package,
    // different-signing-cert APK cannot coexist with it or update it in place.
    //
    // No keystore is included in this repo (see .gitignore's *.jks/*.keystore
    // exclusion) — generate your own, e.g.:
    //   keytool -genkeypair -v -keystore release.keystore -alias wireguard \
    //     -keyalg RSA -keysize 4096 -validity 10950
    // and provide the path/passwords via environment variables (never commit
    // them) or a local, gitignored gradle.properties override.
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("WG_KEYSTORE_PATH") ?: "../../keystore/release.keystore")
            storePassword = System.getenv("WG_KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("WG_KEY_ALIAS") ?: "wireguard"
            keyPassword = System.getenv("WG_KEY_PASSWORD") ?: ""
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-android-optimize.txt")
            signingConfig = signingConfigs.getByName("release")
            packaging {
                resources {
                    excludes += "DebugProbesKt.bin"
                    excludes += "kotlin-tooling-metadata.json"
                    excludes += "META-INF/*.version"
                }
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        create("googleplay") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
    lint {
        disable += "LongLogTag"
        warning += "MissingTranslation"
        warning += "ImpliedQuantity"
    }
}

dependencies {
    implementation(project(":tunnel"))
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.viewpager2)
    implementation(libs.google.material)
    implementation(libs.zxing.android.embedded)
    implementation(libs.kotlinx.coroutines.android)
    coreLibraryDesugaring(libs.desugarJdkLibs)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:unchecked")
    options.isDeprecation = true
}
