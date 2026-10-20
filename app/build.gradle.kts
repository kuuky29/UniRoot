@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val appName = "Uni-Root"
val appVersionName = "5.0.0"

val sdk36CompatDir = layout.buildDirectory.dir("generated/sdk36Compat").get().asFile.apply {
    val pkgDir = resolve("com/uniroot/app/autoroot").apply { mkdirs() }
    pkgDir.resolve("Sdk36Compat.kt").writeText(
        """
        package com.uniroot.app.autoroot

        import android.app.Notification

        internal fun Notification.Builder.setRequestPromotedOngoing(promoted: Boolean): Notification.Builder {
            runCatching {
                Notification.Builder::class.java
                    .getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                    .invoke(this, promoted)
            }
            return this
        }
        """.trimIndent()
    )
}

android {
    namespace = "com.uniroot.app"
    compileSdk = 36
    sourceSets {
        getByName("main") {
            kotlin.srcDir(sdk36CompatDir)
        }
    }
    defaultConfig {
        applicationId = "com.example.universalsystemporter"
        minSdk = 31
        targetSdk = 36
        versionCode = 300
        versionName = appVersionName
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation:1.11.4")
    implementation("androidx.compose.material3:material3:1.5.0-alpha18")
    implementation("androidx.graphics:graphics-shapes:1.0.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:aidl:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
