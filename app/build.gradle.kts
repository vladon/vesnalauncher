import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.vladon.vesna.launcher"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.vladon.vesna.launcher"
        minSdk = 26
        targetSdk = 37
        versionCode = 71
        versionName = "0.4.66"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    dependenciesInfo {
        // F-Droid rejects the Play Console dependency metadata signing block.
        includeInApk = false
    }

    // Package the approved adaptive icon handoff directly so artwork cannot drift.
    sourceSets.getByName("main").res.directories.add(rootProject.file("design/system/adaptive-icon/res").path)

    signingConfigs {
        create("release") {
            // Signing material comes from CI secrets; local release builds stay
            // unsigned unless the environment provides the keystore.
            val keystorePath = System.getenv("VESNA_RELEASE_KEYSTORE")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = File(keystorePath)
                storePassword = System.getenv("VESNA_RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("VESNA_RELEASE_KEY_ALIAS") ?: "vesna"
                keyPassword = System.getenv("VESNA_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (!System.getenv("VESNA_RELEASE_KEYSTORE").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// Room exports each schema version to app/schemas for migration review.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.core)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
