plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.keepfit.core.model"
    compileSdk = 36

    defaultConfig {
        minSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

