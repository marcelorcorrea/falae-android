// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.google.devtools.ksp") version "1.8.10-1.0.9" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.google.com")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.0.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.21")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jcenter.bintray.com/")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
