plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.CasparvdBroek.bletomqtt"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.CasparvdBroek.bletomqtt"
        minSdk = 24
        targetSdk = 29
        versionCode = 4
        versionName = "1.02.03"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        baseline = file("lint-baseline.xml")
    }
    
    // Enable app bundle for Play Store
    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = false
        }
        abi {
            enableSplit = true
        }
    }
}

dependencies {
    implementation(libs.blessed.android)
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}