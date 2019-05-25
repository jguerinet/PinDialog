plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-android-extensions")
}

group = "com.guerinet"
version = "3.0.5"

android {
    compileSdkVersion(Version.targetSdk)

    defaultConfig {
        minSdkVersion(Version.minSdk)
        targetSdkVersion(Version.targetSdk)
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.0.2")
    implementation(kotlin("stdlib-jdk8", Version.kotlin))
}

apply("https://raw.githubusercontent.com/jguerinet/Gradle-Artifact-Scripts/master/android-kotlin-artifacts.gradle")
