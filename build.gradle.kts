// The Kotlin Gradle plugin version must be declared before the plugins block so that
// AGP 9's built-in Kotlin support resolves this version instead of the older KGP
// runtime version embedded in AGP. The Compose compiler plugin version must match it.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
}
