plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.cliqnotifier"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.cliqnotifier"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // إعدادات التوقيع المباشر من ملف release.jks داخل مجلد app
    signingConfigs {
        create("release") {
            storeFile = file("release.jks")
            storePassword = "كلمة_المرور_التي_كتبتها"
            keyAlias = "cliq"
            keyPassword = "كلمة_المرور_التي_كتبتها"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // توقيع نسخة الـ Debug بنفس المفتاح
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        viewBinding = true
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
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.1")
}
