import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.share.with"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.share.with"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        FileInputStream(localPropertiesFile).use { localProperties.load(it) }
    }
    val keystorePassword = localProperties.getProperty("keystore.password") ?: ""

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = keystorePassword
            keyAlias = "sharewith"
            keyPassword = keystorePassword
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
        resources {
            excludes += "META-INF/INDEX.LIST"
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += listOf("OldTargetApi", "GradleDependency", "NewerVersionAvailable")
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    // Standard test integration
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.json)

    // Android Activity Compose
    implementation(libs.androidx.activity.compose)

    // JetBrains Compose
    implementation(compose.runtime)
    implementation(compose.ui)
    implementation(compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // Ktor HTTP Server (Runs on Android CIO)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.host.common)

    // QR Code Generator
    implementation(libs.qrcode.kotlin)

    // Android Document Picker and File system abstraction
    implementation(libs.androidx.documentfile)
}
