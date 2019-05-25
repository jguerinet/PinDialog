plugins {
    id("com.github.ben-manes.versions") version "0.21.0"
}

apply("https://raw.githubusercontent.com/jguerinet/Gradle-Artifact-Scripts/master/spotless.gradle")

buildscript {

    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:3.4.1")
        classpath("com.diffplug.spotless:spotless-plugin-gradle:3.22.0")
        classpath(kotlin("gradle-plugin", Version.kotlin))
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
}
