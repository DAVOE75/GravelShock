plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}

android {
    namespace = "com.gravelshock.karoo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gravelshock.karoo"
        minSdk = 28
        targetSdk = 33
        versionCode = 5
        versionName = "0.0.5"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore.jks")
            storePassword = "gravelshock"
            keyAlias = "gravelshock"
            keyPassword = "gravelshock"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Karoo SDK desde AAR local
    implementation(files("libs/karoo-ext.aar"))
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Serialización JSON requerida por Karoo System
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // AndroidX básicos
    implementation("androidx.core:core-ktx:1.13.1")
    
    // Timber logging (Requerido por Karoo Ext SDK internamente)
    implementation("com.jakewharton.timber:timber:5.0.1")

    testImplementation("junit:junit:4.13.2")
}