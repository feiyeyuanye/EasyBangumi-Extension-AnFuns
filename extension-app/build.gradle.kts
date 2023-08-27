plugins {
    id ("com.android.application")
    id ("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.heyanle.easybangumi_extension.anfuns"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.heyanle.easybangumi_extension.anfuns"
        minSdk =  21
        targetSdk =  33
        versionCode = 2
        versionName = "1.2"

    }

    buildTypes {
        release {
            postprocessing {
                isRemoveUnusedCode = false
                isRemoveUnusedResources = false
                isObfuscate = false
                isOptimizeCode = false
                proguardFiles("proguard-rules.pro")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    dependenciesInfo{
        includeInApk = false
        includeInBundle = false
    }
}


dependencies {
    compileOnly("io.github.easybangumiorg:extension-api:1.0-SNAPSHOT")
}