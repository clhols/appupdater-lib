plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.compose")
    id("kotlin-parcelize")
    id("kotlinx-serialization")
    id("maven-publish")
}

android {
    compileSdk = 30
    buildToolsVersion = "30.0.3"

    buildFeatures {
        // Disable unused AGP features
        aidl = false
        renderScript = false
        resValues = false
        shaders = false
    }

    defaultConfig {
        minSdk = 21
        targetSdk = 30

        buildTypes {
            getByName("release") {
                isMinifyEnabled = false
                proguardFiles("proguard-rules.pro")
                consumerProguardFiles("proguard-rules.pro")
            }
        }
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.5.0-native-mt"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    implementation("androidx.activity:activity-compose:1.3.0-rc02")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

afterEvaluate {
    publishing {
        publications {
            create("appupdaterLib", MavenPublication::class.java) {
                groupId = "dk.youtec"
                artifactId = "appupdater-lib"
                version = "1.0.0"

                artifact("$buildDir/outputs/aar/appupdater-release.aar")
            }
        }
    }
}