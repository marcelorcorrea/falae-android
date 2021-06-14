import java.io.FileInputStream
import java.util.Properties

val kotlin_version: String by project
val lifecycle_version: String by project
val room_version: String by project

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures.viewBinding = true
    val RELEASE_PROPERTIES = "keys/release_keystore.properties"

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file(RELEASE_PROPERTIES)
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        }
        getByName("debug") {
            storeFile = file("../keys/debug.keystore")
            storePassword = "falaeandroid"
            keyAlias = "falaedebugkey"
            keyPassword = "falaeapp"
        }
    }
    compileSdkVersion(29)
    defaultConfig {
        applicationId = "org.falaeapp.falae"
        minSdkVersion(15)
        targetSdkVersion(29)
        versionCode = 17
        versionName = "1.0.17"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
    testBuildType = "debug"
    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "BASE_URL", "https://10.0.2.2:3000")
        }

        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "BASE_URL", "\"https://www.falaeapp.org\"")
        }
    }
}

repositories {
    google()
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("commons-io:commons-io:20030203.000550")
    implementation("com.android.support:design:29.0.0")
    implementation("com.android.support:appcompat-v7:29.0.0")
    implementation("com.android.support:recyclerview-v7:29.0.0")
    implementation("com.android.support:support-v4:29.0.0")
    implementation("com.android.support:gridlayout-v7:29.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.android.volley:volley:1.2.0")
    implementation("jp.wasabeef:picasso-transformations:2.1.2")
    implementation("com.google.android.gms:play-services-base:17.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycle_version")
    implementation("androidx.room:room-runtime:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest-library:2.1")
    testImplementation("org.mockito:mockito-core:2.27.0")
    testImplementation("androidx.test.espresso:espresso-core:3.4.0-beta02")
}
