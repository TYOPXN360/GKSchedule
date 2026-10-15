import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // kotlin-android removed: AGP 9.0+ has built-in Kotlin support
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.ty.gkschedule"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.ty.gkschedule"
        minSdk = 31
        targetSdk = 36
        versionCode = 24
        versionName = "2.3.8"
    }

    signingConfigs {
        create("release") {
            // ponytail: 密钥材料只在环境变量(CI=账户级secrets)或 local.properties(已gitignore)里，
            // 仓库里不留任何明文口令
            val props = Properties().apply {
                val f = rootProject.file("local.properties")
                if (f.exists()) f.inputStream().use { stream -> load(stream) }
            }
            fun secret(key: String): String? =
                (System.getenv(key) ?: props.getProperty(key))?.takeIf { it.isNotBlank() }
            val storePath = secret("RELEASE_KEYSTORE")
                ?: (rootProject.projectDir.resolve("keystore/gkschedule.keystore").takeIf { it.exists() }?.path)
            require(!storePath.isNullOrBlank()) {
                "Release signing keystore not configured: set RELEASE_KEYSTORE (env or local.properties)"
            }
            storeFile = file(storePath)
            storePassword = secret("RELEASE_KEYSTORE_PASSWORD")
                ?: error("RELEASE_KEYSTORE_PASSWORD not set (env or local.properties)")
            keyAlias = secret("RELEASE_KEY_ALIAS")
                ?: error("RELEASE_KEY_ALIAS not set (env or local.properties)")
            keyPassword = secret("RELEASE_KEY_PASSWORD")
                ?: error("RELEASE_KEY_PASSWORD not set (env or local.properties)")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            keepDebugSymbols += listOf(
                "**/libandroidx.graphics.path.so",
                "**/libdatastore_shared_counter.so"
            )
        }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    // Material Components Android (github.com/material-components/material-components-android)
    implementation(libs.material.android)

    // OkHttp for API calls
    implementation(libs.okhttp)

    // ZXing for QR code generation
    implementation(libs.zxing.core)

    // AppCompat for locale switching
    implementation(libs.appcompat)

    // AndroidX
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.documentfile)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // WorkManager for periodic sync
    implementation(libs.workmanager.runtime)

    // Miuix-blur for in-window backdrop blur
    implementation(libs.miuix.blur)
}
