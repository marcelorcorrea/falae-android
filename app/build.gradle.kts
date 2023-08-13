import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

val kotlinVersion: String by project
val lifecycleVersion: String by project
val roomVersion: String by project

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures.viewBinding = true
    val releasePropertiesPath = "keys/release_keystore.properties"

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file(releasePropertiesPath)
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
            } else {
                throw FileNotFoundException("Release properties not found at this path: $keystorePropertiesFile")
            }
        }
        getByName("debug") {
            storeFile = file("../keys/debug.keystore")
            storePassword = "falaeandroid"
            keyAlias = "falaedebugkey"
            keyPassword = "falaeapp"
        }
    }
    defaultConfig {
        multiDexEnabled = true
        applicationId = "org.falaeapp.falae"
        minSdk = 15
        compileSdk = 34
        targetSdk = 34
        versionCode = 22
        versionName = "1.0.22"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }
    testBuildType = "debug"
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "BASE_URL", "\"https://www.falaeapp.org\"")
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "BASE_URL", "\"https://www.falaeapp.org\"")
        }
    }
    namespace = "org.falaeapp.falae"
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.majorVersion
    }
}

repositories {
    google()
    mavenCentral()
    maven("https://jcenter.bintray.com/")
}

dependencies {
    implementation("com.android.support:multidex:1.0.3")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("com.google.code.gson:gson:2.10")
    implementation("commons-io:commons-io:20030203.000550")
    implementation("com.android.support:design:32.0.0")
    implementation("com.android.support:appcompat-v7:32.0.0")
    implementation("com.android.support:recyclerview-v7:32.0.0")
    implementation("com.android.support:support-v4:32.0.0")
    implementation("com.android.support:gridlayout-v7:32.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.android.volley:volley:1.2.1")
    implementation("jp.wasabeef:picasso-transformations:2.2.0")
    implementation("com.google.android.gms:play-services-base:18.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.1")
    ksp("androidx.room:room-compiler:$roomVersion")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest-library:2.2")
    testImplementation("org.mockito:mockito-core:4.10.0")
    testImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
