plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

import java.util.Properties
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Sync

abstract class PrepareLegalAssets : Sync() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasReleaseSigning = keystorePropertiesFile.exists()
val generatedLegalAssetsDirectory = layout.buildDirectory.dir("generated/legal-assets")
val prepareLegalAssets by tasks.registering(PrepareLegalAssets::class) {
    outputDirectory.set(generatedLegalAssetsDirectory)
    into(outputDirectory)
    into("legal") {
        from(rootProject.file("LICENSE")) {
            rename { "KEEPFIT_APACHE_2_0.txt" }
        }
        from(rootProject.file("MEDIA-LICENSE.md"))
        from(rootProject.file("THIRD_PARTY_NOTICES.md"))
    }
}

if (hasReleaseSigning) {
    keystorePropertiesFile.inputStream().use(keystoreProperties::load)
}
android {
    namespace = "com.keepfit.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.keepfit.app"
        minSdk = 31
        targetSdk = 36
        versionCode = 24
        versionName = "0.24.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(keystoreProperties.getProperty("storeFile")) {
                    "keystore.properties is missing storeFile."
                })
                storePassword = requireNotNull(keystoreProperties.getProperty("storePassword")) {
                    "keystore.properties is missing storePassword."
                }
                keyAlias = requireNotNull(keystoreProperties.getProperty("keyAlias")) {
                    "keystore.properties is missing keyAlias."
                }
                keyPassword = requireNotNull(keystoreProperties.getProperty("keyPassword")) {
                    "keystore.properties is missing keyPassword."
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            prepareLegalAssets,
            PrepareLegalAssets::outputDirectory,
        )
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:media"))
    implementation(project(":core:preferences"))
    implementation(project(":feature:assistant"))
    implementation(project(":feature:nutrition"))
    implementation(project(":feature:review"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:steps"))
    implementation(project(":feature:transformation"))
    implementation(project(":feature:workouts"))

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.android)

    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.health.connect.client)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
