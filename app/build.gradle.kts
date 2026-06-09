import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.share.with"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.share.with"
        minSdk = 23
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
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Standard test integration
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation(libs.junit.jupiter.engine)

    // Android Activity Compose
    implementation(libs.androidx.activity.compose)

    // JetBrains Compose
    implementation(compose.runtime)
    implementation(compose.ui)
    implementation(compose.foundation)
    implementation(libs.compose.material3)
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")

    // Ktor HTTP Server (Runs on Android Netty or CIO)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.cors)

    // QR Code Generator
    implementation(libs.qrcode.kotlin)

    // Android Document Picker and File system abstraction
    implementation(libs.androidx.documentfile)
}
